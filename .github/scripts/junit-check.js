// Publica el resultado de los tests unitarios (XML JUnit que genera Gradle en
// app/build/test-results/testDebugUnitTest/) como check runs, igual que
// jest-check.js en trazabilidad-api:
// - "JUnit tests": resumen ("N clases: X passed, Y failed, Z skipped") con el detalle por clase.
// - Un check por test ("<Clase> › <test>"), para ver cada uno con su tilde en la PR.
// Se llama desde actions/github-script; si el token es de la GitHub App del CI,
// los checks aparecen sueltos (con el ícono de la app) y no agrupados bajo el workflow.

const fs = require('fs');
const path = require('path');

const attr = (tag, name) => {
  const m = tag.match(new RegExp(`\\s${name}="([^"]*)"`));
  return m ? m[1] : '';
};

const unescape = (s) =>
  s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#10;/g, '\n')
    .replace(/&#13;/g, '')
    .replace(/&amp;/g, '&');

// Archivo fuente de la clase de test: com.x.FooTest -> app/src/test/java/com/x/FooTest.kt
const archivoDeClase = (clase) =>
  `app/src/test/java/${clase.replace(/\$.*$/, '').replace(/\./g, '/')}.kt`;

function parseJunit(xml) {
  const suites = [];
  const suiteRe = /<testsuite\s[^>]*>([\s\S]*?)<\/testsuite>|<testsuite\s[^>]*\/>/g;
  let s;
  while ((s = suiteRe.exec(xml))) {
    const open = s[0].match(/<testsuite\s[^>]*>/)[0];
    const body = s[1] ?? '';
    const clase = unescape(attr(open, 'name'));
    const cases = [];
    const caseRe = /<testcase\s[^>]*?(?:\/>|>([\s\S]*?)<\/testcase>)/g;
    let c;
    while ((c = caseRe.exec(body))) {
      const inner = c[1] ?? '';
      const failure = inner.match(/<(failure|error)([^>]*)>([\s\S]*?)<\/\1>|<(failure|error)([^>]*)\/>/);
      // El cuerpo del <failure> es el stack trace (empieza con el mensaje); si viene
      // vacío se usa el atributo message.
      const message = failure
        ? unescape((failure[3] ?? '').trim() || attr(failure[2] ?? failure[5] ?? '', 'message'))
        : '';
      cases.push({
        name: unescape(attr(c[0], 'name')),
        failed: Boolean(failure),
        skipped: /<skipped/.test(inner),
        message,
      });
    }
    suites.push({
      clase,
      corto: clase.split('.').pop(),
      file: archivoDeClase(clase),
      time: Number(attr(open, 'time')) || 0,
      cases,
    });
  }
  return suites;
}

// Gradle deja un TEST-<clase>.xml por clase de test.
function leerReportes(dir) {
  if (!fs.existsSync(dir)) return null;
  const archivos = fs.readdirSync(dir).filter((f) => /^TEST-.*\.xml$/.test(f)).sort();
  if (archivos.length === 0) return null;
  return archivos.flatMap((f) => parseJunit(fs.readFileSync(path.join(dir, f), 'utf8')));
}

function resumen(suites) {
  const all = suites.flatMap((s) => s.cases);
  const failed = all.filter((c) => c.failed).length;
  const skipped = all.filter((c) => c.skipped).length;
  return { clases: suites.length, passed: all.length - failed - skipped, failed, skipped };
}

function markdown(suites, r) {
  const filas = suites.map((s) => {
    const f = s.cases.filter((c) => c.failed).length;
    const k = s.cases.filter((c) => c.skipped).length;
    const p = s.cases.length - f - k;
    return `| ${f ? '❌' : '✅'} \`${s.corto}\` | ${p} | ${f} | ${k} | ${s.time.toFixed(1)}s |`;
  });
  return [
    `**${r.clases} clases**: ${r.passed} passed, ${r.failed} failed, ${r.skipped} skipped`,
    '',
    '| Clase | Passed | Failed | Skipped | Tiempo |',
    '|---|---:|---:|---:|---:|',
    ...filas,
  ].join('\n');
}

function detalleFallas(suites) {
  const out = [];
  for (const s of suites) {
    for (const c of s.cases.filter((x) => x.failed)) {
      out.push(`### ❌ ${s.corto} › ${c.name}\n\n\`\`\`\n${c.message.slice(0, 4000)}\n\`\`\``);
    }
  }
  return out.join('\n\n').slice(0, 65000);
}

// Nombre del check de cada test; GitHub muestra un solo check por nombre, así que
// los repetidos se numeran.
function nombresPorTest(suites) {
  const vistos = new Map();
  return suites.flatMap((s) =>
    s.cases.map((c) => {
      const base = `${s.corto} › ${c.name}`.slice(0, 180);
      const n = (vistos.get(base) ?? 0) + 1;
      vistos.set(base, n);
      return { suite: s, test: c, nombre: n > 1 ? `${base} (${n})` : base };
    }),
  );
}

// Crear muchos checks seguidos puede pegar contra el rate limit secundario de GitHub:
// en ese caso espera lo que indique retry-after y reintenta.
async function conReintento(fn, core) {
  for (let intento = 1; ; intento++) {
    try {
      return await fn();
    } catch (err) {
      const limite = (err.status === 403 || err.status === 429) && intento < 5;
      if (!limite) throw err;
      const h = err.response?.headers?.['retry-after'];
      const espera = h != null && !Number.isNaN(Number(h)) ? Number(h) : 60;
      core.info(`Rate limit de GitHub, reintento en ${espera}s`);
      await new Promise((r) => setTimeout(r, espera * 1000));
    }
  }
}

// Línea del test fallido según el stack trace: "(FooTest.kt:123)".
const lineaDelFallo = (suite, message) => {
  const nombre = suite.file.split('/').pop().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const m = message.match(new RegExp(`\\(${nombre}:(\\d+)\\)`));
  return m ? Number(m[1]) : 1;
};

const anotacion = (suite, test) => {
  const line = lineaDelFallo(suite, test.message);
  return {
    path: suite.file,
    start_line: line,
    end_line: line,
    annotation_level: 'failure',
    title: test.name.slice(0, 255),
    message: test.message.slice(0, 2000) || 'Test fallido',
  };
};

module.exports = async ({
  github, context, core,
  dir = 'app/build/test-results/testDebugUnitTest',
  name = 'JUnit tests',
  porTest = true,
}) => {
  const head_sha = context.payload.pull_request?.head.sha ?? context.sha;
  const { owner, repo } = context.repo;

  const suites = leerReportes(dir);
  if (!suites) {
    await github.rest.checks.create({
      owner, repo, head_sha, name,
      status: 'completed',
      conclusion: 'failure',
      output: {
        title: 'No se generó el reporte de tests',
        summary: `No hay reportes TEST-*.xml en ${dir}: falló la compilación o Gradle antes de correr los tests.`,
      },
    });
    return;
  }

  const r = resumen(suites);
  const annotations = suites
    .flatMap((s) => s.cases.filter((c) => c.failed).map((c) => anotacion(s, c)))
    .slice(0, 50);

  const { data: check } = await github.rest.checks.create({
    owner, repo, head_sha, name,
    status: 'completed',
    conclusion: r.failed > 0 ? 'failure' : 'success',
    output: {
      title: `${r.clases} clases: ${r.passed} passed, ${r.failed} failed, ${r.skipped} skipped`,
      summary: markdown(suites, r),
      text: detalleFallas(suites) || undefined,
      annotations,
    },
  });
  // Sin details_url, "Details" lleva a la homepage de la App; que abra el resumen del check.
  await github.rest.checks.update({ owner, repo, check_run_id: check.id, details_url: check.html_url });
  core.info(`Check "${name}": ${r.clases} clases, ${r.passed} passed, ${r.failed} failed, ${r.skipped} skipped`);

  if (!porTest) return;
  for (const { suite, test, nombre } of nombresPorTest(suites)) {
    const conclusion = test.failed ? 'failure' : test.skipped ? 'skipped' : 'success';
    await conReintento(() => github.rest.checks.create({
      owner, repo, head_sha,
      name: nombre,
      status: 'completed',
      conclusion,
      details_url: check.html_url, // Details abre el resumen "JUnit tests"
      output: {
        title: test.failed ? 'Failed' : test.skipped ? 'Skipped' : 'Passed',
        summary: `\`${suite.clase}\` › ${test.name}`,
        text: test.failed ? `\`\`\`\n${test.message.slice(0, 60000)}\n\`\`\`` : undefined,
        annotations: test.failed ? [anotacion(suite, test)] : undefined,
      },
    }), core);
  }
  core.info(`Checks por test: ${r.passed + r.failed + r.skipped}`);
};

module.exports.parseJunit = parseJunit;
module.exports.leerReportes = leerReportes;
module.exports.resumen = resumen;
module.exports.nombresPorTest = nombresPorTest;
module.exports.lineaDelFallo = lineaDelFallo;

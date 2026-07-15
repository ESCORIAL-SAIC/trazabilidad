package com.escorial.trazabilidad.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.escorial.trazabilidad.ui.common.CampoPickeo
import com.escorial.trazabilidad.ui.common.rememberEscaner
import com.escorial.trazabilidad.ui.navigation.Routes
import kotlinx.coroutines.launch

/** Pantalla de entrada / escaneo (TabItemBlanco), estilo EKart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(nav: NavController, vm: ScanViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var etiqueta by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Tecla "Atrás": si el menú está abierto, lo cierra (no sale de la app).
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val escanear = rememberEscaner { codigo ->
        etiqueta = codigo
        vm.buscar(codigo)
    }

    LaunchedEffect(state.navegarA) {
        val destino = when (state.navegarA) {
            "CONTROLADOR" -> Routes.CONTROLADOR
            "REPARADOR" -> Routes.REPARADOR
            "ESTADO" -> Routes.ESTADO
            else -> null
        }
        if (destino != null) {
            etiqueta = ""
            vm.navegacionConsumida()
            nav.navigate(destino)
        }
    }

    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Trazabilidad", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${state.config.tipo} · ${state.config.puestoNombre}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar menú")
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    selected = false,
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        nav.navigate(Routes.CONFIG)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trazabilidad") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    },
                    colors = barColors,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CampoPickeo(
                    value = etiqueta,
                    onValueChange = { etiqueta = it },
                    label = "Etiqueta",
                    autoFocus = true,
                    keyboardType = KeyboardType.Number,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    onEnter = { codigo -> vm.buscar(codigo) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Puesto", style = MaterialTheme.typography.labelMedium)
                        Text(state.config.puestoNombre, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Tipo de producto", style = MaterialTheme.typography.labelMedium)
                        Text(state.config.tipo, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Button(
                    onClick = { vm.buscar(etiqueta) },
                    enabled = !state.cargando,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.cargando) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Buscar")
                }
                OutlinedButton(
                    onClick = escanear,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Escanear con cámara")
                }
            }
        }
    }

    state.error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.limpiarError() },
            confirmButton = { TextButton(onClick = { vm.limpiarError() }) { Text("OK") } },
            title = { Text("Error") }, text = { Text(msg) },
        )
    }
}

package pe.upeu.ecorecicla.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePuntoScreen(
    puntoId: Long,
    viewModel: DetallePuntoViewModel,
    onRegistrarPesajeClick: () -> Unit,
    onEditarPesajeClick: (Long) -> Unit,
    onVolverClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sesion by viewModel.sesionActual.collectAsState()

    var mostrarDialogoAnular by remember { mutableStateOf<Pesaje?>(null) }
    var motivoAnulacion by remember { mutableStateOf("") }

    var mostrarDialogoOmision by remember { mutableStateOf(false) }
    var motivoOmision by remember { mutableStateOf("") }

    val puedeCrear = viewModel.tienePrivilegio("PESAJE_CREAR")
    val puedeEditar = viewModel.tienePrivilegio("PESAJE_EDITAR")
    val puedeAnular = viewModel.tienePrivilegio("PESAJE_ANULAR")
    val puedeCerrarRuta = viewModel.tienePrivilegio("RUTA_CERRAR")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Punto #$puntoId - Pesajes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (puedeCerrarRuta) {
                        FilledTonalButton(
                            onClick = { viewModel.cerrarRuta() },
                            enabled = !uiState.accionEnProgreso,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cerrar Ruta", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (puedeCrear) {
                ExtendedFloatingActionButton(
                    onClick = onRegistrarPesajeClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Registrar Pesaje") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de acción secundaria: Registrar Omisión
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { mostrarDialogoOmision = true },
                    enabled = !uiState.accionEnProgreso
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Registrar Omisión", fontSize = 13.sp)
                }
            }

            // Mensajes de éxito y error
            if (uiState.mensajeExito != null) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.mensajeExito ?: "",
                            color = Color(0xFF1B5E20),
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { viewModel.limpiarMensajes() }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (uiState.mensajeError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.mensajeError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { viewModel.limpiarMensajes() }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Listado de pesajes
            when {
                uiState.cargando && uiState.pesajes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.pesajes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sin pesajes registrados en este punto",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.pesajes, key = { it.id }) { pesaje ->
                            PesajeItemCard(
                                pesaje = pesaje,
                                puedeEditar = puedeEditar,
                                puedeAnular = puedeAnular,
                                onEditar = { onEditarPesajeClick(pesaje.id) },
                                onAnular = { mostrarDialogoAnular = pesaje }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para anular pesaje (solo supervisor)
    mostrarDialogoAnular?.let { pesaje ->
        AlertDialog(
            onDismissRequest = { mostrarDialogoAnular = null },
            title = { Text("Anular Pesaje #${pesaje.id}") },
            text = {
                Column {
                    Text("Indique el motivo de la anulación lógica:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = motivoAnulacion,
                        onValueChange = { motivoAnulacion = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivoAnulacion.isNotBlank()) {
                            viewModel.anularPesaje(pesaje.id, motivoAnulacion)
                            mostrarDialogoAnular = null
                            motivoAnulacion = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Anulación")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAnular = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para registrar motivo de omisión
    if (mostrarDialogoOmision) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOmision = false },
            title = { Text("Registrar Omisión de Punto") },
            text = {
                Column {
                    Text("Ingrese el motivo por el cual no se pudo atender este punto:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = motivoOmision,
                        onValueChange = { motivoOmision = it },
                        label = { Text("Motivo de omisión") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivoOmision.isNotBlank()) {
                            viewModel.registrarOmision(motivoOmision)
                            mostrarDialogoOmision = false
                            motivoOmision = ""
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoOmision = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PesajeItemCard(
    pesaje: Pesaje,
    puedeEditar: Boolean,
    puedeAnular: Boolean,
    onEditar: () -> Unit,
    onAnular: () -> Unit
) {
    val esAnulado = pesaje.estado is EstadoPesaje.Anulado

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esAnulado) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pesaje.material} (${if (pesaje.id < 0) "Provisional #${pesaje.id}" else "ID #${pesaje.id}"})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Chips de estado
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (pesaje.pendiente) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = "PENDIENTE",
                                color = Color(0xFFE65100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (esAnulado) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFCDD2)
                        ) {
                            Text(
                                text = "ANULADO",
                                color = Color(0xFFB71C1C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "REGISTRADO",
                                color = Color(0xFF1B5E20),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bruto: ${pesaje.pesoBruto} kg", fontSize = 13.sp)
                Text(text = "Tara: ${pesaje.tara} kg", fontSize = 13.sp)
                Text(
                    text = "Neto: ${pesaje.pesoNeto} kg",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (esAnulado) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Operario: ${pesaje.operario}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pesaje.fechaHora.replace("T", " "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Motivo visible si fue anulado
            if (esAnulado) {
                val motivo = (pesaje.estado as EstadoPesaje.Anulado).motivo
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Motivo de anulación: \"$motivo\"",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFFB71C1C)
                )
            }

            // Botones de acción si no está anulado
            if (!esAnulado) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (puedeEditar) {
                        TextButton(onClick = onEditar) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Corregir", fontSize = 12.sp)
                        }
                    }

                    if (puedeAnular) {
                        TextButton(
                            onClick = onAnular,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anular", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

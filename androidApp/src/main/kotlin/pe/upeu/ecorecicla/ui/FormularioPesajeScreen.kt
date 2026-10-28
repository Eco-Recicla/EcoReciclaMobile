package pe.upeu.ecorecicla.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.upeu.ecorecicla.presentation.formulario.FormularioPesajeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPesajeScreen(
    puntoId: Long,
    pesajeIdParaEditar: Long?,
    viewModel: FormularioPesajeViewModel,
    onGuardadoExitoso: () -> Unit,
    onVolverClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val esEdicion = pesajeIdParaEditar != null

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            onGuardadoExitoso()
        }
    }

    val materiales = listOf("Plástico PET", "Plástico Film", "Cartón Corrugado", "Papel Blanco", "Metal Chatarra", "Vidrio")
    var expandedMaterial by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (esEdicion) "Corregir Pesaje #$pesajeIdParaEditar" else "Registrar Pesaje",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selector de Material
            ExposedDropdownMenuBox(
                expanded = expandedMaterial,
                onExpandedChange = { expandedMaterial = !expandedMaterial }
            ) {
                OutlinedTextField(
                    value = uiState.material,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Material") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMaterial) },
                    isError = uiState.errorMaterial != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedMaterial,
                    onDismissRequest = { expandedMaterial = false }
                ) {
                    materiales.forEach { mat ->
                        DropdownMenuItem(
                            text = { Text(mat) },
                            onClick = {
                                viewModel.onMaterialChanged(mat)
                                expandedMaterial = false
                            }
                        )
                    }
                }
            }
            if (uiState.errorMaterial != null) {
                Text(
                    text = uiState.errorMaterial ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Campo Peso Bruto
            OutlinedTextField(
                value = uiState.pesoBruto,
                onValueChange = { viewModel.onPesoBrutoChanged(it) },
                label = { Text("Peso Bruto (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = uiState.errorPesoBruto != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.errorPesoBruto != null) {
                Text(
                    text = uiState.errorPesoBruto ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Campo Tara
            OutlinedTextField(
                value = uiState.tara,
                onValueChange = { viewModel.onTaraChanged(it) },
                label = { Text("Tara del Contenedor (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = uiState.errorTara != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.errorTara != null) {
                Text(
                    text = uiState.errorTara ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Cálculo dinámico de Peso Neto (RN-03 visual)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.pesoNetoCalculado != null && uiState.pesoNetoCalculado!! > 0)
                        Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Peso Neto Estimado (Bruto - Tara):",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.pesoNetoCalculado != null) "${uiState.pesoNetoCalculado} kg" else "-- kg",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.pesoNetoCalculado != null && uiState.pesoNetoCalculado!! > 0)
                                Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        Icons.Default.Scale,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Observaciones
            OutlinedTextField(
                value = uiState.observacion,
                onValueChange = { viewModel.onObservacionChanged(it) },
                label = { Text("Observaciones (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Mensaje General de Error
            if (uiState.mensajeGeneral != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.mensajeGeneral ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Guardar
            Button(
                onClick = { viewModel.guardar() },
                enabled = !uiState.guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.guardando) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (esEdicion) "Guardar Corrección" else "Registrar Pesaje",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

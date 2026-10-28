package pe.upeu.ecorecicla.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onVolverClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Módulo Exclusivo para Administrador",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Gestión de Padrón y Usuarios (Sin privilegios de pesaje)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = "Usuarios Registrados del Sistema:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            val usuariosMock = listOf(
                Triple("operario1", "OPERARIO", "Ruta Diaria - Registro de Pesajes"),
                Triple("operario2", "OPERARIO", "Ruta Diaria - Registro de Pesajes"),
                Triple("supervisor1", "SUPERVISOR", "Supervisión - Corrección, Anulación y Cierre"),
                Triple("admin", "ADMINISTRADOR", "Gestión de Padrón y Roles")
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(usuariosMock) { (user, rol, desc) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = user, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Rol: $rol", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

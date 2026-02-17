package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marsphotos.data.local.entities.CalificacionUnidadEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalifUnidadesScreen(
    snUiState: SNUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackClick) {
                Text("← Volver")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Calificaciones por Unidad",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contenido
        when (snUiState) {
            is SNUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando...")
                    }
                }
            }
            is SNUiState.CalifUnidadesSuccess -> {
                if (snUiState.calificaciones.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay calificaciones registradas")
                    }
                } else {
                    // Fecha de última actualización
                    Text(
                        text = "Última actualización: ${formatDate(snUiState.lastUpdated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agrupar por materia
                    val groupedByMateria = snUiState.calificaciones.groupBy { it.materia }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        groupedByMateria.forEach { (materia, calificaciones) ->
                            item {
                                CalifUnidadesCard(materia, calificaciones)
                            }
                        }
                    }
                }
            }
            is SNUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = snUiState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) {
                            Text("Volver")
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Seleccione una opción")
                }
            }
        }
    }
}

@Composable
private fun CalifUnidadesCard(
    materia: String,
    calificaciones: List<CalificacionUnidadEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = materia,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (calificaciones.isNotEmpty()) {
                Text(
                    text = "Clave: ${calificaciones.first().clvOficial}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mostrar calificaciones por unidad
            calificaciones.sortedBy { it.unidad }.forEach { calif ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Unidad ${calif.unidad}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = calif.calificacion.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (calif.fecha.isNotEmpty()) {
                    Text(
                        text = "Fecha: ${calif.fecha}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (calif.observaciones.isNotEmpty()) {
                    Text(
                        text = "Obs: ${calif.observaciones}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

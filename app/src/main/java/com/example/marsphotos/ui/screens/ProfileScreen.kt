/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.ui.theme.MarsPhotosTheme

/**
 * Pantalla que muestra el perfil académico del alumno.
 * Se muestra después de un login exitoso.
 */
@Composable
fun ProfileScreen(
    snUiState: SNUiState,
    profile: ProfileStudent?,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (snUiState) {
            is SNUiState.Loading -> {
                LoadingProfileContent()
            }
            is SNUiState.ProfileSuccess -> {
                ProfileContent(
                    profile = snUiState.profile,
                    onLogoutClick = onLogoutClick
                )
            }
            is SNUiState.LoginSuccess -> {
                // Si solo tenemos el login exitoso, mostramos loading mientras carga el perfil
                LoadingProfileContent()
            }
            is SNUiState.Error -> {
                ErrorProfileContent(onLogoutClick = onLogoutClick)
            }
            else -> {
                // Si llegamos aquí sin perfil, mostrar lo que tengamos
                if (profile != null) {
                    ProfileContent(
                        profile = profile,
                        onLogoutClick = onLogoutClick
                    )
                } else {
                    ErrorProfileContent(onLogoutClick = onLogoutClick)
                }
            }
        }
    }
}

@Composable
private fun LoadingProfileContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cargando perfil académico...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorProfileContent(onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error al cargar el perfil",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No se pudo obtener la información del perfil académico.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogoutClick) {
            Text("Volver al inicio")
        }
    }
}

@Composable
private fun ProfileContent(
    profile: ProfileStudent,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con icono y nombre
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Perfil",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = profile.nombre.ifEmpty { "Alumno" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = profile.matricula,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card de información académica
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Información Académica",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Info,
                    label = "Carrera",
                    value = profile.carrera.ifEmpty { "No disponible" }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Info,
                    label = "Especialidad",
                    value = profile.especialidad.ifEmpty { "No disponible" }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Info,
                    label = "Semestre",
                    value = profile.semestre.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Promedio",
                    value = "%.2f".format(profile.promedio)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card de créditos
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Créditos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Créditos Acumulados",
                    value = profile.creditosAcumulados.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Créditos Actuales",
                    value = profile.creditosActuales.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Carga Mínima",
                    value = profile.cdtsCargaMinima.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Carga Máxima",
                    value = profile.cdtsCargaMaxima.toString()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card de información adicional
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Información Adicional",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileInfoRow(
                    icon = Icons.Default.DateRange,
                    label = "Fecha de Reinscripción",
                    value = profile.fechaReins.ifEmpty { "No disponible" }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Info,
                    label = "Lineamiento",
                    value = profile.lineamiento.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Info,
                    label = "Modelo Educativo",
                    value = profile.modEducativo.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Estatus",
                    value = profile.estatus.ifEmpty { "No disponible" }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de cerrar sesión
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Cerrar sesión"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cerrar Sesión",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MarsPhotosTheme {
        ProfileScreen(
            snUiState = SNUiState.ProfileSuccess(
                profile = ProfileStudent(
                    matricula = "S19120153",
                    nombre = "Juan Pérez García",
                    carrera = "Ingeniería en Sistemas Computacionales",
                    especialidad = "Desarrollo de Software",
                    semestre = 8,
                    creditosAcumulados = 220,
                    creditosActuales = 30,
                    promedio = 88.5,
                    cdtsCargaMinima = 20,
                    cdtsCargaMaxima = 36,
                    lineamiento = 2015,
                    fechaReins = "2024-01-15",
                    estatus = "Activo",
                    modEducativo = 2
                )
            ),
            profile = null,
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenLoadingPreview() {
    MarsPhotosTheme {
        ProfileScreen(
            snUiState = SNUiState.Loading,
            profile = null,
            onLogoutClick = {}
        )
    }
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marsphotos.model.ProfileStudent
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
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
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cargando perfil...")
                Spacer(modifier = Modifier.weight(1f))
            }
            is SNUiState.ProfileSuccess -> {
                ProfileContent(profile = snUiState.profile, onLogoutClick = onLogoutClick)
            }
            is SNUiState.LoginSuccess -> {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Obteniendo perfil...")
                Spacer(modifier = Modifier.weight(1f))
            }
            is SNUiState.Error -> {
                Spacer(modifier = Modifier.weight(1f))
                Text("Error al cargar el perfil", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onLogoutClick) {
                    Text("Volver")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            else -> {
                if (profile != null) {
                    ProfileContent(profile = profile, onLogoutClick = onLogoutClick)
                }
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
private fun ProfileContent(
    profile: ProfileStudent,
    onLogoutClick: () -> Unit
) {
    Text(
        text = profile.nombre.ifEmpty { "Alumno" },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = profile.matricula,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Información académica
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Información Académica", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Carrera", profile.carrera.ifEmpty { "No disponible" })
            InfoRow("Especialidad", profile.especialidad.ifEmpty { "No disponible" })
            InfoRow("Semestre", profile.semestre.toString())
            InfoRow("Estatus", profile.estatus.ifEmpty { "No disponible" })
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Créditos
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Créditos", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Acumulados", profile.creditosAcumulados.toString())
            InfoRow("Actuales", profile.creditosActuales.toString())
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onLogoutClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Cerrar Sesión")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

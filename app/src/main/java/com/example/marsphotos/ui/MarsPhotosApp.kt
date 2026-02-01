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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.marsphotos.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marsphotos.R
import com.example.marsphotos.ui.screens.LoginScreen
import com.example.marsphotos.ui.screens.ProfileScreen
import com.example.marsphotos.ui.screens.SNUiState
import com.example.marsphotos.ui.screens.SNViewModel

@Composable
fun MarsPhotosApp() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // ViewModel para SICENET
    val snViewModel: SNViewModel = viewModel(factory = SNViewModel.Factory)

    // Determinar qué pantalla mostrar basándose en el estado
    val showProfile = when (snViewModel.snUiState) {
        is SNUiState.LoginSuccess,
        is SNUiState.ProfileSuccess -> true
        else -> false
    }

    // Título dinámico según la pantalla
    val appBarTitle = if (showProfile) "Perfil Académico" else stringResource(R.string.app_name)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MarsTopAppBar(
                scrollBehavior = scrollBehavior,
                title = appBarTitle
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showProfile) {
                // Si el login fue exitoso, obtener el perfil y mostrarlo
                LaunchedEffect(snViewModel.snUiState) {
                    if (snViewModel.snUiState is SNUiState.LoginSuccess) {
                        // Obtener el perfil académico después del login
                        snViewModel.getPerfilAcademico()
                    }
                }

                ProfileScreen(
                    snUiState = snViewModel.snUiState,
                    profile = snViewModel.profileStudent,
                    onLogoutClick = {
                        // Resetear el estado para volver al login
                        snViewModel.logout()
                    }
                )
            } else {
                // Mostrar pantalla de login
                LoginScreen(
                    snUiState = snViewModel.snUiState,
                    onLoginClick = { matricula, password ->
                        snViewModel.login(matricula, password)
                    }
                )
            }
        }
    }
}

@Composable
fun MarsTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    title: String = "SICENET"
) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        modifier = modifier
    )
}

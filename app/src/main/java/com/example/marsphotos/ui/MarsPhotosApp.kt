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

@file:OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)

package com.example.marsphotos.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marsphotos.R
import com.example.marsphotos.ui.screens.*
import kotlinx.serialization.InternalSerializationApi

enum class SicenetScreen {
    Login,
    Profile,
    CargaAcademica,
    Kardex,
    CalifUnidades,
    CalifFinal
}

@Composable
fun MarsPhotosApp() {
    val snViewModel: SNViewModel = viewModel(factory = SNViewModel.Factory)
    var currentScreen by remember { mutableStateOf(SicenetScreen.Login) }
    var currentProfile by remember { mutableStateOf<com.example.marsphotos.model.ProfileStudent?>(null) }

    Scaffold(
        topBar = { MarsTopAppBar() }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                SicenetScreen.Login -> {
                    when (val state = snViewModel.snUiState) {
                        is SNUiState.NotLoggedIn, is SNUiState.Error, is SNUiState.Loading -> {
                            LoginScreen(
                                snUiState = snViewModel.snUiState,
                                onLoginClick = { matricula, password ->
                                    snViewModel.login(matricula, password)
                                }
                            )
                        }
                        is SNUiState.LoginSuccess -> {
                            LaunchedEffect(Unit) {
                                snViewModel.getPerfilAcademico()
                            }
                            LoginScreen(
                                snUiState = snViewModel.snUiState,
                                onLoginClick = { _, _ -> }
                            )
                        }
                        is SNUiState.ProfileSuccess -> {
                            currentProfile = state.profile
                            currentScreen = SicenetScreen.Profile
                        }
                        else -> {
                            LoginScreen(
                                snUiState = snViewModel.snUiState,
                                onLoginClick = { matricula, password ->
                                    snViewModel.login(matricula, password)
                                }
                            )
                        }
                    }
                }

                SicenetScreen.Profile -> {
                    ProfileScreen(
                        snUiState = snViewModel.snUiState,
                        profile = currentProfile,
                        onLogoutClick = {
                            snViewModel.logout()
                            currentScreen = SicenetScreen.Login
                            currentProfile = null
                        },
                        onCargaAcademicaClick = {
                            currentProfile?.let {
                                snViewModel.getCargaAcademica()
                                currentScreen = SicenetScreen.CargaAcademica
                            }
                        },
                        onKardexClick = {
                            currentProfile?.let {
                                snViewModel.getKardex(it.lineamiento)
                                currentScreen = SicenetScreen.Kardex
                            }
                        },
                        onCalifUnidadesClick = {
                            snViewModel.getCalifUnidades()
                            currentScreen = SicenetScreen.CalifUnidades
                        },
                        onCalifFinalClick = {
                            currentProfile?.let {
                                snViewModel.getCalifFinal(it.modEducativo)
                                currentScreen = SicenetScreen.CalifFinal
                            }
                        }
                    )
                }

                SicenetScreen.CargaAcademica -> {
                    CargaAcademicaScreen(
                        snUiState = snViewModel.snUiState,
                        onBackClick = {
                            currentScreen = SicenetScreen.Profile
                        }
                    )
                }

                SicenetScreen.Kardex -> {
                    KardexScreen(
                        snUiState = snViewModel.snUiState,
                        onBackClick = {
                            currentScreen = SicenetScreen.Profile
                        }
                    )
                }

                SicenetScreen.CalifUnidades -> {
                    CalifUnidadesScreen(
                        snUiState = snViewModel.snUiState,
                        onBackClick = {
                            currentScreen = SicenetScreen.Profile
                        }
                    )
                }

                SicenetScreen.CalifFinal -> {
                    CalifFinalScreen(
                        snUiState = snViewModel.snUiState,
                        onBackClick = {
                            currentScreen = SicenetScreen.Profile
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MarsTopAppBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        modifier = modifier
    )
}

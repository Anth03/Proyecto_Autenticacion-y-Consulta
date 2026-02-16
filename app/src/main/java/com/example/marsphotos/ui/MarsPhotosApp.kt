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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marsphotos.R
import com.example.marsphotos.ui.screens.LoginScreen
import com.example.marsphotos.ui.screens.ProfileScreen
import com.example.marsphotos.ui.screens.SNUiState
import com.example.marsphotos.ui.screens.SNViewModel
import kotlinx.serialization.InternalSerializationApi

@Composable
fun MarsPhotosApp() {
    val snViewModel: SNViewModel = viewModel(factory = SNViewModel.Factory)

    Scaffold(
        topBar = { MarsTopAppBar() }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val state = snViewModel.snUiState) {
                is SNUiState.NotLoggedIn, is SNUiState.Error -> {
                    LoginScreen(
                        snUiState = snViewModel.snUiState,
                        onLoginClick = { matricula, password ->
                            snViewModel.login(matricula, password)
                        }
                    )
                }
                is SNUiState.Loading -> {
                    LoginScreen(
                        snUiState = snViewModel.snUiState,
                        onLoginClick = { _, _ -> }
                    )
                }
                is SNUiState.LoginSuccess -> {
                    LaunchedEffect(Unit) {
                        snViewModel.getPerfilAcademico()
                    }
                    ProfileScreen(
                        snUiState = snViewModel.snUiState,
                        profile = null,
                        onLogoutClick = { snViewModel.logout() }
                    )
                }
                is SNUiState.ProfileSuccess -> {
                    ProfileScreen(
                        snUiState = snViewModel.snUiState,
                        profile = state.profile,
                        onLogoutClick = { snViewModel.logout() }
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

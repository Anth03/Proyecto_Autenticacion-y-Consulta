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
@file:OptIn(InternalSerializationApi::class)

package com.example.marsphotos.ui.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.marsphotos.MarsPhotosApplication
import com.example.marsphotos.data.SNRepository
import com.example.marsphotos.model.AccesoLoginResult
import com.example.marsphotos.model.ProfileStudent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi

sealed interface SNUiState {
    object Loading : SNUiState
    data class Error(val message: String = "Error desconocido") : SNUiState
    object NotLoggedIn : SNUiState
    data class LoginSuccess(val accesoResult: AccesoLoginResult) : SNUiState
    data class ProfileSuccess(val profile: ProfileStudent) : SNUiState
}

class SNViewModel(private val snRepository: SNRepository) : ViewModel() {

    var snUiState: SNUiState by mutableStateOf(SNUiState.NotLoggedIn)
        private set

    private var isLoggingIn = false
    private var isLoadingProfile = false

    fun login(matricula: String, password: String) {
        if (isLoggingIn) return
        isLoggingIn = true

        // Limpiar cookies del usuario anterior antes del nuevo login
        snRepository.clearCache()
        snUiState = SNUiState.Loading

        Log.d("SNViewModel", "Iniciando login LIMPIO para: $matricula")

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    snRepository.accesoObjeto(matricula, password)
                }

                if (result.acceso) {
                    Log.d("SNViewModel", "Login exitoso: ${result.matricula}")
                    snUiState = SNUiState.LoginSuccess(result)
                } else {
                    snUiState = SNUiState.Error("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                Log.e("SNViewModel", "Error login: ${e.message}", e)
                snUiState = SNUiState.Error(e.message ?: "Error de conexión")
            } finally {
                isLoggingIn = false
            }
        }
    }

    fun getPerfilAcademico() {
        if (isLoadingProfile) return
        isLoadingProfile = true

        viewModelScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    snRepository.getPerfilAcademico()
                }
                snUiState = SNUiState.ProfileSuccess(profile)
            } catch (e: Exception) {
                Log.e("SNViewModel", "Error perfil: ${e.message}", e)
                snUiState = SNUiState.Error(e.message ?: "Error al obtener perfil")
            } finally {
                isLoadingProfile = false
            }
        }
    }

    fun logout() {
        // Limpiar cookies de sesión
        snRepository.clearCache()
        snUiState = SNUiState.NotLoggedIn
        Log.d("SNViewModel", "Logout realizado - sesión limpiada")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MarsPhotosApplication)
                val snRepository = application.container.snRepository
                SNViewModel(snRepository = snRepository)
            }
        }
    }
}

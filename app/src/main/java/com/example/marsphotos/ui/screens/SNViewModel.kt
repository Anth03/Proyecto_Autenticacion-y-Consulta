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
import retrofit2.HttpException
import java.io.IOException

/**
 * Estados de UI para SICENET
 */
sealed interface SNUiState {
    object Loading : SNUiState
    object Error : SNUiState

    // Estado cuando el usuario no ha iniciado sesión
    object NotLoggedIn : SNUiState

    // Estado cuando el login fue exitoso
    data class LoginSuccess(val accesoResult: AccesoLoginResult) : SNUiState

    // Estado cuando se obtuvo el perfil académico
    data class ProfileSuccess(val profile: ProfileStudent) : SNUiState
}

class SNViewModel(private val snRepository: SNRepository) : ViewModel() {

    /** Estado de la UI de SICENET */
    var snUiState: SNUiState by mutableStateOf(SNUiState.NotLoggedIn)
        private set

    /** Datos del usuario logueado */
    var accesoLoginResult: AccesoLoginResult? by mutableStateOf(null)
        private set

    /** Perfil académico del alumno */
    var profileStudent: ProfileStudent? by mutableStateOf(null)
        private set

    init {
        // No hacemos login automático - esperamos que el usuario ingrese credenciales
    }

    /**
     * Realiza el login en SICENET.
     * Las cookies de sesión se guardan automáticamente para peticiones futuras.
     *
     * @param matricula Matrícula del alumno (ej: "S19120153")
     * @param password Contraseña (puede contener caracteres especiales)
     */
    fun login(matricula: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snUiState = SNUiState.Loading
            snUiState = try {
                Log.d("SNViewModel", "Intentando login para: $matricula")

                // Llamar al repositorio para hacer login
                val result = snRepository.accesoObjeto(matricula, password)
                accesoLoginResult = result

                Log.d("SNViewModel", "Login exitoso: ${result.nombre}")
                SNUiState.LoginSuccess(result)

            } catch (e: IOException) {
                Log.e("SNViewModel", "Error de red en login: ${e.message}")
                SNUiState.Error
            } catch (e: HttpException) {
                Log.e("SNViewModel", "Error HTTP en login: ${e.message}")
                SNUiState.Error
            } catch (e: Exception) {
                Log.e("SNViewModel", "Error en login: ${e.message}")
                SNUiState.Error
            }
        }
    }

    /**
     * Obtiene el perfil académico del alumno.
     * IMPORTANTE: Requiere haber hecho login previamente.
     */
    fun getPerfilAcademico() {
        viewModelScope.launch(Dispatchers.IO) {
            snUiState = SNUiState.Loading
            snUiState = try {
                Log.d("SNViewModel", "Obteniendo perfil académico...")

                val profile = snRepository.getPerfilAcademico()
                profileStudent = profile

                Log.d("SNViewModel", "Perfil obtenido: ${profile.nombre}")
                SNUiState.ProfileSuccess(profile)

            } catch (e: IOException) {
                Log.e("SNViewModel", "Error de red obteniendo perfil: ${e.message}")
                SNUiState.Error
            } catch (e: HttpException) {
                Log.e("SNViewModel", "Error HTTP obteniendo perfil: ${e.message}")
                SNUiState.Error
            } catch (e: Exception) {
                Log.e("SNViewModel", "Error obteniendo perfil: ${e.message}")
                SNUiState.Error
            }
        }
    }

    /**
     * Flujo completo: Login + obtener perfil académico
     */
    fun loginAndGetProfile(matricula: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snUiState = SNUiState.Loading
            try {
                // Paso 1: Login
                Log.d("SNViewModel", "Paso 1: Login para $matricula")
                val loginResult = snRepository.accesoObjeto(matricula, password)
                accesoLoginResult = loginResult
                Log.d("SNViewModel", "Login exitoso: ${loginResult.nombre}")

                // Paso 2: Obtener perfil (la cookie ya está guardada)
                Log.d("SNViewModel", "Paso 2: Obteniendo perfil académico...")
                val profile = snRepository.getPerfilAcademico()
                profileStudent = profile
                Log.d("SNViewModel", "Perfil obtenido: ${profile.nombre}")

                snUiState = SNUiState.ProfileSuccess(profile)

            } catch (e: Exception) {
                Log.e("SNViewModel", "Error en loginAndGetProfile: ${e.message}")
                snUiState = SNUiState.Error
            }
        }
    }

    /**
     * Cierra la sesión y limpia todos los datos del usuario.
     * Regresa al estado NotLoggedIn para mostrar la pantalla de login.
     */
    fun logout() {
        Log.d("SNViewModel", "Cerrando sesión...")
        accesoLoginResult = null
        profileStudent = null
        snUiState = SNUiState.NotLoggedIn
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

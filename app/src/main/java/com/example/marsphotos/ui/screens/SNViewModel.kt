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
import com.example.marsphotos.data.LocalSNRepository
import com.example.marsphotos.data.SNRepository
import com.example.marsphotos.data.local.entities.*
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
    data class CargaAcademicaSuccess(val carga: List<CargaAcademicaEntity>, val lastUpdated: Long) : SNUiState
    data class KardexSuccess(val kardex: List<KardexEntity>, val lastUpdated: Long) : SNUiState
    data class CalifUnidadesSuccess(val calificaciones: List<CalificacionUnidadEntity>, val lastUpdated: Long) : SNUiState
    data class CalifFinalSuccess(val calificaciones: List<CalificacionFinalEntity>, val lastUpdated: Long) : SNUiState
}

class SNViewModel(
    private val snRepository: SNRepository,
    private val localRepository: LocalSNRepository
) : ViewModel() {

    var snUiState: SNUiState by mutableStateOf(SNUiState.NotLoggedIn)
        private set

    private var isLoggingIn = false
    private var isLoadingProfile = false
    private var currentMatricula: String = ""

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
                    currentMatricula = result.matricula
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

                // Guardar en base de datos local
                withContext(Dispatchers.IO) {
                    localRepository.saveProfile(profile)
                }

                currentMatricula = profile.matricula
                snUiState = SNUiState.ProfileSuccess(profile)
            } catch (e: Exception) {
                Log.e("SNViewModel", "Error perfil: ${e.message}", e)
                snUiState = SNUiState.Error(e.message ?: "Error al obtener perfil")
            } finally {
                isLoadingProfile = false
            }
        }
    }

    fun getCargaAcademica() {
        snUiState = SNUiState.Loading

        viewModelScope.launch {
            try {
                // Intentar obtener de la red
                val cargaJson = withContext(Dispatchers.IO) {
                    snRepository.getCargaAcademica()
                }

                Log.d("SNViewModel", "Carga académica JSON: $cargaJson")

                // Parsear JSON y guardar en BD local
                val cargaList = parseCargaAcademica(cargaJson, currentMatricula)

                if (cargaList.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        localRepository.saveCargaAcademica(cargaList)
                    }
                    Log.d("SNViewModel", "Carga académica guardada: ${cargaList.size} materias")
                }

                val lastUpdated = System.currentTimeMillis()
                snUiState = SNUiState.CargaAcademicaSuccess(cargaList, lastUpdated)

            } catch (e: Exception) {
                Log.e("SNViewModel", "Error carga académica: ${e.message}", e)

                // Intentar cargar de BD local
                try {
                    val carga = withContext(Dispatchers.IO) {
                        localRepository.getCargaAcademicaByMatricula(currentMatricula)
                    }
                    val lastUpdated = carga.firstOrNull()?.lastUpdated ?: System.currentTimeMillis()
                    snUiState = SNUiState.CargaAcademicaSuccess(carga, lastUpdated)
                } catch (e2: Exception) {
                    snUiState = SNUiState.Error("No hay datos disponibles")
                }
            }
        }
    }

    fun getKardex(lineamiento: Int) {
        snUiState = SNUiState.Loading

        viewModelScope.launch {
            try {
                // Intentar obtener de la red
                val kardexJson = withContext(Dispatchers.IO) {
                    snRepository.getKardexConPromedio(lineamiento)
                }

                Log.d("SNViewModel", "Kardex JSON: $kardexJson")

                // Parsear JSON y guardar en BD local
                val kardexList = parseKardex(kardexJson, currentMatricula)

                if (kardexList.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        localRepository.saveKardex(kardexList)
                    }
                    Log.d("SNViewModel", "Kardex guardado: ${kardexList.size} materias")
                }

                val lastUpdated = System.currentTimeMillis()
                snUiState = SNUiState.KardexSuccess(kardexList, lastUpdated)

            } catch (e: Exception) {
                Log.e("SNViewModel", "Error kardex: ${e.message}", e)

                // Intentar cargar de BD local
                try {
                    val kardex = withContext(Dispatchers.IO) {
                        localRepository.getKardexByMatricula(currentMatricula)
                    }
                    val lastUpdated = kardex.firstOrNull()?.lastUpdated ?: System.currentTimeMillis()
                    snUiState = SNUiState.KardexSuccess(kardex, lastUpdated)
                } catch (e2: Exception) {
                    snUiState = SNUiState.Error("No hay datos disponibles")
                }
            }
        }
    }

    fun getCalifUnidades() {
        snUiState = SNUiState.Loading

        viewModelScope.launch {
            try {
                // Intentar obtener de la red
                val califJson = withContext(Dispatchers.IO) {
                    snRepository.getCalifUnidades()
                }

                Log.d("SNViewModel", "Calificaciones unidades JSON: $califJson")

                // Parsear JSON y guardar en BD local
                val califList = parseCalifUnidades(califJson, currentMatricula)

                if (califList.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        localRepository.saveCalifUnidades(califList)
                    }
                    Log.d("SNViewModel", "Calificaciones unidades guardadas: ${califList.size}")
                }

                val lastUpdated = System.currentTimeMillis()
                snUiState = SNUiState.CalifUnidadesSuccess(califList, lastUpdated)

            } catch (e: Exception) {
                Log.e("SNViewModel", "Error calificaciones unidades: ${e.message}", e)

                // Intentar cargar de BD local
                try {
                    val calif = withContext(Dispatchers.IO) {
                        localRepository.getCalifUnidadesByMatricula(currentMatricula)
                    }
                    val lastUpdated = calif.firstOrNull()?.lastUpdated ?: System.currentTimeMillis()
                    snUiState = SNUiState.CalifUnidadesSuccess(calif, lastUpdated)
                } catch (e2: Exception) {
                    snUiState = SNUiState.Error("No hay datos disponibles")
                }
            }
        }
    }

    fun getCalifFinal(modEducativo: Int) {
        snUiState = SNUiState.Loading

        viewModelScope.launch {
            try {
                // Intentar obtener de la red
                val califJson = withContext(Dispatchers.IO) {
                    snRepository.getCalifFinal(modEducativo)
                }

                Log.d("SNViewModel", "Calificaciones finales JSON: $califJson")

                // Parsear JSON y guardar en BD local
                val califList = parseCalifFinal(califJson, currentMatricula)

                if (califList.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        localRepository.saveCalifFinal(califList)
                    }
                    Log.d("SNViewModel", "Calificaciones finales guardadas: ${califList.size}")
                }

                val lastUpdated = System.currentTimeMillis()
                snUiState = SNUiState.CalifFinalSuccess(califList, lastUpdated)

            } catch (e: Exception) {
                Log.e("SNViewModel", "Error calificaciones finales: ${e.message}", e)

                // Intentar cargar de BD local
                try {
                    val calif = withContext(Dispatchers.IO) {
                        localRepository.getCalifFinalByMatricula(currentMatricula)
                    }
                    val lastUpdated = calif.firstOrNull()?.lastUpdated ?: System.currentTimeMillis()
                    snUiState = SNUiState.CalifFinalSuccess(calif, lastUpdated)
                } catch (e2: Exception) {
                    snUiState = SNUiState.Error("No hay datos disponibles")
                }
            }
        }
    }

    fun logout() {
        // Limpiar cookies de sesión
        snRepository.clearCache()
        snUiState = SNUiState.NotLoggedIn
        currentMatricula = ""
        Log.d("SNViewModel", "Logout realizado - sesión limpiada")
    }

    // ============ Funciones de parseo de JSON ============

    private fun parseCargaAcademica(jsonString: String, matricula: String): List<CargaAcademicaEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d("SNViewModel", "Carga académica vacía")
                return emptyList()
            }

            val items = mutableListOf<CargaAcademicaEntity>()

            // El JSON puede ser un objeto con una propiedad o un array directo
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                // Buscar el array dentro del objeto
                jsonObj.optJSONArray("lstCarga")
                    ?: jsonObj.optJSONArray("Carga")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d("SNViewModel", "Carga académica JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    CargaAcademicaEntity(
                        matricula = matricula,
                        clvOficial = obj.optString("clvOficial", obj.optString("ClvOficial", "")),
                        materia = obj.optString("Materia", obj.optString("materia", "")),
                        grupo = obj.optString("Grupo", obj.optString("grupo", "")),
                        creditos = obj.optInt("C", obj.optInt("Creditos", 0)),
                        docente = obj.optString("Docente", obj.optString("docente", "")),
                        observaciones = obj.optString("Observaciones", ""),
                        estadoMateria = obj.optInt("EstadoMateria", 0),
                        semestre = obj.optInt("Semestre", obj.optInt("semestre", 0))
                    )
                )
            }

            Log.d("SNViewModel", "Carga académica parseada: ${items.size} materias")
            items
        } catch (e: Exception) {
            Log.e("SNViewModel", "Error parseando carga académica: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseKardex(jsonString: String, matricula: String): List<KardexEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d("SNViewModel", "Kardex vacío")
                return emptyList()
            }

            val items = mutableListOf<KardexEntity>()

            // El JSON puede ser un objeto con lstKardex o un array directo
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                // Es un objeto, extraer lstKardex
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstKardex") ?: org.json.JSONArray()
            } else {
                // Es un array directo
                org.json.JSONArray(jsonString)
            }

            Log.d("SNViewModel", "Kardex JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // Campos según el formato real de SICENET
                val semestre = obj.optString("S1", "0").toIntOrNull() ?: 0
                val periodo = "${obj.optString("P1", "")} ${obj.optString("A1", "")}"

                items.add(
                    KardexEntity(
                        matricula = matricula,
                        clvOficial = obj.optString("ClvOfiMat", obj.optString("ClvMat", "")),
                        materia = obj.optString("Materia", ""),
                        semestre = semestre,
                        creditos = obj.optInt("Cdts", 0),
                        calificacion = obj.optInt("Calif", 0).toString(),
                        acreditacion = obj.optString("Acred", ""),
                        periodo = periodo.trim(),
                        observaciones = ""
                    )
                )
            }

            Log.d("SNViewModel", "Kardex parseado: ${items.size} materias")
            items
        } catch (e: Exception) {
            Log.e("SNViewModel", "Error parseando kardex: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseCalifUnidades(jsonString: String, matricula: String): List<CalificacionUnidadEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d("SNViewModel", "Calificaciones unidades vacías")
                return emptyList()
            }

            val items = mutableListOf<CalificacionUnidadEntity>()

            // El JSON puede ser un objeto con una propiedad o un array directo
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstCalif")
                    ?: jsonObj.optJSONArray("Calificaciones")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d("SNViewModel", "CalifUnidades JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val clvMateria = obj.optString("Materia", obj.optString("materia", ""))
                val nombreMateria = obj.optString("Observaciones", clvMateria)
                val grupo = obj.optString("Grupo", obj.optString("grupo", ""))

                // Extraer calificaciones de cada unidad (U1, U2, U3, etc. o C1, C2, C3, etc.)
                for (u in 1..10) {
                    var calStr = obj.optString("U$u", "")
                    if (calStr.isEmpty() || calStr == "null") {
                        calStr = obj.optString("C$u", "")
                    }

                    if (calStr.isNotEmpty() && calStr != "null" && calStr != "--") {
                        val cal = calStr.toDoubleOrNull() ?: 0.0
                        if (cal > 0) {
                            items.add(
                                CalificacionUnidadEntity(
                                    matricula = matricula,
                                    clvOficial = clvMateria,
                                    materia = if (nombreMateria.isNotEmpty()) nombreMateria else clvMateria,
                                    unidad = u,
                                    calificacion = cal,
                                    fecha = "",
                                    observaciones = if (grupo.isNotEmpty()) "Grupo: $grupo" else ""
                                )
                            )
                        }
                    }
                }
            }

            Log.d("SNViewModel", "Calificaciones unidades parseadas: ${items.size}")
            items
        } catch (e: Exception) {
            Log.e("SNViewModel", "Error parseando calificaciones unidades: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseCalifFinal(jsonString: String, matricula: String): List<CalificacionFinalEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d("SNViewModel", "Calificaciones finales vacías")
                return emptyList()
            }

            val items = mutableListOf<CalificacionFinalEntity>()

            // El JSON puede ser un objeto con una propiedad o un array directo
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstFinal")
                    ?: jsonObj.optJSONArray("Calificaciones")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d("SNViewModel", "CalifFinal JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                items.add(
                    CalificacionFinalEntity(
                        matricula = matricula,
                        clvOficial = obj.optString("clvMat", obj.optString("ClvMat", "")),
                        materia = obj.optString("materia", obj.optString("Materia", "")),
                        grupo = obj.optString("grupo", obj.optString("Grupo", "")),
                        calificacion = obj.optString("calif", obj.optInt("Calif", 0).toString()),
                        acreditacion = obj.optString("aclesitado", obj.optString("Acred", "")),
                        periodo = obj.optString("tipo", obj.optString("Periodo", "")),
                        creditos = obj.optInt("C", obj.optInt("Cdts", 0)),
                        observaciones = obj.optString("Observaciones", "")
                    )
                )
            }

            Log.d("SNViewModel", "Calificaciones finales parseadas: ${items.size}")
            items
        } catch (e: Exception) {
            Log.e("SNViewModel", "Error parseando calificaciones finales: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MarsPhotosApplication)
                val snRepository = application.container.snRepository
                val localRepository = application.container.localSNRepository
                SNViewModel(snRepository = snRepository, localRepository = localRepository)
            }
        }
    }
}

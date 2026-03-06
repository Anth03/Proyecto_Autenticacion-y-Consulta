/*
 * SNViewModel con WorkManager
 * Implementa sincronización en segundo plano usando Workers
 */
@file:OptIn(InternalSerializationApi::class)

package com.example.marsphotos.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.marsphotos.MarsPhotosApplication
import com.example.marsphotos.data.AppContainer
import com.example.marsphotos.data.LocalSNRepository
import com.example.marsphotos.data.SNRepository
import com.example.marsphotos.data.local.entities.*
import com.example.marsphotos.model.AccesoLoginResult
import com.example.marsphotos.model.ProfileStudent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import java.util.UUID

class SNViewModelWithWorkers(
    private val snRepository: SNRepository,
    private val localRepository: LocalSNRepository,
    private val container: AppContainer,
    private val context: Context
) : ViewModel() {

    private val TAG = "SNViewModelWorkers"
    private val workManager: WorkManager = container.workManager

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    var snUiState: SNUiState by mutableStateOf(SNUiState.NotLoggedIn)
        private set

    // Credenciales guardadas para reautenticación
    private var savedMatricula: String = ""
    private var savedPassword: String = ""
    private var currentLineamiento: Int = 3
    private var currentModEducativo: Int = 2

    /**
     * Verifica si hay conexión a internet
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * LOGIN + PERFIL usando WorkManager (Punto 2a)
     *
     * CON internet: Inicia Workers para login+perfil -> guarda en BD -> muestra en UI
     * SIN internet: Carga perfil de BD local (si existe sesión guardada)
     */
    fun login(matricula: String, password: String) {
        snUiState = SNUiState.Loading
        savedMatricula = matricula
        savedPassword = password

        if (isNetworkAvailable()) {
            Log.d(TAG, "CON internet - Iniciando Workers para login+perfil")

            // Limpiar cookies antes de nuevo login
            snRepository.clearCache()

            // Iniciar cadena de Workers
            val workerId = container.startLoginAndProfileSync(matricula, password)

            // Monitorear el estado del Worker
            monitorLoginProfileWorker(workerId)
        } else {
            Log.d(TAG, "SIN internet - Intentando cargar de BD local")
            loadProfileFromLocalDb(matricula)
        }
    }

    /**
     * Monitorea el Worker de login+perfil y actualiza la UI cuando termine
     */
    private fun monitorLoginProfileWorker(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collectLatest { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> {
                        Log.d(TAG, "Worker LOGIN_PROFILE en ejecución...")
                        snUiState = SNUiState.Loading
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Worker LOGIN_PROFILE completado exitosamente")
                        // Cargar el perfil de la BD local (ya fue guardado por SaveToLocalDbWorker)
                        loadProfileFromLocalDb(savedMatricula)
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("ERROR") ?: "Error desconocido"
                        Log.e(TAG, "Worker LOGIN_PROFILE falló: $error")
                        snUiState = SNUiState.Error(error)
                    }
                    WorkInfo.State.CANCELLED -> {
                        Log.d(TAG, "Worker LOGIN_PROFILE cancelado")
                        snUiState = SNUiState.Error("Operación cancelada")
                    }
                    else -> { /* ENQUEUED, BLOCKED - esperando */ }
                }
            }
        }
    }

    /**
     * Carga el perfil del estudiante desde la BD local
     */
    private fun loadProfileFromLocalDb(matricula: String) {
        viewModelScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    localRepository.getPerfilAcademicoByMatricula(matricula)
                }
                if (profile != null) {
                    currentLineamiento = profile.lineamiento
                    currentModEducativo = profile.modEducativo
                    snUiState = SNUiState.ProfileSuccess(profile)
                    Log.d(TAG, "Perfil cargado de BD local: ${profile.nombre}")
                } else {
                    snUiState = SNUiState.Error("No hay datos guardados. Se requiere conexión a internet.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando perfil de BD local: ${e.message}", e)
                snUiState = SNUiState.Error("Error al cargar datos locales: ${e.message}")
            }
        }
    }

    /**
     * CARGA ACADÉMICA usando WorkManager (Punto 2b)
     */
    fun getCargaAcademica() {
        snUiState = SNUiState.Loading

        if (isNetworkAvailable()) {
            Log.d(TAG, "CON internet - Iniciando Workers para carga académica")
            val workerId = container.startCargaAcademicaSync(savedMatricula)
            monitorCargaAcademicaWorker(workerId)
        } else {
            Log.d(TAG, "SIN internet - Cargando carga académica de BD local")
            loadCargaAcademicaFromLocalDb()
        }
    }

    private fun monitorCargaAcademicaWorker(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collectLatest { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Worker CARGA_ACADEMICA completado")
                        loadCargaAcademicaFromLocalDb()
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("ERROR") ?: "Error desconocido"
                        Log.e(TAG, "Worker CARGA_ACADEMICA falló: $error")
                        // Intentar cargar de BD local como fallback
                        loadCargaAcademicaFromLocalDb()
                    }
                    else -> { }
                }
            }
        }
    }

    private fun loadCargaAcademicaFromLocalDb() {
        viewModelScope.launch {
            try {
                val carga = withContext(Dispatchers.IO) {
                    localRepository.getCargaAcademicaByMatricula(savedMatricula)
                }
                val lastUpdated = carga.firstOrNull()?.lastUpdated ?: 0L
                snUiState = SNUiState.CargaAcademicaSuccess(carga, lastUpdated)
                Log.d(TAG, "Carga académica de BD local: ${carga.size} materias, última actualización: $lastUpdated")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando carga académica: ${e.message}", e)
                snUiState = SNUiState.Error("No hay datos disponibles")
            }
        }
    }

    /**
     * KARDEX usando WorkManager (Punto 2b)
     */
    fun getKardex(lineamiento: Int) {
        snUiState = SNUiState.Loading
        currentLineamiento = lineamiento

        if (isNetworkAvailable()) {
            Log.d(TAG, "CON internet - Iniciando Workers para kardex")
            val workerId = container.startKardexSync(savedMatricula, lineamiento)
            monitorKardexWorker(workerId)
        } else {
            Log.d(TAG, "SIN internet - Cargando kardex de BD local")
            loadKardexFromLocalDb()
        }
    }

    private fun monitorKardexWorker(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collectLatest { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Worker KARDEX completado")
                        loadKardexFromLocalDb()
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("ERROR") ?: "Error desconocido"
                        Log.e(TAG, "Worker KARDEX falló: $error")
                        loadKardexFromLocalDb()
                    }
                    else -> { }
                }
            }
        }
    }

    private fun loadKardexFromLocalDb() {
        viewModelScope.launch {
            try {
                val kardex = withContext(Dispatchers.IO) {
                    localRepository.getKardexByMatricula(savedMatricula)
                }
                val lastUpdated = kardex.firstOrNull()?.lastUpdated ?: 0L
                snUiState = SNUiState.KardexSuccess(kardex, lastUpdated)
                Log.d(TAG, "Kardex de BD local: ${kardex.size} materias, última actualización: $lastUpdated")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando kardex: ${e.message}", e)
                snUiState = SNUiState.Error("No hay datos disponibles")
            }
        }
    }

    /**
     * CALIFICACIONES POR UNIDAD usando WorkManager (Punto 2b)
     */
    fun getCalifUnidades() {
        snUiState = SNUiState.Loading

        if (isNetworkAvailable()) {
            Log.d(TAG, "CON internet - Iniciando Workers para calificaciones unidades")
            val workerId = container.startCalifUnidadesSync(savedMatricula)
            monitorCalifUnidadesWorker(workerId)
        } else {
            Log.d(TAG, "SIN internet - Cargando calificaciones unidades de BD local")
            loadCalifUnidadesFromLocalDb()
        }
    }

    private fun monitorCalifUnidadesWorker(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collectLatest { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Worker CALIF_UNIDADES completado")
                        loadCalifUnidadesFromLocalDb()
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("ERROR") ?: "Error desconocido"
                        Log.e(TAG, "Worker CALIF_UNIDADES falló: $error")
                        loadCalifUnidadesFromLocalDb()
                    }
                    else -> { }
                }
            }
        }
    }

    private fun loadCalifUnidadesFromLocalDb() {
        viewModelScope.launch {
            try {
                val calif = withContext(Dispatchers.IO) {
                    localRepository.getCalifUnidadesByMatricula(savedMatricula)
                }
                val lastUpdated = calif.firstOrNull()?.lastUpdated ?: 0L
                snUiState = SNUiState.CalifUnidadesSuccess(calif, lastUpdated)
                Log.d(TAG, "Calificaciones unidades de BD local: ${calif.size}, última actualización: $lastUpdated")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando calificaciones unidades: ${e.message}", e)
                snUiState = SNUiState.Error("No hay datos disponibles")
            }
        }
    }

    /**
     * CALIFICACIONES FINALES usando WorkManager (Punto 2b)
     */
    fun getCalifFinal(modEducativo: Int) {
        snUiState = SNUiState.Loading
        currentModEducativo = modEducativo

        if (isNetworkAvailable()) {
            Log.d(TAG, "CON internet - Iniciando Workers para calificaciones finales")
            val workerId = container.startCalifFinalSync(savedMatricula, modEducativo)
            monitorCalifFinalWorker(workerId)
        } else {
            Log.d(TAG, "SIN internet - Cargando calificaciones finales de BD local")
            loadCalifFinalFromLocalDb()
        }
    }

    private fun monitorCalifFinalWorker(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collectLatest { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Worker CALIF_FINAL completado")
                        loadCalifFinalFromLocalDb()
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("ERROR") ?: "Error desconocido"
                        Log.e(TAG, "Worker CALIF_FINAL falló: $error")
                        loadCalifFinalFromLocalDb()
                    }
                    else -> { }
                }
            }
        }
    }

    private fun loadCalifFinalFromLocalDb() {
        viewModelScope.launch {
            try {
                val calif = withContext(Dispatchers.IO) {
                    localRepository.getCalifFinalByMatricula(savedMatricula)
                }
                val lastUpdated = calif.firstOrNull()?.lastUpdated ?: 0L
                snUiState = SNUiState.CalifFinalSuccess(calif, lastUpdated)
                Log.d(TAG, "Calificaciones finales de BD local: ${calif.size}, última actualización: $lastUpdated")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando calificaciones finales: ${e.message}", e)
                snUiState = SNUiState.Error("No hay datos disponibles")
            }
        }
    }

    /**
     * Obtener perfil directamente (para navegación)
     */
    fun getPerfilAcademico() {
        if (snUiState is SNUiState.ProfileSuccess) return
        loadProfileFromLocalDb(savedMatricula)
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        snRepository.clearCache()
        savedMatricula = ""
        savedPassword = ""
        snUiState = SNUiState.NotLoggedIn
        Log.d(TAG, "Sesión cerrada")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MarsPhotosApplication)
                SNViewModelWithWorkers(
                    snRepository = application.container.snRepository,
                    localRepository = application.container.localSNRepository,
                    container = application.container,
                    context = application.applicationContext
                )
            }
        }
    }
}

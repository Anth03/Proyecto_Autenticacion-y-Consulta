@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.marsphotos.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.marsphotos.MarsPhotosApplication
import com.example.marsphotos.data.local.entities.*
import com.example.marsphotos.model.ProfileStudent
import kotlinx.serialization.json.Json

/**
 * Worker para guardar datos en la base de datos local Room
 * Este es el segundo worker en la cadena
 */
class SaveToLocalDbWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "SaveToLocalDbWorker"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as MarsPhotosApplication
            val localRepository = app.container.localSNRepository

            val matricula = inputData.getString("MATRICULA") ?: ""

            Log.d(TAG, "Guardando datos para: $matricula")

            // Determinar qué datos guardar basado en las claves de entrada
            when {
                inputData.keyValueMap.containsKey("PROFILE_JSON") -> {
                    val profileJson = inputData.getString("PROFILE_JSON") ?: ""
                    val profile = json.decodeFromString<ProfileStudent>(profileJson)
                    localRepository.saveProfile(profile)
                    Log.d(TAG, "Perfil guardado en BD local")
                }

                inputData.keyValueMap.containsKey("CARGA_JSON") -> {
                    val cargaJson = inputData.getString("CARGA_JSON") ?: "[]"
                    val cargaList = parseCargaAcademica(cargaJson, matricula)
                    localRepository.saveCargaAcademica(cargaList)
                    Log.d(TAG, "Carga académica guardada: ${cargaList.size} materias")
                }

                inputData.keyValueMap.containsKey("KARDEX_JSON") -> {
                    val kardexJson = inputData.getString("KARDEX_JSON") ?: "[]"
                    val kardexList = parseKardex(kardexJson, matricula)
                    localRepository.saveKardex(kardexList)
                    Log.d(TAG, "Kardex guardado: ${kardexList.size} materias")
                }

                inputData.keyValueMap.containsKey("CALIF_UNIDADES_JSON") -> {
                    val califJson = inputData.getString("CALIF_UNIDADES_JSON") ?: "[]"
                    val califList = parseCalifUnidades(califJson, matricula)
                    localRepository.saveCalifUnidades(califList)
                    Log.d(TAG, "Calificaciones por unidad guardadas: ${califList.size}")
                }

                inputData.keyValueMap.containsKey("CALIF_FINAL_JSON") -> {
                    val califJson = inputData.getString("CALIF_FINAL_JSON") ?: "[]"
                    val califList = parseCalifFinal(califJson, matricula)
                    localRepository.saveCalifFinal(califList)
                    Log.d(TAG, "Calificaciones finales guardadas: ${califList.size}")
                }
            }

            Result.success(workDataOf("SAVED" to true, "MATRICULA" to matricula))

        } catch (e: Exception) {
            Log.e(TAG, "Error guardando en BD local: ${e.message}", e)
            Result.failure(workDataOf("ERROR" to (e.message ?: "Error guardando datos")))
        }
    }

    // Funciones helper para parsear los JSON de SICENET y convertir a entidades
    private fun parseCargaAcademica(jsonString: String, matricula: String): List<CargaAcademicaEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]") return emptyList()

            // Parseo simple de JSON array
            val items = mutableListOf<CargaAcademicaEntity>()
            // TODO: Implementar parseo real según formato de respuesta de SICENET
            // Por ahora retornamos lista vacía
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando carga académica: ${e.message}")
            emptyList()
        }
    }

    private fun parseKardex(jsonString: String, matricula: String): List<KardexEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]") return emptyList()

            val items = mutableListOf<KardexEntity>()
            // TODO: Implementar parseo real según formato de respuesta de SICENET
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando kardex: ${e.message}")
            emptyList()
        }
    }

    private fun parseCalifUnidades(jsonString: String, matricula: String): List<CalificacionUnidadEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]") return emptyList()

            val items = mutableListOf<CalificacionUnidadEntity>()
            // TODO: Implementar parseo real según formato de respuesta de SICENET
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando calificaciones unidad: ${e.message}")
            emptyList()
        }
    }

    private fun parseCalifFinal(jsonString: String, matricula: String): List<CalificacionFinalEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]") return emptyList()

            val items = mutableListOf<CalificacionFinalEntity>()
            // TODO: Implementar parseo real según formato de respuesta de SICENET
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando calificaciones finales: ${e.message}")
            emptyList()
        }
    }
}

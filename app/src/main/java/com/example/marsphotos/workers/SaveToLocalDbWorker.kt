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

            Log.d(TAG, "Guardando datos para matrícula: '$matricula'")

            // Validar que la matrícula no esté vacía
            if (matricula.isEmpty()) {
                Log.e(TAG, "ERROR: Matrícula vacía - no se puede guardar correctamente")
            }

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
                    localRepository.saveCargaAcademica(cargaList, matricula)
                    Log.d(TAG, "Carga académica guardada: ${cargaList.size} materias para $matricula")
                }

                inputData.keyValueMap.containsKey("KARDEX_JSON") -> {
                    val kardexJson = inputData.getString("KARDEX_JSON") ?: "[]"
                    val kardexList = parseKardex(kardexJson, matricula)
                    localRepository.saveKardex(kardexList, matricula)
                    Log.d(TAG, "Kardex guardado: ${kardexList.size} materias para $matricula")
                }

                inputData.keyValueMap.containsKey("CALIF_UNIDADES_JSON") -> {
                    val califJson = inputData.getString("CALIF_UNIDADES_JSON") ?: "[]"
                    val califList = parseCalifUnidades(califJson, matricula)
                    localRepository.saveCalifUnidades(califList, matricula)
                    Log.d(TAG, "Calificaciones por unidad guardadas: ${califList.size} para $matricula")
                }

                inputData.keyValueMap.containsKey("CALIF_FINAL_JSON") -> {
                    val califJson = inputData.getString("CALIF_FINAL_JSON") ?: "[]"
                    val califList = parseCalifFinal(califJson, matricula)
                    localRepository.saveCalifFinal(califList, matricula)
                    Log.d(TAG, "Calificaciones finales guardadas: ${califList.size} para $matricula")
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
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d(TAG, "Carga académica vacía")
                return emptyList()
            }

            val items = mutableListOf<CargaAcademicaEntity>()
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstCarga")
                    ?: jsonObj.optJSONArray("Carga")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d(TAG, "Carga académica JSON tiene ${jsonArray.length()} elementos")

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

            Log.d(TAG, "Carga académica parseada: ${items.size} materias")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando carga académica: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseKardex(jsonString: String, matricula: String): List<KardexEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d(TAG, "Kardex vacío")
                return emptyList()
            }

            val items = mutableListOf<KardexEntity>()
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstKardex") ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d(TAG, "Kardex JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

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

            Log.d(TAG, "Kardex parseado: ${items.size} materias")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando kardex: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseCalifUnidades(jsonString: String, matricula: String): List<CalificacionUnidadEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d(TAG, "Calificaciones unidades vacías")
                return emptyList()
            }

            val items = mutableListOf<CalificacionUnidadEntity>()
            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstCalif")
                    ?: jsonObj.optJSONArray("Calificaciones")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d(TAG, "CalifUnidades JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val clvMateria = obj.optString("Materia", obj.optString("materia", ""))
                val nombreMateria = obj.optString("Observaciones", clvMateria)
                val grupo = obj.optString("Grupo", obj.optString("grupo", ""))

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

            Log.d(TAG, "Calificaciones unidades parseadas: ${items.size}")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando calificaciones unidades: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseCalifFinal(jsonString: String, matricula: String): List<CalificacionFinalEntity> {
        return try {
            if (jsonString.isEmpty() || jsonString == "[]" || jsonString == "null") {
                Log.d(TAG, "Calificaciones finales vacías")
                return emptyList()
            }

            val items = mutableListOf<CalificacionFinalEntity>()

            val jsonArray = if (jsonString.trim().startsWith("{")) {
                val jsonObj = org.json.JSONObject(jsonString)
                jsonObj.optJSONArray("lstFinal")
                    ?: jsonObj.optJSONArray("Calificaciones")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d(TAG, "CalifFinal JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                items.add(
                    CalificacionFinalEntity(
                        matricula = matricula,
                        clvOficial = obj.optString("clvMat", obj.optString("ClvMat", "")),
                        materia = obj.optString("materia", obj.optString("Materia", "")),
                        grupo = obj.optString("grupo", obj.optString("Grupo", "")),
                        calificacion = obj.optString("calif", obj.optInt("Calif", 0).toString()),
                        acreditacion = obj.optString("acreditado", obj.optString("Acred", "")),
                        periodo = obj.optString("tipo", obj.optString("Periodo", "")),
                        creditos = obj.optInt("C", obj.optInt("Cdts", 0)),
                        observaciones = obj.optString("Observaciones", "")
                    )
                )
            }

            Log.d(TAG, "Calificaciones finales parseadas: ${items.size}")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando calificaciones finales: ${e.message}", e)
            emptyList()
        }
    }
}

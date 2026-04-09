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

    private fun org.json.JSONObject.optStringAny(vararg keys: String): String {
        for (key in keys) {
            if (!has(key)) continue
            val value = opt(key)
            if (value == null || value == org.json.JSONObject.NULL) continue
            val text = value.toString().trim()
            if (text.isNotEmpty() && !text.equals("null", ignoreCase = true)) {
                return text
            }
        }
        return ""
    }

    private fun org.json.JSONObject.optIntAny(vararg keys: String): Int {
        for (key in keys) {
            if (!has(key)) continue
            val value = opt(key)
            if (value == null || value == org.json.JSONObject.NULL) continue
            when (value) {
                is Number -> return value.toInt()
                is String -> value.trim().toIntOrNull()?.let { return it }
            }
        }
        return 0
    }

    private fun org.json.JSONObject.optDoubleAny(vararg keys: String): Double {
        for (key in keys) {
            if (!has(key)) continue
            val value = opt(key)
            if (value == null || value == org.json.JSONObject.NULL) continue
            when (value) {
                is Number -> return value.toDouble()
                is String -> value.trim().toDoubleOrNull()?.let { return it }
            }
        }
        return 0.0
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
                    ?: jsonObj.optJSONArray("carga")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d(TAG, "Carga académica JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // Log de claves disponibles (solo primer elemento)
                if (i == 0) {
                    val keys = obj.keys().asSequence().toList()
                    Log.d(TAG, "CargaAcademica - claves disponibles: $keys")
                }

                items.add(
                    CargaAcademicaEntity(
                        matricula = matricula,
                        clvOficial = obj.optStringAny(
                            "clvOficial", "ClvOficial", "ClvMat", "ClvOfiMat",
                            "clave", "Clave", "clvMat"
                        ),
                        materia = obj.optStringAny("Materia", "materia", "NomMat", "nombreMateria"),
                        grupo = obj.optStringAny("Grupo", "grupo"),
                        creditos = obj.optIntAny("C", "Creditos", "creditos", "Cdts", "cred"),
                        docente = obj.optStringAny(
                            "Docente", "docente", "nomDocente", "NomDocente",
                            "profesor", "Profesor", "nombreDocente"
                        ),
                        observaciones = obj.optStringAny("Observaciones", "observaciones", "obs"),
                        estadoMateria = obj.optIntAny("EstadoMateria", "estadoMateria", "estado"),
                        semestre = obj.optIntAny(
                            "Semestre", "semestre", "SemActual", "semActual", "sem"
                        )
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

                // Log de claves disponibles (solo para el primer elemento)
                if (i == 0) {
                    val keys = obj.keys().asSequence().toList()
                    Log.d(TAG, "Kardex - claves disponibles: $keys")
                }

                val semestre = obj.optIntAny("S1", "S2", "S3", "Semestre")
                val periodo = listOf(
                    obj.optStringAny("P1", "P2", "P3", "Periodo"),
                    obj.optStringAny("A1", "A2", "A3", "Anio")
                ).filter { it.isNotBlank() }.joinToString(" ")

                items.add(
                    KardexEntity(
                        matricula = matricula,
                        clvOficial = obj.optStringAny("ClvOfiMat", "ClvMat", "clvOficial"),
                        materia = obj.optStringAny("Materia", "materia"),
                        semestre = semestre,
                        creditos = obj.optIntAny("Cdts", "Creditos", "creditos"),
                        calificacion = obj.optStringAny("Calif", "calif").ifBlank {
                            obj.optIntAny("Calif", "calif").takeIf { it != 0 }?.toString() ?: ""
                        },
                        acreditacion = obj.optStringAny("Acred", "acreditacion", "acreditado"),
                        periodo = periodo.trim(),
                        observaciones = obj.optStringAny("Observaciones", "observaciones")
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

                // Log de claves disponibles (solo primer elemento)
                if (i == 0) {
                    val keys = obj.keys().asSequence().toList()
                    Log.d(TAG, "CalifUnidades - claves disponibles: $keys")
                }

                val clvMateria = obj.optStringAny("ClvMat", "ClvOfiMat", "clvOficial", "Materia", "materia")
                val nombreMateria = obj.optStringAny("Materia", "materia", "NomMat", "nombreMateria").ifBlank { clvMateria }
                val grupo = obj.optStringAny("Grupo", "grupo")

                for (u in 1..10) {
                    var calStr = obj.optStringAny("U$u")
                    if (calStr.isEmpty() || calStr == "null") {
                        calStr = obj.optStringAny("C$u")
                    }

                    if (calStr.isNotEmpty() && calStr != "null" && calStr != "--") {
                        val cal = calStr.toDoubleOrNull() ?: 0.0
                        if (cal > 0) {
                            items.add(
                                CalificacionUnidadEntity(
                                    matricula = matricula,
                                    clvOficial = clvMateria,
                                    materia = nombreMateria,
                                    unidad = u,
                                    calificacion = cal,
                                    fecha = obj.optStringAny("Fecha$u", "fecha$u", "Fecha"),
                                    observaciones = if (grupo.isNotEmpty()) "Grupo: $grupo" else obj.optStringAny("Observaciones", "observaciones")
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
                    ?: jsonObj.optJSONArray("lstCargaCalif")
                    ?: jsonObj.optJSONArray("lstCalif")
                    ?: jsonObj.optJSONArray("Calificaciones")
                    ?: org.json.JSONArray()
            } else {
                org.json.JSONArray(jsonString)
            }

            Log.d(TAG, "CalifFinal JSON tiene ${jsonArray.length()} elementos")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // Log de claves disponibles (solo para el primer elemento)
                if (i == 0) {
                    val keys = obj.keys().asSequence().toList()
                    Log.d(TAG, "CalifFinal - claves disponibles: $keys")
                }

                // Calificacion: intentar múltiples variantes
                val calStr = obj.optStringAny("calif", "Calif", "calFinal", "califFinal", "calificacion", "Calificacion")
                val calificacion = calStr.ifBlank {
                    val calNum = obj.optDoubleAny("calif", "Calif", "calFinal", "califFinal", "calificacion")
                    if (calNum != 0.0) calNum.toString() else calStr
                }

                // Periodo: combinar P1+A1 si están disponibles
                val p1 = obj.optStringAny("P1", "P2", "periodo", "Periodo", "tipo")
                val a1 = obj.optStringAny("A1", "A2", "anio", "Anio")
                val periodo = when {
                    p1.isNotBlank() && a1.isNotBlank() -> "$p1 $a1"
                    p1.isNotBlank() -> p1
                    else -> ""
                }

                items.add(
                    CalificacionFinalEntity(
                        matricula = matricula,
                        clvOficial = obj.optStringAny(
                            "clvMat", "ClvMat", "ClvOfiMat", "clvOficial",
                            "clave", "Clave", "clv"
                        ),
                        materia = obj.optStringAny("materia", "Materia", "NomMat", "nombreMateria"),
                        grupo = obj.optStringAny("grupo", "Grupo"),
                        calificacion = calificacion,
                        acreditacion = obj.optStringAny(
                            "acreditado", "Acred", "acreditacion",
                            "acred", "tipoAcred", "tipo_acred", "tipoExamen"
                        ),
                        periodo = periodo,
                        creditos = obj.optIntAny("C", "Cdts", "Creditos", "creditos", "cred", "cdts"),
                        observaciones = obj.optStringAny("Observaciones", "observaciones", "obs")
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

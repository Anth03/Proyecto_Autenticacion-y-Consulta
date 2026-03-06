@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.marsphotos.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.marsphotos.MarsPhotosApplication

class SicenetSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MATRICULA = "MATRICULA"
        const val KEY_PASSWORD = "PASSWORD"
        const val KEY_QUERY_TYPE = "QUERY_TYPE"
        const val KEY_LINEAMIENTO = "LINEAMIENTO"
        const val KEY_MOD_EDUCATIVO = "MOD_EDUCATIVO"
    }

    private val TAG = "SicenetSyncWorker"

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as MarsPhotosApplication
            val repository = app.container.snRepository

            // Obtener tipo de consulta
            val queryType = inputData.getString(KEY_QUERY_TYPE) ?: "PROFILE"
            val matricula = inputData.getString(KEY_MATRICULA) ?: ""

            Log.d(TAG, "Iniciando sincronización: $queryType para $matricula")

            val resultData = when (queryType) {
                "LOGIN_AND_PROFILE" -> {
                    // Primero hacer login
                    val password = inputData.getString(KEY_PASSWORD) ?: ""
                    val loginResult = repository.acceso(matricula, password)
                    Log.d(TAG, "Login completado: $loginResult")

                    // Luego obtener perfil
                    val profileResult = repository.getPerfilAcademico()
                    Log.d(TAG, "Perfil obtenido: ${profileResult.nombre}")

                    workDataOf(
                        "PROFILE_JSON" to profileResult.toJson(),
                        "MATRICULA" to profileResult.matricula,
                        "LOGIN_SUCCESS" to true
                    )
                }

                "LOGIN" -> {
                    val password = inputData.getString(KEY_PASSWORD) ?: ""
                    val loginResult = repository.acceso(matricula, password)
                    workDataOf(
                        "LOGIN_RESULT" to loginResult,
                        "MATRICULA" to matricula
                    )
                }

                "PROFILE" -> {
                    val profileResult = repository.getPerfilAcademico()
                    workDataOf(
                        "PROFILE_JSON" to profileResult.toJson(),
                        "MATRICULA" to matricula
                    )
                }

                "CARGA_ACADEMICA" -> {
                    val cargaResult = repository.getCargaAcademica()
                    workDataOf(
                        "CARGA_JSON" to cargaResult,
                        "MATRICULA" to matricula
                    )
                }

                "KARDEX" -> {
                    val lineamiento = inputData.getInt(KEY_LINEAMIENTO, 3)
                    val kardexResult = repository.getKardexConPromedio(lineamiento)
                    workDataOf(
                        "KARDEX_JSON" to kardexResult,
                        "MATRICULA" to matricula
                    )
                }

                "CALIF_UNIDADES" -> {
                    val califResult = repository.getCalifUnidades()
                    workDataOf(
                        "CALIF_UNIDADES_JSON" to califResult,
                        "MATRICULA" to matricula
                    )
                }

                "CALIF_FINAL" -> {
                    val modEducativo = inputData.getInt(KEY_MOD_EDUCATIVO, 2)
                    val califResult = repository.getCalifFinal(modEducativo)
                    workDataOf(
                        "CALIF_FINAL_JSON" to califResult,
                        "MATRICULA" to matricula
                    )
                }

                else -> workDataOf("ERROR" to "Tipo de consulta no válido")
            }

            Log.d(TAG, "Sincronización exitosa: $queryType")
            Result.success(resultData)

        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización: ${e.message}", e)
            Result.failure(workDataOf("ERROR" to (e.message ?: "Error desconocido")))
        }
    }

    // Helper para convertir ProfileStudent a JSON
    private fun com.example.marsphotos.model.ProfileStudent.toJson(): String {
        return """{"matricula":"$matricula","nombre":"$nombre","carrera":"$carrera","especialidad":"$especialidad","semestre":$semestre,"creditosAcumulados":$creditosAcumulados,"creditosActuales":$creditosActuales,"lineamiento":$lineamiento,"fechaReins":"$fechaReins","estatus":"$estatus","modEducativo":$modEducativo,"inscrito":$inscrito,"adeudo":$adeudo,"adeudoDescripcion":"$adeudoDescripcion","urlFoto":"$urlFoto"}"""
    }
}

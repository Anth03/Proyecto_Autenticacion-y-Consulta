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

package com.example.marsphotos.data

import android.content.Context
import android.util.Log
import com.example.marsphotos.model.AccesoLoginResult
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.network.SICENETWService
import com.example.marsphotos.network.bodyAccesoLogin
import com.example.marsphotos.network.bodyPerfilAcademico
import com.example.marsphotos.network.bodyCargaAcademica
import com.example.marsphotos.network.bodyKardexConPromedio
import com.example.marsphotos.network.bodyCalifUnidades
import com.example.marsphotos.network.bodyCalifFinal
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

interface SNRepository {
    suspend fun acceso(m: String, p: String): String

    suspend fun accesoObjeto(m: String, p: String): AccesoLoginResult

    suspend fun getPerfilAcademico(): ProfileStudent

    suspend fun getCargaAcademica(): String

    suspend fun getKardexConPromedio(lineamiento: Int): String

    suspend fun getCalifUnidades(): String

    suspend fun getCalifFinal(modEducativo: Int): String

    fun clearCache()
}


class DBLocalSNRepository(val apiDB: Any) : SNRepository {
    override suspend fun acceso(m: String, p: String): String {
        // Preparar Room para almacenamiento local
        // apiDB.acceso( Usuario(matricula = m) )
        return ""
    }

    override suspend fun accesoObjeto(m: String, p: String): AccesoLoginResult {
        // Implementar con Room
        return AccesoLoginResult()
    }

    override suspend fun getPerfilAcademico(): ProfileStudent {
        // Implementar con Room
        return ProfileStudent()
    }

    override suspend fun getCargaAcademica(): String {
        return ""
    }

    override suspend fun getKardexConPromedio(lineamiento: Int): String {
        return ""
    }

    override suspend fun getCalifUnidades(): String {
        return ""
    }

    override suspend fun getCalifFinal(modEducativo: Int): String {
        return ""
    }

    override fun clearCache() {
    }
}

class NetworSNRepository(
    private val snApiService: SICENETWService,
    private val context: Context
) : SNRepository {

    // Configuración de JSON para parsear las respuestas
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun clearCache() {
        val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d("SICENET", "Cookies de sesión limpiadas")
    }

    override suspend fun acceso(m: String, p: String): String {
        try {
            // Crear el body SOAP con las credenciales
            // Usamos String.format con %s para manejar caracteres especiales correctamente
            val soapBody = String.format(bodyAccesoLogin, m, p)

            Log.d("SICENET", "Realizando login para matrícula: $m")

            val response = snApiService.acceso(
                soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType())
            )

            val responseString = response.string()
            Log.d("SICENET", "Respuesta login: $responseString")

            // Extraer el resultado del XML SOAP
            val result = extractResultFromSoap(responseString, "accesoLoginResult")
            Log.d("SICENET", "Resultado extraído: $result")

            return result
        } catch (e: Exception) {
            Log.e("SICENET", "Error en acceso: ${e.message}", e)
            throw e
        }
    }

    override suspend fun accesoObjeto(m: String, p: String): AccesoLoginResult {
        val resultJson = acceso(m, p)

        return if (resultJson.isNotEmpty() && resultJson != "null") {
            try {
                json.decodeFromString<AccesoLoginResult>(resultJson)
            } catch (e: Exception) {
                Log.e("SICENET", "Error parseando AccesoLoginResult: ${e.message}")
                AccesoLoginResult()
            }
        } else {
            AccesoLoginResult()
        }
    }

    override suspend fun getPerfilAcademico(): ProfileStudent {
        try {
            Log.d("SICENET", "Obteniendo perfil académico...")

            val response = snApiService.getPerfilAcademico(
                bodyPerfilAcademico.toRequestBody("text/xml; charset=utf-8".toMediaType())
            )

            val responseString = response.string()
            Log.d("SICENET", "Respuesta perfil: $responseString")

            // Extraer el resultado del XML SOAP
            val result = extractResultFromSoap(responseString, "getAlumnoAcademicoWithLineamientoResult")
            Log.d("SICENET", "Perfil extraído: $result")

            return if (result.isNotEmpty() && result != "null") {
                try {
                    json.decodeFromString<ProfileStudent>(result)
                } catch (e: Exception) {
                    Log.e("SICENET", "Error parseando ProfileStudent: ${e.message}")
                    ProfileStudent()
                }
            } else {
                ProfileStudent()
            }
        } catch (e: Exception) {
            Log.e("SICENET", "Error obteniendo perfil: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getCargaAcademica(): String {
        try {
            Log.d("SICENET", "Obteniendo carga académica...")

            val response = snApiService.getCargaAcademica(
                bodyCargaAcademica.toRequestBody("text/xml; charset=utf-8".toMediaType())
            )

            val responseString = response.string()
            Log.d("SICENET", "Respuesta carga académica: $responseString")

            val result = extractResultFromSoap(responseString, "getCargaAcademicaByAlumnoResult")
            Log.d("SICENET", "Carga académica extraída: $result")

            return result
        } catch (e: Exception) {
            Log.e("SICENET", "Error obteniendo carga académica: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getKardexConPromedio(lineamiento: Int): String {
        try {
            Log.d("SICENET", "Obteniendo kardex con promedio (lineamiento: $lineamiento)...")

            val soapBody = String.format(bodyKardexConPromedio, lineamiento)

            val response = snApiService.getKardexConPromedio(
                soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType())
            )

            val responseString = response.string()
            Log.d("SICENET", "Respuesta kardex: $responseString")

            val result = extractResultFromSoap(responseString, "getAllKardexConPromedioByAlumnoResult")
            Log.d("SICENET", "Kardex extraído: $result")

            return result
        } catch (e: Exception) {
            Log.e("SICENET", "Error obteniendo kardex: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getCalifUnidades(): String {
        try {
            Log.d("SICENET", "Obteniendo calificaciones por unidad...")

            val response = snApiService.getCalifUnidades(
                bodyCalifUnidades.toRequestBody("text/xml; charset=utf-8".toMediaType())
            )

            val responseString = response.string()
            Log.d("SICENET", "Respuesta calificaciones unidades: $responseString")

            val result = extractResultFromSoap(responseString, "getCalifUnidadesByAlumnoResult")
            Log.d("SICENET", "Calificaciones unidades extraídas: $result")

            return result
        } catch (e: Exception) {
            Log.e("SICENET", "Error obteniendo calificaciones por unidad: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getCalifFinal(modEducativo: Int): String {
        try {
            Log.d("SICENET", "Obteniendo calificaciones finales (modEducativo: $modEducativo)...")

            val soapBody = String.format(bodyCalifFinal, modEducativo)

            val response = snApiService.getCalifFinal(
                soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType())
            )

            val responseString = response.string()
            Log.d("SICENET", "Respuesta calificaciones finales: $responseString")

            val result = extractResultFromSoap(responseString, "getAllCalifFinalByAlumnosResult")
            Log.d("SICENET", "Calificaciones finales extraídas: $result")

            return result
        } catch (e: Exception) {
            Log.e("SICENET", "Error obteniendo calificaciones finales: ${e.message}", e)
            throw e
        }
    }

    private fun extractResultFromSoap(xmlResponse: String, tagName: String): String {
        return try {
            val startTag = "<$tagName>"
            val endTag = "</$tagName>"

            val startIndex = xmlResponse.indexOf(startTag)
            val endIndex = xmlResponse.indexOf(endTag)

            if (startIndex != -1 && endIndex != -1) {
                xmlResponse.substring(startIndex + startTag.length, endIndex)
            } else {
                Log.w("SICENET", "No se encontró la etiqueta: $tagName")
                ""
            }
        } catch (e: Exception) {
            Log.e("SICENET", "Error extrayendo resultado: ${e.message}")
            ""
        }
    }
}

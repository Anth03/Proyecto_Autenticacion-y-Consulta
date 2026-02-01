package com.example.marsphotos.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Body SOAP para el login/acceso
 * Usamos %s (minúscula) para evitar problemas con caracteres especiales
 * El String.format() manejará correctamente caracteres como $, %, \, etc.
 */
val bodyAccesoLogin =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <accesoLogin xmlns="http://tempuri.org/">
      <strMatricula>%s</strMatricula>
      <strContrasenia>%s</strContrasenia>
      <tipoUsuario>ALUMNO</tipoUsuario>
    </accesoLogin>
  </soap:Body>
</soap:Envelope>""".trimIndent()

/**
 * Body SOAP para obtener el perfil académico del alumno
 * No requiere parámetros, solo la cookie de sesión en el header
 */
val bodyPerfilAcademico =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAlumnoAcademicoWithLineamiento xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>""".trimIndent()

// Mantener compatibilidad con código existente
@Deprecated("Usar bodyAccesoLogin en su lugar", ReplaceWith("bodyAccesoLogin"))
val bodyacceso = bodyAccesoLogin

interface SICENETWService {

    /**
     * Método para autenticación/login en SICENET
     * Retorna el resultado del login y guarda la cookie de sesión automáticamente
     * gracias a ReceivedCookiesInterceptor
     */
    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/accesoLogin"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun acceso(@Body soap: RequestBody): ResponseBody

    /**
     * Método para obtener el perfil académico del alumno
     * Requiere que se haya hecho login previamente (la cookie se envía automáticamente
     * gracias a AddCookiesInterceptor)
     */
    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/getAlumnoAcademicoWithLineamiento"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun getPerfilAcademico(@Body soap: RequestBody): ResponseBody

    @GET("/")
    suspend fun con(): ResponseBody
}
package com.example.marsphotos.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

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

val bodyPerfilAcademico =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAlumnoAcademicoWithLineamiento xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>""".trimIndent()

val bodyCargaAcademica =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getCargaAcademicaByAlumno xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>""".trimIndent()

val bodyKardexConPromedio =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAllKardexConPromedioByAlumno xmlns="http://tempuri.org/">
      <aluLineamiento>%d</aluLineamiento>
    </getAllKardexConPromedioByAlumno>
  </soap:Body>
</soap:Envelope>""".trimIndent()

val bodyCalifUnidades =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getCalifUnidadesByAlumno xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>""".trimIndent()

val bodyCalifFinal =
    """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAllCalifFinalByAlumnos xmlns="http://tempuri.org/">
      <bytModEducativo>%d</bytModEducativo>
    </getAllCalifFinalByAlumnos>
  </soap:Body>
</soap:Envelope>""".trimIndent()

// Mantener compatibilidad con código existente
@Deprecated("Usar bodyAccesoLogin en su lugar", ReplaceWith("bodyAccesoLogin"))
val bodyacceso = bodyAccesoLogin

interface SICENETWService {

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/accesoLogin"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun acceso(@Body soap: RequestBody): ResponseBody

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/getAlumnoAcademicoWithLineamiento"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun getPerfilAcademico(@Body soap: RequestBody): ResponseBody

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/getCargaAcademicaByAlumno"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun getCargaAcademica(@Body soap: RequestBody): ResponseBody

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/getAllKardexConPromedioByAlumno"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun getKardexConPromedio(@Body soap: RequestBody): ResponseBody

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/getCalifUnidadesByAlumno"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun getCalifUnidades(@Body soap: RequestBody): ResponseBody

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://tempuri.org/getAllCalifFinalByAlumnos"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun getCalifFinal(@Body soap: RequestBody): ResponseBody

    @GET("/")
    suspend fun con(): ResponseBody
}
package com.example.marsphotos.model

import kotlinx.serialization.Serializable

/**
 * Modelo que representa el perfil académico del alumno de SICENET.
 * Los campos se mapean desde la respuesta JSON que viene dentro del XML SOAP.
 * Nombres exactos de SICENET: cdtosAcumulados, cdtosActuales, semActual, etc.
 */
@Serializable
data class ProfileStudent(
    val matricula: String = "",
    val nombre: String = "",
    val carrera: String = "",
    val especialidad: String = "",
    val semActual: Int = 0,
    val cdtosAcumulados: Int = 0,
    val cdtosActuales: Int = 0,
    val lineamiento: Int = 0,
    val fechaReins: String = "",
    val estatus: String = "",
    val modEducativo: Int = 0,
    val inscrito: Boolean = false,
    val adeudo: Boolean = false,
    val adeudoDescripcion: String = "",
    val urlFoto: String = ""
) {
    // Propiedades para acceder con nombres más legibles
    val semestre: Int get() = semActual
    val creditosAcumulados: Int get() = cdtosAcumulados
    val creditosActuales: Int get() = cdtosActuales
}

/**
 * Modelo para el resultado del login de SICENET.
 * Contiene información básica del usuario autenticado.
 */
@Serializable
data class AccesoLoginResult(
    val acceso: Boolean = false,
    val matricula: String = "",
    val contrasenia: String = "",
    val tipoUsuario: Int = 0,
    val estatus: String = ""
)

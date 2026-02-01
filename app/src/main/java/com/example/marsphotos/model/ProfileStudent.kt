package com.example.marsphotos.model

import kotlinx.serialization.Serializable

/**
 * Modelo que representa el perfil académico del alumno de SICENET.
 * Los campos se mapean desde la respuesta JSON que viene dentro del XML SOAP.
 */
@Serializable
data class ProfileStudent(
    val matricula: String = "",
    val nombre: String = "",
    val carrera: String = "",
    val especialidad: String = "",
    val semestre: Int = 0,
    val creditosAcumulados: Int = 0,
    val creditosActuales: Int = 0,
    val promedio: Double = 0.0,
    val cdtsCargaMinima: Int = 0,
    val cdtsCargaMaxima: Int = 0,
    val lineamiento: Int = 0,
    val fechaReins: String = "",
    val estatus: String = "",
    val modEducativo: Int = 0
)

/**
 * Modelo para el resultado del login de SICENET.
 * Contiene información básica del usuario autenticado.
 */
@Serializable
data class AccesoLoginResult(
    val accession: String = "",
    val matricula: String = "",
    val nombre: String = "",
    val tipoUsuario: Int = 0,
    val contraspipiena: String = "",
    val estatus: String = ""
)

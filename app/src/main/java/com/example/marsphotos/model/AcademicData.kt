package com.example.marsphotos.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
/**
 * Modelo para una materia en la carga académica
 */
@Serializable
data class CargaAcademica(
    val clvOficial: String = "",
    val materia: String = "",
    val grupo: String = "",
    val creditos: Int = 0,
    val docente: String = "",
    val observaciones: String = "",
    val estadoMateria: Int = 0,
    val semestre: Int = 0
)

@InternalSerializationApi
/**
 * Modelo para una materia en el kardex
 */
@Serializable
data class Kardex(
    val clvOficial: String = "",
    val materia: String = "",
    val semestre: Int = 0,
    val creditos: Int = 0,
    val calificacion: String = "",
    val acreditacion: String = "",
    val periodo: String = "",
    val observaciones: String = ""
)

@InternalSerializationApi
/**
 * Modelo para kardex con promedio
 */
@Serializable
data class KardexConPromedio(
    val kardex: List<Kardex> = emptyList(),
    val promedioGral: Double = 0.0,
    val creditosAcumulados: Int = 0
)

@InternalSerializationApi
/**
 * Modelo para calificaciones por unidad
 */
@Serializable
data class CalificacionUnidad(
    val clvOficial: String = "",
    val materia: String = "",
    val unidad: Int = 0,
    val calificacion: Double = 0.0,
    val fecha: String = "",
    val observaciones: String = ""
)

@InternalSerializationApi
/**
 * Modelo para calificación final
 */
@Serializable
data class CalificacionFinal(
    val clvOficial: String = "",
    val materia: String = "",
    val grupo: String = "",
    val calificacion: String = "",
    val acreditacion: String = "",
    val periodo: String = "",
    val creditos: Int = 0,
    val observaciones: String = ""
)

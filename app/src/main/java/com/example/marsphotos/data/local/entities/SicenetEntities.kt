package com.example.marsphotos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar el perfil del estudiante
 */
@Entity(tableName = "profile_student")
data class ProfileStudentEntity(
    @PrimaryKey
    val matricula: String,
    val nombre: String,
    val carrera: String,
    val especialidad: String,
    val semActual: Int,
    val cdtosAcumulados: Int,
    val cdtosActuales: Int,
    val lineamiento: Int,
    val fechaReins: String,
    val estatus: String,
    val modEducativo: Int,
    val inscrito: Boolean,
    val adeudo: Boolean,
    val adeudoDescripcion: String,
    val urlFoto: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar la carga académica
 */
@Entity(tableName = "carga_academica")
data class CargaAcademicaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val matricula: String,
    val clvOficial: String,
    val materia: String,
    val grupo: String,
    val creditos: Int,
    val docente: String,
    val observaciones: String,
    val estadoMateria: Int,
    val semestre: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar el kardex
 */
@Entity(tableName = "kardex")
data class KardexEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val matricula: String,
    val clvOficial: String,
    val materia: String,
    val semestre: Int,
    val creditos: Int,
    val calificacion: String,
    val acreditacion: String,
    val periodo: String,
    val observaciones: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar calificaciones por unidad
 */
@Entity(tableName = "calificaciones_unidad")
data class CalificacionUnidadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val matricula: String,
    val clvOficial: String,
    val materia: String,
    val unidad: Int,
    val calificacion: Double,
    val fecha: String,
    val observaciones: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar calificaciones finales
 */
@Entity(tableName = "calificaciones_final")
data class CalificacionFinalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val matricula: String,
    val clvOficial: String,
    val materia: String,
    val grupo: String,
    val calificacion: String,
    val acreditacion: String,
    val periodo: String,
    val creditos: Int,
    val observaciones: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

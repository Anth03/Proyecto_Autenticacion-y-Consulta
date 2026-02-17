package com.example.marsphotos.data.local

import androidx.room.*
import com.example.marsphotos.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SicenetDao {

    // ============ ProfileStudent ============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileStudentEntity)

    @Query("SELECT * FROM profile_student WHERE matricula = :matricula")
    fun getProfile(matricula: String): Flow<ProfileStudentEntity?>

    @Query("SELECT * FROM profile_student WHERE matricula = :matricula")
    suspend fun getProfileSync(matricula: String): ProfileStudentEntity?

    @Query("DELETE FROM profile_student WHERE matricula = :matricula")
    suspend fun deleteProfile(matricula: String)

    // ============ CargaAcademica ============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargaAcademica(carga: List<CargaAcademicaEntity>)

    @Query("SELECT * FROM carga_academica WHERE matricula = :matricula")
    fun getCargaAcademica(matricula: String): Flow<List<CargaAcademicaEntity>>

    @Query("SELECT * FROM carga_academica WHERE matricula = :matricula")
    suspend fun getCargaAcademicaSync(matricula: String): List<CargaAcademicaEntity>

    @Query("DELETE FROM carga_academica WHERE matricula = :matricula")
    suspend fun deleteCargaAcademica(matricula: String)

    // ============ Kardex ============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKardex(kardex: List<KardexEntity>)

    @Query("SELECT * FROM kardex WHERE matricula = :matricula")
    fun getKardex(matricula: String): Flow<List<KardexEntity>>

    @Query("SELECT * FROM kardex WHERE matricula = :matricula")
    suspend fun getKardexSync(matricula: String): List<KardexEntity>

    @Query("DELETE FROM kardex WHERE matricula = :matricula")
    suspend fun deleteKardex(matricula: String)

    // ============ Calificaciones Unidad ============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalificacionesUnidad(calificaciones: List<CalificacionUnidadEntity>)

    @Query("SELECT * FROM calificaciones_unidad WHERE matricula = :matricula")
    fun getCalificacionesUnidad(matricula: String): Flow<List<CalificacionUnidadEntity>>

    @Query("SELECT * FROM calificaciones_unidad WHERE matricula = :matricula")
    suspend fun getCalificacionesUnidadSync(matricula: String): List<CalificacionUnidadEntity>

    @Query("DELETE FROM calificaciones_unidad WHERE matricula = :matricula")
    suspend fun deleteCalificacionesUnidad(matricula: String)

    // ============ Calificaciones Final ============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalificacionesFinal(calificaciones: List<CalificacionFinalEntity>)

    @Query("SELECT * FROM calificaciones_final WHERE matricula = :matricula")
    fun getCalificacionesFinal(matricula: String): Flow<List<CalificacionFinalEntity>>

    @Query("SELECT * FROM calificaciones_final WHERE matricula = :matricula")
    suspend fun getCalificacionesFinalSync(matricula: String): List<CalificacionFinalEntity>

    @Query("DELETE FROM calificaciones_final WHERE matricula = :matricula")
    suspend fun deleteCalificacionesFinal(matricula: String)

    // ============ Limpieza general ============
    @Query("DELETE FROM profile_student")
    suspend fun clearAllProfiles()

    @Query("DELETE FROM carga_academica")
    suspend fun clearAllCargaAcademica()

    @Query("DELETE FROM kardex")
    suspend fun clearAllKardex()

    @Query("DELETE FROM calificaciones_unidad")
    suspend fun clearAllCalificacionesUnidad()

    @Query("DELETE FROM calificaciones_final")
    suspend fun clearAllCalificacionesFinal()
}

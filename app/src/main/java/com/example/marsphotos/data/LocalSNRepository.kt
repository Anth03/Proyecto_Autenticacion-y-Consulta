@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.marsphotos.data

import android.util.Log
import com.example.marsphotos.data.local.SicenetDao
import com.example.marsphotos.data.local.entities.*
import com.example.marsphotos.model.ProfileStudent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio local que utiliza Room para almacenar datos de SICENET offline
 */
class LocalSNRepository(private val dao: SicenetDao) {

    // ============ Profile ============

    suspend fun getPerfilAcademicoByMatricula(matricula: String): ProfileStudent? {
        val entity = dao.getProfileSync(matricula)
        return entity?.toProfileStudent()
    }

    fun getPerfilFlow(matricula: String): Flow<ProfileStudent?> {
        return dao.getProfile(matricula).map { it?.toProfileStudent() }
    }

    suspend fun saveProfile(profile: ProfileStudent) {
        try {
            val entity = ProfileStudentEntity(
                matricula = profile.matricula,
                nombre = profile.nombre,
                carrera = profile.carrera,
                especialidad = profile.especialidad,
                semActual = profile.semestre,
                cdtosAcumulados = profile.creditosAcumulados,
                cdtosActuales = profile.creditosActuales,
                lineamiento = profile.lineamiento,
                fechaReins = profile.fechaReins,
                estatus = profile.estatus,
                modEducativo = profile.modEducativo,
                inscrito = profile.inscrito,
                adeudo = profile.adeudo,
                adeudoDescripcion = profile.adeudoDescripcion,
                urlFoto = profile.urlFoto
            )
            dao.insertProfile(entity)
            Log.d("LocalSNRepository", "Perfil guardado: ${profile.matricula}")
        } catch (e: Exception) {
            Log.e("LocalSNRepository", "Error guardando perfil: ${e.message}", e)
            throw e
        }
    }

    // ============ Carga Académica ============

    suspend fun getCargaAcademicaByMatricula(matricula: String): List<CargaAcademicaEntity> {
        return dao.getCargaAcademicaSync(matricula)
    }

    fun getCargaAcademicaFlow(matricula: String): Flow<List<CargaAcademicaEntity>> {
        return dao.getCargaAcademica(matricula)
    }

    suspend fun saveCargaAcademica(cargaList: List<CargaAcademicaEntity>) {
        try {
            dao.insertCargaAcademica(cargaList)
            Log.d("LocalSNRepository", "Carga académica guardada: ${cargaList.size} materias")
        } catch (e: Exception) {
            Log.e("LocalSNRepository", "Error guardando carga académica: ${e.message}", e)
            throw e
        }
    }

    // ============ Kardex ============

    suspend fun getKardexByMatricula(matricula: String): List<KardexEntity> {
        return dao.getKardexSync(matricula)
    }

    fun getKardexFlow(matricula: String): Flow<List<KardexEntity>> {
        return dao.getKardex(matricula)
    }

    suspend fun saveKardex(kardexList: List<KardexEntity>) {
        try {
            dao.insertKardex(kardexList)
            Log.d("LocalSNRepository", "Kardex guardado: ${kardexList.size} materias")
        } catch (e: Exception) {
            Log.e("LocalSNRepository", "Error guardando kardex: ${e.message}", e)
            throw e
        }
    }

    // ============ Calificaciones Unidad ============

    suspend fun getCalifUnidadesByMatricula(matricula: String): List<CalificacionUnidadEntity> {
        return dao.getCalificacionesUnidadSync(matricula)
    }

    fun getCalifUnidadesFlow(matricula: String): Flow<List<CalificacionUnidadEntity>> {
        return dao.getCalificacionesUnidad(matricula)
    }

    suspend fun saveCalifUnidades(califList: List<CalificacionUnidadEntity>) {
        try {
            dao.insertCalificacionesUnidad(califList)
            Log.d("LocalSNRepository", "Calificaciones por unidad guardadas: ${califList.size}")
        } catch (e: Exception) {
            Log.e("LocalSNRepository", "Error guardando calificaciones unidad: ${e.message}", e)
            throw e
        }
    }

    // ============ Calificaciones Final ============

    suspend fun getCalifFinalByMatricula(matricula: String): List<CalificacionFinalEntity> {
        return dao.getCalificacionesFinalSync(matricula)
    }

    fun getCalifFinalFlow(matricula: String): Flow<List<CalificacionFinalEntity>> {
        return dao.getCalificacionesFinal(matricula)
    }

    suspend fun saveCalifFinal(califList: List<CalificacionFinalEntity>) {
        try {
            dao.insertCalificacionesFinal(califList)
            Log.d("LocalSNRepository", "Calificaciones finales guardadas: ${califList.size}")
        } catch (e: Exception) {
            Log.e("LocalSNRepository", "Error guardando calificaciones finales: ${e.message}", e)
            throw e
        }
    }


    // ============ Helpers ============
    private fun ProfileStudentEntity.toProfileStudent(): ProfileStudent {
        return ProfileStudent(
            matricula = matricula,
            nombre = nombre,
            carrera = carrera,
            especialidad = especialidad,
            semActual = semActual,
            cdtosAcumulados = cdtosAcumulados,
            cdtosActuales = cdtosActuales,
            lineamiento = lineamiento,
            fechaReins = fechaReins,
            estatus = estatus,
            modEducativo = modEducativo,
            inscrito = inscrito,
            adeudo = adeudo,
            adeudoDescripcion = adeudoDescripcion,
            urlFoto = urlFoto
        )
    }
}

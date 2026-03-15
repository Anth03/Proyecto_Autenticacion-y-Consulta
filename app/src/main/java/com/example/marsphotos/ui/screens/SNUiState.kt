package com.example.marsphotos.ui.screens

import com.example.marsphotos.data.local.entities.*
import com.example.marsphotos.model.AccesoLoginResult
import com.example.marsphotos.model.ProfileStudent
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
sealed class SNUiState {

    object NotLoggedIn : SNUiState()

    object Loading : SNUiState()

    data class LoginSuccess(val result: AccesoLoginResult) : SNUiState()

    data class ProfileSuccess(val profile: ProfileStudent) : SNUiState()

    data class CargaAcademicaSuccess(
        val carga: List<CargaAcademicaEntity>,
        val lastUpdated: Long = 0L
    ) : SNUiState()

    data class KardexSuccess(
        val kardex: List<KardexEntity>,
        val lastUpdated: Long = 0L
    ) : SNUiState()

    data class CalifUnidadesSuccess(
        val calificaciones: List<CalificacionUnidadEntity>,
        val lastUpdated: Long = 0L
    ) : SNUiState()

    data class CalifFinalSuccess(
        val calificaciones: List<CalificacionFinalEntity>,
        val lastUpdated: Long = 0L
    ) : SNUiState()

    data class Error(val message: String) : SNUiState()
}

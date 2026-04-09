package com.example.marsphotos.provider

import android.net.Uri

object SicenetProviderContract {
    const val AUTHORITY = "com.example.marsphotos.provider"

    const val PATH_CARGA = "carga_academica"
    const val PATH_KARDEX = "kardex"

    val CARGA_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CARGA")
    val KARDEX_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_KARDEX")

    object CommonColumns {
        const val COLUMN_ID = "id"
        const val COLUMN_MATRICULA = "matricula"
        const val COLUMN_LAST_UPDATED = "lastUpdated"
    }

    object CargaColumns {
        const val COLUMN_CLV_OFICIAL = "clvOficial"
        const val COLUMN_MATERIA = "materia"
        const val COLUMN_GRUPO = "grupo"
        const val COLUMN_CREDITOS = "creditos"
        const val COLUMN_DOCENTE = "docente"
        const val COLUMN_OBSERVACIONES = "observaciones"
        const val COLUMN_ESTADO_MATERIA = "estadoMateria"
        const val COLUMN_SEMESTRE = "semestre"
    }

    object KardexColumns {
        const val COLUMN_CLV_OFICIAL = "clvOficial"
        const val COLUMN_MATERIA = "materia"
        const val COLUMN_SEMESTRE = "semestre"
        const val COLUMN_CREDITOS = "creditos"
        const val COLUMN_CALIFICACION = "calificacion"
        const val COLUMN_ACREDITACION = "acreditacion"
        const val COLUMN_PERIODO = "periodo"
        const val COLUMN_OBSERVACIONES = "observaciones"
    }
}


package com.example.sicenetclient

import android.net.Uri

object ProviderContract {
    const val AUTHORITY = "com.example.marsphotos.provider"
    private const val PATH_CARGA = "carga_academica"
    private const val PATH_KARDEX = "kardex"

    val CARGA_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CARGA")
    val KARDEX_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_KARDEX")
}


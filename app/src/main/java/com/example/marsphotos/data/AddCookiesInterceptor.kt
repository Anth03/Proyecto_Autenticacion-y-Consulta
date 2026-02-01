package com.example.marsphotos.data

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Este interceptor agrega todas las cookies guardadas en SharedPreferences a cada petición HTTP.
 * Las cookies se guardaron previamente por ReceivedCookiesInterceptor cuando se recibieron del servidor.
 *
 * IMPORTANTE: Esto es necesario para mantener la sesión con SICENET después del login.
 */
class AddCookiesInterceptor(
    private val context: Context
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet(PREF_COOKIES, HashSet()) as HashSet<String>?

        // Agregar cada cookie al header de la petición
        preferences?.forEach { cookie ->
            builder.addHeader("Cookie", cookie)
            Log.d("SICENET_COOKIES", "Agregando cookie: $cookie")
        }

        return chain.proceed(builder.build())
    }

    companion object {
        const val PREF_COOKIES = "PREF_COOKIES"
    }
}
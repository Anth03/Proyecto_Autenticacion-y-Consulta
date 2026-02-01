package com.example.marsphotos.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor que agrega las cookies guardadas a cada petición HTTP.
 * Necesario para mantener la sesión con SICENET después del login.
 */
class AddCookiesInterceptor(
    private val context: Context
) : Interceptor {

    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val cookies = prefs.getStringSet(PREF_COOKIES, emptySet()) ?: emptySet()

        cookies.forEach { cookie ->
            builder.addHeader("Cookie", cookie)
        }

        return chain.proceed(builder.build())
    }

    companion object {
        const val PREF_NAME = "sicenet_prefs"
        const val PREF_COOKIES = "cookies"
    }
}
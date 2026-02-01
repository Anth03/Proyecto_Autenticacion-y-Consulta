package com.example.marsphotos.data

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Este interceptor captura las cookies de sesión que vienen en la respuesta HTTP
 * del servidor SICENET y las guarda en SharedPreferences.
 *
 * IMPORTANTE: Cuando el login es exitoso, SICENET devuelve una cookie de sesión
 * (por ejemplo: .ASPXAUTH) que debe incluirse en todas las peticiones posteriores.
 * Este interceptor captura automáticamente esas cookies.
 */
class ReceivedCookiesInterceptor(
    private val context: Context
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse: Response = chain.proceed(chain.request())

        // Si la respuesta contiene cookies, guardarlas
        if (originalResponse.headers("Set-Cookie").isNotEmpty()) {
            val cookies = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(PREF_COOKIES, HashSet()) as HashSet<String>?

            for (header in originalResponse.headers("Set-Cookie")) {
                cookies?.add(header)
                Log.d("SICENET_COOKIES", "Cookie recibida: $header")
            }

            // Guardar las cookies en SharedPreferences
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(PREF_COOKIES, cookies)
                .apply()

            Log.d("SICENET_COOKIES", "Cookies guardadas: ${cookies?.size ?: 0}")
        }

        return originalResponse
    }

    companion object {
        const val PREF_COOKIES = "PREF_COOKIES"
    }
}
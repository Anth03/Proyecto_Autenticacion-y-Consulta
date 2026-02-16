package com.example.marsphotos.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class ReceivedCookiesInterceptor(
    private val context: Context
) : Interceptor {

    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse: Response = chain.proceed(chain.request())

        if (originalResponse.headers("Set-Cookie").isNotEmpty()) {
            val cookies = HashSet(prefs.getStringSet(PREF_COOKIES, emptySet()) ?: emptySet())

            for (header in originalResponse.headers("Set-Cookie")) {
                cookies.add(header)
            }

            prefs.edit().putStringSet(PREF_COOKIES, cookies).apply()
        }

        return originalResponse
    }

    companion object {
        const val PREF_NAME = "sicenet_prefs"
        const val PREF_COOKIES = "cookies"
    }
}
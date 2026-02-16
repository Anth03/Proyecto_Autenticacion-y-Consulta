/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.marsphotos.data

import android.content.Context
import androidx.work.*
import com.example.marsphotos.data.local.SicenetDatabase
import com.example.marsphotos.network.MarsApiService
import com.example.marsphotos.network.SICENETWService
import com.example.marsphotos.workers.SaveToLocalDbWorker
import com.example.marsphotos.workers.SicenetSyncWorker
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

interface AppContainer {
    val marsPhotosRepository: MarsPhotosRepository
    val snRepository: SNRepository
    val localSNRepository: LocalSNRepository
    val workManager: WorkManager

    /**
     * Inicia la sincronización de datos de SICENET usando WorkManager
     * Retorna el ID único del trabajo para monitoreo
     */
    fun startSicenetSync(matricula: String, password: String): Operation
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val baseUrl = "https://android-kotlin-fun-mars-server.appspot.com/"
    private val baseUrlSN = "https://sicenet.surguanajuato.tecnm.mx"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AddCookiesInterceptor(context))
            .addInterceptor(ReceivedCookiesInterceptor(context))
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(baseUrl)
            .build()
    }

    private val retrofitSN: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrlSN)
            .client(client)
            .build()
    }

    private val retrofitService: MarsApiService by lazy {
        retrofit.create(MarsApiService::class.java)
    }

    private val retrofitServiceSN: SICENETWService by lazy {
        retrofitSN.create(SICENETWService::class.java)
    }

    override val marsPhotosRepository: MarsPhotosRepository by lazy {
        NetworkMarsPhotosRepository(retrofitService)
    }

    override val snRepository: SNRepository by lazy {
        NetworSNRepository(retrofitServiceSN, context)
    }

    override val localSNRepository: LocalSNRepository by lazy {
        val database = SicenetDatabase.getDatabase(context)
        LocalSNRepository(database)
    }

    override val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }

    /**
     * Inicia la cadena de Workers para sincronizar datos de SICENET
     *
     * Worker 1 (SicenetSyncWorker): Consulta datos del servicio web
     * Worker 2 (SaveToLocalDbWorker): Almacena datos en la BD local
     *
     * Los workers se ejecutan secuencialmente y solo si hay conexión a internet
     */
    override fun startSicenetSync(matricula: String, password: String): Operation {
        // Constraints: Solo ejecutar si hay conexión a internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Datos de entrada para el primer worker
        val inputData = workDataOf(
            SicenetSyncWorker.KEY_MATRICULA to matricula,
            SicenetSyncWorker.KEY_PASSWORD to password
        )

        // Worker 1: Consultar datos de SICENET
        val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("sicenet_sync")
            .build()

        // Worker 2: Guardar en base de datos local
        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
            .addTag("sicenet_save")
            .build()

        // Encadenar los workers: syncWorkRequest -> saveWorkRequest
        // El segundo worker recibe los datos de salida del primero
        return workManager
            .beginUniqueWork(
                "sicenet_sync_chain",
                ExistingWorkPolicy.REPLACE, // Reemplazar trabajos existentes
                syncWorkRequest
            )
            .then(saveWorkRequest)
            .enqueue()
    }
}

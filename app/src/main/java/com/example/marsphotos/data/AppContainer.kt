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

    fun startLoginAndProfileSync(matricula: String, password: String): java.util.UUID

    fun startCargaAcademicaSync(matricula: String): java.util.UUID

    fun startKardexSync(matricula: String, lineamiento: Int): java.util.UUID

    fun startCalifUnidadesSync(matricula: String): java.util.UUID

    fun startCalifFinalSync(matricula: String, modEducativo: Int): java.util.UUID
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

    private val sicenetDatabase: SicenetDatabase by lazy {
        SicenetDatabase.getDatabase(context)
    }

    override val localSNRepository: LocalSNRepository by lazy {
        LocalSNRepository(sicenetDatabase.sicenetDao())
    }

    override val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }

    override fun startLoginAndProfileSync(matricula: String, password: String): java.util.UUID {
        // Constraints: Solo ejecutar si hay conexión a internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Datos de entrada para el primer worker
        val inputData = workDataOf(
            SicenetSyncWorker.KEY_MATRICULA to matricula,
            SicenetSyncWorker.KEY_PASSWORD to password,
            SicenetSyncWorker.KEY_QUERY_TYPE to "LOGIN_AND_PROFILE"
        )

        // Worker 1: Consultar login y perfil de SICENET
        val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("sicenet_login_profile")
            .build()

        // Worker 2: Guardar en base de datos local
        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
            .addTag("sicenet_save_profile")
            .build()

        // Encadenar los workers: syncWorkRequest -> saveWorkRequest
        workManager
            .beginUniqueWork(
                "sicenet_login_profile_chain",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            .then(saveWorkRequest)
            .enqueue()

        return syncWorkRequest.id
    }

    override fun startCargaAcademicaSync(matricula: String): java.util.UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            SicenetSyncWorker.KEY_MATRICULA to matricula,
            SicenetSyncWorker.KEY_QUERY_TYPE to "CARGA_ACADEMICA"
        )

        val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("sicenet_carga")
            .build()

        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
            .addTag("sicenet_save_carga")
            .build()

        workManager
            .beginUniqueWork(
                "sicenet_carga_chain",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            .then(saveWorkRequest)
            .enqueue()

        return syncWorkRequest.id
    }

    override fun startKardexSync(matricula: String, lineamiento: Int): java.util.UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            SicenetSyncWorker.KEY_MATRICULA to matricula,
            SicenetSyncWorker.KEY_QUERY_TYPE to "KARDEX",
            SicenetSyncWorker.KEY_LINEAMIENTO to lineamiento
        )

        val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("sicenet_kardex")
            .build()

        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
            .addTag("sicenet_save_kardex")
            .build()

        workManager
            .beginUniqueWork(
                "sicenet_kardex_chain",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            .then(saveWorkRequest)
            .enqueue()

        return syncWorkRequest.id
    }

    override fun startCalifUnidadesSync(matricula: String): java.util.UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            SicenetSyncWorker.KEY_MATRICULA to matricula,
            SicenetSyncWorker.KEY_QUERY_TYPE to "CALIF_UNIDADES"
        )

        val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("sicenet_calif_unidades")
            .build()

        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
            .addTag("sicenet_save_calif_unidades")
            .build()

        workManager
            .beginUniqueWork(
                "sicenet_calif_unidades_chain",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            .then(saveWorkRequest)
            .enqueue()

        return syncWorkRequest.id
    }

    override fun startCalifFinalSync(matricula: String, modEducativo: Int): java.util.UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            SicenetSyncWorker.KEY_MATRICULA to matricula,
            SicenetSyncWorker.KEY_QUERY_TYPE to "CALIF_FINAL",
            SicenetSyncWorker.KEY_MOD_EDUCATIVO to modEducativo
        )

        val syncWorkRequest = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("sicenet_calif_final")
            .build()

        val saveWorkRequest = OneTimeWorkRequestBuilder<SaveToLocalDbWorker>()
            .addTag("sicenet_save_calif_final")
            .build()

        workManager
            .beginUniqueWork(
                "sicenet_calif_final_chain",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            .then(saveWorkRequest)
            .enqueue()

        return syncWorkRequest.id
    }
}

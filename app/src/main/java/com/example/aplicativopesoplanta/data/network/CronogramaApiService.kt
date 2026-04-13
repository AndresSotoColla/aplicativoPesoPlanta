package com.example.aplicativopesoplanta.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class CronogramaItem(
    val bloque: String,
    val fecha_actividad: String,
    val observaciones: String
)

data class SamplingUploadRequest(
    val bloque: String,
    val peso: Double,
    val sistemaRadicular: String,
    val fusarium: String, // "Si" or "No"
    val fecha: String,
    val fecha_envio: String,
    val meristemo: String, // "Si" or "No"
    val hallazgos: String,
    val Observaciones: String,
    val fecha_muestreo: String,
    val usuario: String
)

interface CronogramaApiService {
    @GET("consultor/api/cronograma_semana_peso_planta")
    suspend fun getCronograma(): List<CronogramaItem>

    @POST("consultor/api_muestreos")
    suspend fun uploadSampling(@Body sampling: SamplingUploadRequest): Response<Unit>
}

object NetworkModule {
    private const val BASE_URL = "https://interno.control.agricolaguapa.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val apiService: CronogramaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CronogramaApiService::class.java)
    }
}

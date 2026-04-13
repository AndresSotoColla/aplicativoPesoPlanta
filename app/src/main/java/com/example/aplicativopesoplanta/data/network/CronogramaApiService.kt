package com.example.aplicativopesoplanta.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

data class CronogramaItem(
    val bloque: String,
    val fecha_actividad: String,
    val observaciones: String
)

interface CronogramaApiService {
    @GET("consultor/api/cronograma_semana_peso_planta")
    suspend fun getCronograma(): List<CronogramaItem>
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

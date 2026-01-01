package com.example.battleship.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Usamos 'object' en lugar de 'class' para que solo exista una instancia en toda la app
object BattleshipRetrofit {
    // Esta es una dirección de prueba. Más adelante pondremos la de tu servidor real.
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    val instance: BattleshipApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(BattleshipApiService::class.java)
    }
}
package com.example.battleship.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BattleshipRetrofit {
    // CAMBIA ESTO por la IP de tu ordenador donde corre el servidor Python/Node
    // Si usas el emulador de Android, usa "http://10.0.2.2:5000/"
    // Si usas un móvil físico, usa la IP de tu PC (ej: "http://192.168.1.35:5000/")
    const val BASE_URL = "http://10.0.2.2:5000/"
    // Aquí creamos la instancia "Mágica" que MainActivity está buscando
    val instance: BattleshipApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BattleshipApiService::class.java)
    }
}
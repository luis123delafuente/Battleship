package com.example.battleship.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object responsible for creating and configuring the Retrofit client.
 * This object provides a central point to access the API service throughout the application.
 */
object BattleshipRetrofit {

    /**
     * Base URL for the backend server.
     *
     * Configuration notes:
     * - Android Emulator: Use "http://10.0.2.2:5000/" to access the host machine's localhost.
     * - Physical Device: Use your PC's local IP address (e.g., "http://192.168.1.35:5000/").
     */
    private const val BASE_URL = "http://10.0.2.2:5000/"

    /**
     * The singleton instance of [BattleshipApiService].
     * It uses lazy initialization to ensure the client is built only when accessed for the first time.
     */
    val instance: BattleshipApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BattleshipApiService::class.java)
    }
}
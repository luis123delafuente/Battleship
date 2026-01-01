package com.example.battleship.data.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path

// Definimos el formato de los datos que enviamos (Move)
data class MoveRequest(
    val game_id: String,
    val player: String,
    val row: Int,
    val col: Int
)

// Definimos qué puede hacer la app
interface BattleshipApiService {
    @GET("game_status/{id}")
    suspend fun getStatus(@Path("id") gameId: String): GameResponse

    @POST("attack")
    suspend fun sendAttack(@Body move: MoveRequest): AttackResponse
}

// Modelos de respuesta (lo que nos devuelve el servidor)
data class GameResponse(val status: String)
data class AttackResponse(val hit: Boolean, val next_turn: String)
package com.example.battleship.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 1. La Interfaz con las funciones que llamas en MainActivity
interface BattleshipApiService {
    @GET("/game/state") // Asegúrate de que esta ruta coincida con tu servidor
    suspend fun getGameState(@Query("gameId") gameId: String): GameStateResponse

    @POST("/game/join")
    suspend fun joinGame(@Body action: GameAction): GameStateResponse

    @POST("/game/attack")
    suspend fun sendAttack(@Body move: MoveRequest): AttackResponse


    // 2. Añade esta función a la interfaz BattleshipApiService
    @POST("/game/place")
    suspend fun placeShips(@Body request: PlaceShipsRequest): GameStateResponse

}



// 2. Los Modelos de Datos (Data Classes) necesarios

data class GameStateResponse(
    val gameId: String,
    val player1: String?,
    val player2: String?,
    val turn: String?,       // "Turno" en MainActivity
    val status: String,      // "WAITING", "PLAYING"
    val winner: String?,
    val lastMoveRow: Int?,   // Para saber si te dispararon
    val lastMoveCol: Int?
)

data class GameAction(
    val gameId: String,
    val playerName: String
)

data class MoveRequest(
    val gameId: String,
    val playerName: String,
    val row: Int,
    val col: Int
)

data class AttackResponse(
    val status: String,      // "HIT", "MISS"
    val lastMoveRow: Int?,
    val lastMoveCol: Int?
)

data class PlaceShipsRequest(
    val gameId: String,
    val playerName: String,
    val ships: List<Int> // Enviaremos los índices (del 0 al 24) donde pusiste barcos
)
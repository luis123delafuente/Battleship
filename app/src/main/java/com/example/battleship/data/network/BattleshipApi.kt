package com.example.battleship.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit Interface defining the API endpoints for communicating with the Python Flask server.
 * All functions are suspending to support Coroutines.
 */
interface BattleshipApiService {

    /**
     * Polling endpoint to get the current snapshot of the game.
     * Used to check for opponent moves, turn changes, or game over states.
     *
     * @param gameId The unique identifier of the game room.
     * @return [GameStateResponse] containing the board status and player info.
     */
    @GET("/game/state")
    suspend fun getGameState(@Query("gameId") gameId: String): GameStateResponse

    /**
     * Request to create or join a game lobby.
     *
     * @param action Contains the game ID and the player's name.
     * @return [GameStateResponse] containing the initial lobby state.
     */
    @POST("/game/join")
    suspend fun joinGame(@Body action: GameAction): GameStateResponse

    /**
     * Sends an attack coordinate to the server.
     *
     * @param move Contains the target row and column.
     * @return [AttackResponse] indicating if the shot was a HIT or MISS.
     */
    @POST("/game/attack")
    suspend fun sendAttack(@Body move: MoveRequest): AttackResponse

    /**
     * Sends the initial ship placement configuration to the server.
     * This is called during the SETUP phase.
     *
     * @param request Contains the list of grid indices where ships are placed.
     * @return [GameStateResponse] indicating if the player is ready.
     */
    @POST("/game/place")
    suspend fun placeShips(@Body request: PlaceShipsRequest): GameStateResponse
}

// ==========================================
// DATA TRANSFER OBJECTS (DTOs)
// ==========================================

/**
 * Represents the full state of the game returned by the server.
 */
data class GameStateResponse(
    val gameId: String,
    val player1: String?,
    val player2: String?,
    val turn: String?,       // Name of the player whose turn it is
    val status: String,      // Game Phase: "LOBBY", "SETUP", "WAITING", "PLAYING", "FINISHED"
    val winner: String?,     // Name of the winner, or null if game is ongoing
    val lastMoveRow: Int?,   // Row of the last shot fired (for UI updates)
    val lastMoveCol: Int?    // Column of the last shot fired
)

/**
 * Payload sent when joining a game.
 */
data class GameAction(
    val gameId: String,
    val playerName: String
)

/**
 * Payload sent when attacking a coordinate.
 */
data class MoveRequest(
    val gameId: String,
    val playerName: String,
    val row: Int,
    val col: Int
)

/**
 * Response received after firing a shot.
 */
data class AttackResponse(
    val status: String,      // Result: "HIT" or "MISS"
    val lastMoveRow: Int?,
    val lastMoveCol: Int?
)

/**
 * Payload sent to register ship positions.
 */
data class PlaceShipsRequest(
    val gameId: String,
    val playerName: String,
    val ships: List<Int> // List of 1D indices (0 to 24) representing the 5x5 grid positions
)
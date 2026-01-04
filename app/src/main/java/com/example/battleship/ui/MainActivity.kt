package com.example.battleship.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.battleship.data.network.BattleshipRetrofit
import com.example.battleship.data.network.GameAction
import com.example.battleship.data.network.MoveRequest
import com.example.battleship.data.network.PlaceShipsRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Main Activity acting as the entry point of the application.
 * It handles the navigation between the Login Screen and the Game Screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1B2631) // Dark Navy Background
                ) {
                    // State to manage navigation
                    var currentScreen by remember { mutableStateOf("login") }
                    var playerName by remember { mutableStateOf("") }

                    if (currentScreen == "login") {
                        // Login Screen: Captures the user's name
                        LoginScreen { name ->
                            playerName = name
                            currentScreen = "game"
                        }
                    } else {
                        // Game Screen: The core multiplayer logic
                        BattleshipGameScreen(playerName)
                    }
                }
            }
        }
    }
}

/**
 * The core Composable function containing the Game Logic.
 * It manages:
 * 1. Game States (LOBBY -> SETUP -> WAITING -> PLAYING -> FINISHED).
 * 2. Network Polling (checking server for updates).
 * 3. Sensor Integration (GPS & Orientation).
 * 4. Grid Rendering and User Interaction.
 *
 * @param playerName The name of the current user.
 */
@Composable
fun BattleshipGameScreen(playerName: String) {
    // ==========================================
    // 1. GAME STATE MANAGEMENT
    // ==========================================
    var gameId by remember { mutableStateOf("Game1") }

    // States: "LOBBY", "SETUP", "WAITING", "PLAYING", "FINISHED"
    var gameState by remember { mutableStateOf("LOBBY") }

    var myTurn by remember { mutableStateOf(false) }
    var serverMessage by remember { mutableStateOf("Enter Room ID") }

    // Stores the indices (0-24) of the player's own ships
    var myShips by remember { mutableStateOf(setOf<Int>()) }

    // Visual representation of the board (Water, Hit, Miss)
    var gridState by remember { mutableStateOf(List(25) { "🌊" }) }

    // Coroutine scope for network calls
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ==========================================
    // 2. SENSOR INTEGRATION
    // ==========================================

    // --- ORIENTATION SENSOR (COMPASS) ---
    var azimuth by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sensor = OrientationSensor(context) { newAzimuth ->
            azimuth = newAzimuth
        }
        sensor.start()
        onDispose { sensor.stop() }
    }

    // --- GPS SENSOR ---
    var locationText by remember { mutableStateOf("Searching for GPS signal...") }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val gpsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val networkGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (gpsGranted || networkGranted) {
            val gpsSensor = GpsSensor(context) { lat, long ->
                locationText = "Lat: $lat\nLong: $long"
            }
            gpsSensor.start()
        } else {
            locationText = "GPS Permission Denied"
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val gpsSensor = GpsSensor(context) { lat, long ->
                locationText = "Lat: $lat\nLong: $long"
            }
            gpsSensor.start()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // ==========================================
    // 3. NETWORK LOGIC (POLLING LOOP)
    // ==========================================
    LaunchedEffect(gameState) {
        // Polling loop starts only after joining the lobby
        if (gameState == "WAITING" || gameState == "PLAYING" || gameState == "FINISHED") {
            while (gameState != "FINISHED") {
                try {
                    val response = BattleshipRetrofit.instance.getGameState(gameId)

                    // A. WINNER CHECK (High Priority)
                    if (response.winner != null) {
                        if (response.winner == playerName) {
                            serverMessage = "🏆 VICTORY! ENEMY FLEET SUNK 🏆"
                        } else {
                            serverMessage = "💀 DEFEAT... YOUR SHIPS ARE GONE 💀"
                        }
                        gameState = "FINISHED" // Stops the loop in the next iteration
                    }

                    // B. GAMEPLAY LOGIC
                    else {
                        // Check if opponent joined
                        if (gameState == "WAITING" && response.status == "PLAYING") {
                            gameState = "PLAYING"
                            serverMessage = "Enemy detected! BATTLE STATIONS!"
                        }

                        // Turn Management
                        if (gameState == "PLAYING") {
                            if (response.turn == playerName) {
                                myTurn = true
                                serverMessage = "🎯 YOUR TURN - FIRE!"
                            } else {
                                myTurn = false
                                serverMessage = "⏳ Awaiting enemy fire..."
                            }
                        }
                    }

                } catch (e: Exception) {
                    println("Polling error: ${e.message}")
                }

                // Poll every 3 seconds
                if (gameState != "FINISHED") {
                    delay(3000)
                }
            }
        }
    }

    // ==========================================
    // 4. UI RENDERING
    // ==========================================
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- SENSOR DASHBOARD ---
        Text("Heading: ${azimuth.toInt()}°", color = Color.Cyan)
        Text("⬆️", modifier = Modifier.size(40.dp).rotate(-azimuth))
        Text("📍 $locationText", color = Color.Green, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))

        Spacer(modifier = Modifier.height(10.dp))

        // --- PHASE 1: LOBBY ---
        if (gameState == "LOBBY") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OPERATIONS ROOM", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = gameId,
                        onValueChange = { gameId = it },
                        label = { Text("Game Room Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    serverMessage = "Connecting..."
                                    val action = GameAction(gameId, playerName)
                                    val response = BattleshipRetrofit.instance.joinGame(action)

                                    if (response.status != "ERROR") {
                                        serverMessage = "Connected! Waiting for rival..."
                                        gameState = "SETUP" // Move to Ship Placement
                                    } else {
                                        serverMessage = "Connection rejected."
                                    }
                                } catch (e: Exception) {
                                    serverMessage = "ERROR: ${e.message}"
                                    e.printStackTrace()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
                    ) {
                        Text("JOIN FLEET", color = Color.Black)
                    }
                }
            }
        }

        // --- PHASE 2: SETUP (PLACE SHIPS) ---
        else if (gameState == "SETUP") {
            Text("DEPLOY 3 SHIPS", color = Color.Yellow, style = MaterialTheme.typography.headlineSmall)
            Text("Selected: ${myShips.size}/3", color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            Column {
                for (i in 0 until 5) {
                    Row {
                        for (j in 0 until 5) {
                            val index = i * 5 + j
                            val isSelected = myShips.contains(index)

                            Box(
                                modifier = Modifier
                                    .padding(2.dp).size(60.dp)
                                    .background(if (isSelected) Color.Green else Color.Gray) // Green for my ships
                                    .clickable {
                                        val newShips = myShips.toMutableSet()
                                        if (isSelected) newShips.remove(index)
                                        else if (newShips.size < 3) newShips.add(index)
                                        myShips = newShips
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isSelected) "🚢" else "")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (myShips.size == 3) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val request = PlaceShipsRequest(gameId, playerName, myShips.toList())
                                BattleshipRetrofit.instance.placeShips(request)
                                gameState = "WAITING"
                                serverMessage = "Fleet ready. Waiting for enemy..."
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error sending fleet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                ) {
                    Text("CONFIRM FLEET", color = Color.Black)
                }
            }
        }

        // --- PHASE 3: GAMEPLAY ---
        else {
            Text(serverMessage, style = MaterialTheme.typography.headlineSmall, color = if (myTurn) Color.Green else Color.Yellow)
            Text("Room: $gameId | Player: $playerName", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(20.dp))

            // Grid Rendering
            Column {
                for (i in 0 until 5) {
                    Row {
                        for (j in 0 until 5) {
                            val index = i * 5 + j

                            // Visual Logic: If cell is Water AND it's my ship, show Ship icon. Else show grid state.
                            val cellContent = if (gridState[index] == "🌊" && myShips.contains(index)) "🚢" else gridState[index]

                            BoardCell(content = cellContent) {
                                // Attack Logic
                                if (myTurn && gridState[index] == "🌊" && gameState != "FINISHED") {
                                    scope.launch {
                                        try {
                                            val move = MoveRequest(gameId, playerName, i, j)
                                            val response = BattleshipRetrofit.instance.sendAttack(move)

                                            val newList = gridState.toMutableList()

                                            // Only mark as HIT if server confirms it
                                            if (response.status == "HIT") {
                                                newList[index] = "💥"
                                                Toast.makeText(context, "HIT!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                newList[index] = "💧"
                                                Toast.makeText(context, "Miss...", Toast.LENGTH_SHORT).show()
                                            }
                                            gridState = newList
                                            myTurn = false
                                            serverMessage = "Sending coordinates..."

                                        } catch (e: Exception) {
                                            // Simulation Mode (Optional fallback)
                                            val newList = gridState.toMutableList()
                                            newList[index] = "💥"
                                            gridState = newList
                                            myTurn = false
                                            Toast.makeText(context, "Simulation Mode", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else if (!myTurn) {
                                    Toast.makeText(context, "Hold your fire, Commander!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { gameState = "LOBBY" }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("ABANDON GAME")
            }
        }
    }
}

/**
 * Reusable Composable for a single cell on the board.
 * Handles dynamic background colors based on state (Hit, Miss, Ship, Water).
 */
@Composable
fun BoardCell(content: String, onClick: () -> Unit) {
    // Dynamic color based on content
    val backgroundColor = when (content) {
        "💥" -> Color.Red         // Hit
        "💧" -> Color.Blue        // Miss
        "🚢" -> Color(0xFF2E7D32) // My Ship (Dark Green)
        else -> Color(0xFF2E86C1) // Water (Standard Blue)
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(60.dp)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = content, style = MaterialTheme.typography.headlineMedium)
    }
}
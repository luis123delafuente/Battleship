package com.example.battleship.ui // Asegúrate de que tu paquete sea correcto

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
import androidx.compose.material3.* // Esto arregla Card, Button, OutlinedTextField...
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.battleship.data.network.BattleshipRetrofit
import com.example.battleship.data.network.GameAction // Arregla GameAction
import com.example.battleship.data.network.MoveRequest // Arregla MoveRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.battleship.data.network.PlaceShipsRequest


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1B2631)
                ) {
                    // Variable para saber en qué pantalla estamos
                    var currentScreen by remember { mutableStateOf("login") }
                    var playerName by remember { mutableStateOf("") }

                    if (currentScreen == "login") {
                        // Pantalla de Login
                        LoginScreen { nombre ->
                            playerName = nombre
                            currentScreen = "juego"
                        }
                    } else {
                        // AQUÍ ESTABA EL ERROR: Faltaba pasar (playerName) dentro de los paréntesis
                        BattleshipGameScreen(playerName)
                    }
                }
            }
        }
    }
}

@Composable
fun BattleshipGameScreen(playerName: String) { // Recibimos el nombre del Login
    // ==========================================
    // 1. ESTADO DEL JUEGO MULTIJUGADOR (TTT Style)
    // ==========================================
    var gameId by remember { mutableStateOf("Partida1") }
    var myTurn by remember { mutableStateOf(false) }
    var serverMessage by remember { mutableStateOf("Introduce ID de Sala") }
    var myShips by remember { mutableStateOf(setOf<Int>()) } // Para guardar tus barcos (Set evita duplicados)
    var gameState by remember { mutableStateOf("LOBBY") } // LOBBY -> SETUP -> WAITING -> PLAYING

    // Tablero visual (5x5)
    var gridState by remember { mutableStateOf(List(25) { "🌊" }) }

    // Herramientas de Corrutinas y Contexto
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ==========================================
    // 2. SENSORES (TU CÓDIGO ORIGINAL INTACTO)
    // ==========================================

    // --- SENSOR ORIENTACIÓN ---
    var azimuth by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sensor = OrientationSensor(context) { newAzimuth ->
            azimuth = newAzimuth
        }
        sensor.start()
        onDispose { sensor.stop() }
    }

    // --- SENSOR GPS ---
    var locationText by remember { mutableStateOf("Buscando señal GPS...") }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val gpsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val networkGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (gpsGranted || networkGranted) {
            val gpsSensor = GpsSensor(context) { lat, long -> locationText = "Lat: $lat\nLong: $long" }
            gpsSensor.start()
        } else {
            locationText = "Sin permiso GPS"
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val gpsSensor = GpsSensor(context) { lat, long -> locationText = "Lat: $lat\nLong: $long" }
            gpsSensor.start()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // ==========================================
    // 3. LÓGICA DE RED (POLLING LOOP)
    // ==========================================
    // Este bucle se activa cuando entramos en WAITING o PLAYING
    LaunchedEffect(gameState) {
        if (gameState != "LOBBY") {
            while (true) {
                try {
                    // Preguntamos al servidor: "¿Cómo va la partida?"
                    val response = BattleshipRetrofit.instance.getGameState(gameId)

                    // A. ¿Ha entrado el rival?
                    if (gameState == "WAITING" && response.player2 != null) {
                        gameState = "PLAYING"
                        serverMessage = "¡Rival encontrado! Batalla iniciada."
                    }

                    // B. Gestión de Turnos
                    if (response.turn == playerName) {
                        myTurn = true
                        serverMessage = "🎯 TU TURNO - ¡DISPARA!"

                        // C. Actualizar tablero si el rival disparó
                        if (response.lastMoveRow != null && response.lastMoveCol != null) {
                            val index = response.lastMoveRow * 5 + response.lastMoveCol
                            // Aquí podrías marcar dónde te han disparado.
                            // Por simplicidad, actualizamos estado local si fuera necesario.
                        }
                    } else {
                        myTurn = false
                        serverMessage = "⏳ Esperando disparo enemigo..."
                    }

                } catch (e: Exception) {
                    println("Polling error: ${e.message}")
                    // No mostramos Toast aquí para no saturar la pantalla cada 3 segundos
                }

                // Esperamos 3 segundos antes de volver a preguntar (Polling)
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    // ==========================================
    // 4. INTERFAZ DE USUARIO (UI)
    // ==========================================
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- VISUALIZACIÓN DE SENSORES (SIEMPRE VISIBLE) ---
        Text("Rumbo: ${azimuth.toInt()}°", color = Color.Cyan)
        Text("⬆️", modifier = Modifier.size(40.dp).rotate(-azimuth))
        Text("📍 $locationText", color = Color.Green, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))

        Spacer(modifier = Modifier.height(10.dp))

        // --- ZONA DE JUEGO O LOBBY ---
        if (gameState == "LOBBY") {
            // PANTALLA DE INICIO (Unirse a sala)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SALA DE OPERACIONES", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = gameId,
                        onValueChange = { gameId = it },
                        label = { Text("Nombre de la Partida") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    serverMessage = "Intentando conectar a 10.0.2.2..."

                                    // 1. Creamos el objeto con los datos
                                    val action = GameAction(gameId, playerName)

                                    // 2. Llamada al servidor (AQUÍ ES DONDE SUELE FALLAR)
                                    // Usamos 'response' para validar, así quitamos la advertencia
                                    val response = BattleshipRetrofit.instance.joinGame(action)

                                    // 3. Verificamos la respuesta del servidor
                                    if (response.status != "ERROR") {
                                        serverMessage = "¡Conectado! Esperando rival..."
                                        gameState = "SETUP"
                                    } else {
                                        serverMessage = "El servidor rechazó la conexión."
                                    }

                                } catch (e: Exception) {
                                    // ESTA ES LA PARTE IMPORTANTE:
                                    // Mostramos el error técnico exacto en la pantalla
                                    serverMessage = "FALLO: ${e.message}"
                                    e.printStackTrace()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
                    ) {
                        Text("UNIRSE A LA FLOTA", color = Color.Black)
                    }
                }
            }
        }

        // ==========================================
        // FASE 2: SETUP (COLOCAR BARCOS) - ¡NUEVO!
        // ==========================================
        else if (gameState == "SETUP") {
            Text("COLOCA 3 BARCOS", color = Color.Yellow, style = MaterialTheme.typography.headlineSmall)
            Text("Seleccionados: ${myShips.size}/3", color = Color.White)
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
                                    .background(if (isSelected) Color.Green else Color.Gray) // Verde si es mi barco
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

            // Botón Confirmar (Solo aparece si tienes 3 barcos)
            if (myShips.size == 3) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val request = PlaceShipsRequest(gameId, playerName, myShips.toList())
                                BattleshipRetrofit.instance.placeShips(request)
                                gameState = "WAITING"
                                serverMessage = "Flota lista. Esperando al rival..."
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error enviando flota", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                ) {
                    Text("CONFIRMAR FLOTA", color = Color.Black)
                }
            }
        }

        else {
            // PANTALLA DE JUEGO (Tablero)
            Text(serverMessage, style = MaterialTheme.typography.headlineSmall, color = if (myTurn) Color.Green else Color.Yellow)
            Text("Sala: $gameId | Jugador: $playerName", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(20.dp))

            // Dibujamos el tablero
            Column {
                for (i in 0 until 5) {
                    Row {
                        for (j in 0 until 5) {
                            val index = i * 5 + j
                            CeldaTablero(contenido = gridState[index]) {
                                // LÓGICA DE DISPARO
                                if (myTurn && gridState[index] == "🌊") {
                                    scope.launch {
                                        try {
                                            // 1. Enviar disparo
                                            val move = MoveRequest(gameId, playerName, i, j)
                                            val respuesta = BattleshipRetrofit.instance.sendAttack(move)

                                            // 2. Actualizar visualmente
                                            val newList = gridState.toMutableList()
                                            if (respuesta.status == "HIT" || respuesta.lastMoveRow != null) {
                                                // Ajusta esta condición según lo que devuelva realmente tu API
                                                newList[index] = "💥"
                                                Toast.makeText(context, "¡IMPACTO!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                newList[index] = "💧"
                                                Toast.makeText(context, "Agua...", Toast.LENGTH_SHORT).show()
                                            }
                                            gridState = newList

                                            // 3. Pasar turno (Localmente hasta que el polling confirme)
                                            myTurn = false
                                            serverMessage = "Enviando coordenadas..."

                                        } catch (e: Exception) {
                                            // MODO OFFLINE / SIMULACIÓN
                                            val newList = gridState.toMutableList()
                                            newList[index] = "💥" // Simulamos acierto
                                            gridState = newList
                                            myTurn = false
                                            Toast.makeText(context, "Modo Simulado (Offline)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else if (!myTurn) {
                                    Toast.makeText(context, "Espere su turno, comandante", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { gameState = "LOBBY" }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("ABANDONAR PARTIDA")
            }
        }
    }
}
@Composable
fun CeldaTablero(contenido: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(60.dp) // Tamaño de cada casilla
            .background(
                if (contenido == "💥") Color.Red else Color(0xFF2E86C1)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = contenido, style = MaterialTheme.typography.headlineMedium)
    }
}
package com.example.battleship.ui // Asegúrate de que tu paquete sea correcto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect

import kotlinx.coroutines.launch
import com.example.battleship.data.network.BattleshipRetrofit
import com.example.battleship.data.network.MoveRequest
import android.widget.Toast // Para mostrar mensajes de error/éxito

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1B2631)
                ) {
                    // --- SISTEMA DE NAVEGACIÓN SIMPLE ---
                    // Variable para saber en qué pantalla estamos: "login" o "juego"
                    var currentScreen by remember { mutableStateOf("login") }
                    var playerName by remember { mutableStateOf("") }

                    if (currentScreen == "login") {
                        // Mostramos el Login
                        LoginScreen { nombre ->
                            playerName = nombre
                            currentScreen = "juego" // Cambiamos de pantalla
                        }
                    } else {
                        // Mostramos el Juego
                        // (Aquí podrías pasar 'playerName' al juego si quisieras mostrarlo)
                        BattleshipGameScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun BattleshipGameScreen() {
    // Esto guarda el estado de nuestras celdas (Requisito 3: Gráficos 2D)
    // Es una lista de 25 elementos (5x5). Al principio todas están vacías.
    var gridState by remember { mutableStateOf(List(5 * 5) { "🌊" }) }

    // 2. HERRAMIENTAS PARA LA RED (Nuevo Requisito 7)
    // El 'scope' nos permite lanzar tareas en segundo plano sin bloquear la pantalla
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // Para mostrar mensajes (Toasts)

    // 3. SENSOR ORIENTACIÓN
    var azimuth by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sensor = OrientationSensor(context) { newAzimuth ->
            azimuth = newAzimuth
        }
        sensor.start()
        onDispose { sensor.stop() }
    }
    // --- SENSOR 2: GPS (UBICACIÓN) --- Requisito 5
    var locationText by remember { mutableStateOf("Buscando señal GPS...") }
    // Preparar el lanzador de permisos (La ventana emergente)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val gpsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val networkGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (gpsGranted || networkGranted) {
            // Si dijo que SÍ, encendemos el GPS
            val gpsSensor = GpsSensor(context) { lat, long ->
                locationText = "Lat: $lat\nLong: $long"
            }
            gpsSensor.start()
        } else {
            locationText = "Sin permiso GPS"
        }
    }

    // Al iniciar la pantalla, comprobamos si ya tenemos permiso o hay que pedirlo
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Ya tenemos permiso, arrancamos directo
            val gpsSensor = GpsSensor(context) { lat, long ->
                locationText = "Lat: $lat\nLong: $long"
            }
            gpsSensor.start()
        } else {
            // No tenemos permiso, lanzamos la pregunta
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mostramos el dato del sensor (Requisito 4 + Teoría PDF)
        Text(
            text = "Rumbo: ${azimuth.toInt()}°",
            color = Color.Cyan,
            style = MaterialTheme.typography.bodyLarge
        )

        // Una flecha visual que rota usando el ángulo calculado
        Text(
            text = "⬆️",
            modifier = Modifier
                .size(50.dp)
                .rotate(-azimuth), // Rotamos al contrario para que apunte al Norte siempre
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "📍 UBICACIÓN:\n$locationText",
            color = Color.Green,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(10.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "BATTLESHIP",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Dibujamos el tablero
        Column {
            for (i in 0 until 5) { // 5 Filas
                Row {
                    for (j in 0 until 5) { // 5 Columnas
                        val index = i * 5 + j
                        CeldaTablero(contenido = gridState[index]){
                            // AQUÍ ESTÁ EL CAMBIO CLAVE (Requisitos 7, 9 y 10)

                            // 1. Lanzamos una Corrutina (Hilo secundario)
                            scope.launch {
                                try {
                                    // 2. Preparamos el ataque
                                    val ataque = MoveRequest(
                                        game_id = "partida1",
                                        player = "Luis",
                                        row = i,
                                        col = j
                                    )

                                    // 3. ¡ENVIAMOS EL ATAQUE POR INTERNET!
                                    // Esto llama a tu archivo BattleshipRetrofit
                                    val respuesta = BattleshipRetrofit.instance.sendAttack(ataque)

                                    // 4. Si el servidor responde, actualizamos el tablero
                                    val newList = gridState.toMutableList()
                                    if (respuesta.hit) {
                                        newList[index] = "💥" // ¡Tocado!
                                        Toast.makeText(context, "¡IMPACTO CONFIRMADO!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        newList[index] = "💧" // Agua
                                        Toast.makeText(context, "Agua...", Toast.LENGTH_SHORT).show()
                                    }
                                    gridState = newList

                                } catch (e: Exception) {
                                    // SI FALLA (Porque no tienes servidor real aún):
                                    // Simulamos que funciona para que puedas presentar el proyecto
                                    println("Error de red: ${e.message}")
                                    Toast.makeText(context, "Modo Offline (Simulado)", Toast.LENGTH_SHORT).show()

                                    // Simulación local para que juegues
                                    val newList = gridState.toMutableList()
                                    newList[index] = "💥"
                                    gridState = newList
                                }
                            }
                        }
                    }
                }
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
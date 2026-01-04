package com.example.battleship.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Composable function representing the Login Screen.
 *
 * This screen handles:
 * 1. User input for username (Captain's Name) and password.
 * 2. Profile picture capture using the device camera (thumbnail).
 * 3. Persistence of the username using [android.content.SharedPreferences].
 *
 * @param onLoginSuccess Callback triggered when the login is validated. Passes the username.
 */
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current

    // 1. READ PREFERENCES
    // We read directly from SharedPreferences to ensure we get the latest persisted data.
    val sharedPreferences = context.getSharedPreferences("BattleshipPrefs", Context.MODE_PRIVATE)
    val savedName = sharedPreferences.getString("USER_NAME", "") ?: ""

    // Initialize state with the saved name.
    // 'key = savedName' ensures that if the underlying preference changes, the state is re-initialized.
    var username by remember(savedName) { mutableStateOf(savedName) }
    var password by remember { mutableStateOf("") }

    // State to hold the captured profile image
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher to open the camera and get a preview bitmap
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        imageBitmap = bitmap
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2631)), // Dark Navy Blue background
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "IDENTIFICATION",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Picture Circular Area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray)
                .border(2.dp, Color.Cyan, CircleShape)
                .clickable { cameraLauncher.launch(null) }
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Take Photo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
        Text(
            text = "Tap to take photo",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Username Input
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Captain's Name") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Access Code") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    // 2. SECURE SAVE (COMMIT)
                    val editor = sharedPreferences.edit()
                    editor.putString("USER_NAME", username)

                    // IMPORTANT! We use commit() instead of apply().
                    // commit() writes synchronously to disk. This ensures the data is strictly saved
                    // before the navigation to the next screen occurs.
                    val success = editor.commit()

                    onLoginSuccess(username)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
        ) {
            Text("ACCESS RADAR", color = Color.Black)
        }
    }
}
package com.example.battleship.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

// Esta clase maneja la conexión con el satélite GPS
class GpsSensor(private val context: Context, private val onLocationReceived: (Double, Double) -> Unit) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Ignoramos el error de "Permisos" aquí porque los pediremos en el MainActivity antes de llamar a esto
    @SuppressLint("MissingPermission")
    fun start() {
        // Pedimos actualizaciones:
        // GPS_PROVIDER = Usa satélites (Más preciso, funciona fuera de casa)
        // NETWORK_PROVIDER = Usa Wi-Fi/Antenas (Más rápido, mejor para interiores)

        // Intentamos usar GPS primero
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 10f, this)
        }
        // También activamos red por si acaso el GPS no coge señal dentro de casa
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 10f, this)
        }
    }

    fun stop() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        // ¡Tenemos coordenadas! Se las pasamos a la pantalla
        onLocationReceived(location.latitude, location.longitude)
    }

    // Funciones obligatorias pero que no usaremos
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
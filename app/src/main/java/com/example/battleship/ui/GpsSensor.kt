package com.example.battleship.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

/**
 * Handles GPS and Network location updates.
 * This class acts as a wrapper around Android's [LocationManager] to simplify fetching coordinates.
 *
 * It utilizes a hybrid approach requesting updates from both GPS (satellites) and Network (Wi-Fi/Cell)
 * to ensure coverage both indoors and outdoors.
 *
 * @property context The application or activity context.
 * @property onLocationReceived Callback function invoked when coordinates (latitude, longitude) change.
 */
class GpsSensor(
    private val context: Context,
    private val onLocationReceived: (Double, Double) -> Unit
) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Starts listening for location updates.
     *
     * It requests updates from two providers:
     * 1. [LocationManager.GPS_PROVIDER]: High accuracy, uses satellites. Ideal for outdoors.
     * 2. [LocationManager.NETWORK_PROVIDER]: Faster fix, uses Wi-Fi/Cell towers. Ideal for indoors.
     *
     * Note: The [SuppressLint] annotation is used because runtime permissions are handled
     * and verified in the UI layer (MainActivity) before invoking this method.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        // Configuration: Update every 2 seconds (2000ms) or every 10 meters
        val minTimeMs = 2000L
        val minDistanceM = 10f

        // Attempt to use GPS Provider (Precision)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceM,
                this
            )
        }

        // Attempt to use Network Provider (Speed/Indoors coverage)
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTimeMs,
                minDistanceM,
                this
            )
        }
    }

    /**
     * Stops listening for updates to conserve battery life.
     * This should be called when the Composable is disposed or the Activity pauses.
     */
    fun stop() {
        locationManager.removeUpdates(this)
    }

    /**
     * Callback triggered by the system when a new location is detected.
     */
    override fun onLocationChanged(location: Location) {
        // Pass the new coordinates to the UI callback
        onLocationReceived(location.latitude, location.longitude)
    }

    // --- Boilerplate methods required by LocationListener interface ---

    override fun onProviderEnabled(provider: String) {
        // Optional: Logic when user enables GPS/Network
    }

    override fun onProviderDisabled(provider: String) {
        // Optional: Logic when user disables GPS/Network
    }

    @Deprecated("Deprecated in API level 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }
}
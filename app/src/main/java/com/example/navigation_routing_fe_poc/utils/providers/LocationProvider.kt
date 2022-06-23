package com.example.navigation_routing_fe_poc.utils.providers

import android.util.Log
import com.here.sdk.consent.Consent
import com.here.sdk.consent.ConsentEngine
import com.here.sdk.core.Location
import com.here.sdk.core.LocationListener
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.location.*
import java.util.concurrent.TimeUnit

class LocationProvider {
    private val LOG_TAG: String = "Location Provider"

    private var locationEngine: LocationEngine? = null
    private var updateListener: LocationListener? = null

    private val locationStatusListener: LocationStatusListener = object : LocationStatusListener {
        override fun onStatusChanged(locationEngineStatus: LocationEngineStatus) {
            Log.d(LOG_TAG, "Location engine status: " + locationEngineStatus.name)
        }

        override fun onFeaturesNotAvailable(features: List<LocationFeature>) {
            for (feature in features) {
                Log.d(LOG_TAG, "Location feature not available: " + feature.name)
            }
        }
    }

    fun HEREPositioningProvider() {
        val consentEngine: ConsentEngine
        try {
            consentEngine = ConsentEngine()
            locationEngine = LocationEngine()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization failed: " + e.message)
        }

        // Ask user to optionally opt in to HERE's data collection / improvement program.
        if (consentEngine.userConsentState == Consent.UserReply.NOT_HANDLED) {
            consentEngine.requestUserConsent()
        }
    }

    fun getLastKnownLocation(): Location? {
        return locationEngine!!.lastKnownLocation
    }

    // Does nothing when engine is already running.
    fun startLocating(updateListener: LocationListener, accuracy: LocationAccuracy?) {
        if (locationEngine!!.isStarted) {
            return
        }
        this.updateListener = updateListener
        val locationOptions = LocationOptions()
        //Use WiFi and satellite (GNSS) positioning only.
        locationOptions.wifiPositioningOptions.enabled = true
        locationOptions.satellitePositioningOptions.enabled = true
        locationOptions.sensorOptions.enabled = false
        locationOptions.cellularPositioningOptions.enabled = false

        // Receive a location approximately every minute, but not more often than every 30 seconds.
        //locationOptions.notificationOptions.smallestIntervalMilliseconds = TimeUnit.SECONDS.toMillis(5) //30
        //locationOptions.notificationOptions.desiredIntervalMilliseconds = TimeUnit.SECONDS.toMillis(10) //60
        // Set listeners to get location updates.
        locationEngine!!.addLocationListener(updateListener)
        locationEngine!!.addLocationStatusListener(locationStatusListener)
        locationEngine!!.start(locationOptions)
    }

    // Does nothing when engine is already stopped.
    fun stopLocating() {
        if (!locationEngine!!.isStarted) {
            return
        }

        // Remove listeners and stop location engine.
        locationEngine!!.removeLocationListener(updateListener!!)
        locationEngine!!.removeLocationStatusListener(locationStatusListener)
        locationEngine!!.stop()
    }
}
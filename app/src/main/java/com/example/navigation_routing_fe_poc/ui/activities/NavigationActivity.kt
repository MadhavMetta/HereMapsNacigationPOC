package com.example.navigation_routing_fe_poc.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.navigation_routing_fe_poc.R
import com.example.navigation_routing_fe_poc.databinding.ActivityNavigationBinding
import com.example.navigation_routing_fe_poc.utils.providers.RoutingProvider
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.LocationListener
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.location.LocationAccuracy
import com.here.sdk.mapview.MapScheme
import com.here.sdk.navigation.LocationSimulator
import com.here.sdk.navigation.LocationSimulatorOptions
import com.here.sdk.navigation.ManeuverNotificationListener
import com.here.sdk.navigation.VisualNavigator
import com.here.sdk.routing.*

class NavigationActivity : AppCompatActivity() {
    private val TAG = NavigationActivity::class.java.simpleName
    private lateinit var binding: ActivityNavigationBinding
    private lateinit var routingProvider: RoutingProvider
    private lateinit var routingEngine:RoutingEngine
    private lateinit var visualNavigator: VisualNavigator
    private lateinit var locationSimulator:LocationSimulator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_navigation)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.navigation)
        binding.mapView.onCreate(savedInstanceState)
        loadMapScene()
    }

    private fun loadMapScene() {
        binding.mapView.mapScene.loadScene(
            MapScheme.NORMAL_DAY
        ) { mapError ->
            if (mapError == null) {
                routingProvider = RoutingProvider()
                routingProvider.setContext_MapView(this, binding.mapView)
                routingProvider.locationProvider.HEREPositioningProvider()
                routingProvider.locationProvider.startLocating(
                    {
                       /* Toast.makeText(
                            this,
                            "Received location: " + it.coordinates.latitude.toString() + ", " + it.coordinates.longitude,
                            Toast.LENGTH_SHORT
                        ).show()*/
                        if (::visualNavigator.isInitialized)
                            visualNavigator.onLocationUpdated(it)
                    }, LocationAccuracy.NAVIGATION
                )
                routingProvider.clearMap()
                calculateRoute()
            } else {
                Log.d(
                    TAG,
                    "Loading map failed: mapErrorCode: " + mapError.name
                )
            }
        }
    }
    private fun calculateRoute() {
        routingEngine = try {
            RoutingEngine()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization of RoutingEngine failed: " + e.error.name)
        }
        val startWaypoint = Waypoint(GeoCoordinates(17.4540424,78.3709083))
        val destinationWaypoint = Waypoint(GeoCoordinates(17.459726, 78.354156))
        routingEngine.calculateRoute(
            ArrayList(listOf(startWaypoint, destinationWaypoint)),
            CarOptions()
        ) { routingError: RoutingError?, routes: List<Route?>? ->
            if (routingError == null) {
                val route = routes!![0]
                route?.let { startGuidance(it) }
            } else {
                Log.e("Route calculation error", routingError.toString())
            }
        }
    }
    private fun startGuidance(route: Route) {
        visualNavigator = try {
            // Without a route set, this starts tracking mode.
            VisualNavigator()
        } catch (e: InstantiationErrorException) {
            throw java.lang.RuntimeException("Initialization of VisualNavigator failed: " + e.error.name)
        }

        // This enables a navigation view including a rendered navigation arrow.
        visualNavigator.startRendering(binding.mapView)

        // Hook in one of the many listeners. Here we set up a listener to get instructions on the maneuvers to take while driving.
        // For more details, please check the "Navigation" example app and the Developer's Guide.
        visualNavigator.maneuverNotificationListener =
            ManeuverNotificationListener { maneuverText: String? ->
                Log.d(
                    "ManeuverNotifications",
                    maneuverText!!
                )
            }

        // Set a route to follow. This leaves tracking mode.
        visualNavigator.route = route

        // VisualNavigator acts as LocationListener to receive location updates directly from a location provider.
        // Any progress along the route is a result of getting a new location fed into the VisualNavigator.
        //setupLocationSource(visualNavigator, route);

        //herePositioningProvider.startLocating(visualNavigator, LocationAccuracy.NAVIGATION);
    }
    private fun setupLocationSource(locationListener: LocationListener, route: Route) {
        locationSimulator = try {
            // Provides fake GPS signals based on the route geometry.
            LocationSimulator(route, LocationSimulatorOptions())
        } catch (e: InstantiationErrorException) {
            throw java.lang.RuntimeException("Initialization of LocationSimulator failed: " + e.error.name)
        }
        locationSimulator.listener = locationListener
        locationSimulator.start()
    }
    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        routingProvider.locationProvider.stopLocating()
    }
}
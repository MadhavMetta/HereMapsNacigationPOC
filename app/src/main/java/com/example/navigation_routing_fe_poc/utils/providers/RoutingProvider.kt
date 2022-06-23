package com.example.navigation_routing_fe_poc.utils.providers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.navigation_routing_fe_poc.R
import com.example.navigation_routing_fe_poc.response.Legs
import com.here.sdk.animation.EasingFunction
import com.here.sdk.core.*
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.*
import com.here.sdk.routing.*
import com.here.time.Duration
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class RoutingProvider {
    private val TAG: String = "Routing Provider"
    private var context: Context? = null
    private var mapView: MapView? = null
    lateinit var locationProvider: LocationProvider
    private val mapMarkerList: MutableList<MapMarker> = ArrayList()
    private val mapPolylines: MutableList<MapPolyline> = ArrayList()
    private val mapPolygons: MutableList<MapPolygon> = ArrayList()
    private var routingEngine: RoutingEngine? = null
    private var startGeoCoordinates: GeoCoordinates? = null
    private var destinationGeoCoordinates: GeoCoordinates? = null

    companion object {
        const val accuracyRadiusInMeters = 50.0
    }

    fun setContext_MapView(contextRP: Context?, mapViewRP: MapView) {
        context = contextRP
        mapView = mapViewRP
        locationProvider = LocationProvider()
        val camera = mapView!!.camera
        val distanceInMeters = (1000 * 10).toDouble()
        val latLang: Location? = getInitialLatKnownLocation(contextRP)
        val geoCoordinates = if (latLang != null)
            GeoCoordinates(latLang.latitude, latLang.longitude)
        else
            GeoCoordinates(17.4417, 78.4416)
        camera.lookAt(geoCoordinates, distanceInMeters)
        camera.zoomTo(18.0)
        addMyLocationToMap(geoCoordinates, accuracyRadiusInMeters)
        routingEngine = try {
            RoutingEngine()
        } catch (e: InstantiationErrorException) {
            throw java.lang.RuntimeException("Initialization of RoutingEngine failed: " + e.error.name)
        }
    }

    fun addMyLocationToMap(geoCoordinates: GeoCoordinates, accuracyRadiusInMeters: Double) {
        //Transparent halo around the current location: the true geographic coordinates lie with a probability of 68% within that.
        val locationAccuracyCircle =
            MapPolygon(
                GeoPolygon(GeoCircle(geoCoordinates, accuracyRadiusInMeters)),
                Color.valueOf(android.graphics.Color.parseColor("#dfe9f9"))
            )
        //Solid circle on top of the current location.
        val locationCenterCircle =
            MapPolygon(
                GeoPolygon(GeoCircle(geoCoordinates, 2.5)),
                Color.valueOf(android.graphics.Color.parseColor("#1a73e8"))
            )

        mapPolygons.add(locationAccuracyCircle)
        mapPolygons.add(locationCenterCircle)
        //Add the circle to the map.
        mapView!!.mapScene.addMapPolygon(locationAccuracyCircle)
        mapView!!.mapScene.addMapPolygon(locationCenterCircle)
    }

    /*@SuppressLint("MissingPermission")
    private fun getInitialLatKnownLocation(context: Context?): Location? {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, true)
        return provider?.let { locationManager.getLastKnownLocation(it) }
    }*/
    @SuppressLint("MissingPermission")
    private fun getInitialLatKnownLocation(context: Context?): Location? {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val latLang = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || latLang.accuracy < bestLocation.accuracy) {
                bestLocation = latLang
            }
        }
        return bestLocation
    }


    fun addRoute(context: Context, sourceGeoCoordinates: GeoCoordinates? = null) {
//        startGeoCoordinates = locationProvider.getLastKnownLocation()?.coordinates
        //startGeoCoordinates = createRandomGeoCoordinatesAroundMapCenter()
        startGeoCoordinates = if (sourceGeoCoordinates == null) {
            val latLang: Location? = getInitialLatKnownLocation(context)
            latLang?.let {
                GeoCoordinates(latLang.latitude, latLang.longitude)
            }
        } else
            sourceGeoCoordinates
        //destinationGeoCoordinates = createRandomGeoCoordinatesAroundMapCenter()
        destinationGeoCoordinates = GeoCoordinates(
            17.4489,
            78.3832
        )//GeoCoordinates(17.4421,78.3772)RDM //GeoCoordinates(17.4489,78.3832)HM
        val startWaypoint = Waypoint(startGeoCoordinates!!)
        val destinationWaypoint = Waypoint(destinationGeoCoordinates!!)
        //val waypoints: List<Waypoint> = ArrayList(listOf(startWaypoint, destinationWaypoint))
        val waypoints: List<Waypoint> = ArrayList(
            listOf(
                startWaypoint, Waypoint(GeoCoordinates(17.4489, 78.3832)),
                Waypoint(GeoCoordinates(17.4526, 78.3783))
            )
        )
        routingEngine!!.calculateRoute(
            waypoints,
            CarOptions()
        ) { routingError, routes ->
            if (routingError == null) {
                val route = routes!![0]
                //showRouteDetails(route)
                showRouteOnMap(route, waypoints)
                logRouteSectionDetails(route)
                logRouteViolations(route)
            } else {
                showDialog("Error while calculating a route:", routingError.toString())
            }
        }
    }

    fun drawRouting(context: Context, geoCoordinates: ArrayList<Legs>) {
        clearMap()

        val geoCoordinatesPrepared: ArrayList<GeoCoordinates> = arrayListOf()
        geoCoordinates.forEach {
            it.Geometry.LineString.forEach { itLine ->
                geoCoordinatesPrepared.add(GeoCoordinates(itLine[1], itLine[0]))
            }
        }
        val geoPolyLine = GeoPolyline(geoCoordinatesPrepared)
        val widthInPixels = 20.0
        val mapPolyline =
            MapPolyline(geoPolyLine, widthInPixels, Color.valueOf(android.graphics.Color.BLUE))
        mapPolylines.add(mapPolyline)
        mapView!!.mapScene.addMapPolyline(mapPolyline)
        for (index in geoCoordinates.indices) {
            if (index == 0) {
                addCircleMapMarker(
                    GeoCoordinates(
                        geoCoordinates[index].StartPosition.last(),
                        geoCoordinates[index].StartPosition.first()
                    ), R.drawable.bike
                )
                addCircleMapMarker(
                    GeoCoordinates(
                        geoCoordinates[index].EndPosition.last(),
                        geoCoordinates[index].EndPosition.first()
                    ), R.drawable.dest_1
                )
                continue
            }
            if (index == geoCoordinates.size - 1) {
                addCircleMapMarker(
                    GeoCoordinates(
                        geoCoordinates[index].StartPosition.last(),
                        geoCoordinates[index].StartPosition.first()
                    ), getMapIcon(index-1)
                )
                addCircleMapMarker(
                    GeoCoordinates(
                        geoCoordinates[index].EndPosition.last(),
                        geoCoordinates[index].EndPosition.first()
                    ), getMapIcon(index)
                )
            } else
                addCircleMapMarker(
                    GeoCoordinates(
                        geoCoordinates[index].StartPosition.last(),
                        geoCoordinates[index].StartPosition.first()
                    ), getMapIcon(index-1)
                )
        }
        //addCircleMapMarker(geoCoordinatesPrepared.first(), R.drawable.bike)
        //addCircleMapMarker(geoCoordinatesPrepared.last(), R.drawable.dest_1)

        val waypoints = listOf<Waypoint>(
            Waypoint(geoCoordinatesPrepared.first()),
            Waypoint(geoCoordinatesPrepared.last())
        )
        routingEngine!!.calculateRoute(
            waypoints,
            CarOptions()
        ) { routingError, routes ->
            if (routingError == null) {
                val route = routes!![0]
                animateToRoute(route)
            } else {
                showDialog("Error while calculating a route:", routingError.toString())
            }
        }
    }

    private fun getMapIcon(index: Int): Int {
        return when (index) {
            0 -> R.drawable.dest_1
            1 -> R.drawable.dest_2
            2 -> R.drawable.dest_3
            3 -> R.drawable.dest_4
            4 -> R.drawable.dest_5
            else -> R.drawable.dest_5
        }
    }

    // A route may contain several warnings, for example, when a certain route option could not be fulfilled.
    // An implementation may decide to reject a route if one or more violations are detected.
    private fun logRouteViolations(route: Route) {
        for (section in route.sections) {
            for (notice in section.sectionNotices) {
                Log.e(TAG, "This route contains the following warning: " + notice.code.toString())
            }
        }
    }

    private fun logRouteSectionDetails(route: Route) {
        val dateFormat: DateFormat = SimpleDateFormat("HH:mm")
        for (i in route.sections.indices) {
            val section = route.sections[i]
            Log.d(TAG, "Route Section : " + (i + 1))
            Log.d(TAG, "Route Section Departure Time : " + dateFormat.format(section.departureTime))
            Log.d(TAG, "Route Section Arrival Time : " + dateFormat.format(section.arrivalTime))
            Log.d(TAG, "Route Section length : " + section.lengthInMeters + " m")
            Log.d(TAG, "Route Section duration : " + section.duration.seconds + " s")
        }
    }

    private fun showRouteDetails(route: Route) {
        val estimatedTravelTimeInSeconds = route.duration.seconds
        val lengthInMeters = route.lengthInMeters
        val routeDetails = ("Travel Time: " + formatTime(estimatedTravelTimeInSeconds)
                + ", Length: " + formatLength(lengthInMeters))
        showDialog("Route Details", routeDetails)
    }

    private fun formatTime(sec: Long): String {
        val hours = (sec / 3600).toInt()
        val minutes = (sec % 3600 / 60).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    private fun formatLength(meters: Int): String {
        val kilometers = meters / 1000
        val remainingMeters = meters % 1000
        return String.format(Locale.getDefault(), "%02d.%02d km", kilometers, remainingMeters)
    }

    private fun showRouteOnMap(route: Route, waypoints: List<Waypoint>) {
        // Optionally, clear any previous route.
        clearMap()

        // Show route as polyline.
        val routeGeoPolyline = route.geometry
        val widthInPixels = 10f
        val routeMapPolyline = MapPolyline(
            routeGeoPolyline,
            widthInPixels.toDouble(),
            Color.valueOf(0f, 0.56f, 0.54f, 0.63f)
        ) // RGBA
        mapView!!.mapScene.addMapPolyline(routeMapPolyline)
        mapPolylines.add(routeMapPolyline)
        val startPoint = route.sections[0].departurePlace.mapMatchedCoordinates
        val destination = route.sections[route.sections.size - 1].arrivalPlace.mapMatchedCoordinates

        // Draw a circle to indicate starting point and destination.
        for (index in waypoints.indices) {
            if (index == 0)
                addCircleMapMarker(waypoints[index].coordinates, R.drawable.bike)
            else
                addCircleMapMarker(waypoints[index].coordinates, R.drawable.dest_1)
        }
        /*addCircleMapMarker(startPoint, R.drawable.bike)
        addCircleMapMarker(destination, R.drawable.dest_1)*/
        animateToRoute(route)
        // Log maneuver instructions per route section.
        val sections = route.sections
        for (section in sections) {
            logManeuverInstructions(section)
        }
    }

    private fun animateToRoute(route: Route) {
        // Untilt and unrotate the map.
        val bearing = 30.0
        val tilt = 30.0
        // We want to show the route fitting in the map view without any additional padding.
        val origin = Point2D(50.0, 0.0)
        val sizeInPixels = Size2D(
            mapView!!.width.toDouble() - 100,
            mapView!!.height.toDouble()
        )
        val mapViewport = Rectangle2D(origin, sizeInPixels)

        // Animate to route.
        val update = MapCameraUpdateFactory.lookAt(
            route.boundingBox,
            GeoOrientationUpdate(bearing, tilt),
            mapViewport
        )
        val animation = MapCameraAnimationFactory.createAnimation(
            update,
            Duration.ofMillis(800),
            EasingFunction.IN_CUBIC
        )
        mapView!!.camera.startAnimation(animation)
    }

    private fun logManeuverInstructions(section: Section) {
        Log.d(TAG, "Log maneuver instructions per route section:")
        val maneuverInstructions = section.maneuvers
        for (maneuverInstruction in maneuverInstructions) {
            val maneuverAction = maneuverInstruction.action
            val maneuverLocation = maneuverInstruction.coordinates
            val maneuverInfo = (maneuverInstruction.text
                    + ", Action: " + maneuverAction.name
                    + ", Location: " + maneuverLocation.toString())
            Log.d(TAG, maneuverInfo)
        }
    }

    fun clearMap() {
        clearWaypointMapMarker()
        clearPolygons()
        clearRoute()
    }

    private fun clearPolygons() {
        for (mapMarker in mapPolygons) {
            mapView!!.mapScene.removeMapPolygon(mapMarker)
        }
        mapPolygons.clear()
    }

    private fun clearWaypointMapMarker() {
        for (mapMarker in mapMarkerList) {
            mapView!!.mapScene.removeMapMarker(mapMarker)
        }
        mapMarkerList.clear()
    }

    private fun clearRoute() {
        for (mapPolyline in mapPolylines) {
            mapView!!.mapScene.removeMapPolyline(mapPolyline)
        }
        mapPolylines.clear()
    }

    private fun createRandomGeoCoordinatesAroundMapCenter(): GeoCoordinates? {
        val centerGeoCoordinates = mapView!!.viewToGeoCoordinates(
            Point2D(
                (mapView!!.width / 2).toDouble(),
                (mapView!!.height / 2).toDouble()
            )
        )
            ?: // Should never happen for center coordinates.
            throw RuntimeException("CenterGeoCoordinates are null")
        val lat = centerGeoCoordinates.latitude
        val lon = centerGeoCoordinates.longitude
        return GeoCoordinates(
            getRandom(lat - 0.02, lat + 0.02),
            getRandom(lon - 0.02, lon + 0.02)
        )
    }

    private fun getRandom(min: Double, max: Double): Double {
        return min + Math.random() * (max - min)
    }

    private fun addCircleMapMarker(geoCoordinates: GeoCoordinates, resourceId: Int) {
        val mapImage = MapImageFactory.fromResource(context!!.resources, resourceId)
        val mapMarker = MapMarker(geoCoordinates, mapImage)
        mapView!!.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(
            context!!
        )
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }
}
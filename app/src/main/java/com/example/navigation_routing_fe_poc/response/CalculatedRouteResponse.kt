package com.example.navigation_routing_fe_poc.response

data class CalculatedRouteResponse(val data:RouteData)

data class RouteData(val Legs:ArrayList<Legs>,val Summary:Summary)

data class Legs(
    val Distance:Double,
    val DurationSeconds:Long,
    val EndPosition:ArrayList<Double>,
    val Geometry:Geometry,
    val StartPosition:ArrayList<Double>,
    val Steps:ArrayList<Steps>,
)
data class Steps(
    val Distance:Double,
    val DurationSeconds:Int,
    val EndPosition:ArrayList<Double>,
    val GeometryOffset:Int,
    val StartPosition:ArrayList<Double>,
)
data class Geometry(val LineString:ArrayList<ArrayList<Double>>)
data class Summary(
    val DataSource:String,
    val Distance:Double,
    val DistanceUnit:String,
    val DurationSeconds:Long,
    val RouteBBox:ArrayList<Double>
)

package com.example.navigation_routing_fe_poc.ui.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.navigation_routing_fe_poc.PermissionsRequester
import com.example.navigation_routing_fe_poc.R
import com.example.navigation_routing_fe_poc.utils.providers.RoutingProvider
import com.example.navigation_routing_fe_poc.network.APIClient
import com.example.navigation_routing_fe_poc.network.MyRetrofitBuilder
import com.example.navigation_routing_fe_poc.objects.CalculatedRouteRequest
import com.example.navigation_routing_fe_poc.response.CalculatedRouteResponse
import com.google.gson.Gson
import com.here.sdk.consent.Consent
import com.here.sdk.consent.ConsentEngine
import com.here.sdk.location.*
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class RoutingListActivity : AppCompatActivity() {

    private val TAG = RoutingListActivity::class.java.simpleName

    lateinit var permissionsRequestor: PermissionsRequester
    lateinit var mapView: MapView
    lateinit var tvDistance: TextView
    lateinit var tvTime: TextView
    lateinit var clJourneyDetails:ConstraintLayout
    lateinit var routingProvider: RoutingProvider
    lateinit var btnStartNavigation: Button
    val progressDialog:ProgressDialog by lazy {
        ProgressDialog(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get a MapView instance from layout.
        mapView = findViewById<MapView>(R.id.map_view)
        tvDistance = findViewById<TextView>(R.id.tvDistance)
        tvTime = findViewById<TextView>(R.id.tvTime)
        clJourneyDetails = findViewById(R.id.clJourneyDetails)
        btnStartNavigation = findViewById(R.id.btnStartNavigation)
        mapView.onCreate(savedInstanceState)
        handleAndroidPermissions()
        initClickListener()
        title = "${intent.extras?.getString("title")} Route"
    }

    private fun initClickListener() {
        btnStartNavigation.setOnClickListener {
            showMessage("Started...")
        }
    }

    private fun handleAndroidPermissions() {
        permissionsRequestor = PermissionsRequester(this)
        permissionsRequestor.request(object : PermissionsRequester.ResultListener {
            override fun permissionsGranted() {
                val consentEngine = ConsentEngine()
                if (consentEngine.userConsentState == Consent.UserReply.NOT_HANDLED)
                    consentEngine.requestUserConsent()
                loadMapScene()
                //initLocationEngine()
            }

            override fun permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.")
            }

        })
    }
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun loadMapScene() {
        mapView.mapScene.loadScene(
            MapScheme.NORMAL_DAY
        ) { mapError ->
            if (mapError == null) {
                routingProvider = RoutingProvider()
                routingProvider.setContext_MapView(this, mapView)
                routingProvider.locationProvider.HEREPositioningProvider()
                routingProvider.locationProvider.startLocating(
                    {
                        //showMessage("lat = ${it.coordinates.latitude} , long = ${it.coordinates.longitude}")
                        //routingProvider.clearMap()
                        //routingProvider.addRoute(this,GeoCoordinates(it.coordinates.latitude,it.coordinates.longitude))
                        //routingProvider.addMyLocationToMap(GeoCoordinates(it.coordinates.latitude,it.coordinates.longitude),RoutingProvider.accuracyRadiusInMeters)
                    }, LocationAccuracy.BEST_AVAILABLE
                )
                getRouteMap()
            } else {
                Log.d(
                    TAG,
                    "Loading map failed: mapErrorCode: " + mapError.name
                )
            }
        }
    }
    private fun getRouteMap() {
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Finding route...")
        progressDialog.show()
        var feId = intent.extras?.getString("id")
        if (feId.isNullOrEmpty()) feId = ""
        val gitHubClient = MyRetrofitBuilder.getRetrofit().create(APIClient::class.java)
        val client = gitHubClient.getCalculatedRoute(CalculatedRouteRequest(feId))
        client.enqueue(object : retrofit2.Callback<CalculatedRouteResponse>{
            override fun onResponse(
                call: Call<CalculatedRouteResponse>,
                response: Response<CalculatedRouteResponse>
            ) {
                progressDialog.dismiss()
                if (response.body()!=null) {
                    clJourneyDetails.visibility = View.VISIBLE
                    val distance =
                        DecimalFormat("##.##").format(response.body()?.data?.Summary?.Distance)
                    tvDistance.text = "Distance: $distance km"
                    tvTime.text =
                        "Time: ${TimeUnit.SECONDS.toMinutes(response.body()?.data?.Summary?.DurationSeconds!!)} min"
                    routingProvider.drawRouting(this@RoutingListActivity, response.body()?.data!!.Legs)
                }
                else
                    Toast.makeText(this@RoutingListActivity, response.message(), Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<CalculatedRouteResponse>, t: Throwable) {
                progressDialog.dismiss()
                val json = loadJSONFromAsset()
                json?.let {
                    clJourneyDetails.visibility = View.VISIBLE
                    val gson = Gson()
                    val mMineUserEntity = gson.fromJson(json, CalculatedRouteResponse::class.java)
                    val distance = DecimalFormat("##.##").format(mMineUserEntity.data.Summary.Distance)
                    tvDistance.text = "Distance: $distance km"
                    tvTime.text = "Time: ${TimeUnit.SECONDS.toMinutes(mMineUserEntity.data.Summary.DurationSeconds)} min"
                    routingProvider.drawRouting(this@RoutingListActivity,mMineUserEntity.data.Legs)
                }
                Toast.makeText(this@RoutingListActivity,t.toString(),Toast.LENGTH_LONG).show()
            }
        })
    }
    fun loadJSONFromAsset(): String? {
        var json: String? = null
        json = try {
            val `is`: InputStream = this.assets.open("route_data.json")
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.defaultCharset())
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }
    fun addRouteButtonClicked(view: View?) {
        //routingProvider.addRoute(this)
        getRouteMap()
    }


    fun clearMapButtonClicked(view: View?) {
        routingProvider.clearMap()
        clJourneyDetails.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        routingProvider.locationProvider.stopLocating()
    }

}
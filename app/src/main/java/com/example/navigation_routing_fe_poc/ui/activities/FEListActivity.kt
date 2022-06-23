package com.example.navigation_routing_fe_poc.ui.activities

import android.Manifest
import android.R
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.navigation_routing_fe_poc.databinding.ActivityFelistBinding


class FEListActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var binding: ActivityFelistBinding
    private var feList = arrayOf("--Select--", "FE 1", "FE 2", "FE 3", "FE 4", "FE 5")
    private var onUserInteraction = false
    private var selectedPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFelistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initClickListener()
        requestPermission()
    }

    private fun requestPermission() {
        val isAccessFineLocationPermissionEnabled =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val isAccessCoarsePermissionEnabled =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val isActivityRecognitionPermissionEnabled =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            } else PermissionChecker.PERMISSION_GRANTED
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        val storagePermissionEnabled =
            isAccessFineLocationPermissionEnabled == PermissionChecker.PERMISSION_GRANTED &&
                    isAccessCoarsePermissionEnabled == PermissionChecker.PERMISSION_GRANTED &&
                    isActivityRecognitionPermissionEnabled == PermissionChecker.PERMISSION_GRANTED
        if (storagePermissionEnabled.not())
            locationPermissionResultLauncher.launch(permissions)
    }

    private val locationPermissionResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var allAreGranted = true
            for (b in result.values) {
                allAreGranted = allAreGranted && b
            }
            if (allAreGranted.not()){
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                startActivity(intent)
            }

        }

    private fun initClickListener() {
        binding.spFE.onItemSelectedListener = this
        val spAdapter: ArrayAdapter<*> =
            ArrayAdapter<Any?>(this, R.layout.simple_spinner_item, feList)
        spAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spFE.adapter = spAdapter
        binding.btnLogin.setOnClickListener {
           /* val selectedFe = when (selectedPosition) {
                1 -> "FE1"
                2 -> "FE2"
                3 -> "FE3"
                4 -> "FE4"
                5 -> "FE5"
                else -> ""
            }
            val intent = Intent(this, RoutingListActivity::class.java)
            intent.putExtra("id", selectedFe)
            intent.putExtra("title", binding.spFE.selectedItem.toString())
            startActivity(intent)*/
            startActivity(Intent(this,NavigationActivity::class.java))
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        onUserInteraction = true
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        binding.btnLogin.isEnabled = position != 0
        if (onUserInteraction)
            selectedPosition = position
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
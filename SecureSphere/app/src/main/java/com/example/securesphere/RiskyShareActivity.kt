package com.example.securesphere

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

class RiskyShareActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var dbHelper: LocationDbHelper

    private val dbRef = FirebaseDatabase.getInstance().getReference("live_locations")
    private val currentUserId = "fyp_test_user"
    private var isSharingEnabled = false

    // Explicitly typed list to avoid any "uninferred type parameter" or argument mismatch errors
    private val userDefinedZones = mutableListOf<Pair<CustomRiskyZone, String>>()
    private var userLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "SecureSphere/1.0 (Android FYP Project)"
        val basePath = File(filesDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")

        setContentView(R.layout.activity_risky_share)

        dbHelper = LocationDbHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        map = findViewById(R.id.mapView)

        map.setMultiTouchControls(true)
        map.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        map.controller.setZoom(16.0)

        setupMapInteractions()
        loadPermanentZonesFromDb()

        val btnToggleShare = findViewById<ToggleButton>(R.id.btnToggleShare)
        btnToggleShare.setOnCheckedChangeListener { _, isChecked ->
            isSharingEnabled = isChecked
            if (isChecked) {
                requestDevicePermissions()
            } else {
                stopLocationUpdates()
                dbRef.child(currentUserId).removeValue()
                Toast.makeText(this, "Tracking Engine Deactivated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPermanentZonesFromDb() {
        userDefinedZones.clear()
        val savedData: List<Pair<CustomRiskyZone, String>> = dbHelper.getAllZones()

        // Using explicit index tracking to avoid ambiguous 'iterator()' expressions
        for (i in 0 until savedData.size) {
            val item = savedData[i]
            userDefinedZones.add(item)
            renderZoneOnMap(item.first, item.second)
        }
        map.invalidate()
    }

    private fun renderZoneOnMap(zone: CustomRiskyZone, type: String) {
        val marker = Marker(map).apply {
            position = zone.geoPoint
            title = "[$type] ${zone.name}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marker)

        val circlePoints = Polygon.pointsAsCircle(zone.geoPoint, zone.radiusMeters.toDouble())
        val circle = Polygon().apply {
            points = circlePoints

            // Cleaned up painting calls using standard color mappings without deprecation warnings
            val targetColor = if (type == "DANGER") Color.RED else Color.GREEN
            val targetFill = if (type == "DANGER") Color.argb(50, 255, 0, 0) else Color.argb(50, 0, 255, 0)

            outlinePaint.color = targetColor
            outlinePaint.style = Paint.Style.STROKE
            outlinePaint.strokeWidth = 2f

            fillPaint.color = targetFill
            fillPaint.style = Paint.Style.FILL
        }
        map.overlays.add(circle)
    }

    private fun setupMapInteractions() {
        val mReceive = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { showCreateZoneDialog(it) }
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(mReceive))
    }

    private fun showCreateZoneDialog(geoPoint: GeoPoint) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Configure Permanent Perimeter")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputName = EditText(this).apply { hint = "Location Name (e.g., Main Street Corridor)" }
        layout.addView(inputName)

        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(0, 20, 0, 20)
        }
        val rbDanger = RadioButton(this).apply { text = "Danger Area"; id = View.generateViewId() }
        val rbSafe = RadioButton(this).apply { text = "Safe Area"; id = View.generateViewId() }
        radioGroup.addView(rbDanger)
        radioGroup.addView(rbSafe)
        rbDanger.isChecked = true
        layout.addView(radioGroup)

        val radiusSpinner = Spinner(this)
        val radiusOptions = arrayOf("100 Meters (Tight Perimeter)", "250 Meters (Standard Block)", "500 Meters (Wide Zone)")
        radiusSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, radiusOptions)
        layout.addView(radiusSpinner)

        builder.setView(layout)

        builder.setPositiveButton("Commit to Storage") { dialog, _ ->
            val name = inputName.text.toString().trim()
            val zoneType = if (radioGroup.checkedRadioButtonId == rbDanger.id) "DANGER" else "SAFE"
            val radius = when(radiusSpinner.selectedItemPosition) {
                0 -> 100f
                1 -> 250f
                else -> 500f
            }

            if (name.isNotEmpty()) {
                val isSaved = dbHelper.saveZone(name, geoPoint.latitude, geoPoint.longitude, radius, zoneType)
                if (isSaved) {
                    val newZone = CustomRiskyZone(name, geoPoint, radius)
                    userDefinedZones.add(Pair(newZone, zoneType))
                    renderZoneOnMap(newZone, zoneType)
                    map.invalidate()
                    Toast.makeText(this, "Zone locked to database permanently!", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun requestDevicePermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 200)
        } else {
            checkDeviceLocationSettings()
        }
    }

    private fun checkDeviceLocationSettings() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)

        client.checkLocationSettings(builder.build()).addOnSuccessListener {
            startSecurityMonitoring()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, 101)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Intentionally caught fallback
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSecurityMonitoring() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isSharingEnabled) return
                val currentGpsData = locationResult.lastLocation ?: return
                val currentGeoPoint = GeoPoint(currentGpsData.latitude, currentGpsData.longitude)

                if (userLocationMarker == null) {
                    map.controller.setCenter(currentGeoPoint)
                    map.controller.animateTo(currentGeoPoint)

                    userLocationMarker = Marker(map).apply {
                        position = currentGeoPoint
                        title = "Your Position"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(userLocationMarker)
                } else {
                    userLocationMarker?.position = currentGeoPoint
                }

                evaluateProximityAlerts(currentGpsData)

                val payload = mapOf(
                    "latitude" to currentGpsData.latitude,
                    "longitude" to currentGpsData.longitude,
                    "timestamp" to System.currentTimeMillis()
                )
                dbRef.child(currentUserId).setValue(payload)
                map.invalidate()
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun evaluateProximityAlerts(userLocation: Location) {
        val etContact1 = findViewById<EditText>(R.id.etContact1)
        val etContact2 = findViewById<EditText>(R.id.etContact2)
        val email1 = etContact1.text.toString().trim()
        val email2 = etContact2.text.toString().trim()

        val zonesList: List<Pair<CustomRiskyZone, String>> = userDefinedZones
        for (i in 0 until zonesList.size) {
            val item = zonesList[i]
            val zone = item.first
            val type = item.second

            val computationResult = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                zone.geoPoint.latitude, zone.geoPoint.longitude,
                computationResult
            )

            if (computationResult[0] <= zone.radiusMeters) {
                if (type == "DANGER") {
                    Toast.makeText(this, "🚨 CRITICAL BREACH: Inside Danger Zone: ${zone.name}!", Toast.LENGTH_SHORT).show()
                    if (email1.isNotEmpty() || email2.isNotEmpty()) {
                        triggerEmergencySmsPipeline(zone.name, userLocation.latitude, userLocation.longitude, email1, email2)
                    }
                } else {
                    Toast.makeText(this, "🍏 Safe Perimeter Confirmed: ${zone.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun triggerEmergencySmsPipeline(zoneName: String, lat: Double, lng: Double, email1: String, email2: String) {
        val emergencyMessage = "CRITICAL ALERT: Target has entered Unsafe Area [ $zoneName ]. Current Fix: http://maps.google.com/?q=$lat,$lng"

        val sosRef = FirebaseDatabase.getInstance().getReference("emergency_alerts").child(currentUserId)
        val alertPayload = mapOf(
            "status" to "CRITICAL_BREACH",
            "message" to emergencyMessage,
            "dangerZone" to zoneName,
            "latitude" to lat,
            "longitude" to lng,
            "notifiedContacts" to listOf(email1, email2),
            "timestamp" to System.currentTimeMillis()
        )
        sosRef.setValue(alertPayload)
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        if (isSharingEnabled) dbRef.child(currentUserId).removeValue()
    }
}
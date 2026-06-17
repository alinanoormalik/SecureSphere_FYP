package com.example.securesphere

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class RiskyShareActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val dbRef = FirebaseDatabase.getInstance().getReference("live_locations")
    private val currentUserId = "fyp_test_user"
    private val backendUrl = "http://10.0.2.2:5000/api/save-zone" // 10.0.2.2 maps directly to your laptop's localhost inside an Android Emulator

    // Uses the CustomRiskyZone data class defined globally in LocationDbHelper.kt
    private val userDefinedZones = mutableListOf<CustomRiskyZone>()
    private var userLocationMarker: Marker? = null
    private var isSharingEnabled = false
    private var isEmergencyFired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_risky_share)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        map = findViewById(R.id.mapView)

        map.setMultiTouchControls(true)
        map.controller.setZoom(17.5)

        setupMapInteractions()

        val btnToggleShare = findViewById<ToggleButton>(R.id.btnToggleShare)
        btnToggleShare.setOnCheckedChangeListener { _, isChecked ->
            isSharingEnabled = isChecked
            if (!isChecked) {
                dbRef.child(currentUserId).removeValue()
                Toast.makeText(this, "Live Tracking Terminated", Toast.LENGTH_SHORT).show()
            }
        }

        checkLocationHardwareAndPermissions()
    }

    private fun setupMapInteractions() {
        val mReceive = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

            // LONG PRESS ON MAP CRITERIA
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { showCreateZoneDialog(it) }
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(mReceive))
    }

    private fun showCreateZoneDialog(geoPoint: GeoPoint) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Flag Dangerous Zone Globally")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputName = EditText(this).apply { hint = "Zone Name (e.g., Unlit Area)" }
        layout.addView(inputName)

        val inputRadius = EditText(this).apply {
            hint = "Danger Radius in meters (e.g., 100)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(inputRadius)

        builder.setView(layout)

        builder.setPositiveButton("Save and Secure Area") { dialog, _ ->
            val name = inputName.text.toString().trim()
            val radiusStr = inputRadius.text.toString().trim()

            if (name.isNotEmpty() && radiusStr.isNotEmpty()) {
                val radius = radiusStr.toFloat()
                plotZoneOnMap(name, geoPoint, radius)
                sendZoneToBackendAPI(name, geoPoint.latitude, geoPoint.longitude, radius)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun plotZoneOnMap(name: String, geoPoint: GeoPoint, radius: Float) {
        userDefinedZones.add(CustomRiskyZone(name, geoPoint, radius))

        val marker = Marker(map).apply {
            position = geoPoint
            title = name
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marker)

        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(geoPoint, radius.toDouble())
            fillColor = Color.argb(60, 244, 67, 54)
            strokeColor = Color.RED
            strokeWidth = 3f
        }
        map.overlays.add(circle)
        map.invalidate()
        Toast.makeText(this, "Dangerous zone deployed on map surface!", Toast.LENGTH_SHORT).show()
    }

    private fun sendZoneToBackendAPI(name: String, lat: Double, lon: Double, radius: Float) {
        thread {
            try {
                val url = URL(backendUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true

                val jsonPayload = JSONObject().apply {
                    put("user_id", currentUserId)
                    put("zone_name", name)
                    put("latitude", lat)
                    put("longitude", lon)
                    put("radius_meters", radius)
                }

                OutputStreamWriter(conn.outputStream).use { os ->
                    os.write(jsonPayload.toString())
                    os.flush()
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkLocationHardwareAndPermissions() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            AlertDialog.Builder(this)
                .setTitle("GPS Hardware Required")
                .setMessage("Please turn on your system location services/GPS hardware to lock onto your coordinates.")
                .setPositiveButton("Turn On GPS") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 400)
        } else {
            getExactInitialLocationFix()
        }
    }

    // THE CORE LOCATION FIX ENGINE
    @SuppressLint("MissingPermission")
    private fun getExactInitialLocationFix() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentGeoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.setCenter(currentGeoPoint)
                updateLocationMarkerOnMap(currentGeoPoint)
                startSecurityMonitoringEngine()
            } else {
                // FORCE ENGINE FALLBACK: Solves the empty location cache issue immediately
                val activeRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMaxUpdates(1)
                    .build()

                fusedLocationClient.requestLocationUpdates(activeRequest, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val freshLocation = locationResult.lastLocation ?: return
                        val freshGeoPoint = GeoPoint(freshLocation.latitude, freshLocation.longitude)
                        map.controller.setCenter(freshGeoPoint)
                        updateLocationMarkerOnMap(freshGeoPoint)
                        startSecurityMonitoringEngine()
                    }
                }, mainLooper)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSecurityMonitoringEngine() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val currentGpsData = locationResult.lastLocation ?: return
                val currentGeoPoint = GeoPoint(currentGpsData.latitude, currentGpsData.longitude)

                updateLocationMarkerOnMap(currentGeoPoint)
                checkRiskyProximityLocally(currentGpsData)

                if (isSharingEnabled) {
                    val payload = mapOf(
                        "latitude" to currentGpsData.latitude,
                        "longitude" to currentGpsData.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                    dbRef.child(currentUserId).setValue(payload)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun updateLocationMarkerOnMap(geoPoint: GeoPoint) {
        if (userLocationMarker == null) {
            userLocationMarker = Marker(map).apply {
                position = geoPoint
                title = "Your Position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            map.overlays.add(userLocationMarker)
        } else {
            userLocationMarker?.position = geoPoint
        }
        map.invalidate()
    }

    private fun checkRiskyProximityLocally(userLocation: Location) {
        for (zone in userDefinedZones) {
            val computationResult = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                zone.geoPoint.latitude, zone.geoPoint.longitude,
                computationResult
            )

            if (computationResult[0] <= zone.radiusMeters) {
                if (!isEmergencyFired) {
                    isEmergencyFired = true
                    triggerEmergencyProtocol(zone.name)
                }
                return
            }
        }
        isEmergencyFired = false
    }

    // THE EMERGENCY PROTOCOLS AND CONTACT PHONE DIALER
    private fun triggerEmergencyProtocol(zoneTitle: String) {
        AlertDialog.Builder(this)
            .setTitle("🚨 WARNING: RISKY AREA DETECTED")
            .setMessage("You have entered a restricted threat perimeter: '$zoneTitle'. Would you like to call your emergency contact instantly?")
            .setCancelable(false)
            .setPositiveButton("CALL NOW") { _, _ ->
                val emergencyNumber = "15"
                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$emergencyNumber")
                }
                startActivity(callIntent)
            }
            .setNegativeButton("DISMISS", null)
            .show()
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == 400 && gr.isNotEmpty() && gr[0] == PackageManager.PERMISSION_GRANTED) {
            getExactInitialLocationFix()
        } else {
            Toast.makeText(this, "Location permission rejected.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        if (isSharingEnabled) dbRef.child(currentUserId).removeValue()
    }
}
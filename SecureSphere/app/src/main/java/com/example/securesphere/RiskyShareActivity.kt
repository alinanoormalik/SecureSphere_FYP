package com.example.securesphere

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

data class CustomRiskyZone(
    val name: String,
    val geoPoint: GeoPoint,
    val radiusMeters: Float
)

class RiskyShareActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val dbRef = FirebaseDatabase.getInstance().getReference("live_locations")
    private val currentUserId = "fyp_test_user"
    private var isSharingEnabled = false

    private val userDefinedZones = mutableListOf<CustomRiskyZone>()
    private var userLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRITICAL FOR OSMDROID: Configure local cache files before layout inflation
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_risky_share)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        map = findViewById(R.id.mapView)

        // Basic Map Configs
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        // Setup Long Press Event Listener to match custom risk area drawing logic
        setupMapInteractions()

        val btnToggleShare = findViewById<ToggleButton>(R.id.btnToggleShare)
        btnToggleShare.setOnCheckedChangeListener { _, isChecked ->
            isSharingEnabled = isChecked
            if (!isChecked) {
                dbRef.child(currentUserId).removeValue()
                Toast.makeText(this, "Tracking Feed Terminated", Toast.LENGTH_SHORT).show()
            }
        }

        requestDevicePermissions()
    }

    private fun setupMapInteractions() {
        val mReceive = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { showCreateZoneDialog(it) }
                return true
            }
        }
        val overlayEvents = MapEventsOverlay(mReceive)
        map.overlays.add(overlayEvents)
    }

    private fun showCreateZoneDialog(geoPoint: GeoPoint) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Define Custom Risky Zone")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputName = EditText(this).apply { hint = "Zone Name (e.g., Unlit Shortcut)" }
        layout.addView(inputName)

        val inputRadius = EditText(this).apply {
            hint = "Radius in meters (e.g., 200)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(inputRadius)

        builder.setView(layout)

        builder.setPositiveButton("Save Zone") { dialog, _ ->
            val name = inputName.text.toString().trim()
            val radiusStr = inputRadius.text.toString().trim()

            if (name.isNotEmpty() && radiusStr.isNotEmpty()) {
                val radius = radiusStr.toFloat()
                val newZone = CustomRiskyZone(name, geoPoint, radius)
                userDefinedZones.add(newZone)

                // Render Marker on OpenStreetMap
                val marker = Marker(map).apply {
                    position = geoPoint
                    title = name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)

                // Draw Circle Polygon around dangerous zone
                val circlePoints = Polygon.pointsAsCircle(geoPoint, radius.toDouble())
                val circle = Polygon().apply {
                    points = circlePoints
                    fillColor = Color.argb(60, 255, 0, 0)
                    strokeColor = Color.RED
                    strokeWidth = 2f
                }
                map.overlays.add(circle)
                map.invalidate() // Refresh map UI layout

                Toast.makeText(this, "Custom Zone Saved!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun requestDevicePermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (requiredPermissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 200)
        } else {
            startSecurityMonitoring()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSecurityMonitoring() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val currentGpsData = locationResult.lastLocation ?: return
                val currentGeoPoint = GeoPoint(currentGpsData.latitude, currentGpsData.longitude)

                // Move map focal frame smoothly to your current location
                if (userDefinedZones.isEmpty() && userLocationMarker == null) {
                    map.controller.animateTo(currentGeoPoint)
                }

                // Update or Create User Position Marker
                if (userLocationMarker == null) {
                    userLocationMarker = Marker(map).apply {
                        position = currentGeoPoint
                        title = "Your Position"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(userLocationMarker)
                } else {
                    userLocationMarker?.position = currentGeoPoint
                }

                // Mathematical Threat Evaluation Loop
                checkRiskyProximityLocally(currentGpsData)

                // Push Live Coordinates Array payload to Firebase
                if (isSharingEnabled) {
                    val payload = mapOf(
                        "latitude" to currentGpsData.latitude,
                        "longitude" to currentGpsData.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                    dbRef.child(currentUserId).setValue(payload)
                }
                map.invalidate()
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
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
                Toast.makeText(this, "⚠️ PRIVACY ALERT: Inside your custom zone: ${zone.name}!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        if (isSharingEnabled) {
            dbRef.child(currentUserId).removeValue()
        }
    }
}
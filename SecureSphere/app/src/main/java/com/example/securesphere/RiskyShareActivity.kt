package com.example.securesphere

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.Button
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
    private lateinit var sharedPreferences: SharedPreferences

    private val dbRef = FirebaseDatabase.getInstance().getReference("live_locations")
    private val currentUserId = "fyp_test_user"
    private val backendUrl = "http://10.0.2.2:5000/api/save-zone"

    private val userDefinedZones = mutableListOf<CustomRiskyZone>()
    private var userLocationMarker: Marker? = null
    private var isSharingEnabled = false
    private var isEmergencyFired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        sharedPreferences = getSharedPreferences("SecureSpherePrefs", Context.MODE_PRIVATE)

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

        // BUTTON TO MANUALLY SET/CHANGE EMERGENCY CONTACT
        val btnSetContact = findViewById<Button>(R.id.btnSetEmergencyContact)
        btnSetContact.setOnClickListener {
            showSetContactDialog()
        }

        checkLocationHardwareAndPermissions()
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

    // DIALOG TO SAVE USER'S EMERGENCY CONTACT NUMBER
    private fun showSetContactDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Emergency Contact")
        builder.setMessage("Enter the WhatsApp number (with country code, e.g., 923001234567)")

        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE
        val savedNumber = sharedPreferences.getString("emergency_no", "")
        input.setText(savedNumber)

        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val number = input.text.toString().trim().replace("+", "").replace(" ", "")
            if (number.isNotEmpty()) {
                sharedPreferences.edit().putString("emergency_no", number).apply()
                Toast.makeText(this, "Emergency contact saved!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showCreateZoneDialog(geoPoint: GeoPoint) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Flag Dangerous Zone Globally")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val inputName = EditText(this).apply { hint = "Zone Name" }
        layout.addView(inputName)
        val inputRadius = EditText(this).apply {
            hint = "Danger Radius (meters)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(inputRadius)
        builder.setView(layout)
        builder.setPositiveButton("Save") { _, _ ->
            val name = inputName.text.toString().trim()
            val radiusStr = inputRadius.text.toString().trim()
            if (name.isNotEmpty() && radiusStr.isNotEmpty()) {
                val radius = radiusStr.toFloat()
                plotZoneOnMap(name, geoPoint, radius)
                sendZoneToBackendAPI(name, geoPoint.latitude, geoPoint.longitude, radius)
            }
        }
        builder.setNegativeButton("Cancel", null)
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
                    put("user_id", currentUserId); put("zone_name", name)
                    put("latitude", lat); put("longitude", lon); put("radius_meters", radius)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()); it.flush() }
                conn.responseCode; conn.disconnect()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun checkLocationHardwareAndPermissions() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this).setTitle("GPS Required").setMessage("Please turn on GPS.")
                .setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .show()
            return
        }
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 400)
        } else {
            showHowToUseManual()
        }
    }

    // NEW PIECE OF CODE: DISPLAYS A 3-STEP USER MANUAL
    private fun showHowToUseManual() {
        val manualMessage = """
            1. SET EMERGENCY CONTACT: Click the 'Set Emergency Contact' button at the top to save your trusted WhatsApp number.
            
            2. FLAG RISKY ZONES: Long-press anywhere on the map to define your dangerous/risky area.
            
            3. ACTIVATE LIVE MONITORING: Flip the 'LIVE OFF' switch to 'LIVE ON' to stream your location. If you cross into any flagged danger zone, an automatic SOS alert system triggers instantly.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📖 Quick Setup Guide")
            .setMessage(manualMessage)
            .setCancelable(false)
            .setPositiveButton("Got It!") { _, _ ->
                getExactInitialLocationFix()
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getExactInitialLocationFix() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentGeoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.setCenter(currentGeoPoint)
                updateLocationMarkerOnMap(currentGeoPoint)
                startSecurityMonitoringEngine()
            } else {
                val activeRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMaxUpdates(1).build()
                fusedLocationClient.requestLocationUpdates(activeRequest, object : LocationCallback() {
                    override fun onLocationResult(lr: LocationResult) {
                        lr.lastLocation?.let {
                            val gp = GeoPoint(it.latitude, it.longitude)
                            map.controller.setCenter(gp)
                            updateLocationMarkerOnMap(gp)
                            startSecurityMonitoringEngine()
                        }
                    }
                }, mainLooper)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSecurityMonitoringEngine() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                val gps = lr.lastLocation ?: return
                val gp = GeoPoint(gps.latitude, gps.longitude)
                updateLocationMarkerOnMap(gp)
                checkRiskyProximityLocally(gps)
                if (isSharingEnabled) {
                    dbRef.child(currentUserId).setValue(mapOf("latitude" to gps.latitude, "longitude" to gps.longitude, "timestamp" to System.currentTimeMillis()))
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun updateLocationMarkerOnMap(geoPoint: GeoPoint) {
        if (userLocationMarker == null) {
            userLocationMarker = Marker(map).apply { position = geoPoint; title = "Your Position" }
            map.overlays.add(userLocationMarker)
        } else { userLocationMarker?.position = geoPoint }
        map.invalidate()
    }

    private fun checkRiskyProximityLocally(userLocation: Location) {
        for (zone in userDefinedZones) {
            val res = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, zone.geoPoint.latitude, zone.geoPoint.longitude, res)
            if (res[0] <= zone.radiusMeters) {
                if (!isEmergencyFired) {
                    isEmergencyFired = true
                    triggerEmergencyProtocol(zone.name)
                }
                return
            }
        }
        isEmergencyFired = false
    }

    // UPDATED EMERGENCY PROTOCOL WITH WHATSAPP OPTION
    private fun triggerEmergencyProtocol(zoneTitle: String) {
        val lastPos = userLocationMarker?.position

        AlertDialog.Builder(this)
            .setTitle("🚨 WARNING: RISKY AREA")
            .setMessage("Entered: '$zoneTitle'. Choose emergency action:")
            .setCancelable(false)
            .setPositiveButton("CALL 15") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:15") })
            }
            .setNeutralButton("WHATSAPP ALERT") { _, _ ->
                if (lastPos != null) {
                    sendWhatsAppAlert(zoneTitle, lastPos.latitude, lastPos.longitude)
                }
            }
            .setNegativeButton("DISMISS", null)
            .show()
    }

    private fun sendWhatsAppAlert(zoneName: String, lat: Double, lon: Double) {
        val number = sharedPreferences.getString("emergency_no", "")

        if (number.isNullOrEmpty()) {
            Toast.makeText(this, "Please set an emergency contact first!", Toast.LENGTH_LONG).show()
            showSetContactDialog()
            return
        }

        val mapLink = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
        val message = "🚨 EMERGENCY: I've entered a risky area: $zoneName. My location: $mapLink"

        try {
            val url = "https://api.whatsapp.com/send?phone=$number&text=${Uri.encode(message)}"
            startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == 400 && gr.isNotEmpty() && gr[0] == PackageManager.PERMISSION_GRANTED) {
            showHowToUseManual()
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
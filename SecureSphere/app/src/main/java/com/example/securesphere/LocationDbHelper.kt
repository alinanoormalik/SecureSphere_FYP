package com.example.securesphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.osmdroid.util.GeoPoint

data class CustomRiskyZone(
    val name: String,
    val geoPoint: GeoPoint,
    val radiusMeters: Float
)

class LocationDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "SecureSphereLoc.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_ZONES = "risky_zones"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_LAT = "latitude"
        const val COL_LNG = "longitude"
        const val COL_RADIUS = "radius"
        const val COL_TYPE = "type"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE $TABLE_ZONES ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_NAME TEXT,"
                + "$COL_LAT REAL,"
                + "$COL_LNG REAL,"
                + "$COL_RADIUS REAL,"
                + "$COL_TYPE TEXT)")
        db?.execSQL(createTable)
    }

    // Fixed: Renamed from upgrade to onUpgrade to correctly override the abstract base class member
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ZONES")
        onCreate(db)
    }

    fun saveZone(name: String, lat: Double, lng: Double, radius: Float, type: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_RADIUS, radius)
            put(COL_TYPE, type)
        }
        val result = db.insert(TABLE_ZONES, null, values)
        db.close()
        return result != -1L
    }

    fun getAllZones(): List<Pair<CustomRiskyZone, String>> {
        val list = mutableListOf<Pair<CustomRiskyZone, String>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ZONES", null)

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAT))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LNG))
                val radius = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_RADIUS))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))

                val zone = CustomRiskyZone(name, GeoPoint(lat, lng), radius)
                list.add(Pair(zone, type))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}
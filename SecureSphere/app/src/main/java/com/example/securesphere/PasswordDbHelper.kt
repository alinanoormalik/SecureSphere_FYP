package com.example.securesphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PasswordDbHelper(context: Context) : SQLiteOpenHelper(context, "SecureVault.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Creates the table
        val createTable = "CREATE TABLE passwords (id INTEGER PRIMARY KEY, title TEXT, username TEXT, encrypted_pass TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS passwords")
        onCreate(db)
    }

    fun addPassword(title: String, user: String, pass: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()

        // ENCRYPT DATA BEFORE SAVING
        val encryptedPass = EncryptionHelper.encrypt(pass)

        cv.put("title", title)
        cv.put("username", user)
        cv.put("encrypted_pass", encryptedPass)

        val result = db.insert("passwords", null, cv)
        return result != -1L
    }

    fun getAllPasswords(): ArrayList<String> {
        val list = ArrayList<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM passwords", null)

        if (cursor.moveToFirst()) {
            do {
                val title = cursor.getString(1)
                val user = cursor.getString(2)
                val encPass = cursor.getString(3)

                // DECRYPT DATA WHEN READING
                val realPass = EncryptionHelper.decrypt(encPass)

                list.add("Site: $title\nUser: $user\nPass: $realPass")
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
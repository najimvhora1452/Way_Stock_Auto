package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class WayStockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("waystock-inventory-app")
                    .setApplicationId("com.aistudio.waystock.invapp")
                    .setApiKey("AIzaSyDummyKeyForWayStockInventorySync")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("WayStockApp", "FirebaseApp initialized successfully")
            }
        } catch (e: Exception) {
            Log.w("WayStockApp", "FirebaseApp init note: ${e.message}")
        }
    }
}

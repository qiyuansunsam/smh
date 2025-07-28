package com.hairstyle.app

import android.app.Application

class HairStyleApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global components here
        initializeApp()
    }
    
    private fun initializeApp() {
        // Initialize any libraries or global settings
        // For example: Glide configuration, crash reporting, etc.
    }
}
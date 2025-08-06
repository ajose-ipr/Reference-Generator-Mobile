package com.ipr.reference_generator

import android.app.Application
import com.google.firebase.FirebaseApp

class ReferenceGeneratorApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}
//activities/SplashActvity
package com.ipr.reference_generator.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ipr.reference_generator.MainActivity
import com.ipr.reference_generator.R
import androidx.lifecycle.lifecycleScope
import com.ipr.reference_generator.network.FirebaseRepository
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        repository = FirebaseRepository.getInstance(this)

        // Initialize default data and check auth
        lifecycleScope.launch {
            repository.initializeDefaultData()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 2000)
    }

    private fun checkAuthAndNavigate() {
        if (repository.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}


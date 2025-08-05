//MainActivity.kt
package com.ipr.reference_generator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ipr.reference_generator.R
import com.ipr.reference_generator.activities.LoginActivity
import com.ipr.reference_generator.fragments.EntriesFragment
import com.ipr.reference_generator.fragments.HomeFragment
import com.ipr.reference_generator.fragments.SettingsFragment
import com.ipr.reference_generator.network.Repository

class MainActivity : AppCompatActivity() {

    private lateinit var repository: Repository
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = Repository.getInstance(this)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupNavigation()
        observeAuthState()

        // Load initial fragment
        loadFragment(HomeFragment())
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
            R.id.nav_home -> loadFragment(HomeFragment())
            R.id.nav_entries -> loadFragment(EntriesFragment())
            R.id.nav_settings -> loadFragment(SettingsFragment())
                else -> false
        }
        }

        // Hide settings for non-admin users
        repository.currentUser.observe(this) { user ->
                bottomNavigation.menu.findItem(R.id.nav_settings).isVisible = user?.role == "admin"
        }
    }

    private fun observeAuthState() {
        repository.isLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        return true
    }
}

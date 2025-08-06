package com.ipr.reference_generator
//MainActivity.kt

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
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ipr.reference_generator.databinding.ActivityMainBinding
import com.ipr.reference_generator.network.FirebaseRepository
import com.ipr.reference_generator.utils.AppUtils

class MainActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DISABLE edge-to-edge for now to show ActionBar properly
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Ensure ActionBar is visible
        supportActionBar?.show()
        supportActionBar?.title = "IPR Reference Generator"

        repository = FirebaseRepository.getInstance(this)
        bottomNavigation = binding.bottomNavigation

        setupNavigation()
        observeAuthState()

        // Load initial fragment
        loadFragment(HomeFragment())
    }

    private fun setupWindowInsets() {
        // Handle system window insets for the fragment container
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            insets
        }

        // Handle insets for bottom navigation
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportActionBar?.title = "Home"
                    loadFragment(HomeFragment())
                }
                R.id.nav_entries -> {
                    supportActionBar?.title = "Entries"
                    loadFragment(EntriesFragment())
                }
                R.id.nav_settings -> {
                    supportActionBar?.title = "Settings"
                    loadFragment(SettingsFragment())
                }
                else -> false
            }
        }

        // Hide settings for non-admin users
        repository.currentUser.observe(this) { user ->
            bottomNavigation.menu.findItem(R.id.nav_settings).isVisible = user?.role == "admin"
            if (user != null) {
                supportActionBar?.subtitle = "Welcome, ${user.username}"
            }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            R.id.action_profile -> {
                showUserProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                repository.logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserProfile() {
        val user = repository.getCurrentUser()
        if (user != null) {
            AlertDialog.Builder(this)
                .setTitle("User Profile")
                .setMessage("Username: ${user.username}\nEmail: ${user.email}\nRole: ${user.role}\nJoined: ${AppUtils.formatDate(user.createdAt)}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}

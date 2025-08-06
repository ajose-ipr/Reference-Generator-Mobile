package com.ipr.reference_generator.activities
//activities/LoginActivity.kt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.ipr.reference_generator.MainActivity
import com.ipr.reference_generator.R
import com.ipr.reference_generator.network.FirebaseRepository
import com.ipr.reference_generator.utils.AppUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        repository = FirebaseRepository.getInstance(this)

        initViews()
        setupClickListeners()
        observeAuthState()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener { validateAndLogin() }
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeAuthState() {
        repository.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        repository.authError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateAndLogin() {
        val emailOrUsername = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Update validation
        if (emailOrUsername.isEmpty()) {
            tilEmail.error = "Email or username is required"
            return
        }

        val passwordError = AppUtils.validateInput(password, "password")
        if (passwordError != null) {
            tilPassword.error = passwordError
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            repository.loginWithEmail(emailOrUsername, password) // Same method name, updated logic
                .onSuccess { user ->
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, "Welcome back, ${user.username}!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, error.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnRegister.isEnabled = !loading
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading

        // Optional: Change button text when loading
        btnLogin.text = if (loading) getString(R.string.loading) else getString(R.string.login)
    }
}
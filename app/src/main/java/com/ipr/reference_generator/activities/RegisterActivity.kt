// activities/RegisterActivity.kt
package com.ipr.reference_generator.activities

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
import com.ipr.reference_generator.network.Repository
import com.ipr.reference_generator.utils.AppUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var repository: Repository
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        repository = Repository.getInstance(this)

        initViews()
        setupClickListeners()
        observeAuthState()
        setupPasswordValidation()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister) // Fixed: was btnLogin
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { validateAndRegister() }
        btnBackToLogin.setOnClickListener {
            finish() // Go back to LoginActivity
        }
    }

    private fun observeAuthState() {
        repository.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun setupPasswordValidation() {
        // Real-time password confirmation validation
        etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePasswordMatch()
            }
        }
    }

    private fun validatePasswordMatch() {
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
        } else {
            tilConfirmPassword.error = null
        }
    }

    private fun validateAndRegister() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Clear previous errors
        tilUsername.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        // Validate inputs
        val usernameError = AppUtils.validateInput(username, "username")
        val passwordError = AppUtils.validateInput(password, "password")

        if (usernameError != null) {
            tilUsername.error = usernameError
        }

        if (passwordError != null) {
            tilPassword.error = passwordError
        }

        if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
        }

        // Check if there are any validation errors
        if (tilUsername.error != null || tilPassword.error != null || tilConfirmPassword.error != null) {
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            repository.register(username, password)
                .onSuccess {
                    setLoading(false)
                    Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                    // Navigation will be handled by observeAuthState()
                }
                .onFailure { exception: Throwable ->
                    setLoading(false)
                    Toast.makeText(this@RegisterActivity, exception.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        btnBackToLogin.isEnabled = !loading

        // Disable input fields during loading
        etUsername.isEnabled = !loading
        etPassword.isEnabled = !loading
        etConfirmPassword.isEnabled = !loading
    }
}

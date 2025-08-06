package com.ipr.reference_generator.activities
// activities/RegisterActivity.kt

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

class RegisterActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        repository = FirebaseRepository.getInstance(this)

        initViews()
        setupClickListeners()
        observeAuthState()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tilUsername = findViewById(R.id.tilUsername)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { validateAndRegister() }
        btnBackToLogin.setOnClickListener { finish() }
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

    private fun validateAndRegister() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Clear previous errors
        tilUsername.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        // Validate inputs
        val usernameError = AppUtils.validateInput(username, "username")
        val emailError = AppUtils.validateInput(email, "email")
        val passwordError = AppUtils.validateInput(password, "password")

        if (usernameError != null) {
            tilUsername.error = usernameError
            return
        }

        if (emailError != null) {
            tilEmail.error = emailError
            return
        }

        if (passwordError != null) {
            tilPassword.error = passwordError
            return
        }

        if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            repository.registerWithEmail(username, email, password)
                .onSuccess { user ->
                    setLoading(false)
                    Toast.makeText(this@RegisterActivity, "Welcome, ${user.username}!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    setLoading(false)
                    Toast.makeText(this@RegisterActivity, error.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        btnBackToLogin.isEnabled = !loading
        etUsername.isEnabled = !loading
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading
        etConfirmPassword.isEnabled = !loading
    }
}

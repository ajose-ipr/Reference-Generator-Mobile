//activities/LoginActivity.kt
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

class LoginActivity : AppCompatActivity() {

    private lateinit var repository: Repository
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        repository = Repository.getInstance(this)

        initViews()
        setupClickListeners()
        observeAuthState()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tilUsername = findViewById(R.id.tilUsername)
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
    }

    private fun validateAndLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tilUsername.error = AppUtils.validateInput(username, "username")
        tilPassword.error = AppUtils.validateInput(password, "password")

        if (tilUsername.error != null || tilPassword.error != null) return

        setLoading(true)

        lifecycleScope.launch {
            repository.login(username, password)
                .onSuccess {
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { exception: Throwable ->
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, exception.message ?: "Login failed", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnRegister.isEnabled = !loading
    }
}
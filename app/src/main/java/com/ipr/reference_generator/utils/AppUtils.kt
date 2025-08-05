// utils/AppUtils.kt
package com.ipr.reference_generator.utils
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore


object AppUtils {
    const val BASE_URL = "http://your-backend-url.com/api/"
    const val PREFS_NAME = "reference_generator_prefs"
    const val TOKEN_KEY = "auth_token"

    // Mappings
    val PARTICULARS_MAPPING = mapOf(
        "TC" to "Type Check",
        "GC" to "Grid Connection",
        "PQM" to "Power Quality Monitor",
        "EVF" to "Emergency Verification",
        "OPT" to "Optimization"
    )

    val CLIENT_CODE_MAPPING = mapOf(
        "HFEX" to "Haryana Electricity Exchange",
        "ADN" to "Adani Power",
        "HEXA" to "Hexagon Energy"
    )

    val SITE_NAME_MAPPING = mapOf(
        "SJPR" to "SARJAPUR",
        "BNSK" to "BANSHANKARI",
        "GRID" to "Grid Station"
    )

    fun getFullForm(code: String, type: String): String {
        return when (type) {
            "PARTICULARS" -> PARTICULARS_MAPPING[code] ?: code
            "CLIENT_CODE" -> CLIENT_CODE_MAPPING[code] ?: code
            "SITE_NAME" -> SITE_NAME_MAPPING[code] ?: code
            else -> code
        }
    }

    fun validateInput(value: String, type: String): String? {
        return when (type) {
            "username" -> when {
                value.isBlank() -> "Username is required"
                value.length < 3 -> "Username must be at least 3 characters"
                else -> null
            }
            "password" -> when {
                value.isBlank() -> "Password is required"
                value.length < 6 -> "Password must be at least 6 characters"
                else -> null
            }
            "particulars" -> if (value.isBlank()) "Particulars is required" else null
            "client_code" -> when {
                value.isBlank() -> "Client code is required"
                value.length !in 2..4 -> "Client code must be 2-4 characters"
                else -> null
            }
            "capacity" -> when {
                value.isBlank() -> "Capacity is required"
                value.toDoubleOrNull()?.let { it <= 0 } == true -> "Capacity must be greater than 0"
                value.toDoubleOrNull() == null -> "Invalid capacity"
                else -> null
            }
            else -> null
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun isCurrentMonth(timestamp: Long): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        calendar.timeInMillis = timestamp
        return currentMonth == calendar.get(java.util.Calendar.MONTH) &&
                currentYear == calendar.get(java.util.Calendar.YEAR)
    }
}
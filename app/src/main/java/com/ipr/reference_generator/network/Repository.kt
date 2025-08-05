// network/Repository.kt
package com.ipr.reference_generator.network

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ipr.reference_generator.models.AuthResponse
import com.ipr.reference_generator.models.DashboardStats
import com.ipr.reference_generator.models.DropdownOption
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.models.User
import com.ipr.reference_generator.utils.AppUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.firebase.auth.FirebaseAuth              // For accessing FirebaseAuth directly
import com.google.firebase.auth.ktx.auth                 // For Firebase.auth (KTX extension)
import com.google.firebase.ktx.Firebase                  // Needed for Firebase.auth and other KTX features
import com.google.firebase.firestore.ktx.firestore    // ✅ For Firebase.firestore (KTX extension)



class Repository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: Repository? = null

        fun getInstance(context: Context): Repository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(AppUtils.PREFS_NAME, Context.MODE_PRIVATE)
    private val apiService: ApiService

    init {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val token = getToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(AppUtils.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    // Auth methods
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun saveToken(token: String) {
        prefs.edit().putString(AppUtils.TOKEN_KEY, token).apply()
    }

    fun getToken(): String? = prefs.getString(AppUtils.TOKEN_KEY, null)

    fun saveUser(user: User) {
        val gson = com.google.gson.Gson()
        prefs.edit().putString("user_data", gson.toJson(user)).apply()
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun getUser(): User? {
        val gson = com.google.gson.Gson()
        val userJson = prefs.getString("user_data", null)
        return userJson?.let { gson.fromJson(it, User::class.java) }
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    fun checkAuthState() {
        val user = getUser()
        _currentUser.value = user
        _isLoggedIn.value = user != null
    }

    // API calls
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(mapOf("username" to username, "password" to password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveToken(authResponse.token)
                authResponse.user?.let { saveUser(it) }
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(mapOf("username" to username, "password" to password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveToken(authResponse.token)
                authResponse.user?.let { saveUser(it) }
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEntries(search: String = ""): Result<List<Entry>> {
        return try {
            val response = apiService.getEntries(search = search)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch entries"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEntry(entry: Map<String, Any>): Result<Entry> {
        return try {
            val response = apiService.createEntry(entry)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to create entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            val response = apiService.getDashboardStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch stats"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDropdownOptions(type: String): Result<List<DropdownOption>> {
        return try {
            val response = apiService.getDropdownOptions(type)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch dropdown options"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addCustomDropdownOption(type: String, value: String, callback: (Boolean, String?) -> Unit) {
        // Launch coroutine for API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create request body
                val requestBody = mapOf(
                    "type" to type,
                    "value" to value.uppercase().trim(),
                    "isCustom" to true
                )

                val response = apiService.addDropdownOption(requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success == true) {
                            callback(true, null)
                        } else {
                            callback(false, apiResponse.message ?: "Failed to add custom option")
                        }
                    } else {
                        // Handle HTTP error responses
                        val errorMessage = when (response.code()) {
                            400 -> "Invalid option data"
                            409 -> "Option already exists"
                            422 -> "Validation failed"
                            403 -> "Permission denied"
                            500 -> "Server error"
                            else -> "Failed to add custom option"
                        }
                        callback(false, errorMessage)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when (e) {
                        is java.net.UnknownHostException -> "No internet connection"
                        is java.net.SocketTimeoutException -> "Connection timeout"
                        is java.net.ConnectException -> "Could not connect to server"
                        else -> "Network error: ${e.message}"
                    }
                    callback(false, errorMessage)
                }
            }
        }
    }

}

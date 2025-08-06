package com.ipr.reference_generator.network

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.tasks.await
import java.util.*
import com.ipr.reference_generator.models.DropdownOption
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.models.EntryRequest
import com.ipr.reference_generator.models.User
import com.ipr.reference_generator.utils.AppUtils
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
class FirebaseRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FirebaseRepository? = null
        private const val TAG = "FirebaseRepository"

        fun getInstance(context: Context): FirebaseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Collections
    private val usersCollection = firestore.collection("users")
    private val entriesCollection = firestore.collection("entries")
    private val dropdownOptionsCollection = firestore.collection("dropdown_options")
    private val countersCollection = firestore.collection("counters")

    // LiveData
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _authError = MutableLiveData<String?>()
    val authError: LiveData<String?> = _authError

    init {
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                GlobalScope.launch {
                    val user = fetchUserProfile(firebaseUser.uid)
                    withContext(Dispatchers.Main) {
                        if (user != null) {
                            _currentUser.value = user
                            _isLoggedIn.value = true
                        } else {
                            _currentUser.value = null
                            _isLoggedIn.value = false
                        }
                    }
                }
            } else {
                _currentUser.value = null
                _isLoggedIn.value = false
            }
        }
    }

    // ========== Authentication Methods ==========

    suspend fun loginWithEmail(emailOrUsername: String, password: String): Result<User> {
        return try {
            val email = if (emailOrUsername.contains("@")) {
                emailOrUsername
            } else {
                val userQuery = usersCollection.whereEqualTo("username", emailOrUsername).get().await()
                if (userQuery.documents.isEmpty()) {
                    return Result.failure(Exception("Username not found"))
                }
                userQuery.documents[0].getString("email") ?: return Result.failure(Exception("Email not found"))
            }

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                var user = fetchUserProfile(firebaseUser.uid)

                // If user exists but role needs updating (for existing admin)
                if (user != null && email.equals("alphonsajose145@gmail.com", ignoreCase = true) && user.role != "admin") {
                    // Update role to admin
                    usersCollection.document(firebaseUser.uid).update("role", "admin").await()
                    user = user.copy(role = "admin")
                    Log.d(TAG, "Updated existing user to admin role")
                }

                if (user != null) {
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    Log.d(TAG, "User logged in: ${user.email}, role: ${user.role}")
                    Result.success(user)
                } else {
                    Result.failure(Exception("User profile not found"))
                }
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            _authError.value = getAuthErrorMessage(e)
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(username: String, email: String, password: String): Result<User> {
        return try {
            val usernameExists = checkUsernameExists(username)
            if (usernameExists) {
                return Result.failure(Exception("Username already exists"))
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Check if this is the admin email
                val role = if (email.equals("alphonsajose145@gmail.com", ignoreCase = true)) {
                    "admin"
                } else {
                    "user"
                }

                val user = User(
                    uid = firebaseUser.uid,
                    username = username,
                    email = email,
                    role = role,
                    createdAt = Date(),
                    isActive = true
                )

                usersCollection.document(firebaseUser.uid).set(user).await()

                _currentUser.value = user
                _isLoggedIn.value = true

                Log.d(TAG, "User registered with email: $email, role: $role")
                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            _authError.value = getAuthErrorMessage(e)
            Result.failure(e)
        }
    }

    private suspend fun checkUsernameExists(username: String): Boolean {
        return try {
            val query = usersCollection.whereEqualTo("username", username).get().await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun fetchUserProfile(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject<User>()
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _isLoggedIn.value = false
        _authError.value = null
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser
    fun getCurrentUser(): User? = _currentUser.value
    fun isUserLoggedIn(): Boolean = _isLoggedIn.value == true
    fun isAdmin(): Boolean = getCurrentUser()?.role == "admin"

    // ========== Entry Management ==========

    suspend fun createEntry(request: EntryRequest): Result<Entry> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))

            // Generate next serial number
            val nextSlNo = getNextSerialNumber()

            // Generate reference code
            val referenceCode = generateReferenceCode(request, nextSlNo)

            // Create entry with explicit field mapping
            val entryData = hashMapOf(
                "SL_NO" to nextSlNo,
                "USER_NAME" to currentUser.username,
                "PARTICULARS" to request.particulars,
                "CLIENT_CODE" to request.clientCode,
                "CAPACITY_MW" to request.capacityMW,
                "STATE_NAME" to request.stateName,
                "SITE_NAME" to request.siteName,
                "REFERENCE_CODE" to referenceCode,
                "CREATED_BY" to currentUser.username,
                "CREATED_AT" to Date(),
                "MODIFIED_BY" to null,
                "MODIFIED_AT" to null,
                "isActive" to true
            )

            val docRef = entriesCollection.add(entryData).await()

            // Update with document ID
            entriesCollection.document(docRef.id).update("id", docRef.id).await()

            // Create Entry object for return
            val entry = Entry(
                id = docRef.id,
                slNo = nextSlNo,
                userName = currentUser.username,
                particulars = request.particulars,
                clientCode = request.clientCode,
                capacityMW = request.capacityMW,
                stateName = request.stateName,
                siteName = request.siteName,
                referenceCode = referenceCode,
                createdBy = currentUser.username,
                createdAt = Date(),
                isActive = true
            )

            Log.d(TAG, "Entry created successfully: ${entry.referenceCode}")
            Result.success(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating entry", e)
            Result.failure(e)
        }
    }

    suspend fun updateEntry(entryId: String, request: EntryRequest): Result<Entry> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))

            // Only admins can edit entries
            if (!isAdmin()) {
                return Result.failure(Exception("Only admins can edit entries"))
            }

            val entry = entriesCollection.document(entryId).get().await().toObject<Entry>()
            if (entry == null) {
                return Result.failure(Exception("Entry not found"))
            }

            val updates = hashMapOf<String, Any>(
                "PARTICULARS" to request.particulars,
                "CLIENT_CODE" to request.clientCode,
                "CAPACITY_MW" to request.capacityMW,
                "STATE_NAME" to request.stateName,
                "SITE_NAME" to request.siteName,
                "MODIFIED_BY" to currentUser.username,
                "MODIFIED_AT" to Date()
            )

            entriesCollection.document(entryId).update(updates).await()

            val updatedDoc = entriesCollection.document(entryId).get().await()
            val updatedEntry = updatedDoc.toObject<Entry>()

            if (updatedEntry != null) {
                Result.success(updatedEntry)
            } else {
                Result.failure(Exception("Failed to fetch updated entry"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating entry", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(entryId: String): Result<Boolean> {
        return try {
            // Check if user is admin
            if (!isAdmin()) {
                return Result.failure(Exception("Only admins can delete entries"))
            }

            entriesCollection.document(entryId).update("isActive", false).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIXED: Improved getEntries method with better error handling and logging
    suspend fun getEntries(searchQuery: String = ""): Result<List<Entry>> {
        return try {
            Log.d(TAG, "Getting entries with search query: '$searchQuery'")

            // Simple query first
            val snapshot = entriesCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()

            Log.d(TAG, "Found ${snapshot.documents.size} documents")

            val entries = mutableListOf<Entry>()

            for (document in snapshot.documents) {
                try {
                    Log.d(TAG, "Processing document: ${document.id}")
                    Log.d(TAG, "Document data: ${document.data}")

                    val entry = document.toObject<Entry>()
                    if (entry != null) {
                        // Ensure ID is set
                        val entryWithId = entry.copy(id = document.id)
                        entries.add(entryWithId)
                        Log.d(TAG, "Successfully converted entry: ${entry.referenceCode}")
                    } else {
                        Log.w(TAG, "Failed to convert document ${document.id} to Entry object")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing document ${document.id}", e)
                }
            }

            // Sort by serial number (descending - newest first)
            val sortedEntries = entries.sortedByDescending { it.slNo }

            // Apply search filter if provided
            val filteredEntries = if (searchQuery.trim().isNotEmpty()) {
                sortedEntries.filter { entry ->
                    entry.referenceCode.contains(searchQuery, ignoreCase = true) ||
                            entry.particulars.contains(searchQuery, ignoreCase = true) ||
                            entry.clientCode.contains(searchQuery, ignoreCase = true) ||
                            entry.createdBy.contains(searchQuery, ignoreCase = true)
                }
            } else {
                sortedEntries
            }

            Log.d(TAG, "Returning ${filteredEntries.size} entries after filtering")
            Result.success(filteredEntries)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting entries", e)
            Result.failure(e)
        }
    }

    suspend fun getUserEntries(username: String): Result<List<Entry>> {
        return try {
            val snapshot = entriesCollection
                .whereEqualTo("isActive", true)
                .whereEqualTo("CREATED_BY", username)
                .get()
                .await()

            val entries = snapshot.toObjects<Entry>()
                .sortedByDescending { it.slNo }

            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user entries", e)
            Result.failure(e)
        }
    }

    // ========== Dropdown Options ==========

    suspend fun getDropdownOptions(type: String): Result<List<DropdownOption>> {
        return try {
            val snapshot = dropdownOptionsCollection
                .whereEqualTo("type", type)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val options = snapshot.toObjects<DropdownOption>()
            Result.success(options)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomDropdownOption(type: String, value: String, displayName: String): Result<DropdownOption> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))

            val option = DropdownOption(
                id = "",
                type = type,
                value = value.uppercase(),
                displayName = displayName,
                isActive = true,
                isCustom = true,
                createdAt = Date(),
                createdBy = currentUser.username
            )

            val docRef = dropdownOptionsCollection.add(option).await()
            val savedOption = option.copy(id = docRef.id)

            docRef.update("id", docRef.id).await()

            Result.success(savedOption)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Helper Methods ==========

    private suspend fun getNextSerialNumber(): Int {
        return try {
            val counterDoc = countersCollection.document("entries")
            val snapshot = counterDoc.get().await()

            if (snapshot.exists()) {
                val currentCount = snapshot.getLong("count")?.toInt() ?: 0
                val nextCount = currentCount + 1
                counterDoc.update("count", nextCount).await()
                nextCount
            } else {
                counterDoc.set(mapOf("count" to 1)).await()
                1
            }
        } catch (e: Exception) {
            // Fallback: count existing entries + 1
            val entries = entriesCollection.get().await()
            entries.size() + 1
        }
    }

    private fun generateReferenceCode(request: EntryRequest, slNo: Int): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString().takeLast(2)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)

        val cumulativeCount = String.format("%04d", slNo)
        val incrementalCount = String.format("%02d", slNo % 100)

        return "IPR/${request.particulars}/${request.clientCode}/${request.capacityMW.toInt()}MW/${request.stateName}/${request.siteName}/$year$month/$incrementalCount"
    }

    private fun getAuthErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("INVALID_EMAIL") == true -> "Invalid email address"
            exception.message?.contains("WEAK_PASSWORD") == true -> "Password is too weak"
            exception.message?.contains("EMAIL_ALREADY_IN_USE") == true -> "Email is already registered"
            exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Invalid email or password"
            exception.message?.contains("USER_NOT_FOUND") == true -> "No account found with this email"
            exception.message?.contains("WRONG_PASSWORD") == true -> "Incorrect password"
            exception.message?.contains("USER_DISABLED") == true -> "This account has been disabled"
            exception.message?.contains("TOO_MANY_REQUESTS") == true -> "Too many failed attempts. Please try again later"
            exception.message?.contains("NETWORK_ERROR") == true -> "Network error. Please check your connection"
            else -> exception.message ?: "Authentication failed"
        }
    }

    // ========== Data Initialization ==========

    suspend fun initializeDefaultData() {
        try {
            val snapshot = dropdownOptionsCollection.limit(1).get().await()
            if (!snapshot.isEmpty) return

            val defaultOptions = listOf(
                // Particulars
                DropdownOption("", "PARTICULARS", "TC", "Type Check", true, false, Date(), "system"),
                DropdownOption("", "PARTICULARS", "GC", "Grid Connection", true, false, Date(), "system"),
                DropdownOption("", "PARTICULARS", "PQM", "Power Quality Monitor", true, false, Date(), "system"),
                DropdownOption("", "PARTICULARS", "EVF", "Emergency Verification", true, false, Date(), "system"),
                DropdownOption("", "PARTICULARS", "OPT", "Optimization", true, false, Date(), "system"),

                // Client Codes
                DropdownOption("", "CLIENT_CODE", "HFEX", "Haryana Electricity Exchange", true, false, Date(), "system"),
                DropdownOption("", "CLIENT_CODE", "ADN", "Adani Power", true, false, Date(), "system"),
                DropdownOption("", "CLIENT_CODE", "HEXA", "Hexagon Energy", true, false, Date(), "system"),
                DropdownOption("", "CLIENT_CODE", "GE", "General Electric", true, false, Date(), "system"),

                // Site Names
                DropdownOption("", "SITE_NAME", "SJPR", "SARJAPUR", true, false, Date(), "system"),
                DropdownOption("", "SITE_NAME", "BNSK", "BANSHANKARI", true, false, Date(), "system"),
                DropdownOption("", "SITE_NAME", "GRID", "Grid Station", true, false, Date(), "system"),

                // State Names
                DropdownOption("", "STATE_NAME", "KA", "Karnataka", true, false, Date(), "system"),
                DropdownOption("", "STATE_NAME", "TN", "Tamil Nadu", true, false, Date(), "system"),
                DropdownOption("", "STATE_NAME", "AP", "Andhra Pradesh", true, false, Date(), "system"),
                DropdownOption("", "STATE_NAME", "TS", "Telangana", true, false, Date(), "system")
            )

            for (option in defaultOptions) {
                val docRef = dropdownOptionsCollection.add(option).await()
                docRef.update("id", docRef.id).await()
            }

            countersCollection.document("entries").set(mapOf("count" to 0)).await()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing default data", e)
        }
    }
}
// models/Models.kt
package com.ipr.reference_generator.models

data class User(
    val id: String = "",
    val username: String = "",
    val role: String = "user", // user or admin
    val createdAt: Long = System.currentTimeMillis()
)

data class AuthResponse(
    val success: Boolean = true,
    val token: String = "",
    val user: User? = null,
    val message: String = ""
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String
)

// ========== Entry Models ==========
data class Entry(
    val _id: String = "",
    val SL_NO: Int = 0,
    val USER_NAME: String = "",
    val PARTICULARS: String = "",
    val CLIENT_CODE: String = "",
    val CAPACITY_MW: Double = 0.0,
    val STATE_NAME: String = "",
    val SITE_NAME: String = "",
    val CUMULATIVE_COUNT: String = "",
    val INCREMENTAL_COUNT: String = "",
    val REFERENCE_CODE: String = "",
    val CREATED_BY: String = "",
    val CREATED_AT: Long = System.currentTimeMillis(),
    val MODIFIED_BY: String? = null,
    val MODIFIED_AT: Long? = null,
    val isActive: Boolean = true
)

data class EntryRequest(
    val PARTICULARS: String,
    val CLIENT_CODE: String,
    val CAPACITY_MW: Double,
    val STATE_NAME: String,
    val SITE_NAME: String
)

data class EntryResponse(
    val success: Boolean = true,
    val entry: Entry? = null,
    val message: String = ""
)

// ========== Dropdown Models ==========
data class DropdownOption(
    val _id: String = "",
    val type: String = "", // PARTICULARS, CLIENT_CODE, SITE_NAME, STATE_NAME
    val value: String = "",
    val isActive: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

data class DropdownRequest(
    val type: String,
    val value: String,
    val isCustom: Boolean = true
)

// ========== API Response Models ==========
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String = "",
    val error: String? = null
)

data class PaginatedResponse<T>(
    val entries: List<T> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = false
)

// ========== Dashboard Models ==========
data class DashboardStats(
    val total: Int = 0,
    val totalCapacity: Double = 0.0,
    val averageCapacity: Double = 0.0,
    val thisMonth: Int = 0,
    val byParticulars: Map<String, Int> = emptyMap(),
    val byClient: Map<String, Int> = emptyMap(),
    val byState: Map<String, Int> = emptyMap(),
    val bySite: Map<String, Int> = emptyMap(),
    val recentEntries: List<Entry> = emptyList(),
    val monthlyTrend: List<MonthlyData> = emptyList()
)

data class MonthlyData(
    val month: String,
    val count: Int,
    val capacity: Double
)

// ========== Settings Models ==========
data class SystemInfo(
    val totalUsers: Int = 0,
    val totalEntries: Int = 0,
    val totalDropdownOptions: Int = 0,
    val activeDropdownOptions: Int = 0,
    val customDropdownOptions: Int = 0,
    val systemVersion: String = "1.0.0",
    val lastBackup: Long = 0
)

data class AuditLog(
    val _id: String = "",
    val userId: String = "",
    val username: String = "",
    val action: String = "", // CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    val entityType: String = "", // ENTRY, DROPDOWN_OPTION, USER
    val entityId: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String = ""
)

// ========== Export Models ==========
data class ExportRequest(
    val format: String = "csv", // csv, json, excel
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val filters: Map<String, String> = emptyMap(),
    val includeInactive: Boolean = false
)

data class ExportResponse(
    val success: Boolean = true,
    val downloadUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val expiresAt: Long = 0,
    val message: String = ""
)

// ========== Network Result Models ==========
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    data class Loading<T>(val isLoading: Boolean = true) : NetworkResult<T>()
}

// ========== UI State Models ==========
data class LoadingState(
    val isLoading: Boolean = false,
    val message: String = ""
)

data class ErrorState(
    val hasError: Boolean = false,
    val message: String = "",
    val code: Int? = null,
    val canRetry: Boolean = true
)

data class ValidationResult(
    val isValid: Boolean = true,
    val errors: Map<String, String> = emptyMap()
)

// ========== Filter & Search Models ==========
data class EntryFilter(
    val search: String = "",
    val particulars: String = "",
    val clientCode: String = "",
    val stateName: String = "",
    val siteName: String = "",
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val createdBy: String = "",
    val sortBy: String = "CREATED_AT",
    val sortOrder: String = "desc", // asc, desc
    val page: Int = 1,
    val limit: Int = 20
)

data class DropdownFilter(
    val type: String = "",
    val isActive: Boolean? = null,
    val isCustom: Boolean? = null,
    val search: String = ""
)

// ========== Preferences Models ==========
data class UserPreferences(
    val theme: String = "system", // light, dark, system
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val defaultSortOrder: String = "desc",
    val itemsPerPage: Int = 20,
    val showInactiveOptions: Boolean = false
)

// ========== Utility Models ==========
data class AppVersion(
    val versionName: String,
    val versionCode: Int,
    val buildDate: Long,
    val isDebug: Boolean
)

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val osVersion: String,
    val appVersion: String,
    val lastSyncTime: Long
)

// ========== Extension Functions ==========
fun Entry.toDisplayString(): String {
    return "$REFERENCE_CODE - $PARTICULARS ($CAPACITY_MW MW)"
}

fun User.isAdmin(): Boolean {
    return role == "admin"
}

fun DropdownOption.getDisplayName(): String {
    return if (isCustom) "$value (Custom)" else value
}

fun DashboardStats.getTotalCapacityFormatted(): String {
    return String.format("%.1f MW", totalCapacity)
}

fun DashboardStats.getAverageCapacityFormatted(): String {
    return String.format("%.1f MW", averageCapacity)
}

// ========== Type Aliases ==========
typealias EntryList = List<Entry>
typealias DropdownOptionList = List<DropdownOption>
typealias StringMap = Map<String, String>
typealias IntMap = Map<String, Int>

// ========== Constants ==========
object ModelConstants {
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_USERNAME_LENGTH = 20
    const val MAX_PARTICULARS_LENGTH = 10
    const val MAX_CLIENT_CODE_LENGTH = 4
    const val MIN_CLIENT_CODE_LENGTH = 2
    const val MAX_STATE_NAME_LENGTH = 4
    const val MIN_STATE_NAME_LENGTH = 2
    const val MAX_SITE_NAME_LENGTH = 4
    const val MIN_SITE_NAME_LENGTH = 2
}

// ========== Enum Classes ==========
enum class UserRole(val value: String) {
    USER("user"),
    ADMIN("admin")
}

enum class EntryStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}

enum class DropdownType(val value: String) {
    PARTICULARS("PARTICULARS"),
    CLIENT_CODE("CLIENT_CODE"),
    STATE_NAME("STATE_NAME"),
    SITE_NAME("SITE_NAME")
}

enum class SortOrder(val value: String) {
    ASC("asc"),
    DESC("desc")
}

enum class ExportFormat(val value: String) {
    CSV("csv"),
    JSON("json"),
    EXCEL("excel")
}

enum class AuditAction(val value: String) {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    LOGIN("LOGIN"),
    LOGOUT("LOGOUT"),
    EXPORT("EXPORT")
}

enum class EntityType(val value: String) {
    ENTRY("ENTRY"),
    DROPDOWN_OPTION("DROPDOWN_OPTION"),
    USER("USER")
}
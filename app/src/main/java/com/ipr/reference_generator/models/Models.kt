package com.ipr.reference_generator.models

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "user", // user or admin
    @ServerTimestamp
    val createdAt: Date? = null,
    val isActive: Boolean = true
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", "", "user", null, true)
}

data class Entry(
    val id: String = "",

    @get:PropertyName("SL_NO")
    @set:PropertyName("SL_NO")
    var slNo: Int = 0,

    @get:PropertyName("USER_NAME")
    @set:PropertyName("USER_NAME")
    var userName: String = "",

    @get:PropertyName("PARTICULARS")
    @set:PropertyName("PARTICULARS")
    var particulars: String = "",

    @get:PropertyName("CLIENT_CODE")
    @set:PropertyName("CLIENT_CODE")
    var clientCode: String = "",

    @get:PropertyName("CAPACITY_MW")
    @set:PropertyName("CAPACITY_MW")
    var capacityMW: Double = 0.0,

    @get:PropertyName("STATE_NAME")
    @set:PropertyName("STATE_NAME")
    var stateName: String = "",

    @get:PropertyName("SITE_NAME")
    @set:PropertyName("SITE_NAME")
    var siteName: String = "",

    @get:PropertyName("REFERENCE_CODE")
    @set:PropertyName("REFERENCE_CODE")
    var referenceCode: String = "",

    @get:PropertyName("CREATED_BY")
    @set:PropertyName("CREATED_BY")
    var createdBy: String = "",

    @get:PropertyName("CREATED_AT")
    @set:PropertyName("CREATED_AT")
    var createdAt: Date? = null,

    @get:PropertyName("MODIFIED_BY")
    @set:PropertyName("MODIFIED_BY")
    var modifiedBy: String? = null,

    @get:PropertyName("MODIFIED_AT")
    @set:PropertyName("MODIFIED_AT")
    var modifiedAt: Date? = null,

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true
) {
    constructor() : this("", 0, "", "", "", 0.0, "", "", "", "", null, null, null, true)
}

data class DropdownOption(
    val id: String = "",
    val type: String = "", // PARTICULARS, CLIENT_CODE, SITE_NAME, STATE_NAME
    val value: String = "",
    val displayName: String = "",
    val isActive: Boolean = true,
    val isCustom: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null,
    val createdBy: String = ""
) {
    constructor() : this("", "", "", "", true, false, null, "")
}

data class DashboardStats(
    val total: Int = 0,
    val totalCapacity: Double = 0.0,
    val averageCapacity: Double = 0.0,
    val thisMonth: Int = 0,
    val byParticulars: Map<String, Int> = emptyMap(),
    val byClient: Map<String, Int> = emptyMap(),
    val recentEntries: List<Entry> = emptyList()
)

data class EntryRequest(
    val particulars: String,
    val clientCode: String,
    val capacityMW: Double,
    val stateName: String,
    val siteName: String
)
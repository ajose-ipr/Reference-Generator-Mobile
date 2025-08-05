// network/ApiService.kt
package com.ipr.reference_generator.network

import com.ipr.reference_generator.models.ApiResponse
import com.ipr.reference_generator.models.AuthResponse
import com.ipr.reference_generator.models.DashboardStats
import com.ipr.reference_generator.models.DropdownOption
import com.ipr.reference_generator.models.Entry
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: Map<String, String>): Response<AuthResponse>

    @GET("entries")
    suspend fun getEntries(
        @Query("page") page: Int = 1,
        @Query("search") search: String = ""
    ): Response<ApiResponse<List<Entry>>>

    @POST("entries")
    suspend fun createEntry(@Body request: Map<String, Any>): Response<ApiResponse<Entry>>

    @PUT("entries/{id}")
    suspend fun updateEntry(@Path("id") id: String, @Body request: Map<String, Any>): Response<ApiResponse<Entry>>

    @DELETE("entries/{id}")
    suspend fun deleteEntry(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("dropdown-options/{type}")
    suspend fun getDropdownOptions(@Path("type") type: String): Response<List<DropdownOption>>

    @GET("export/stats")
    suspend fun getDashboardStats(): Response<DashboardStats>

    @POST("dropdown-options")
    suspend fun addDropdownOption(@Body option: Map<String, Any>): Response<ApiResponse<DropdownOption>>
}
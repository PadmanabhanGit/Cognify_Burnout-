package com.simats.burnouttracker.data.api

import com.simats.burnouttracker.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "https://cognify-backend-kin1.onrender.com/"
    
    // Simple in-memory token for now. 
    // In a real app, you'd use a multiplatform settings library.
    var token: String? = null

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        defaultRequest {
            url(BASE_URL)
            token?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        return try {
            val response = client.post("api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val authResponse: AuthResponse = response.body()
            if (authResponse.success) {
                token = authResponse.token
            }
            authResponse
        } catch (e: Exception) {
            AuthResponse(success = false, message = e.message ?: "Network error")
        }
    }

    suspend fun register(request: RegisterRequest): AuthResponse {
        return try {
            val response = client.post("api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val authResponse: AuthResponse = response.body()
            if (authResponse.success) {
                token = authResponse.token
            }
            authResponse
        } catch (e: Exception) {
            AuthResponse(success = false, message = e.message ?: "Network error")
        }
    }
    
    suspend fun getDashboard(): DashboardResponse {
        return try {
            client.get("api/dashboard").body()
        } catch (e: Exception) {
            DashboardResponse(success = false)
        }
    }
}

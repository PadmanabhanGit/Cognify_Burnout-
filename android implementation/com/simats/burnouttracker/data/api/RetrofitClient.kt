// ─── Retrofit Client Singleton ────────────────────────────────────────────────

package com.simats.burnouttracker.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Use your computer's LAN IP for local testing
    private const val BASE_URL = "http://192.168.1.10:5000/"

    @Volatile
    private var apiService: ApiService? = null

    /**
     * Get the singleton ApiService instance.
     * Call this from your ViewModel or Repository.
     */
    fun getApiService(): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService().also { apiService = it }
        }
    }

    private fun buildApiService(): ApiService {
        // Logging interceptor — shows request/response in Logcat (debug only)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Auth interceptor — auto-attaches Firebase ID token
        val authInterceptor = AuthInterceptor()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

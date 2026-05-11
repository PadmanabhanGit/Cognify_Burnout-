// ─── Retrofit Client Singleton ────────────────────────────────────────────────
// Copy this file into your Android project under:
//   app/src/main/java/com/yourpackage/data/api/

package com.burnouttracker.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ⚠️ CHANGE THIS to match your setup:
    //   • Android Emulator  → "http://10.0.2.2:5000/"
    //   • Physical device    → "http://<your-computer-ip>:5000/"
    //   • Production         → "https://your-api-domain.com/"
    private const val BASE_URL = "http://10.0.2.2:5000/"

    @Volatile
    private var apiService: ApiService? = null

    /**
     * Get the singleton ApiService instance.
     * Call this from your ViewModel or Repository.
     *
     * Example:
     *   val api = RetrofitClient.getApiService(applicationContext)
     *   val response = api.login(LoginRequest("test@email.com", "password123"))
     */
    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService(context).also { apiService = it }
        }
    }

    private fun buildApiService(context: Context): ApiService {
        // Logging interceptor — shows request/response in Logcat (debug only)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Auth interceptor — auto-attaches JWT token
        val authInterceptor = AuthInterceptor(context.applicationContext)

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

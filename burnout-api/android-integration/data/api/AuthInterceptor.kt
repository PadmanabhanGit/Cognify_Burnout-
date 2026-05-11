// ─── Auth Interceptor ─────────────────────────────────────────────────────────
// Automatically attaches the JWT token to every request.
// Copy this file into your Android project under:
//   app/src/main/java/com/yourpackage/data/api/

package com.burnouttracker.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = getToken()

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }

    private fun getToken(): String? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("jwt_token", null)
    }

    companion object {
        /**
         * Save the JWT token after login/register.
         * Call this from your LoginScreen or RegisterScreen after a successful response.
         */
        fun saveToken(context: Context, token: String) {
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("jwt_token", token)
                .apply()
        }

        /**
         * Clear the JWT token on sign-out.
         * Call this from your Profile → Sign Out action.
         */
        fun clearToken(context: Context) {
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("jwt_token")
                .apply()
        }

        /**
         * Check if user is logged in (has a token).
         */
        fun isLoggedIn(context: Context): Boolean {
            return getTokenStatic(context) != null
        }

        private fun getTokenStatic(context: Context): String? {
            return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("jwt_token", null)
        }
    }
}

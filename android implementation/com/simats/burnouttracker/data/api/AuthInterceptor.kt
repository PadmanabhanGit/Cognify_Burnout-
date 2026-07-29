package com.simats.burnouttracker.data.api

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val token = currentUser?.let {
            try {
                // Blocking call is safe here — interceptors run on OkHttp's
                // background dispatcher thread, never the main thread.
                Tasks.await(it.getIdToken(false)).token
            } catch (e: Exception) {
                null
            }
        }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}

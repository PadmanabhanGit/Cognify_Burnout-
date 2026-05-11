// ─── Gradle Dependencies for Retrofit + OkHttp ──────────────────────────────
// Add these lines to your app-level build.gradle.kts (Module :app)
// inside the dependencies { } block.

// ─── PASTE INTO build.gradle.kts (app) dependencies ────────────────────────
/*

dependencies {
    // ... your existing dependencies ...

    // Retrofit (HTTP client)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp (underlying HTTP engine + logging)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson (JSON serialization)
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines (for suspend functions in ApiService)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Lifecycle ViewModel (for viewModelScope coroutines)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
}

*/

// ─── Also add this to your AndroidManifest.xml ─────────────────────────────
/*

<uses-permission android:name="android.permission.INTERNET" />

<!-- If using emulator with localhost, also add this inside <application>: -->
android:usesCleartextTraffic="true"

*/

@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
}

kotlin {
    androidTarget {
        @Suppress("OPT_IN_USAGE")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                // Use KMP-friendly versions if possible
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha07")
                
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                
                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.auth)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.ktor.client.okhttp)
                
                // Firebase
                implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
                implementation("com.google.firebase:firebase-common-ktx:21.0.0")
                implementation(libs.play.services.auth)
                
                // TensorFlow Lite (Android specific)
                implementation("org.tensorflow:tensorflow-lite:2.14.0")
                implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
                implementation(libs.tensorflow.lite.metadata)

                // Retrofit (Keep in androidMain for now, but will need common Ktor later)
                implementation("com.squareup.retrofit2:retrofit:2.11.0")
                implementation("com.squareup.retrofit2:converter-gson:2.11.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

                // Room
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.room.ktx)
                
                // WorkManager
                implementation(libs.androidx.work.runtime.ktx)
            }
        }
        
        val wasmJsMain by getting {
            dependencies {
                // Web specific dependencies if any
                implementation(libs.ktor.client.js)
            }
        }
    }
}

android {
    namespace = "com.simats.burnouttracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.simats.burnouttracker"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        mlModelBinding = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "**/libtensorflowlite_jni.so"
        }
    }
    
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    add("kapt", libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

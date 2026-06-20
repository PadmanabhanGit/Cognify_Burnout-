package com.simats.burnouttracker.utils

enum class PlatformType {
    ANDROID, WEB
}

expect fun getPlatform(): PlatformType

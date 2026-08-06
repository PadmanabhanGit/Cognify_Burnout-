package com.simats.burnouttracker.ui.theme

import androidx.compose.ui.graphics.Color
import com.simats.burnouttracker.utils.AppData

object ThemeColors {
    val background: Color
        get() = if (AppData.isDarkMode) Color(0xFF121212) else Color(0xFFF9FAFB)
        
    val card: Color
        get() = if (AppData.isDarkMode) Color(0xFF1E1E1E) else Color.White
        
    val textPrimary: Color
        get() = if (AppData.isDarkMode) Color(0xFFE5E7EB) else Color(0xFF1F2937)
        
    val textSecondary: Color
        get() = if (AppData.isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        
    val textTertiary: Color
        get() = if (AppData.isDarkMode) Color(0xFF6B7280) else Color(0xFF94A3B8)
        
    val border: Color
        get() = if (AppData.isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
}

package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.SleepMoodLogRequest
import kotlinx.coroutines.launch

@Composable
fun SleepMoodLoggerScreen(navController: NavController) {
    var sleepHours by remember { mutableStateOf(8f) }
    var mood by remember { mutableStateOf("Neutral") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val purpleGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
    )

    Scaffold(
        containerColor = ThemeColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(purpleGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { navController.popBackStack() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Daily Logger",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(text = "How much did you sleep?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Slider(
                    value = sleepHours,
                    onValueChange = { sleepHours = it },
                    valueRange = 0f..12f,
                    steps = 24
                )
                Text(text = "${sleepHours.toInt()} Hours", fontSize = 16.sp)

                Text(text = "How are you feeling?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    listOf("Happy", "Neutral", "Sad").forEach { m ->
                        FilterChip(
                            selected = mood == m,
                            onClick = { mood = m },
                            label = { Text(m) }
                        )
                    }
                }

                Button(
                    onClick = { 
                        scope.launch {
                            isLoading = true
                            println("DEBUG_SAVE: Button clicked. Starting network call...")
                            try {
                                val response = ApiClient.saveSleepMoodLog(
                                    SleepMoodLogRequest(
                                        sleepDuration = sleepHours.toDouble(),
                                        sleepQuality = 8, // Placeholder
                                        mood = mood.lowercase(),
                                        moodScore = when(mood) {
                                            "Happy" -> 10
                                            "Neutral" -> 5
                                            else -> 2
                                        }
                                    )
                                )
                                println("DEBUG_SAVE: Network call finished. Success: ${response.success}")
                                navController.popBackStack()
                            } catch (e: Exception) {
                                println("DEBUG_SAVE: Network call FAILED. Error: ${e.message}")
                                // Show error or fail silently
                                navController.popBackStack()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Log")
                    }
                }
            }
        }
    }
}

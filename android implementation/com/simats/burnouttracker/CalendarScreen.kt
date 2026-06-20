package com.simats.burnouttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.utils.rememberPlatformSettings

@Composable
fun CalendarScreen(navController: NavController) {
    val settings = rememberPlatformSettings("burnout_history")
    
    var currentMonth by remember { mutableStateOf(5) } // May
    var currentYear by remember { mutableStateOf(2024) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    val monthName = when(currentMonth) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }

    val daysInMonth = 31 // Simplified for May
    val firstDayOfWeek = 3 // Wed (placeholder)

    val daysList = mutableListOf<Int?>()
    repeat(firstDayOfWeek) { daysList.add(null) }
    for (i in 1..daysInMonth) daysList.add(i)

    Scaffold(
        bottomBar = { AppBottomNavigation(navController, "calendar") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(text = "Burnout Calendar", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(text = "Track your mental wellness over time", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1F2937),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$monthName, $currentYear",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp).clickable { 
                                    if (currentMonth == 1) {
                                        currentMonth = 12
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp).clickable { 
                                    if (currentMonth == 12) {
                                        currentMonth = 1
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Days of week
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                            Text(text = it, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(250.dp)
                    ) {
                        items(daysList) { day ->
                            if (day == null) {
                                Box(modifier = Modifier.size(40.dp))
                            } else {
                                val risk = if (day % 7 == 0) "HIGH" else if (day % 3 == 0) "MODERATE" else "LOW"
                                
                                val bgColor = when (risk) {
                                    "LOW" -> Color(0xFF10B981)
                                    "MODERATE" -> Color(0xFFF97316)
                                    "HIGH" -> Color(0xFFEF4444)
                                    else -> Color.Transparent
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable {
                                            selectedDay = day
                                            showDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Legend
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LegendItem(Color(0xFF10B981), "Low Risk")
                LegendItem(Color(0xFFF97316), "Moderate")
                LegendItem(Color(0xFFEF4444), "High Risk")
            }
        }

        if (showDialog && selectedDay != null) {
            val risk = if (selectedDay!! % 7 == 0) "High" else if (selectedDay!! % 3 == 0) "Moderate" else "Low"
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Log for $monthName $selectedDay, $currentYear") },
                text = { Text("Burnout Risk: $risk\n\nStatus: Your wellness metrics for this day indicate a $risk risk level.") },
                confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

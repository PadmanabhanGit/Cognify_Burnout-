package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.RetrofitClient
import com.simats.burnouttracker.data.models.ProfileData
import kotlinx.coroutines.launch
import com.simats.burnouttracker.utils.rememberAuthService
import com.simats.burnouttracker.utils.AppData

@Composable
fun PersonalInformationScreen(navController: NavController) {
    val authService = rememberAuthService()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(authService.getCurrentUserEmail() ?: "") }
    var age by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.getApiService().getProfileInfo()
            if (response.isSuccessful) {
                response.body()?.let { data ->
                    firstName = data.firstName ?: ""
                    lastName = data.lastName ?: ""
                    fullName = "${firstName} ${lastName}".trim()
                    AppData.userFullName = fullName
                    age = data.age ?: ""
                    location = data.location ?: ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val headerGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
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
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { navController.popBackStack() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Personal Information",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fill in your details to personalize your experience",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            // Details Form Card
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AssignmentInd,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Your Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            // First Name & Last Name Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    FormLabel("First Name")
                                    FormField(value = firstName, onValueChange = { firstName = it; fullName = "$firstName $lastName".trim() }, placeholder = "John")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    FormLabel("Last Name")
                                    FormField(value = lastName, onValueChange = { lastName = it; fullName = "$firstName $lastName".trim() }, placeholder = "Doe")
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Full Name
                            FormLabel("Full Name")
                            FormField(value = fullName, onValueChange = { fullName = it }, placeholder = "John Doe")

                            Spacer(modifier = Modifier.height(20.dp))

                            // Email Address
                            FormLabel("Email Address")
                            FormField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = "student@example.com",
                                leadingIcon = Icons.Outlined.Email
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Age & Location Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(modifier = Modifier.weight(0.35f)) {
                                    FormLabel("Age")
                                    FormField(value = age, onValueChange = { age = it }, placeholder = "21")
                                }
                                Column(modifier = Modifier.weight(0.65f)) {
                                    FormLabel("Location")
                                    FormField(
                                        value = location,
                                        onValueChange = { location = it },
                                        placeholder = "Chennai, India",
                                        leadingIcon = Icons.Outlined.LocationOn
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val req = ProfileData(
                                    firstName = firstName,
                                    lastName = lastName,
                                    age = age,
                                    location = location
                                )
                                RetrofitClient.getApiService().updateProfileInfo(req)
                                
                                val fullNameStr = "$firstName $lastName".trim()
                                AppData.userFullName = fullNameStr
                                
                                // Sync with Firebase Auth Profile natively
                                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                auth.currentUser?.let { user ->
                                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullNameStr)
                                        .build()
                                    user.updateProfile(profileUpdates)
                                }
                                
                                // Sync locally for immediate Dashboard update.
                                //
                                // Resolved through PrefStores rather than opened
                                // by raw name: `burnout_tracker_prefs` is the
                                // user-scoped DEFAULT store, so the raw handle
                                // wrote into the unscoped legacy file. That file
                                // is shared by every account on the device, which
                                // made this save leak one person's name to the
                                // next account — and it was not the file the
                                // scoped readers consult, so the Dashboard it
                                // meant to update never saw the new value either.
                                val prefs = com.simats.burnouttracker.utils.PrefStores
                                    .open(context, com.simats.burnouttracker.utils.PrefStores.DEFAULT)
                                prefs.edit().putString("firstName", firstName).apply()
                                
                                navController.popBackStack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(buttonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSaving) "Saving..." else "Save Information",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = ThemeColors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = ThemeColors.textTertiary, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = ThemeColors.textTertiary, modifier = Modifier.size(20.dp)) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            unfocusedContainerColor = ThemeColors.background,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFE5E8F0),
            focusedBorderColor = Color(0xFF9333EA)
        ),
        singleLine = true
    )
}

# 🏗️ BurnOut Tracker - System Architecture & Design Document

**Version:** 1.0  
**Date:** May 2026  
**Project Type:** Mental Health & Productivity Tracking Application  
**Tech Stack:** Android (Kotlin + Jetpack Compose) + Node.js Backend + SQLite Database + TensorFlow Lite ML

---

## 1️⃣ EXECUTIVE SUMMARY

BurnOut Tracker is a **multi-layered mental health and productivity monitoring application** that combines:
- **Real-time health tracking** (sleep, mood, physical activity)
- **App usage monitoring** (productivity vs entertainment analysis)
- **ML-powered burnout prediction** (using weighted algorithm + TensorFlow Lite)
- **Personalized recommendations** based on user behavior patterns

The system follows a **Client-Server Architecture** with **JWT-based authentication**, **SQLite persistent storage**, and **ML inference on both device and server**.

---

## 2️⃣ HIGH-LEVEL SYSTEM OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          USER DEVICES                                       │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  Android App (Kotlin + Compose)                                      │   │
│  │  ┌─────────────────┐  ┌──────────────────┐  ┌────────────────────┐  │   │
│  │  │  UI Layer       │  │  Services        │  │  ML Models         │  │   │
│  │  │  23 Screens     │  │  - AppMonitoring │  │  - app_classifier  │  │   │
│  │  │  - Auth         │  │  - Permissions   │  │  - TensorFlow Lite │  │   │
│  │  │  - Dashboard    │  └──────────────────┘  └────────────────────┘  │   │
│  │  │  - Sleep        │                                                 │   │
│  │  │  - Burnout      │  ┌──────────────────────────────────────────┐  │   │
│  │  │  - Analytics    │  │  Data Layer (Retrofit + SharedPrefs)     │  │   │
│  │  └─────────────────┘  │  - API Service Interface                 │  │   │
│  │                       │  - Auth Interceptor (JWT)                │  │   │
│  │                       │  - Retrofit Client (HTTP)                │  │   │
│  │                       └──────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                   │                                          │
│                         (REST API calls + JWT)                              │
│                                   ↓                                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          BACKEND SERVER                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  Express.js API (Node.js, Port 5000)                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │  API Routes (9 endpoints)                                       │ │   │
│  │  │  ├─ /api/auth (Register, Login, Profile)                       │ │   │
│  │  │  ├─ /api/study (Track study sessions, Stats)                   │ │   │
│  │  │  ├─ /api/sleep-mood (Log and retrieve sleep/mood data)         │ │   │
│  │  │  ├─ /api/usage (Log app usage by category)                     │ │   │
│  │  │  ├─ /api/activity (Physical activity sync from Samsung Health) │ │   │
│  │  │  ├─ /api/burnout (Compute ML burnout risk)                     │ │   │
│  │  │  ├─ /api/productivity (Track productivity metrics)             │ │   │
│  │  │  ├─ /api/dashboard (Aggregated data for home screen)           │ │   │
│  │  │  └─ /api/report (Generate weekly/monthly reports)             │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │  Middleware Layer                                               │ │   │
│  │  │  ├─ CORS (Cross-Origin Resource Sharing)                       │ │   │
│  │  │  ├─ Authentication (JWT verification)                          │ │   │
│  │  │  └─ Request/Response logging                                   │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │  Business Logic (ML Algorithm)                                  │ │   │
│  │  │  - Burnout Risk Computation                                    │ │   │
│  │  │  - Weighted score calculation                                  │ │   │
│  │  │  - Recommendation generation                                   │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                   │                                          │
│                           (SQL queries)                                      │
│                                   ↓                                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  SQLite Database (cognify.db)                                        │   │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────────┐ │   │
│  │  │  User & Auth     │  │  Health Data     │  │  Analytics         │ │   │
│  │  │  - users         │  │  - sleep_mood    │  │  - burnout_assess  │ │   │
│  │  │  - Credentials   │  │  - phys_activity │  │  - productivity    │ │   │
│  │  └──────────────────┘  │  - app_usage     │  │  - study_sessions  │ │   │
│  │                        └──────────────────┘  └────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3️⃣ DETAILED ARCHITECTURE LAYERS

### **LAYER 1: PRESENTATION LAYER (Android UI)**

**Technology:** Kotlin + Jetpack Compose + Material Design 3

**Key Components:**

#### **A. Authentication Flow (3 screens)**
- **SplashScreen** → Initial app branding
- **LoginScreen** → Login with email/password
- **RegisterScreen** → New user registration
- **Flow:** Splash → Onboarding (3 slides) → Privacy → Login → Permissions → Dashboard

#### **B. Navigation Structure (23 Total Screens)**
```
Dashboard (Hub)
├── Sleep & Mood (4 screens)
│   ├── SleepMoodScreen (Navigation)
│   ├── SleepMoodDashboardScreen (Summary)
│   ├── SleepMoodLoggerScreen (Log sleep + mood)
│   └── SleepMoodDetailsScreen (Analytics + trends)
│
├── Study Tracking (2 screens)
│   ├── StudyTrackerScreen (Timer + sessions)
│   └── StudyTrackerDetailsScreen (Weekly stats)
│
├── Burnout Analysis (3 screens)
│   ├── BurnoutRiskScreen (ML prediction)
│   ├── GeneralizedActionPlanScreen (Recommendations)
│   └── EntertainmentAppUsageScreen (App analysis)
│
├── Productivity (2 screens)
│   ├── ProductivityScreen (Daily metrics)
│   └── ProductivityDetailsScreen (Trends)
│
├── Analytics & Reports (2 screens)
│   ├── WeeklyReportScreen (Summary)
│   └── CalendarScreen (Historical view)
│
└── Settings (5 screens)
    ├── SettingsScreen (Main hub)
    ├── PersonalInformationScreen
    ├── PrivacyDataScreen
    ├── ChangePasswordScreen
    └── LinkedAccountsScreen
```

#### **C. UI State Management**
- **Flow:** React to async API calls
- **States:** Loading → Success → Error
- **State Persistence:** SharedPreferences for auth tokens + user session
- **Bottom Navigation:** 4 main routes (Home, Stats, Calendar, Profile)

---

### **LAYER 2: DATA & API LAYER**

**Technology:** Retrofit 2 + OkHttp + Gson + Kotlin Coroutines

#### **A. Retrofit API Service Interface**
```
ApiService (Interface)
├── Authentication
│   ├── register(RegisterRequest)
│   ├── login(LoginRequest)
│   └── getProfile()
│
├── Dashboard
│   └── getDashboard()
│
├── Study Tracking
│   ├── startStudySession(StartSessionRequest)
│   ├── stopStudySession(sessionId)
│   ├── getStudyWeeklyStats()
│   └── getStudyMonthlyStats()
│
├── Sleep & Mood
│   ├── saveSleepMoodLog(SleepMoodLogRequest)
│   ├── getRecentSleepMoodLogs(limit)
│   ├── getSleepTrends(days)
│   └── getMoodTrends(days)
│
├── Productivity
│   ├── logProductivity(ProductivityLogRequest)
│   ├── getTodayProductivity()
│   └── getWeeklyProductivity()
│
├── Burnout Prediction
│   ├── computeBurnoutRisk()
│   └── getLatestBurnoutAssessment()
│
└── Reports & Analytics
    ├── getWeeklyReport()
    └── getMonthlyReport()
```

#### **B. Authentication System**
- **JWT Token Store:** SharedPreferences
- **Auto-Injection:** AuthInterceptor adds "Authorization: Bearer <token>" to all requests
- **Token Refresh:** Handled by backend (stateless)
- **Login Flow:**
  1. User enters email/password
  2. API returns JWT token
  3. Token saved to SharedPreferences
  4. AuthInterceptor auto-attaches to future requests
  5. Backend validates token before processing

#### **C. Request/Response Models (Gson-serialized)**
```
Major Models:

Auth:
├── LoginRequest → AuthResponse (+ JWT token)
├── RegisterRequest → AuthResponse
└── UserData

Sleep & Mood:
├── SleepMoodLogRequest → SleepMoodLogResponse
├── SleepMoodLog (MongoDB _id mapping)
└── SleepMoodLogsResponse

Study:
├── StartSessionRequest → StudySessionResponse
├── StudySession
└── StudyWeeklyStats / StudyMonthlyStats

Productivity:
├── ProductivityLogRequest → ProductivityLogResponse
└── ProductivityTodayResponse / ProductivityWeeklyResponse

Burnout:
├── BurnoutComputeResponse (with riskScore + warnings + recommendations)
└── BurnoutAssessmentResponse

Dashboard:
└── DashboardResponse (aggregated summary)
```

#### **D. Retrofit Client Configuration**
- **Base URL:** http://10.0.2.2:5000 (emulator) or http://YOUR_IP:5000 (physical device)
- **Timeout:** OkHttp default (30 seconds)
- **Interceptors:** AuthInterceptor (JWT injection) + LoggingInterceptor (debug)
- **Serialization:** Gson with @SerializedName for "_id" → "id" mapping

---

### **LAYER 3: SERVICE LAYER (Android)**

**Technology:** Android Services + WorkManager (for background tasks)

#### **A. AppMonitoringService**
- **Purpose:** Monitor foreground app usage in real-time
- **Trigger:** Runs on app focus changes
- **Data Collection:**
  - App package name
  - Duration of usage
  - App category (Study, Work, Social Media, Entertainment, etc.)
  - Timestamp
- **Sync:** Sends data to `/api/usage` endpoint every 30 minutes
- **Permission Required:** `PACKAGE_USAGE_STATS` (UsageStatsManager)

#### **B. Future Services (Planned)**
- **SleepTrackingService:** Night-time app monitoring (10 PM - 6 AM)
- **HealthSyncService:** Samsung Health / Google Fit integration
- **NotificationService:** Burnout alerts and recommendations
- **ReportGeneratorService:** Scheduled report generation

---

### **LAYER 4: BUSINESS LOGIC & ML LAYER**

#### **A. On-Device ML (Android)**
**Model:** TensorFlow Lite (v2.14.0)
**Model Files:**
- `app_classifier.tflite` - Classifies app usage into predefined categories
- `vocab.txt` - Vocabulary for text processing

**Inference:**
```
Input: App name + historical usage patterns
↓
Model Processing (on-device, no network required)
↓
Output: App category (Study, Entertainment, Social Media, etc.) + confidence score
```

**Use Case:** Real-time categorization of foreground app

#### **B. Server-Side ML (Burns Computation)**
**Algorithm Type:** Weighted Burnout Risk Score (0-100)

**Input Data (aggregated daily):**
1. Sleep Duration (hours)
2. Sleep Quality (1-10)
3. Physical Activity (steps)
4. App Usage (by category)
5. Study Duration (minutes)
6. Mood Score (1-10)

**Calculation:**
```
Risk Score = (Sleep Factor × 0.25) + (Activity Factor × 0.20) + 
             (Cognitive Load Factor × 0.25) + (Mood Factor × 0.30)

Where:
- Sleep Factor: 0-25 (higher = worse sleep)
- Activity Factor: 0-20 (higher = sedentary)
- Cognitive Load: 0-25 (high entertainment/low productivity)
- Mood Factor: 0-30 (lower mood = higher risk)

Result: Risk Score 0-100
Risk Level: "Low" (<30) | "Moderate" (30-50) | "High" (50-75) | "Critical" (75+)
```

**Output:**
- Risk score (0-100)
- Risk level category
- Specific warnings (personalized)
- Actionable recommendations

**Example Warnings:**
- "Severely low sleep duration detected"
- "Sedentary behavior increases cognitive fatigue"
- "High screen time detected in entertainment apps"
- "Focus ratio is skewed toward escapism"

---

### **LAYER 5: DATABASE LAYER**

**Technology:** SQLite (cognify.db)
**File Location:** `C:\Users\murug\AndroidStudioProjects\BurnOutTracker\cognify-backend\cognify.db`

#### **Schema (7 Core Tables):**

**1. users**
```sql
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  fullName TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL (bcrypt hashed),
  avatarUrl TEXT
)
```

**2. sleep_mood_logs**
```sql
CREATE TABLE sleep_mood_logs (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  date TEXT NOT NULL (ISO format),
  sleepDuration REAL,        -- hours (e.g., 7.5)
  sleepQuality INTEGER,       -- 1-10
  mood TEXT,                  -- "happy", "sad", "anxious", etc.
  moodScore INTEGER,          -- 1-10
  notes TEXT,
  FOREIGN KEY (userId) REFERENCES users(id)
)
```

**3. physical_activity**
```sql
CREATE TABLE physical_activity (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  date TEXT NOT NULL (ISO format),
  steps INTEGER DEFAULT 0,
  calories INTEGER DEFAULT 0,
  activeMinutes INTEGER DEFAULT 0,
  source TEXT,                -- 'SamsungHealth', 'GoogleFit', 'Manual'
  UNIQUE(userId, date),
  FOREIGN KEY (userId) REFERENCES users(id)
)
```

**4. app_usage**
```sql
CREATE TABLE app_usage (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  date TEXT NOT NULL,
  category TEXT NOT NULL,     -- 'Study', 'Work', 'Entertainment', 'Social', etc.
  duration INTEGER NOT NULL,  -- minutes
  UNIQUE(userId, date, category),
  FOREIGN KEY (userId) REFERENCES users(id)
)
```

**5. study_sessions**
```sql
CREATE TABLE study_sessions (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  subject TEXT NOT NULL,      -- 'Math', 'Physics', etc.
  duration INTEGER DEFAULT 0, -- minutes
  startTime TEXT NOT NULL,
  endTime TEXT,
  isActive INTEGER DEFAULT 1, -- 0 = completed, 1 = ongoing
  notes TEXT,
  FOREIGN KEY (userId) REFERENCES users(id)
)
```

**6. productivity_logs**
```sql
CREATE TABLE productivity_logs (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  date TEXT NOT NULL,
  productivityScore INTEGER,  -- 1-10
  focusHours REAL,
  breakHours REAL,
  tasksCompleted INTEGER,
  tasksPlanned INTEGER,
  peakHourStart INTEGER,      -- 0-24
  peakHourEnd INTEGER,        -- 0-24
  distractions INTEGER,
  notes TEXT,
  FOREIGN KEY (userId) REFERENCES users(id)
)
```

**7. burnout_assessments** (ML predictions stored)
```sql
CREATE TABLE burnout_assessments (
  id TEXT PRIMARY KEY,
  userId TEXT NOT NULL,
  date TEXT NOT NULL,
  riskScore INTEGER,          -- 0-100
  riskLevel TEXT,             -- 'Low', 'Moderate', 'High', 'Critical'
  factors TEXT,               -- JSON: {sleep, activity, cognitive, mood}
  wellbeingDimensions TEXT,   -- JSON: aggregated metrics
  warnings TEXT,              -- JSON array of warning messages
  recommendations TEXT,       -- JSON array of recommendations
  FOREIGN KEY (userId) REFERENCES users(id)
)
```

---

### **LAYER 6: BACKEND SERVER (Node.js)**

**Technology:** Express.js + SQLite3 + JWT

#### **A. API Routes & Endpoints**

**1. Authentication (`/api/auth`)**
```
POST /api/auth/register
  Body: {fullName, email, password}
  Response: {success, token, user}
  
POST /api/auth/login
  Body: {email, password}
  Response: {success, token, user}
  
GET /api/auth/profile
  Headers: Authorization: Bearer <token>
  Response: {success, user}
```

**2. Study Tracking (`/api/study`)**
```
POST /api/study/start
  Body: {subject, notes}
  Response: {success, session}
  
PATCH /api/study/stop/{sessionId}
  Response: {success, session}
  
GET /api/study/stats/weekly
  Response: {success, stats: {totalMinutes, sessionsCount, dailyTotals, subjectBreakdown}}
  
GET /api/study/stats/monthly
  Response: {success, stats}
```

**3. Sleep & Mood (`/api/sleep-mood`)**
```
POST /api/sleep-mood/log
  Body: {sleepDuration, sleepQuality, mood, moodScore, notes, date}
  Response: {success, log}
  
GET /api/sleep-mood/logs?limit=30
  Response: {success, logs: []}
  
GET /api/sleep-mood/trends/sleep?days=30
  Response: {success, trends: {daily, average, pattern}}
  
GET /api/sleep-mood/trends/mood?days=30
  Response: {success, trends}
```

**4. App Usage (`/api/usage`)**
```
POST /api/usage/log
  Body: {category, duration, timestamp}
  Response: {success}
  
GET /api/usage/today
  Response: {success, categories: [{name, duration}]}
  
GET /api/usage/weekly
  Response: {success, daily: {}}
```

**5. Physical Activity (`/api/activity`)**
```
POST /api/activity/log
  Body: {steps, calories, activeMinutes, source}
  Response: {success}
  
GET /api/activity/today
  Response: {success, steps, calories}
  
GET /api/activity/weekly
  Response: {success, daily}
```

**6. Burnout Risk (`/api/burnout`) - CORE ML**
```
GET /api/burnout/compute
  Auth Required: YES
  Response: {
    success: true,
    computed: {
      riskScore: 65,
      riskLevel: "High",
      factors: {
        sleepRisk: 20,
        activityRisk: 15,
        cognitiveLoadRisk: 20,
        moodRisk: 10
      },
      warnings: [...],
      recommendations: [...]
    }
  }
  
GET /api/burnout/latest
  Response: {success, assessment}
```

**7. Productivity (`/api/productivity`)**
```
POST /api/productivity/log
  Body: {productivityScore, focusHours, tasksCompleted}
  Response: {success}
  
GET /api/productivity/today
  Response: {success, data}
  
GET /api/productivity/weekly
  Response: {success, daily}
```

**8. Dashboard (`/api/dashboard`)**
```
GET /api/dashboard
  Response: {
    success: true,
    summary: {
      todayActivities: {...},
      recentSleep: {...},
      studyStats: {...},
      recentBurnout: {...}
    }
  }
```

**9. Reports (`/api/report`)**
```
GET /api/report/weekly
  Response: {success, report: {summary, insights, recommendations}}
  
GET /api/report/monthly
  Response: {success, report}
```

#### **B. Middleware Stack**
```
Request Flow:
1. CORS Middleware → Allow cross-origin requests
2. Body Parser → Parse JSON
3. Authentication Middleware → Verify JWT token (if required)
4. Route Handler → Process request
5. Database Query → Fetch/store data
6. Response → Send JSON
```

#### **C. Authentication Flow**
```
User Registration:
1. POST /api/auth/register {email, password}
2. Server hashes password (bcrypt)
3. User stored in database
4. JWT token generated (userId + timestamp)
5. Token returned to app
6. App saves token to SharedPreferences

Subsequent Requests:
1. App sends: GET /api/study/stats/weekly
   with Header: Authorization: Bearer <token>
2. Server middleware extracts token
3. Verifies signature + expiration
4. Attaches userId to request object
5. Route handler processes with userId
6. Returns personalized data
```

---

## 4️⃣ DATA FLOW & INTEGRATION

### **Flow 1: User Registers & Logs In**
```
Android App                              Backend
  │                                        │
  ├─ User enters email + password         │
  │                                        │
  ├─ POST /api/auth/register              │
  │─────────────────────────────────────→ │
  │                                        ├─ Hash password (bcrypt)
  │                                        ├─ Insert into users table
  │                                        ├─ Generate JWT token
  │                                        │
  │  ← {success, token, user}             │
  │                                        │
  ├─ Save token to SharedPreferences      │
  ├─ Navigate to Dashboard                │
  │                                        │
```

### **Flow 2: App Usage Detection & Sync**
```
Android App                              Backend
  │                                        │
  ├─ AppMonitoringService detects         │
  │  (YouTube app: 4 minutes)             │
  │                                        │
  ├─ Categorize via ML                    │
  │  (TensorFlow Lite: "Entertainment")   │
  │                                        │
  ├─ Store locally (cache)                │
  │                                        │
  ├─ Every 30 mins: POST /api/usage/log   │
  │  {category: "Entertainment",          │
  │   duration: 240}                      │
  │─────────────────────────────────────→ │
  │                                        ├─ Insert/Update app_usage table
  │                                        ├─ Return {success}
  │                                        │
  │  ← {success: true}                    │
  │                                        │
```

### **Flow 3: Burnout Risk Computation (Daily, ~10 PM)**
```
Android App                              Backend
  │                                        │
  ├─ User navigates to BurnoutRiskScreen  │
  │                                        │
  ├─ GET /api/burnout/compute             │
  │─────────────────────────────────────→ │
  │                                        ├─ Query today's data:
  │                                        │  - sleep_mood_logs (sleepDuration)
  │                                        │  - physical_activity (steps)
  │                                        │  - app_usage (by category)
  │                                        │  - study_sessions (totalMins)
  │                                        │
  │                                        ├─ Execute ML Algorithm:
  │                                        │  - Sleep factor: < 6h = +25 points
  │                                        │  - Activity factor: < 3k steps = +20
  │                                        │  - Cognitive load: high entertainment = +15
  │                                        │  - Mood score: low mood = variable
  │                                        │
  │                                        ├─ Generate warnings & recommendations
  │                                        ├─ Store in burnout_assessments table
  │                                        │
  │  ← {success, computed: {               │
  │     riskScore: 65,                     │
  │     riskLevel: "High",                 │
  │     factors: {...},                    │
  │     warnings: [...],                   │
  │     recommendations: [...]             │
  │    }}                                  │
  │                                        │
  ├─ Parse response                       │
  ├─ Display score with color coding      │
  ├─ Show warnings & recommendations      │
  │                                        │
```

### **Flow 4: Sleep & Mood Logging**
```
Android App                              Backend
  │                                        │
  ├─ User opens SleepMoodLoggerScreen     │
  │                                        │
  ├─ Selects mood emoji + enters sleep    │
  │  (8.5 hours, quality 8/10, mood "😊") │
  │                                        │
  ├─ Taps "Save Entry"                    │
  │                                        │
  ├─ POST /api/sleep-mood/log             │
  │  {sleepDuration: 8.5,                 │
  │   sleepQuality: 8,                    │
  │   mood: "happy",                      │
  │   moodScore: 9,                       │
  │   date: "2026-05-14"}                 │
  │─────────────────────────────────────→ │
  │                                        ├─ Insert into sleep_mood_logs
  │                                        ├─ Trigger burnout recalculation
  │                                        │
  │  ← {success, log}                     │
  │                                        │
  ├─ Show confirmation                    │
  │                                        │
```

---

## 5️⃣ KEY ARCHITECTURAL DECISIONS

### **Decision 1: JWT-based Authentication (Stateless)**
**Why:** Scalable, no session storage needed on server, stateless architecture

### **Decision 2: SQLite on Backend (vs MongoDB)**
**Why:** Lightweight, file-based, no external database service needed, suitable for local deployment

### **Decision 3: TensorFlow Lite v2.14.0 (vs LiteRT)**
**Why:** Backward compatible with Android 24+ (Samsung A35), flexible 4KB alignment support

### **Decision 4: Server-side Burnout Computation (vs On-Device)**
**Why:** Requires multiple daily data points aggregation, more secure, easier to update algorithm

### **Decision 5: Retrofit + Coroutines (vs Other HTTP libs)**
**Why:** Industry standard, type-safe, integrates well with Compose, excellent async support

### **Decision 6: SharedPreferences for Token Storage (vs Encrypted Shared Preferences)**
**Why:** For MVP, simplified. Future: migrate to Android Keystore for production

---

## 6️⃣ SECURITY ARCHITECTURE

### **Authentication Security**
```
Password → bcrypt (10 salt rounds) → Hashed password stored in DB
JWT Token → Signed with secret key → Verified on every request
Token → Auto-injected by AuthInterceptor → Stateless validation
```

### **Data Security**
```
In Transit: HTTPS (when deployed to production)
At Rest: SQLite file on backend server
Authorization: JWT + userId in request object
```

### **API Security**
```
1. CORS enabled only for mobile app origin
2. JWT validation middleware on protected routes
3. Input validation on all endpoints
4. Rate limiting (recommended for production)
```

---

## 7️⃣ SCALABILITY & FUTURE ENHANCEMENTS

### **Current Capacity**
- Single user: ~5 API calls/day
- Database: SQLite (max ~1M records practical)
- Server: Single Express process

### **Future Scaling (Post-MVP)**
```
1. Move to PostgreSQL for large user base
2. Implement Redis caching for API responses
3. Add message queue (RabbitMQ) for background jobs
4. Containerize with Docker for multi-instance deployment
5. Add CI/CD pipeline (GitHub Actions)
6. Implement analytics pipeline (logs → cloud storage)
```

### **Planned Features**
```
1. Samsung Health / Google Fit native integration
2. Scheduled notifications (WorkManager)
3. Social features (compare stats with friends)
4. Export data (PDF reports)
5. Advanced ML models (time-series prediction)
6. Web dashboard (React app)
```

---

## 8️⃣ TECH STACK SUMMARY

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Frontend** | Android (Kotlin) | Mobile app |
| **UI Framework** | Jetpack Compose | Reactive UI |
| **Navigation** | Compose Navigation | Screen routing |
| **HTTP Client** | Retrofit 2 | API communication |
| **Serialization** | Gson | JSON parsing |
| **Auth** | JWT | Stateless authentication |
| **ML (Device)** | TensorFlow Lite | On-device app classification |
| **Backend Runtime** | Node.js | Server environment |
| **API Framework** | Express.js | HTTP routing + middleware |
| **Database** | SQLite | Data persistence |
| **ORM** | sqlite3 (raw SQL) | Database access |
| **Auth (Server)** | JWT | Token validation |
| **Async** | Coroutines (Android), async/await (Node) | Non-blocking operations |
| **Dependency Mgmt** | Gradle (Android), npm (Node) | Package management |
| **Version Control** | Git | Code management |

---

## 9️⃣ DEPLOYMENT ARCHITECTURE

### **Development Setup**
```
Android Studio
├─ Android Emulator or Physical Device (Samsung A35)
│
Backend (concurrent or separate machine)
├─ Node.js runtime
├─ Port 5000 (development)
└─ SQLite file (cognify.db)
```

### **Production Deployment (Recommended)**
```
Cloud Server (AWS EC2 / Google Cloud / Azure)
├─ Node.js app (PM2 process manager)
├─ Port 443 (HTTPS)
├─ PostgreSQL database (production DB)
├─ Nginx reverse proxy
├─ SSL certificates (Let's Encrypt)
└─ Backup strategy (daily snapshots)

Mobile App
├─ Published on Google Play Store
└─ Backend URL point to production server
```

---

## 🔟 SYSTEM METRICS & KPIs

### **Performance Targets**
```
API Response Time: < 500ms
Database Query Time: < 100ms
ML Computation Time: < 2s
App Startup Time: < 2s
Screen Load Time: < 1s
```

### **Usage Analytics**
```
Track:
- Daily Active Users (DAU)
- Session length
- Feature adoption
- Error rates
- API latency
```

---

## 1️⃣1️⃣ TESTING STRATEGY

### **Unit Tests**
```
- API response parsing (Gson)
- ML algorithm computation
- Date/time calculations
```

### **Integration Tests**
```
- API client + server integration
- Database CRUD operations
- JWT token flow
```

### **UI Tests**
```
- Navigation flow
- Form validation
- Error state handling
```

---

## 1️⃣2️⃣ DOCUMENTATION ARTIFACTS

**Files Generated:**
- ✅ System Architecture Design (this document)
- ✅ Database Schema (cognify.db)
- ✅ API Specification (all endpoints)
- ✅ Deployment Checklist
- ✅ Quick Start Guide
- ✅ Backend Setup Guide

---

## 1️⃣3️⃣ VISIO/DIAGRAM RECOMMENDATIONS

To visualize this architecture, create diagrams for:

1. **System Context Diagram** (Users → App → Server → DB)
2. **Component Diagram** (UI Layer → API Layer → Database Layer)
3. **Sequence Diagrams**
   - Login flow
   - App usage sync
   - Burnout computation
   - Data retrieval
4. **ER Diagram** (Entity-Relationship for all 7 tables)
5. **Data Flow Diagram** (All inputs/outputs)
6. **Deployment Diagram** (Servers, networks, storage)

---

## CONCLUSION

BurnOut Tracker is a **well-structured, layered architecture** that separates concerns across presentation, business logic, and data layers. The system is designed for:

✅ **Ease of Development** - Clear separation of concerns
✅ **Scalability** - Stateless API, can add load balancing
✅ **Maintainability** - Standard frameworks and patterns
✅ **Security** - JWT-based authentication, password hashing
✅ **Performance** - Optimized queries, on-device ML
✅ **User Experience** - Real-time data sync, responsive UI

This document provides the foundation for creating detailed system architecture diagrams in tools like Lucidchart, Draw.io, or Visio.

---

**Document Version:** 1.0  
**Last Updated:** May 14, 2026  
**Author:** BurnOut Tracker Architecture Team


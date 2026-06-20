# 📐 System Architecture - Visual Diagram Guide

## How to Create Architecture Diagrams for BurnOut Tracker

This guide provides detailed instructions for creating each type of architecture diagram using tools like **Lucidchart**, **Draw.io**, **PlantUML**, or **Visio**.

---

## 1. SYSTEM CONTEXT DIAGRAM

**Purpose:** Show high-level system boundaries and external entities
**Tool:** Draw.io or Lucidchart

### Create This Diagram:

```
┌────────────────────┐
│   User/Developer   │
│   (Samsung A35)    │
└─────────┬──────────┘
          │
          ↓ (REST API calls)
┌─────────────────────────────────────────────┐
│         BurnOut Tracker System              │
│ ┌──────────────────────────────────────────┐│
│ │  Android App (Kotlin + Compose)          ││
│ │  - 23 UI Screens                         ││
│ │  - TensorFlow Lite ML                    ││
│ │  - Retrofit API Client                   ││
│ └──────────┬───────────────────────────────┘│
│            │ HTTP/REST (JSON)               │
│ ┌──────────▼───────────────────────────────┐│
│ │  Express.js Backend (Node.js, Port 5000) ││
│ │  - 9 API Routes                          ││
│ │  - JWT Authentication                    ││
│ │  - Burnout ML Algorithm                  ││
│ └──────────┬───────────────────────────────┘│
│            │ SQL queries                    │
│ ┌──────────▼───────────────────────────────┐│
│ │  SQLite Database (cognify.db)            ││
│ │  - 7 Tables                              ││
│ │  - User data, health, analytics          ││
│ └──────────────────────────────────────────┘│
└─────────────────────────────────────────────┘
          │
          ↓ (Optional: External services)
┌────────────────────────────────────────────┐
│  External Health Providers (Future)        │
│  - Samsung Health                          │
│  - Google Fit                              │
│  - Health Cloud APIs                       │
└────────────────────────────────────────────┘
```

**Elements to include in diagram:**
- User/Device (actor)
- BurnOut Tracker System (system boundary - large box)
- Android App component
- Backend component
- Database component
- External systems (dotted lines)

**Key Information to label:**
- Data format (JSON)
- Protocol (HTTP/REST)
- Port numbers (5000)
- Database type (SQLite)

---

## 2. CONTAINER DIAGRAM (Component View)

**Purpose:** Show major containers/components and interactions
**Tool:** C4 Model format in Draw.io

### Create This Diagram:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        Mobile Device (Samsung A35)                       │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                   Android Application                              │ │
│  │                                                                    │ │
│  │  ┌──────────────────────────────────────────────────────────────┐ │ │
│  │  │  Presentation Layer (Jetpack Compose)                        │ │ │
│  │  │  - 23 Composable Screens                                     │ │ │
│  │  │  - Navigation Graph                                          │ │ │
│  │  │  - State Management                                          │ │ │
│  │  └──────────────────────────────────────────────────────────────┘ │ │
│  │                         │                                           │ │
│  │                         ↓                                           │ │
│  │  ┌──────────────────────────────────────────────────────────────┐ │ │
│  │  │  Data Layer (Retrofit + SharedPreferences)                   │ │ │
│  │  │  - ApiService Interface (12 endpoints)                       │ │ │
│  │  │  - AuthInterceptor (JWT injection)                           │ │ │
│  │  │  - RetrofitClient (HTTP setup)                              │ │ │
│  │  │  - Models (Gson serialization)                              │ │ │
│  │  └──────────────────────────────────────────────────────────────┘ │ │
│  │                         │                                           │ │
│  │                         ↓                                           │ │
│  │  ┌──────────────────────────────────────────────────────────────┐ │ │
│  │  │  Service Layer (Android Services)                            │ │ │
│  │  │  - AppMonitoringService                                      │ │ │
│  │  │  - PermissionManager                                         │ │ │
│  │  └──────────────────────────────────────────────────────────────┘ │ │
│  │                                                                    │ │
│  │  ┌──────────────────────────────────────────────────────────────┐ │ │
│  │  │  ML Layer (TensorFlow Lite)                                  │ │ │
│  │  │  - app_classifier.tflite                                     │ │ │
│  │  │  - vocab.txt                                                 │ │ │
│  │  │  - App categorization inference                              │ │ │
│  │  └──────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  SharedPreferences                 Usage Stats Permission               │
│  - JWT token                       - PACKAGE_USAGE_STATS                │
│  - User session                    - INTERNET                           │
│  - Preferences                     - BODY_SENSORS (future)              │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                        HTTPS REST (JSON) Calls
                                    │
                                    ↓
┌──────────────────────────────────────────────────────────────────────────┐
│                         Backend Server                                   │
│                    (Node.js + Express, Port 5000)                        │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  API Routes (Express.js)                                           │ │
│  │  ├─ /api/auth       (Register, Login, Profile)                    │ │
│  │  ├─ /api/study      (Session management)                          │ │
│  │  ├─ /api/sleep-mood (Health logging)                              │ │
│  │  ├─ /api/usage      (App tracking)                                │ │
│  │  ├─ /api/activity   (Physical stats)                              │ │
│  │  ├─ /api/burnout    (ML computation)  ◄── CORE FEATURE           │ │
│  │  ├─ /api/productivity (Metrics)                                   │ │
│  │  ├─ /api/dashboard  (Summary)                                     │ │
│  │  └─ /api/report     (Analytics)                                   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                   │                                      │
│  ┌────────────────────────────────▼────────────────────────────────────┐ │
│  │  Middleware Stack                                                    │ │
│  │  - CORS                                                              │ │
│  │  - Body Parser (JSON)                                               │ │
│  │  - Authentication Middleware (JWT verification)                     │ │
│  │  - Error Handling                                                   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                   │                                      │
│  ┌────────────────────────────────▼────────────────────────────────────┐ │
│  │  Business Logic Layer                                                │ │
│  │  - User authentication                                               │ │
│  │  - Data aggregation                                                  │ │
│  │  - Burnout ML algorithm                                              │ │
│  │  - Recommendation generation                                         │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                   │                                      │
│  ┌────────────────────────────────▼────────────────────────────────────┐ │
│  │  Data Access Layer                                                   │ │
│  │  - SQL query builders                                                │ │
│  │  - Database helpers                                                  │ │
│  │  - Transaction management                                            │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                        SQL Queries (SQLite Protocol)
                                    │
                                    ↓
┌──────────────────────────────────────────────────────────────────────────┐
│                             Database                                     │
│                         SQLite (cognify.db)                              │
│                                                                          │
│  ┌─────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│  │  User &         │  │  Health & Activity   │  │  Analytics &     │  │
│  │  Authentication │  │  Data Tables         │  │  Assessment      │  │
│  │                 │  │                      │  │                  │  │
│  │  - users        │  │  - sleep_mood_logs   │  │  - burnout_      │  │
│  │  - Encrypted    │  │  - physical_activity │  │    assessments   │  │
│  │    credentials  │  │  - app_usage         │  │  - productivity_ │  │
│  │                 │  │  - study_sessions    │  │    logs          │  │
│  │                 │  │                      │  │                  │  │
│  └─────────────────┘  └──────────────────────┘  └──────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

**Color coding:**
- Android: Blue shades
- Backend: Green shades
- Database: Orange shades
- Connections: Arrows with protocol labels

---

## 3. SEQUENCE DIAGRAMS

### A. Login Flow

```
User             App              Server            Database
│                │                │                 │
├─ Enter email   │                │                 │
└───────────────→│                │                 │
│                │                │                 │
│                ├─────────────────│ POST /auth/login│
│                │ {email, pwd}    │                 │
│                │                 ├─────────────────│
│                │                 │ Query users     │
│                │                 ├─────────────────│
│                │                 │ row: {id, pwd}  │
│                │                 │←────────────────│
│                │                 │                 │
│                │                 │ Validate pwd    │
│                │                 │ bcrypt compare  │
│                │                 │                 │
│                │                 │ Generate JWT    │
│                │                 │ token           │
│                │                 │                 │
│                │ {token, user}   │                 │
│                │←────────────────│                 │
│                │                 │                 │
│   Save token   │                 │                 │
│   to prefs     │                 │                 │
│←───────────────│                 │                 │
│                │                 │                 │
│  Navigate to   │                 │                 │
│  Dashboard     │                 │                 │
│                │                 │                 │
```

### B. Burnout Computation Flow

```
User             App              Server            Database
│                │                │                 │
├─ View Score    │                │                 │
└───────────────→│                │                 │
│                │                │                 │
│                ├─────────────────│ GET /burnout/   │
│                │ + JWT Token     │ compute         │
│                │ (auth header)   │                 │
│                │                 ├─────────────────│
│                │                 │ Query sleep_    │
│                │                 │ mood TODAY      │
│                │                 │←────────────────│
│                │                 │ {sleepDuration} │
│                │                 │                 │
│                │                 ├─────────────────│
│                │                 │ Query physical_ │
│                │                 │ activity TODAY  │
│                │                 │←────────────────│
│                │                 │ {steps, mins}   │
│                │                 │                 │
│                │                 ├─────────────────│
│                │                 │ Query app_usage │
│                │                 │ TODAY           │
│                │                 │←────────────────│
│                │                 │ {categories}    │
│                │                 │                 │
│                │                 │ Query study_    │
│                │                 │ sessions TODAY  │
│                │                 │←────────────────│
│                │                 │ {duration}      │
│                │                 │                 │
│                │                 │ Execute ML      │
│                │                 │ Algorithm:      │
│                │                 │ +sleep factor   │
│                │                 │ +activity       │
│                │                 │ +cognitive load │
│                │                 │ +mood           │
│                │                 │ = riskScore 0-100
│                │                 │                 │
│                │                 ├─────────────────│
│                │                 │ INSERT burnout_ │
│                │                 │ assessment      │
│                │                 │←────────────────│
│                │                 │                 │
│                │ {riskScore,     │                 │
│                │  warnings,      │                 │
│                │  recs}          │                 │
│                │←────────────────│                 │
│                │                 │                 │
│   Display on   │                 │                 │
│   Risk Screen  │                 │                 │
│←───────────────│                 │                 │
│                │                 │                 │
```

### C. App Usage Detection & Sync

```
System          Service          Android          Server           DB
│                │                │                │                │
├─ foreground    │                │                │                │
│  app changes   │ Detect package │                │                │
│                │ (YouTube open) │                │                │
│                ├─ TensorFlow    │                │                │
│                │  classify      │                │                │
│                │  "Entertainment"                │                │
│                │                │                │                │
│                │ Store locally  │                │                │
│                │ (cache)        │                │                │
│                │                │                │                │
│                │ [Every 30 mins]│                │                │
│                │────────────────│ POST /usage/log│                │
│                │ {category,     │ {category,     │                │
│                │  duration}     │  duration}     │                │
│                │                │                ├─────────────────│
│                │                │                │ INSERT/UPDATE   │
│                │                │                │ app_usage       │
│                │                │                │←─────────────────│
│                │                │                │                │
│                │                │ {success}      │                │
│                │←───────────────┤────────────────│                │
│                │                │                │                │
```

**Tools for creating sequence diagrams:**
- PlantUML (code-based)
- UMLet
- Lucidchart
- Draw.io
- Miro

---

## 4. ENTITY-RELATIONSHIP DIAGRAM (ERD)

**Purpose:** Show database tables and relationships

```
┌──────────────────┐
│     USERS        │
├──────────────────┤
│ id (PK)          │
│ fullName         │
│ email (UNIQUE)   │
│ password (hashed)│
│ avatarUrl        │
└────────┬─────────┘
         │ 1:N
         │
    ┌────┴────────────────────────────────────────┐
    │                                              │
    ↓                                              ↓
┌─────────────────────────┐              ┌─────────────────────────┐
│  SLEEP_MOOD_LOGS        │              │  PHYSICAL_ACTIVITY      │
├─────────────────────────┤              ├─────────────────────────┤
│ id (PK)                 │              │ id (PK)                 │
│ userId (FK)             │              │ userId (FK)             │
│ date                    │              │ date                    │
│ sleepDuration           │              │ steps                   │
│ sleepQuality (1-10)     │              │ calories                │
│ mood                    │              │ activeMinutes           │
│ moodScore (1-10)        │              │ source                  │
│ notes                   │              │ UNIQUE(userId, date)    │
└─────────────────────────┘              └─────────────────────────┘
    │
    │
    ↓
┌─────────────────────────┐
│  BURNOUT_ASSESSMENTS    │
├─────────────────────────┤
│ id (PK)                 │
│ userId (FK)             │
│ date                    │
│ riskScore (0-100)       │
│ riskLevel               │
│ factors (JSON)          │
│ warnings (JSON)         │
│ recommendations (JSON)  │
└─────────────────────────┘
    │
    │ 1:N
    │
    └─────────────────────────────────────────────┐
         │                                        │
         ↓                                        ↓
┌──────────────────────┐              ┌──────────────────────┐
│  APP_USAGE           │              │  STUDY_SESSIONS      │
├──────────────────────┤              ├──────────────────────┤
│ id (PK)              │              │ id (PK)              │
│ userId (FK)          │              │ userId (FK)          │
│ date                 │              │ subject              │
│ category             │              │ duration             │
│ duration (mins)      │              │ startTime            │
│ UNIQUE(userId,       │              │ endTime              │
│      date,           │              │ isActive             │
│      category)       │              │ notes                │
└──────────────────────┘              └──────────────────────┘
    │
    │ 1:N
    │
    ↓
┌──────────────────────┐
│  PRODUCTIVITY_LOGS   │
├──────────────────────┤
│ id (PK)              │
│ userId (FK)          │
│ date                 │
│ productivityScore    │
│ focusHours           │
│ breakHours           │
│ tasksCompleted       │
│ tasksPlanned         │
│ peakHourStart        │
│ peakHourEnd          │
│ distractions         │
│ notes                │
└──────────────────────┘
```

**Use Lucidchart or Draw.io to create this with:**
- Boxes for tables
- Columns listed inside
- Primary keys (PK) marked
- Foreign keys (FK) marked
- 1:N relationship lines with crow's foot notation

---

## 5. DATA FLOW DIAGRAM (DFD)

**Purpose:** Show how data moves through the system

```
Level 0 - Context Diagram
═══════════════════════════

User ───→ BurnOut Tracker System ───→ External APIs (Health services)
  ↑                                        ↓
  └────────────────────────────────────────┘

Level 1 - Main Processes
════════════════════════

Data Sources (Phone sensors, Manual input)
         │
         ├──→ [1.0 Data Collection] ──→ Local Cache
         │
         ↓
[2.0 Data Synchronization] ──→  Server
         │
         ├──→ [3.0 Data Aggregation] ──→ Database
         │
         ↓
[4.0 ML Computation] 
         │
         ├──→ Warnings + Recommendations
         │
         └──→ [5.0 Display Results] ──→ User UI
```

---

## 6. API FLOW DIAGRAM

**Purpose:** Show all API endpoints and data models

```
┌─────────────────────────────────────────────────────────────────────┐
│                    API ENDPOINTS MAP                                │
└─────────────────────────────────────────────────────────────────────┘

AUTHENTICATION FLOW
═══════════════════
POST /api/auth/register
  ├─ Input: RegisterRequest {fullName, email, password}
  └─ Output: AuthResponse {token, user}
  
POST /api/auth/login
  ├─ Input: LoginRequest {email, password}
  └─ Output: AuthResponse {token, user}

GET /api/auth/profile [Protected]
  └─ Output: ProfileResponse {user}


HEALTH DATA ENDPOINTS
═════════════════════
POST /api/sleep-mood/log [Protected]
  ├─ Input: SleepMoodLogRequest
  └─ Output: SleepMoodLogResponse

GET /api/sleep-mood/logs [Protected]
  ├─ Query: ?limit=30
  └─ Output: SleepMoodLogsResponse

POST /api/activity/log [Protected]
  ├─ Input: {steps, calories, activeMinutes}
  └─ Output: ActivityLogResponse

GET /api/activity/today [Protected]
  └─ Output: ActivityTodayResponse


BURNOUT ENDPOINT (CORE ML)
══════════════════════════
GET /api/burnout/compute [Protected]
  └─ Output: BurnoutComputeResponse
     {
       riskScore: 0-100,
       riskLevel: "Low|Moderate|High|Critical",
       factors: {sleepRisk, activityRisk, cognitiveRisk, moodRisk},
       warnings: [...],
       recommendations: [...]
     }

GET /api/burnout/latest [Protected]
  └─ Output: BurnoutAssessmentResponse


DASHBOARD & ANALYTICS
═════════════════════
GET /api/dashboard [Protected]
  └─ Output: DashboardResponse {summary, widgets}

GET /api/report/weekly [Protected]
  └─ Output: ReportResponse

GET /api/report/monthly [Protected]
  └─ Output: ReportResponse
```

---

## 7. DEPLOYMENT ARCHITECTURE DIAGRAM

**Purpose:** Show production deployment topology

```
┌─────────────────────────────────────────────────────────────────┐
│                    Google Play Store                            │
│                   (APK Distribution)                            │
└──────────────────────────┬──────────────────────────────────────┘
                          │
        ┌─────────────────┴────────────────┐
        ↓                                  ↓
┌──────────────────────┐          ┌──────────────────────┐
│   Samsung A35        │          │   Other Android      │
│   (Testing Device)   │          │   Devices            │
└──────────────────────┘          └──────────────────────┘
        │                                  │
        └─────────────────┬────────────────┘
                          │
                HTTPS REST API Calls
                (Port 443)
                          │
                          ↓
        ┌─────────────────────────────────────────┐
        │     Cloud Provider (AWS/GCP/Azure)      │
        │                                         │
        │  ┌───────────────────────────────────┐  │
        │  │   Load Balancer (Nginx)           │  │
        │  │   - SSL Termination               │  │
        │  │   - Request routing               │  │
        │  └───┬─────────────────────────────┬─┘  │
        │      │                             │    │
        │  ┌───▼──────────────┐   ┌─────────▼──┐ │
        │  │ Node.js App      │   │ Node.js App│ │
        │  │ (PM2 process)    │   │(PM2 process│ │
        │  │ - Port 3000      │   │ - Port 3000│ │
        │  └──────┬───────────┘   └─────┬──────┘ │
        │         │                     │        │
        │         └──────────┬──────────┘        │
        │                    │                  │
        │            ┌───────▼────────┐         │
        │            │  PostgreSQL DB │         │
        │            │ (Production DB)│         │
        │            │ - Backups      │         │
        │            │ - Replication  │         │
        │            └────────────────┘         │
        │                                       │
        │  ┌──────────────────────────────────┐ │
        │  │  Redis Cache (Optional)          │ │
        │  │  - API response caching          │ │
        │  │  - Session storage               │ │
        │  └──────────────────────────────────┘ │
        │                                       │
        │  ┌──────────────────────────────────┐ │
        │  │  S3/Cloud Storage (Backups)      │ │
        │  │  - Daily database dumps          │ │
        │  │  - Upload logs                   │ │
        │  └──────────────────────────────────┘ │
        │                                       │
        └───────────────────────────────────────┘
```

---

## 8. STATE MACHINE DIAGRAM (Authentication)

```
             ┌─────────────────────┐
             │   Initial State     │
             │  (No User Logged)   │
             └──────────┬──────────┘
                        │
          ┌─────────────┴─────────────┐
          ↓                           ↓
    ┌──────────────┐          ┌──────────────┐
    │  LoginScreen │          │RegisterScreen│
    │              │          │              │
    │  - Email     │          │  - Name      │
    │  - Password  │          │  - Email     │
    │  - Login Btn │          │  - Password  │
    └──────┬───────┘          └──────┬───────┘
           │                          │
    Clicks │ "Sign In"          Clicks│"Sign Up"
           │                          │
           ├──────────────┬───────────┤
                          │
                   Valid Credentials
                     + JWT Token
                          │
                          ↓
                  ┌──────────────────┐
                  │ Authenticated    │
                  │ State            │
                  │                  │
                  │ Token saved to   │
                  │ SharedPreferences│
                  └────────┬─────────┘
                           │
                ┌──────────┴──────────┐
                ↓                     ↓
         ┌─────────────┐      ┌──────────────┐
         │ Dashboard   │      │ Other Routes │
         │ Screen      │      │ (protected)  │
         └─────────────┘      └──────────────┘
                │                     │
                └──────────┬──────────┘
                           │
                     User clicks
                     "Logout"
                           │
                           ↓
                  ┌──────────────────┐
                  │ Clear Token      │
                  │ from SharedPrefs │
                  └────────┬─────────┘
                           │
                           ↓
                  ┌──────────────────┐
                  │ Redirect to      │
                  │ LoginScreen      │
                  └──────────────────┘
```

---

## 9. ML ALGORITHM FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────┐
│                 Burnout Risk Computation                │
│              (Executed Daily at ~10 PM)                 │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ↓                             ↓
┌──────────────────┐         ┌──────────────────┐
│ Aggregate Data   │         │ Query Database   │
│ from Today       │         │ for Today        │
└──────────────────┘         └────────┬─────────┘
        │                             │
        │     ┌───────────────────────┘
        │     │
        ↓     ↓
    ┌─────────────────────────────────────┐
    │ Extracted Variables:                │
    │ - sleepHrs (from sleep_mood_logs)   │
    │ - steps (from physical_activity)    │
    │ - entertainment duration            │
    │ - study minutes                     │
    │ - mood score                        │
    └──────────────┬──────────────────────┘
                   │
                   ↓
    ┌──────────────────────────────────────┐
    │        Apply Weighted Formula        │
    ├──────────────────────────────────────┤
    │                                      │
    │ Sleep Factor (0-25):                 │
    │ • < 6h → +25                        │
    │ • < 7h → +10                        │
    │ • 7-8h → 0                          │
    │                                      │
    │ Activity Factor (0-20):              │
    │ • < 3000 steps → +20                │
    │ • < 6000 steps → +10                │
    │ • > 10000 steps → 0                 │
    │                                      │
    │ Cognitive Load Factor (0-25):        │
    │ • Entertainment > 240 min → +15     │
    │ • Ent/Study ratio > 2 → +10         │
    │                                      │
    │ Mood Factor (0-30):                  │
    │ • Calculated from mood score        │
    │                                      │
    └──────────────┬───────────────────────┘
                   │
                   ↓
    ┌──────────────────────────────────────┐
    │  Sum All Factors                     │
    │  riskScore = F1 + F2 + F3 + F4       │
    │  (Range: 0-100)                      │
    └──────────────┬───────────────────────┘
                   │
                   ↓
    ┌──────────────────────────────────────┐
    │  Determine Risk Level                │
    ├──────────────────────────────────────┤
    │ 0-30:   "Low"                        │
    │ 30-50:  "Moderate"                   │
    │ 50-75:  "High"                       │
    │ 75-100: "Critical"                   │
    └──────────────┬───────────────────────┘
                   │
                   ↓
    ┌──────────────────────────────────────┐
    │  Generate Warnings & Recommendations │
    │                                      │
    │  IF sleepHrs < 6:                    │
    │    → "Severely low sleep"            │
    │    → Recommend: "Get 7h tonight"     │
    │                                      │
    │  IF steps < 3000:                    │
    │    → "Sedentary"                     │
    │    → Recommend: "15min walk"         │
    │                                      │
    │  IF entertainment > 240:             │
    │    → "High screen time"              │
    │    → Recommend: "Set app limits"     │
    │                                      │
    └──────────────┬───────────────────────┘
                   │
                   ↓
    ┌──────────────────────────────────────┐
    │  Store Result in Database            │
    │  burnout_assessments table           │
    │                                      │
    │  {                                   │
    │    userId, date, riskScore,          │
    │    riskLevel, factors, warnings,     │
    │    recommendations                   │
    │  }                                   │
    └──────────────┬───────────────────────┘
                   │
                   ↓
    ┌──────────────────────────────────────┐
    │  Return to App                       │
    │                                      │
    │  BurnoutComputeResponse {            │
    │    riskScore: 65,                    │
    │    riskLevel: "High",                │
    │    factors: {...},                   │
    │    warnings: [...],                  │
    │    recommendations: [...]            │
    │  }                                   │
    └──────────────────────────────────────┘
```

---

## TOOLS FOR CREATING DIAGRAMS

| Diagram Type | Best Tools | Format |
|-------------|-----------|---------|
| System Context | Lucidchart, Draw.io, Visio | SVG/PNG |
| Component/Container | Lucidchart, C4-PlantUML, Miro | SVG/PNG |
| Sequence/Flow | PlantUML, UML, Draw.io | SVG/PNG |
| ER Diagram | Lucidchart, MySQL Workbench, pgAdmin | SVG/PNG |
| Data Flow | Draw.io, Visio | SVG/PNG |
| Deployment | CloudCraft, Draw.io, Lucidchart | SVG/PNG |
| State Machine | PlantUML, UMLet, Draw.io | SVG/PNG |

---

## RECOMMENDED APPROACH

### **Step 1: Start with System Context**
Create the highest-level view showing user, app, server, database.

### **Step 2: Create Container Diagram**
Break down the system into major components.

### **Step 3: Add Sequence Diagrams**
Show critical flows:
- Login
- Burnout computation
- App usage sync
- Data retrieval

### **Step 4: Create Database ER Diagram**
Show all tables and relationships.

### **Step 5: Add Data Flow Diagram**
Show how data moves through the system.

### **Step 6: Create Deployment Diagram**
Show production infrastructure.

### **Step 7: Add Supporting Diagrams**
- State machine
- API flow
- ML algorithm

---

## TIPS FOR PROFESSIONAL DIAGRAMS

1. **Use consistent colors:**
   - Android components: Blue
   - Backend: Green
   - Database: Orange
   - External services: Purple

2. **Layout rules:**
   - Left-to-right for data flow
   - Top-to-bottom for layers
   - Arrange logically, avoid crossing lines

3. **Labeling:**
   - Label all arrows with action/data type
   - Include protocol info (HTTP, SQL, etc.)
   - Add port numbers

4. **Notation:**
   - Use UML symbols when applicable
   - Use crow's foot for ERD relationships
   - Use arrow types to show direction

5. **Documentation:**
   - Add legend explaining notation
   - Include version number
   - Date created/updated

---

**Ready to create amazing architecture diagrams!** 🎨📊


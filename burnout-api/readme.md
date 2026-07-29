# Mental Health & Productivity Tracker — Backend API

A Node.js + Express + TypeScript + MongoDB REST API that powers the Android app.

---

## 🚀 Getting Started

### Prerequisites
- Node.js v20+
- MongoDB (local install or [MongoDB Atlas](https://www.mongodb.com/atlas) free cloud cluster)

### 1. Install Dependencies
```bash
npm install
```

### 2. Configure Environment
Edit `.env` and set your MongoDB URI and a strong JWT secret:
```
PORT=5000
MONGODB_URI=mongodb://127.0.0.1:27017/productivity-tracker
JWT_SECRET=your_very_secret_key_here
JWT_EXPIRES_IN=7d
```
> **For MongoDB Atlas** replace `MONGODB_URI` with your Atlas connection string.

### 3. Run the Server
```bash
# Development (auto-restarts on file changes)
npm run dev

# Production build
npm run build
npm start
```

The server starts at **http://localhost:5000**

---

## 📌 API Endpoints

All protected routes require the `Authorization: Bearer <token>` header.

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | ❌ | Register new user |
| POST | `/api/auth/login` | ❌ | Login & get JWT token |
| GET | `/api/auth/profile` | ✅ | Get current user profile |

### Dashboard
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/dashboard` | ✅ | Full dashboard stats for DashboardScreen |

### Study Tracking
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/study/start` | ✅ | Start a study session timer |
| PATCH | `/api/study/stop/:sessionId` | ✅ | Stop timer & save duration |
| GET | `/api/study/stats/weekly` | ✅ | Weekly bar chart data |
| GET | `/api/study/stats/monthly` | ✅ | Monthly line chart data |

### Sleep & Mood
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/sleep-mood/log` | ✅ | Save a sleep/mood entry |
| GET | `/api/sleep-mood/logs?limit=7` | ✅ | Recent logs list |
| GET | `/api/sleep-mood/trends/sleep?days=30` | ✅ | Sleep area chart data |
| GET | `/api/sleep-mood/trends/mood?days=30` | ✅ | Mood line chart data |

### Productivity
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/productivity/log` | ✅ | Log productivity entry |
| GET | `/api/productivity/today` | ✅ | Today's circular score data |
| GET | `/api/productivity/weekly` | ✅ | Weekly trend line chart |

### Burnout Prediction
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/burnout/compute` | ✅ | Auto-calculate risk from logs |
| GET | `/api/burnout/latest` | ✅ | Latest saved assessment |
| POST | `/api/burnout/assessment` | ✅ | Save manual assessment |

### Weekly Report
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/report/weekly` | ✅ | Full weekly report data |

---

## 📁 Project Structure
```
src/
├── config/
│   └── db.ts              # MongoDB connection
├── controllers/
│   ├── authController.ts
│   ├── dashboardController.ts
│   ├── studyController.ts
│   ├── sleepMoodController.ts
│   ├── productivityController.ts
│   ├── burnoutController.ts
│   └── reportController.ts
├── middleware/
│   └── auth.ts            # JWT auth middleware
├── models/
│   ├── User.ts
│   ├── StudySession.ts
│   ├── SleepMoodLog.ts
│   ├── ProductivityLog.ts
│   └── BurnoutAssessment.ts
├── routes/
│   ├── authRoutes.ts
│   ├── dashboardRoutes.ts
│   ├── studyRoutes.ts
│   ├── sleepMoodRoutes.ts
│   ├── productivityRoutes.ts
│   ├── burnoutRoutes.ts
│   └── reportRoutes.ts
└── index.ts               # App entry point
```

## 🔗 Connecting from Android (Kotlin)
Use **Retrofit** in your Android app. Set the base URL to:
- **Emulator**: `http://10.0.2.2:5000/`
- **Physical device** (same Wi-Fi): `http://<your-computer-ip>:5000/`

Send the JWT token in every protected request header:
```
Authorization: Bearer <token_from_login>
```

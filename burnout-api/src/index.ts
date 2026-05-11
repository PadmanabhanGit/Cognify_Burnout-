import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import connectDB from './config/db';

// Routes
import authRoutes from './routes/authRoutes';
import studyRoutes from './routes/studyRoutes';
import sleepMoodRoutes from './routes/sleepMoodRoutes';
import productivityRoutes from './routes/productivityRoutes';
import burnoutRoutes from './routes/burnoutRoutes';
import reportRoutes from './routes/reportRoutes';
import dashboardRoutes from './routes/dashboardRoutes';
import usageRoutes from './routes/usageRoutes';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

// ─── Middleware ───────────────────────────────────────────────────────────────
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ─── Health Check ─────────────────────────────────────────────────────────────
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    message: 'Mental Health & Productivity Tracker API is running',
    timestamp: new Date().toISOString(),
  });
});

// ─── API Routes ───────────────────────────────────────────────────────────────
app.use('/api/auth', authRoutes);           // Login, Register, Profile
app.use('/api/dashboard', dashboardRoutes); // DashboardScreen
app.use('/api/study', studyRoutes);         // StudyTrackingScreen
app.use('/api/sleep-mood', sleepMoodRoutes); // SleepMoodScreen
app.use('/api/productivity', productivityRoutes); // ProductivityScreen
app.use('/api/burnout', burnoutRoutes);     // BurnoutPredictionScreen
app.use('/api/report', reportRoutes);       // WeeklyReportScreen
app.use('/api/usage', usageRoutes);         // EntertainmentAppUsageScreen

// ─── 404 Handler ─────────────────────────────────────────────────────────────
app.use((_req, res) => {
  res.status(404).json({ success: false, message: 'Route not found' });
});

// ─── Start Server ─────────────────────────────────────────────────────────────
connectDB().then(() => {
  app.listen(PORT, () => {
    console.log(`🚀 Server running at http://localhost:${PORT}`);
    console.log(`📋 Health check: http://localhost:${PORT}/health`);
    console.log('\n📌 Available API Routes:');
    console.log('   POST   /api/auth/register');
    console.log('   POST   /api/auth/login');
    console.log('   GET    /api/auth/profile');
    console.log('   GET    /api/dashboard');
    console.log('   POST   /api/study/start');
    console.log('   PATCH  /api/study/stop/:sessionId');
    console.log('   GET    /api/study/stats/weekly');
    console.log('   GET    /api/study/stats/monthly');
    console.log('   POST   /api/sleep-mood/log');
    console.log('   GET    /api/sleep-mood/logs');
    console.log('   GET    /api/sleep-mood/trends/sleep');
    console.log('   GET    /api/sleep-mood/trends/mood');
    console.log('   POST   /api/productivity/log');
    console.log('   GET    /api/productivity/today');
    console.log('   GET    /api/productivity/weekly');
    console.log('   GET    /api/burnout/compute');
    console.log('   GET    /api/burnout/latest');
    console.log('   POST   /api/burnout/assessment');
    console.log('   GET    /api/report/weekly');
    console.log('   POST   /api/usage/sync');
    console.log('   GET    /api/usage/today');
  });
});

export default app;

import { Router } from 'express';
import {
  startSession,
  stopSession,
  logOfflineSession,
  getWeeklyStats,
  getMonthlyStats,
} from '../controllers/studyController';
import authMiddleware from '../middleware/auth';

const router = Router();

// All study routes are protected
router.use(authMiddleware);

// POST /api/study/start              → Start timer on StudyTrackingScreen
router.post('/start', startSession);

// PATCH /api/study/stop/:sessionId   → Stop timer
router.patch('/stop/:sessionId', stopSession);

// POST /api/study/log-offline       → Log a fully completed session directly
router.post('/log-offline', logOfflineSession);

// GET  /api/study/stats/weekly       → Weekly bar chart data
router.get('/stats/weekly', getWeeklyStats);

// GET  /api/study/stats/monthly      → Monthly line chart data
router.get('/stats/monthly', getMonthlyStats);

export default router;

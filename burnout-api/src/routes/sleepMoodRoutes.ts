import { Router } from 'express';
import {
  saveLog,
  getRecentLogs,
  getSleepTrends,
  getMoodTrends,
} from '../controllers/sleepMoodController';
import authMiddleware from '../middleware/auth';

const router = Router();

router.use(authMiddleware);

// POST /api/sleep-mood/log           → "Save Entry" button on SleepMoodScreen
router.post('/log', saveLog);

// GET  /api/sleep-mood/logs          → Recent logs list  (?limit=7)
router.get('/logs', getRecentLogs);

// GET  /api/sleep-mood/trends/sleep  → Sleep area chart  (?days=30)
router.get('/trends/sleep', getSleepTrends);

// GET  /api/sleep-mood/trends/mood   → Mood line chart   (?days=30)
router.get('/trends/mood', getMoodTrends);

export default router;

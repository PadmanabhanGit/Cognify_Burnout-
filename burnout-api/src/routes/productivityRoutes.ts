import { Router } from 'express';
import {
  logProductivity,
  getTodayProductivity,
  getWeeklyProductivity,
} from '../controllers/productivityController';
import authMiddleware from '../middleware/auth';

const router = Router();

router.use(authMiddleware);

// POST /api/productivity/log         → Log a productivity entry
router.post('/log', logProductivity);

// GET  /api/productivity/today       → Circular score indicator data
router.get('/today', getTodayProductivity);

// GET  /api/productivity/weekly      → Weekly trend line chart data
router.get('/weekly', getWeeklyProductivity);

export default router;

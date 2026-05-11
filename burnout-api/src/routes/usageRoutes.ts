import { Router } from 'express';
import { syncUsageData, getTodayUsage } from '../controllers/usageController';
import authMiddleware from '../middleware/auth';

const router = Router();

// Protect all usage routes
router.use(authMiddleware);

// POST /api/usage/sync  -> Sync daily app usage from device
router.post('/sync', syncUsageData);

// GET /api/usage/today  -> Get today's app usage data for the dashboard/screen
router.get('/today', getTodayUsage);

export default router;

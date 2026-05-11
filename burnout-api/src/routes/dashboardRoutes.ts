import { Router } from 'express';
import { getDashboard } from '../controllers/dashboardController';
import authMiddleware from '../middleware/auth';

const router = Router();

// GET /api/dashboard  → Full DashboardScreen payload
router.get('/', authMiddleware, getDashboard);

export default router;

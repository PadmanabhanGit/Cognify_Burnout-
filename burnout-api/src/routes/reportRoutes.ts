import { Router } from 'express';
import { getWeeklyReport } from '../controllers/reportController';
import authMiddleware from '../middleware/auth';

const router = Router();

router.use(authMiddleware);

// GET /api/report/weekly  → Full WeeklyReportScreen payload
router.get('/weekly', getWeeklyReport);

export default router;

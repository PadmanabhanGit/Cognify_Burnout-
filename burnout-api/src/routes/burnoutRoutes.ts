import { Router } from 'express';
import {
  saveAssessment,
  getLatestAssessment,
  computeBurnoutRisk,
} from '../controllers/burnoutController';
import authMiddleware from '../middleware/auth';

const router = Router();

router.use(authMiddleware);

// POST /api/burnout/assessment       → Save a manual assessment
router.post('/assessment', saveAssessment);

// GET  /api/burnout/latest           → Latest assessment (pie chart, radar, warnings)
router.get('/latest', getLatestAssessment);

// GET  /api/burnout/compute          → Auto-compute risk from real logs
router.get('/compute', computeBurnoutRisk);

export default router;

import { Router } from 'express';
import { register, login, getProfile } from '../controllers/authController';
import authMiddleware from '../middleware/auth';

const router = Router();

// POST /api/auth/register  → RegisterScreen
router.post('/register', register);

// POST /api/auth/login     → LoginScreen
router.post('/login', login);

// GET  /api/auth/profile   → ProfileScreen (protected)
router.get('/profile', authMiddleware, getProfile);

export default router;

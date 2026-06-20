const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const db = require('../database');

// @route   GET api/dashboard
// @desc    Get dashboard summary data
router.get('/', auth, (req, res) => {
  // In a real app, you would query all tables and aggregate data
  // For now, returning realistic mock data based on models
  res.json({
    success: true,
    dashboard: {
      quickStats: {
        lastSleepHours: 7.5,
        lastSleepQuality: 8,
        lastMood: "happy",
        lastMoodScore: 9,
        lastProductivityScore: 85,
        weeklyStudyHours: 32.4
      },
      burnoutAlert: {
        riskScore: 35,
        riskLevel: "moderate",
        topWarning: "Your stress levels are elevated. Consider taking breaks."
      },
      featureCards: {
        study: { weeklyHours: 32.4, sessionCount: 12 },
        sleep: { lastDuration: 7.5, lastQuality: 8 },
        burnout: { riskScore: 35, riskLevel: "moderate" },
        productivity: { score: 85 }
      }
    }
  });
});

module.exports = router;

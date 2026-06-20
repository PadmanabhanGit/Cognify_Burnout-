const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const db = require('../database');

// @route   GET api/report/weekly
// @desc    Generate a comprehensive weekly mental health & productivity report
router.get('/weekly', auth, async (req, res) => {
    const userId = req.user.id;

    // Summary Mock-up logic (In production, you'd aggregate real DB data for the last 7 days)
    res.json({
        success: true,
        report: {
            period: { from: "2026-02-18", to: "2026-02-25" },
            summary: {
                avgSleep: 6.8,
                avgMood: 7.2,
                totalStudyHours: 42.5,
                avgProductivity: 78
            },
            wellnessRadar: {
                sleep: 75,
                mood: 80,
                study: 90,
                productivity: 70,
                balance: 65
            },
            achievements: [
                "Maintained a 5-day study streak",
                "Increased average sleep by 45 minutes",
                "Hit peak productivity on Tuesday"
            ],
            concerns: [
                "Late night app usage increased on weekends",
                "Step count is below daily goal of 5000"
            ],
            recommendations: [
                "Try to reduce screen time after 10 PM to improve deep sleep.",
                "Incorporate a 10-minute stretch during long study sessions."
            ]
        }
    });
});

module.exports = router;

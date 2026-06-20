const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const db = require('../database');

// @route   GET api/burnout/compute
// @desc    Compute burnout risk based on holistic data
router.get('/compute', auth, async (req, res) => {
    const userId = req.user.id;
    const today = new Date().toISOString().split('T')[0];

    try {
        // Fetch all necessary data points for today
        const sleepData = await getTodaySleep(userId, today);
        const activityData = await getTodayActivity(userId, today);
        const usageData = await getTodayUsage(userId, today);
        const studyData = await getTodayStudy(userId, today);

        // ─── Algorithm ──────────────────────────────────────────────────────────
        let riskScore = 0;
        let warnings = [];
        let recommendations = [];

        // 1. Sleep Impact (Recovery) - Weight: 25%
        const sleepHrs = sleepData ? sleepData.sleepDuration : 7; // Default 7 if no log
        if (sleepHrs < 6) {
            riskScore += 25;
            warnings.push("Severely low sleep duration detected.");
            recommendations.push("Prioritize getting at least 7 hours of sleep tonight.");
        } else if (sleepHrs < 7) {
            riskScore += 10;
            warnings.push("Sleep is slightly below optimal levels.");
        }

        // 2. Physical Activity (Physical Resilience) - Weight: 20%
        const steps = activityData ? activityData.steps : 0;
        if (steps < 3000) {
            riskScore += 20;
            warnings.push("Sedentary behavior increases cognitive fatigue.");
            recommendations.push("Try a 15-minute walk to clear your mind.");
        } else if (steps < 6000) {
            riskScore += 10;
        }

        // 3. Cognitive Load & Escapism - Weight: 25%
        // High entertainment vs low productivity
        const entertainmentMins = usageData.filter(u => u.category === 'Entertainment' || u.category === 'Social Media')
                                           .reduce((sum, u) => sum + u.duration, 0);
        const studyMins = studyData ? studyData.totalDuration : 0;

        if (entertainmentMins > 240) { // > 4 hours
            riskScore += 15;
            warnings.push("High screen time detected in entertainment apps.");
            recommendations.push("Set app limits for social media.");
        }

        if (studyMins > 0 && (entertainmentMins / studyMins) > 2) {
            riskScore += 10;
            warnings.push("Focus ratio is skewed toward escapism.");
        }

        // 4. Mood & Emotional State - Weight: 30%
        const moodScore = sleepData ? sleepData.moodScore : 7;
        if (moodScore < 5) {
            riskScore += 30;
            warnings.push("Low mood score reported. Stress levels are high.");
            recommendations.push("Consider a mindfulness session or talking to a friend.");
        } else if (moodScore < 7) {
            riskScore += 15;
        }

        // Finalize Risk Level
        let riskLevel = "Low";
        if (riskScore > 75) riskLevel = "Critical";
        else if (riskScore > 50) riskLevel = "High";
        else if (riskScore > 25) riskLevel = "Moderate";

        const responseData = {
            success: true,
            computed: {
                riskScore: Math.min(riskScore, 100),
                riskLevel,
                factors: [
                    { name: "Recovery (Sleep)", score: sleepData ? (sleepHrs/8)*100 : 70 },
                    { name: "Activity (Steps)", score: Math.min((steps/10000)*100, 100) },
                    { name: "Focus Balance", score: studyMins > 0 ? (studyMins/(studyMins+entertainmentMins))*100 : 50 }
                ],
                wellbeingDimensions: {
                    physical: steps > 8000 ? 9 : (steps > 4000 ? 6 : 3),
                    emotional: moodScore,
                    social: 7, // Placeholder
                    intellectual: studyMins > 120 ? 8 : 5,
                    occupational: 6
                },
                warnings,
                recommendations
            }
        };

        res.json(responseData);

    } catch (err) {
        res.status(500).json({ success: false, message: "Error computing burnout risk" });
    }
});

// Helper Functions
function getTodaySleep(userId, date) {
    return new Promise((resolve) => {
        db.get('SELECT * FROM sleep_mood_logs WHERE userId = ? AND date LIKE ?', [userId, `${date}%`], (err, row) => resolve(row));
    });
}

function getTodayActivity(userId, date) {
    return new Promise((resolve) => {
        db.get('SELECT * FROM physical_activity WHERE userId = ? AND date = ?', [userId, date], (err, row) => resolve(row));
    });
}

function getTodayUsage(userId, date) {
    return new Promise((resolve) => {
        db.all('SELECT category, duration FROM app_usage WHERE userId = ? AND date = ?', [userId, date], (err, rows) => resolve(rows || []));
    });
}

function getTodayStudy(userId, date) {
    return new Promise((resolve) => {
        db.get('SELECT SUM(duration) as totalDuration FROM study_sessions WHERE userId = ? AND startTime LIKE ?', [userId, `${date}%`], (err, row) => resolve(row));
    });
}

module.exports = router;

const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { computeBurnoutRisk } = require('../services/burnoutService');

router.get('/', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = new Date().toISOString().split('T')[0];

  try {
    const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(1)
      .get();
    const lastSleep = sleepSnap.empty ? null : sleepSnap.docs[0].data();

    const prodSnap = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(1)
      .get();
    const lastProd = prodSnap.empty ? null : prodSnap.docs[0].data();

    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekAgo.toISOString())
      .get();

    const sessionCount = studySnap.size;
    const weeklyStudyMinutes = studySnap.docs.reduce((sum, doc) => sum + (doc.data().duration || 0), 0);
    const weeklyStudyHours = Math.round((weeklyStudyMinutes / 60) * 10) / 10;

    const burnout = await computeBurnoutRisk(userId, today);

    res.json({
      success: true,
      dashboard: {
        quickStats: {
          lastSleepHours: lastSleep?.sleepDuration ?? null,
          lastSleepQuality: lastSleep?.sleepQuality ?? null,
          lastMood: lastSleep?.mood ?? null,
          lastMoodScore: lastSleep?.moodScore ?? null,
          lastProductivityScore: lastProd?.productivityScore ?? null,
          weeklyStudyHours
        },
        burnoutAlert: {
          riskScore: burnout.riskScore,
          riskLevel: burnout.riskLevel,
          topWarning: burnout.warnings[0] || "No significant risk factors detected today."
        },
        featureCards: {
          study: { weeklyHours: weeklyStudyHours, sessionCount },
          sleep: { lastDuration: lastSleep?.sleepDuration ?? null, lastQuality: lastSleep?.sleepQuality ?? null },
          burnout: { riskScore: burnout.riskScore, riskLevel: burnout.riskLevel },
          productivity: { score: lastProd?.productivityScore ?? null }
        }
      }
    });
  } catch (err) {
    console.error('Dashboard aggregation failed:', err.message);
    res.status(500).json({ success: false, message: 'Error building dashboard' });
  }
});

module.exports = router;
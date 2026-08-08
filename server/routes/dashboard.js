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
      .get();
    const sleepDocs = sleepSnap.docs.map(d => d.data()).sort((a, b) => b.date.localeCompare(a.date));
    const lastSleep = sleepDocs.length > 0 ? sleepDocs[0] : null;

    const prodSnap = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .get();
    const prodDocs = prodSnap.docs.map(d => d.data()).sort((a, b) => b.date.localeCompare(a.date));
    const lastProd = prodDocs.length > 0 ? prodDocs[0] : null;

    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekAgo.toISOString())
      .get();

    const sessionCount = studySnap.size;
    const weeklyStudyMinutes = studySnap.docs.reduce((sum, doc) => sum + (doc.data().duration || 0), 0);
    const weeklyStudyHours = Math.round((weeklyStudyMinutes / 60) * 10) / 10;
    
    const todayStudyMinutes = studySnap.docs
      .filter(doc => (doc.data().startTime || '').startsWith(today))
      .reduce((sum, doc) => sum + (doc.data().duration || 0), 0);

    const burnoutSnap = await db.collection('burnoutAssessments')
      .where('userId', '==', userId)
      .get();
    const burnoutDocs = burnoutSnap.docs.map(d => d.data()).sort((a, b) => b.date.localeCompare(a.date));
    let burnout = null;
    if (burnoutDocs.length > 0) {
      burnout = burnoutDocs[0];
    } else {
      burnout = await computeBurnoutRisk(userId, today);
    }
    
    if (!burnout.warnings) burnout.warnings = [];

    // Fetch User Profile for FirstName
    const userDoc = await db.collection('users').doc(userId).get();
    const userProfile = userDoc.exists ? userDoc.data() : {};
    const firstName = userProfile.firstName || req.user.email.split('@')[0];

    res.json({
      success: true,
      dashboard: {
        user: {
          firstName: firstName
        },
        quickStats: {
          lastSleepHours: lastSleep?.sleepDuration ?? null,
          lastSleepQuality: lastSleep?.sleepQuality ?? null,
          lastMood: lastSleep?.mood ?? null,
          lastMoodScore: lastSleep?.moodScore ?? null,
          lastProductivityScore: lastProd?.productivityScore ?? null,
          weeklyStudyHours,
          weeklyStudyMinutes,
          todayStudyMinutes
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
const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { computeBurnoutRisk } = require('../services/burnoutService');

function getLocalDateString(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function normalizeDateValue(value) {
  if (!value) return getLocalDateString();
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
    const parsed = new Date(trimmed);
    if (!Number.isNaN(parsed.getTime())) return getLocalDateString(parsed);
    return getLocalDateString();
  }
  if (value instanceof Date) return getLocalDateString(value);
  return getLocalDateString();
}

router.get('/', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = getLocalDateString();

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
      .filter(doc => normalizeDateValue(doc.data().startTime) === today)
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

    const usageSnap = await db.collection('appUsage')
      .where('userId', '==', userId)
      .get();
    
    let todayAppUsageMinutes = 0;
    usageSnap.docs.forEach(doc => {
      const data = doc.data();
      if (normalizeDateValue(data.date) !== today) return;
      const cat = data.category || '';
      if (cat.includes('Social') || cat.includes('Gaming') || cat.includes('Stream') || cat.includes('Entertainment')) {
        todayAppUsageMinutes += (data.totalDuration || 0);
      }
    });

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
          todayStudyMinutes,
          todayAppUsageMinutes
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
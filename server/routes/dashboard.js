const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { computeBurnoutRisk } = require('../services/burnoutService');
const { getLocalDateString, normalizeDateValue } = require('../utils/dateUtils');

router.get('/', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = getLocalDateString();

  try {
    const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .orderBy('createdAt', 'desc')
      .limit(1)
      .get();
    const lastSleep = sleepSnap.docs.length > 0 ? sleepSnap.docs[0].data() : null;

    const prodSnap = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(1)
      .get();
    const lastProd = prodSnap.docs.length > 0 ? prodSnap.docs[0].data() : null;

    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekAgo.toISOString())
      .get();

    const studyDocs = studySnap.docs
      .map(doc => ({ id: doc.id, ...doc.data() }));

    const sessionCount = studyDocs.length;
    const weeklyStudyMinutes = studyDocs.reduce((sum, doc) => sum + Number(doc.duration || 0), 0);
    const weeklyStudyHours = Math.round((weeklyStudyMinutes / 60) * 10) / 10;
    
    const completedTodayStudyMinutes = studyDocs
      .filter(doc => normalizeDateValue(doc.startTime) === today)
      .reduce((sum, doc) => sum + Number(doc.duration || 0), 0);

    // An active session has no persisted duration until it is stopped. Include its
    // elapsed time here so the dashboard is consistent with the running mobile timer.
    const now = Date.now();
    const activeSession = studyDocs
      .filter(doc => doc.isActive === true && normalizeDateValue(doc.startTime) === today)
      .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())
      .find(doc => {
        const startedAt = new Date(doc.startTime).getTime();
        return Number.isFinite(startedAt) && now - startedAt < 24 * 60 * 60 * 1000;
      }) || null;
    const activeStudySeconds = activeSession
      ? Math.max(0, Math.floor((now - new Date(activeSession.startTime).getTime()) / 1000))
      : 0;
    const todayStudySeconds = (completedTodayStudyMinutes * 60) + activeStudySeconds;
    const todayStudyMinutes = Math.floor(todayStudySeconds / 60);

    const burnoutSnap = await db.collection('burnoutAssessments')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(1)
      .get();
    let burnout = null;
    if (burnoutSnap.docs.length > 0) {
      burnout = burnoutSnap.docs[0].data();
    } else {
      burnout = await computeBurnoutRisk(userId, today);
    }
    
    if (!burnout.warnings) burnout.warnings = [];

    const usageSnap = await db.collection('appUsage')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .get();
    
    let todayAppUsageSeconds = 0;
    usageSnap.docs.forEach(doc => {
      const data = doc.data();
      const cat = data.category || '';
      if (cat.includes('Social') || cat.includes('Gaming') || cat.includes('Stream') || cat.includes('Entertainment')) {
        todayAppUsageSeconds += Number(data.totalDurationSeconds ?? (data.totalDuration || 0) * 60);
      }
    });
    const todayAppUsageMinutes = Math.floor(todayAppUsageSeconds / 60);

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
          todayStudySeconds,
          activeStudySeconds,
          hasActiveStudySession: Boolean(activeSession),
          todayAppUsageMinutes,
          todayAppUsageSeconds
        },
        burnoutAlert: {
          riskScore: burnout.riskScore,
          riskLevel: burnout.riskLevel,
          topWarning: burnout.warnings[0] || "No significant risk factors detected today.",
          warnings: burnout.warnings || [],
          factors: burnout.factors || []
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

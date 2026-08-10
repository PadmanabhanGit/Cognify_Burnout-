const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { computeBurnoutRisk } = require('../services/burnoutService');
const { getLocalDateString } = require('../utils/dateUtils');

// ─── IST helpers (must match study.js exactly) ────────────────────────────────
// Android device timezone = IST (UTC+05:30).  All calendar-day and week
// boundaries are computed in IST so they match the Android app exactly.
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

function toIST(dateOrString) {
  return new Date(new Date(dateOrString).getTime() + IST_OFFSET_MS);
}

function getISTDateString(dateOrString) {
  const ist = toIST(dateOrString);
  const y = ist.getUTCFullYear();
  const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const d = String(ist.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function getWeekStartISO() {
  const nowIST = toIST(new Date());
  const dayOfWeek = nowIST.getUTCDay(); // 0=Sun … 6=Sat
  const daysFromMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  const mondayIST = new Date(nowIST);
  mondayIST.setUTCDate(nowIST.getUTCDate() - daysFromMonday);
  mondayIST.setUTCHours(0, 0, 0, 0);
  // Shift back to real UTC
  return new Date(mondayIST.getTime() - IST_OFFSET_MS).toISOString();
}
// ─────────────────────────────────────────────────────────────────────────────

router.get('/', auth, async (req, res) => {
  const userId = req.user.uid;
  // today in IST (for app-usage query which uses the `date` field, also written in IST)
  const today = getISTDateString(new Date());

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

    // Fetch this-week sessions using the same IST Monday boundary as study.js
    const weekStartISO = getWeekStartISO();
    const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekStartISO)
      .get();

    const studyDocs = studySnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));

    // Weekly total: completed sessions only
    const completedDocs = studyDocs.filter(doc => doc.isActive === false && doc.duration != null);
    const weeklyStudyMinutes = completedDocs.reduce((sum, doc) => sum + Number(doc.duration || 0), 0);
    const weeklyStudyHours = Math.round((weeklyStudyMinutes / 60) * 10) / 10;
    const sessionCount = studyDocs.length;

    // Today's completed study (IST date match)
    const completedTodayStudyMinutes = completedDocs
      .filter(doc => getISTDateString(doc.startTime) === today)
      .reduce((sum, doc) => sum + Number(doc.duration || 0), 0);

    // Active session: must have started today (IST), not be a zombie (< 24h)
    const now = Date.now();
    const activeSession = studyDocs
      .filter(doc => {
        if (doc.isActive !== true) return false;
        const startedAt = new Date(doc.startTime).getTime();
        if (!Number.isFinite(startedAt) || now - startedAt >= 24 * 60 * 60 * 1000) return false;
        return getISTDateString(doc.startTime) === today;
      })
      .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())[0] || null;

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

    const userDoc = await db.collection('users').doc(userId).get();
    const userProfile = userDoc.exists ? userDoc.data() : {};
    const firstName = userProfile.firstName || req.user.email.split('@')[0];

    res.json({
      success: true,
      dashboard: {
        user: { firstName },
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
          topWarning: burnout.warnings[0] || 'No significant risk factors detected today.',
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

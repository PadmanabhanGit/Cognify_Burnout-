const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { getLocalDateString } = require('../utils/dateUtils');
const { selectCanonicalSleepLog, sortByRecencyDesc } = require('../utils/sleepSelection');

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
    // Sleep values must come from the SAME canonical record the Sleep page uses
    // (see utils/sleepSelection.js) — otherwise the two pages can disagree.
    // The previous `.orderBy('createdAt','desc').limit(1)` also silently dropped
    // any document missing `createdAt`, since Firestore excludes those from an
    // orderBy. Mood is deliberately still taken from the newest log so this
    // change does not alter the Mood card.
    const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .get();
    const allSleepLogs = sleepSnap.docs.map(d => d.data());
    const lastSleep = sortByRecencyDesc(allSleepLogs)[0] || null;   // mood source (unchanged)
    const canonicalSleep = selectCanonicalSleepLog(allSleepLogs);   // sleep source (canonical)

    // ── TODAY's sleep, whatever produced it ──────────────────────────────────
    // Distinct from `canonicalSleep`, which is the most recent DETECTED night
    // and therefore excludes manual entries and can be days old. Used for the
    // Dashboard sleep card, whose rule is "today or nothing": a night the phone
    // failed to detect and the user logged by hand must show that hand-entered
    // value, and a day with no record at all must show an empty state rather
    // than an older night's figure presented as current.
    //
    // Automatic wins over manual for the same date: once real detection lands,
    // it supersedes the estimate the user typed in while waiting for it.
    // Between two of the same kind, the most recently written wins.
    const todaysLogs = allSleepLogs.filter(
      l => l && l.date === today && Number(l.sleepDuration) > 0
    );
    const todaysAutomatic = sortByRecencyDesc(todaysLogs.filter(l => l.source !== 'manual'))[0] || null;
    const todaysManual = sortByRecencyDesc(todaysLogs.filter(l => l.source === 'manual'))[0] || null;
    const todaySleep = todaysAutomatic || todaysManual;

    // Canonical current-IST-day productivity record — the SAME document
    // GET /api/productivity/today reads (productivityLogs/{userId}_{today}),
    // so the Dashboard card and the Productivity page can never disagree.
    // Previously this was `.orderBy('date','desc').limit(1)`, which returned
    // the most recent record from ANY day — so on a day with no productivity
    // log yet, the Dashboard silently showed a stale older score as current.
    const prodDoc = await db.collection('productivityLogs').doc(`${userId}_${today}`).get();
    const lastProd = prodDoc.exists ? prodDoc.data() : null;

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

    // ── Burnout: persisted Android assessment ONLY ───────────────────────────
    // Previously this fell back to computeBurnoutRisk() when no document existed,
    // which produced a score from a completely different algorithm (typically
    // 45 / "Moderate") that silently disagreed with the Android app. The fallback
    // is removed: if Android has not persisted an assessment, we report it as
    // unavailable and let the client render an explicit empty state.
    const burnoutSnap = await db.collection('burnoutAssessments')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(1)
      .get();

    const burnout = burnoutSnap.docs.length > 0 ? burnoutSnap.docs[0].data() : null;
    const burnoutAvailable = burnout !== null;

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
          // Canonical detected sleep — same record as the Sleep page. null when
          // Android has not synced one; never substituted from a manual log.
          lastSleepHours: canonicalSleep?.sleepDuration ?? null,
          lastSleepQuality: canonicalSleep?.sleepQuality ?? null,
          sleepAvailable: canonicalSleep !== null,
          // WHICH NIGHT the value above describes. Additive: no existing field
          // changed, and selection is untouched — this only reports the date of
          // the record selectCanonicalSleepLog already chose.
          //
          // Needed because the card showed a duration with no date, so a night
          // from two days ago read as "last night". Taken from the record itself
          // rather than derived in the browser: the browser's clock is not the
          // authority on which night this is, and deriving it there would drift
          // from the record whenever the two disagree.
          lastSleepDate: canonicalSleep?.date ?? null,
          // Today's sleep for the Dashboard card. null means "nothing recorded
          // today", which the client must render as empty — NOT by falling back
          // to lastSleepHours, which is an older night.
          todaySleepHours: todaySleep?.sleepDuration ?? null,
          todaySleepSource: todaySleep ? (todaySleep.source === 'manual' ? 'manual' : 'automatic') : null,
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
          // available=false means "Android has not synced an assessment yet".
          // All value fields are null in that case — never a substituted number.
          available: burnoutAvailable,
          riskScore: burnoutAvailable ? burnout.riskScore : null,
          riskLevel: burnoutAvailable ? burnout.riskLevel : null,
          assessment: burnoutAvailable ? (burnout.assessment ?? null) : null,
          topWarning: burnoutAvailable ? ((burnout.warnings || [])[0] ?? null) : null,
          warnings: burnoutAvailable ? (burnout.warnings || []) : [],
          factors: burnoutAvailable ? (burnout.factors || []) : [],
          wellbeing: burnoutAvailable ? (burnout.wellbeing ?? null) : null,
          date: burnoutAvailable ? (burnout.date ?? null) : null
        },
        featureCards: {
          study: { weeklyHours: weeklyStudyHours, sessionCount },
          sleep: { lastDuration: canonicalSleep?.sleepDuration ?? null, lastQuality: canonicalSleep?.sleepQuality ?? null },
          burnout: {
            riskScore: burnoutAvailable ? burnout.riskScore : null,
            riskLevel: burnoutAvailable ? burnout.riskLevel : null
          },
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

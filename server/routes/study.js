const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { getLocalDateString } = require('../utils/dateUtils');

// ─── Canonical Timezone ───────────────────────────────────────────────────────
//
// Android uses TimeZone.currentSystemDefault() (Java SimpleDateFormat with no
// explicit TZ, defaulting to the device timezone) = IST (UTC+05:30).
//
// All calendar-day and calendar-week computations on the backend MUST use IST
// so that a session starting at Monday 00:01 IST is attributed to Monday, not
// Sunday (which it would be in UTC).
//
// Stored timestamps are ISO UTC strings (e.g. "2026-08-09T18:31:00.000Z").
// We never rewrite stored data — we only interpret timestamps through the IST lens.
//
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000; // +05:30 = 19800000 ms

/**
 * Convert a UTC Date (or ISO string) to the equivalent IST wall-clock time,
 * still represented as a JavaScript Date object.
 * e.g. 2026-08-09T18:30:00Z → 2026-08-10T00:00:00 (IST midnight, Monday).
 */
function toIST(dateOrString) {
  const utcMs = new Date(dateOrString).getTime();
  return new Date(utcMs + IST_OFFSET_MS);
}

/**
 * Return the IST calendar date (YYYY-MM-DD) for any UTC timestamp.
 * Uses UTC methods on the IST-shifted Date so no local TZ of the server leaks in.
 */
function getISTDateString(dateOrString) {
  const ist = toIST(dateOrString);
  const y = ist.getUTCFullYear();
  const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const d = String(ist.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Return the day name (Mon, Tue … Sun) for any UTC timestamp, evaluated in IST.
 * IST-shifted Date → getUTCDay() → 0=Sun,1=Mon…6=Sat → map to name.
 */
const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
function getISTDayName(dateOrString) {
  return DAY_NAMES[toIST(dateOrString).getUTCDay()];
}

/**
 * IST day-of-week index 0=Mon … 6=Sun (matches Android dayOfWeek.ordinal in
 * kotlinx.datetime where MONDAY=0, TUESDAY=1 … SUNDAY=6).
 */
function getISTMondayBasedDayIndex(dateOrString) {
  const utcDay = toIST(dateOrString).getUTCDay(); // 0=Sun,1=Mon…6=Sat
  return utcDay === 0 ? 6 : utcDay - 1;           // 0=Mon,1=Tue…6=Sun
}

/**
 * Return the Monday 00:00:00 IST of the current week as an ISO UTC string.
 * This is the Firestore-level startTime lower bound for "this week".
 *
 * e.g. If today is Sunday 2026-08-10 in IST:
 *   IST Monday start = 2026-08-04 00:00:00 IST = 2026-08-03T18:30:00.000Z
 */
function getWeekStartISO() {
  const nowIST = toIST(new Date()); // current IST wall-clock as a pseudo-UTC Date
  const dayOfWeek = nowIST.getUTCDay(); // 0=Sun,1=Mon…6=Sat
  const daysFromMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  // Rewind to Monday 00:00:00 IST
  const mondayIST = new Date(nowIST);
  mondayIST.setUTCDate(nowIST.getUTCDate() - daysFromMonday);
  mondayIST.setUTCHours(0, 0, 0, 0);
  // Convert back to real UTC by subtracting the IST offset
  const mondayUTC = new Date(mondayIST.getTime() - IST_OFFSET_MS);
  return mondayUTC.toISOString();
}

// @route   POST api/study/start
// @desc    Start a study session
router.post('/start', auth, async (req, res) => {
  const { subject, notes } = req.body;
  const userId = req.user.uid;
  const startTime = new Date().toISOString();
  try {
    const docRef = await db.collection('studySessions').add({
      userId,
      subject: subject || null,
      notes: notes || null,
      date: getISTDateString(startTime),  // IST calendar date
      startTime,
      endTime: null,
      duration: null,
      isActive: true,
      createdAt: startTime,
    });
    const doc = await docRef.get();
    res.json({ success: true, session: { id: doc.id, ...doc.data() } });
  } catch (err) {
    console.error('Error starting study session:', err.message);
    res.status(500).json({ success: false, message: 'Database error' });
  }
});

// @route   PATCH api/study/stop/:sessionId
// @desc    Stop a study session
router.patch('/stop/:sessionId', auth, async (req, res) => {
  const { sessionId } = req.params;
  const endTime = new Date().toISOString();
  try {
    const docRef = db.collection('studySessions').doc(sessionId);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({ success: false, message: 'Session not found' });
    }

    const session = doc.data();

    if (session.userId !== req.user.uid) {
      return res.status(403).json({ success: false, message: 'Not authorized' });
    }

    // ── Idempotent stop ──────────────────────────────────────────────────────
    // A second stop for the same session returns the session AS IT ALREADY IS,
    // without recomputing anything.
    //
    // Without this the endpoint actively corrupts on retry: `duration` below is
    // derived from `startTime` to NOW, and `endTime`/`date` are overwritten
    // unconditionally — so every repeat call stretched the session to the moment
    // of the retry. A client that timed out after the server had already
    // committed, or died between POST and response, or retried a queued stop,
    // would inflate the recorded study time each time it tried again, and could
    // move the session onto a different IST day.
    //
    // Retrying is now safe, which is what lets the client keep a durable pending
    // stop and flush it whenever it can — the same guarantee the deterministic
    // `${uid}_${date}_automatic` document id gives automatic sleep writes.
    if (session.isActive === false) {
      return res.json({
        success: true,
        alreadyStopped: true,
        session: { id: doc.id, ...session },
      });
    }

    const duration = Math.round((new Date(endTime) - new Date(session.startTime)) / 60000);
    // Use IST date for the date field (matches Android's getLocalDateString() in IST)
    await docRef.update({
      endTime,
      duration,
      isActive: false,
      date: getISTDateString(endTime),
      updatedAt: endTime,
    });
    const updatedDoc = await docRef.get();
    res.json({ success: true, session: { id: updatedDoc.id, ...updatedDoc.data() } });
  } catch (err) {
    console.error('Error stopping study session:', err.message);
    res.status(500).json({ success: false, message: 'Database error' });
  }
});

// @route   GET api/study/stats/weekly
// @desc    Returns study stats for the CURRENT calendar week (Mon 00:00 IST → Sun 23:59:59 IST).
//          "Today" and daily breakdown also use IST calendar dates.
//          Matches Android: TimeZone.currentSystemDefault() = IST, dayOfWeek.ordinal Mon=0…Sun=6.
router.get('/stats/weekly', auth, async (req, res) => {
  const userId = req.user.uid;

  // ── IST-anchored boundaries ──────────────────────────────────────────────
  const weekStartISO = getWeekStartISO();
  const todayStr = getISTDateString(new Date()); // e.g. "2026-08-10" in IST

  // ── Debug log (server-side only, never exposed to browser) ───────────────
  console.log('[STUDY PARITY DEBUG]', {
    timezone: 'IST (UTC+05:30)',
    localToday: todayStr,
    localWeekStart: weekStartISO,
    localWeekEnd: (() => {
      // Sunday 23:59:59 IST of this week
      const s = new Date(weekStartISO);
      const end = new Date(s.getTime() + 7 * 24 * 60 * 60 * 1000 - 1);
      return getISTDateString(end) + ' 23:59:59 IST';
    })(),
  });

  try {
    // Firestore query: only sessions from this week's Monday 00:00 IST onward
    const snapshot = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekStartISO)
      .get();

    const sessions = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));

    // ── Separate completed vs active ─────────────────────────────────────
    const completedSessions = sessions.filter(s => s.isActive === false && s.duration != null);
    const activeSession = sessions
      .filter(s => {
        if (s.isActive !== true) return false;
        const ageHours = (Date.now() - new Date(s.startTime).getTime()) / 3600000;
        return ageHours < 24; // ignore zombie sessions
      })
      .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))[0] || null;

    // ── Week totals (completed only; active duration is null) ────────────
    const weekMinutes = completedSessions.reduce((sum, s) => sum + (s.duration || 0), 0);
    const weekSeconds = weekMinutes * 60;
    const totalHours = Math.round((weekMinutes / 60) * 10) / 10;

    // ── Today's total (completed + active elapsed if started today) ──────
    const completedTodayMinutes = completedSessions
      .filter(s => getISTDateString(s.startTime) === todayStr)
      .reduce((sum, s) => sum + (s.duration || 0), 0);

    let activeElapsedSeconds = 0;
    if (activeSession && getISTDateString(activeSession.startTime) === todayStr) {
      activeElapsedSeconds = Math.max(
        0,
        Math.floor((Date.now() - new Date(activeSession.startTime).getTime()) / 1000)
      );
    }
    const todaySeconds = completedTodayMinutes * 60 + activeElapsedSeconds;
    const todayMinutes = Math.floor(todaySeconds / 60);

    // ── Daily breakdown (Mon–Sun, IST calendar day) ──────────────────────
    // Android maps: dayOfWeek.ordinal → Mon=0, Tue=1 … Sun=6
    // Android chart: weeklyStudyData[0]=Mon … [6]=Sun
    // Backend dailyBreakdown must use the same day names Android reads:
    //   dayMap["Mon"], dayMap["Tue"], … dayMap["Sun"]
    const dailyMap = {};
    const subjectMap = {};

    completedSessions.forEach(s => {
      if (!s.startTime) return;
      const dayName = getISTDayName(s.startTime); // "Mon", "Tue", … "Sun" in IST
      dailyMap[dayName] = (dailyMap[dayName] || 0) + (s.duration || 0);
      const subject = s.subject || null;
      if (subject) {
        subjectMap[subject] = (subjectMap[subject] || 0) + (s.duration || 0);
      }
    });

    // ── Debug log (continued) ────────────────────────────────────────────
    console.log('[STUDY PARITY DEBUG]', {
      weekSessions: sessions.length,
      completedSessions: completedSessions.length,
      todaySessions: completedSessions.filter(s => getISTDateString(s.startTime) === todayStr).length,
      todayMinutes,
      weekMinutes,
      dailyBreakdown: dailyMap,
      subjectBreakdown: subjectMap,
      activeSession: activeSession
        ? { id: activeSession.id, startTime: activeSession.startTime, istDate: getISTDateString(activeSession.startTime) }
        : null,
    });

    res.json({
      success: true,
      stats: {
        // Canonical week fields
        weekMinutes,
        weekSeconds,
        totalMinutes: weekMinutes,  // kept for Android backward-compat (reads stats.totalHours)
        totalHours,                 // kept for Android backward-compat
        // Canonical today fields
        todaySeconds,
        todayMinutes,
        // Active session (Web/Android adds live elapsed client-side)
        activeSession: activeSession
          ? { isActive: true, id: activeSession.id, startTime: activeSession.startTime, subject: activeSession.subject }
          : null,
        hasActiveSession: Boolean(activeSession),
        // Counts
        sessionCount: completedSessions.length,
        sessionsCount: completedSessions.length,
        // Daily breakdown in IST, Mon–Sun, minutes per day
        dailyBreakdown: dailyMap,
        dailyTotals: dailyMap,  // kept for Android backward-compat
        // Subject breakdown for this week
        subjectBreakdown: Object.keys(subjectMap).length > 0 ? subjectMap : null,
        // Recent sessions
        recentSessions: completedSessions
          .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
          .slice(0, 5),
      }
    });
  } catch (err) {
    console.error('Error fetching weekly stats:', err.message);
    res.status(500).json({ success: false });
  }
});

// @route   POST api/study/log-offline
// @desc    Log a completed session from offline queue
router.post('/log-offline', auth, async (req, res) => {
  const { subject, duration, startTime } = req.body;
  const userId = req.user.uid;
  const actualStartTime = startTime || new Date().toISOString();
  const start = new Date(actualStartTime);
  const end = new Date(start.getTime() + (duration || 0) * 60000);
  const endTime = end.toISOString();

  try {
    // Clean up any zombie active session that matches this start time closely
    const zombieSnapshot = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('isActive', '==', true)
      .get();

    const batch = db.batch();
    zombieSnapshot.docs.forEach(doc => {
      const zData = doc.data();
      if (Math.abs(new Date(zData.startTime) - start) < 120000) {
        batch.delete(doc.ref);
      }
    });

    // ── Deterministic identity for offline sessions ──────────────────────────
    // Keyed by (user, exact start instant), because that pair IS the logical
    // identity of a completed session — one person cannot begin two sessions at
    // the same millisecond.
    //
    // Previously this was `.doc()` + `.add()`-style random ids, so every replay
    // of the same queued entry created ANOTHER document: a flush that timed out
    // after the server had committed, or a queue retried from two clients, or
    // the same entry retried on the next app open, each silently duplicated the
    // session and inflated the totals. The client now retries this queue
    // deliberately, which makes that failure mode reachable rather than rare.
    //
    // With merge, a replay resolves to the SAME record — the same guarantee
    // `${uid}_${date}_automatic` gives automatic sleep writes, and the reason
    // retrying is safe at all.
    const offlineDocId = `${userId}_${new Date(actualStartTime).getTime()}_offline`;
    const docRef = db.collection('studySessions').doc(offlineDocId);
    batch.set(docRef, {
      userId,
      subject: subject || null,
      notes: null,
      date: getISTDateString(actualStartTime),  // IST calendar date
      startTime: actualStartTime,
      endTime,
      duration: duration || 0,
      isActive: false,
      createdAt: actualStartTime,
      updatedAt: endTime,
    }, { merge: true });

    await batch.commit();
    res.json({ success: true, message: 'Offline session logged' });
  } catch (err) {
    console.error('Error logging offline session:', err.message);
    res.status(500).json({ success: false, message: 'Database error' });
  }
});

module.exports = router;
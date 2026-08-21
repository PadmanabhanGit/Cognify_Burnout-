const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

// ─── Canonical Timezone ───────────────────────────────────────────────────────
// Same IST (UTC+05:30) convention already used by study.js / burnout.js /
// dashboard.js, duplicated here rather than factored into dateUtils.js to avoid
// touching a shared file for this task.
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

function toIST(dateOrString) {
  return new Date(new Date(dateOrString).getTime() + IST_OFFSET_MS);
}

function getISTDateString(dateOrString = new Date()) {
  const ist = toIST(dateOrString);
  const y = ist.getUTCFullYear();
  const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const d = String(ist.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * The last `n` IST calendar-date strings ending today (inclusive), oldest
 * first. Replaces a Monday-anchored "current week" window: that window
 * excludes yesterday entirely on any Monday (yesterday is always in the
 * PREVIOUS calendar week), which permanently broke the "vs Yesterday"
 * comparison and the 7-Day Trend chart once a week, for every account, not
 * just for freshly-seeded test data. A trailing window has no such gap.
 */
function getTrailingISTDates(n) {
  const nowIST = toIST(new Date());
  const dates = [];
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(nowIST);
    d.setUTCDate(nowIST.getUTCDate() - i);
    const y = d.getUTCFullYear();
    const m = String(d.getUTCMonth() + 1).padStart(2, '0');
    const day = String(d.getUTCDate()).padStart(2, '0');
    dates.push(`${y}-${m}-${day}`);
  }
  return dates;
}

/** Every IST calendar-date string from the 1st of the current month through today. */
function getISTMonthToDateDates() {
  const nowIST = toIST(new Date());
  const y = nowIST.getUTCFullYear();
  const m = nowIST.getUTCMonth();
  const today = nowIST.getUTCDate();
  const dates = [];
  for (let d = 1; d <= today; d++) {
    dates.push(`${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`);
  }
  return dates;
}

/** Deterministic one-document-per-user-per-IST-day id. */
function productivityDocId(userId, istDate) {
  return `${userId}_${istDate}`;
}

// @route   POST api/productivity/log
// @desc    Upsert the CANONICAL productivity document for the current IST day.
//          Previously this used `.collection('productivityLogs').add(...)`,
//          so every screen visit created a new document for the same day.
//          Deterministic id + merge:true makes the same user/day one document.
router.post('/log', auth, async (req, res) => {
  const { productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes, date } = req.body;
  const userId = req.user.uid;
  const logDate = getISTDateString(date || new Date());
  const now = new Date().toISOString();

  try {
    const docRef = db.collection('productivityLogs').doc(productivityDocId(userId, logDate));
    const existing = await docRef.get();

    await docRef.set({
      userId,
      date: logDate,
      productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes,
      createdAt: existing.exists ? (existing.data().createdAt || now) : now,
      updatedAt: now,
    }, { merge: true });

    const doc = await docRef.get();
    res.json({ success: true, log: { id: doc.id, ...doc.data() } });
  } catch (err) {
    console.error('Error saving productivity log:', err.message);
    res.status(500).json({ success: false, message: 'Error saving log' });
  }
});

// @route   GET api/productivity/today
// @desc    The canonical productivity record for the CURRENT IST date.
//          Primary path is a single deterministic document read (no query, no
//          index, no ordering ambiguity). Response contract unchanged:
//          { success, log } where log is the document or null.
router.get('/today', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = getISTDateString(new Date());

  try {
    const canonicalRef = db.collection('productivityLogs').doc(productivityDocId(userId, today));
    const canonicalDoc = await canonicalRef.get();
    if (canonicalDoc.exists) {
      return res.json({ success: true, log: { id: canonicalDoc.id, ...canonicalDoc.data() } });
    }

    // Fallback for documents written before this deploy (auto-id, `.add()`),
    // which will never exist at the deterministic id above. Same cost as the
    // query this endpoint already ran before this change — not a regression,
    // and only reached when the fast path finds nothing. Not used for /weekly.
    const snapshot = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .get();

    const legacyMatches = snapshot.docs
      .map(d => ({ id: d.id, ...d.data() }))
      .filter(l => getISTDateString(l.date) === today)
      .sort((a, b) => {
        const aTime = new Date(a.updatedAt || a.createdAt || a.date || 0).getTime();
        const bTime = new Date(b.updatedAt || b.createdAt || b.date || 0).getTime();
        return bTime - aTime;
      });

    res.json({ success: true, log: legacyMatches[0] || null });
  } catch (err) {
    console.error('Error fetching productivity today:', err.message);
    res.status(500).json({ success: false });
  }
});

// @route   GET api/productivity/weekly
// @desc    Real productivity records for the trailing 7 IST calendar days
//          ending today, plus a month-to-date average. Deterministic document
//          reads via getAll() only — no collection query, no composite index.
//          Days/dates with no canonical record are reported as unavailable
//          and excluded from the average, never fabricated as zero.
router.get('/weekly', auth, async (req, res) => {
  const userId = req.user.uid;
  const trailingDates = getTrailingISTDates(7);
  const monthDates = getISTMonthToDateDates();

  try {
    const refs = trailingDates.map(date => db.collection('productivityLogs').doc(productivityDocId(userId, date)));
    const snapshots = await db.getAll(...refs);

    const days = snapshots.map((snap, i) => {
      if (!snap.exists) {
        return { date: trailingDates[i], available: false, productivityScore: null, focusHours: null };
      }
      const data = snap.data();
      return {
        date: trailingDates[i],
        available: true,
        productivityScore: data.productivityScore ?? null,
        focusHours: data.focusHours ?? null,
      };
    });

    // Month-to-date average. The trailing 7 days above already cover the
    // tail of the month, so only the earlier-in-month dates need fetching
    // when the month is more than a week old.
    const alreadyFetched = new Map(trailingDates.map((d, i) => [d, days[i]]));
    const extraDates = monthDates.filter(d => !alreadyFetched.has(d));
    const extraRefs = extraDates.map(date => db.collection('productivityLogs').doc(productivityDocId(userId, date)));
    const extraSnapshots = extraRefs.length ? await db.getAll(...extraRefs) : [];
    const extraScores = extraSnapshots
      .map(snap => (snap.exists ? snap.data().productivityScore : null))
      .filter(score => typeof score === 'number');

    const monthScores = [
      ...days.filter(d => typeof d.productivityScore === 'number').map(d => d.productivityScore),
      ...extraScores,
    ];
    const monthToDate = {
      average: monthScores.length ? Math.round(monthScores.reduce((a, b) => a + b, 0) / monthScores.length) : null,
      availableDays: monthScores.length,
      totalDays: monthDates.length,
    };

    res.json({ success: true, days, monthToDate });
  } catch (err) {
    console.error('Error fetching weekly productivity:', err.message);
    res.status(500).json({ success: false });
  }
});

module.exports = router;

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

/** Monday..Sunday IST calendar-date strings for the CURRENT week. */
function getISTWeekDates() {
  const nowIST = toIST(new Date());
  const dayOfWeek = nowIST.getUTCDay(); // 0=Sun,1=Mon…6=Sat
  const daysFromMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  const mondayIST = new Date(nowIST);
  mondayIST.setUTCDate(nowIST.getUTCDate() - daysFromMonday);
  mondayIST.setUTCHours(0, 0, 0, 0);

  const dates = [];
  for (let i = 0; i < 7; i++) {
    const d = new Date(mondayIST);
    d.setUTCDate(mondayIST.getUTCDate() + i);
    const y = d.getUTCFullYear();
    const m = String(d.getUTCMonth() + 1).padStart(2, '0');
    const day = String(d.getUTCDate()).padStart(2, '0');
    dates.push(`${y}-${m}-${day}`);
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
// @desc    Real productivity records for the CURRENT IST calendar week
//          (Monday-Sunday, matching study.js). Exactly 7 deterministic document
//          reads via getAll() — no collection query, no composite index, no
//          rolling window. Days with no canonical record are reported as
//          unavailable, never fabricated.
router.get('/weekly', auth, async (req, res) => {
  const userId = req.user.uid;
  const weekDates = getISTWeekDates();

  try {
    const refs = weekDates.map(date => db.collection('productivityLogs').doc(productivityDocId(userId, date)));
    const snapshots = await db.getAll(...refs);

    const days = snapshots.map((snap, i) => {
      if (!snap.exists) {
        return { date: weekDates[i], available: false, productivityScore: null, focusHours: null };
      }
      const data = snap.data();
      return {
        date: weekDates[i],
        available: true,
        productivityScore: data.productivityScore ?? null,
        focusHours: data.focusHours ?? null,
      };
    });

    res.json({ success: true, days });
  } catch (err) {
    console.error('Error fetching weekly productivity:', err.message);
    res.status(500).json({ success: false });
  }
});

module.exports = router;

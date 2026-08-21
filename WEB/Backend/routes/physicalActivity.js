const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

// ─── IST helpers (must match dashboard.js / study.js / burnout.js / usage.js
// / report.js exactly) ──────────────────────────────────────────────────────
// The server has no TZ override and defaults to UTC. Without this, GET /today
// (which has no client-supplied date to fall back on at all) would look up
// the wrong day's document for the first ~5.5 hours of every IST day — Usage
// says today's date, Physical Activity says yesterday's, for the same real
// moment.
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

function getISTDateString(dateOrString = new Date()) {
  const ist = new Date(new Date(dateOrString).getTime() + IST_OFFSET_MS);
  const y = ist.getUTCFullYear();
  const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const d = String(ist.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

// @route   POST api/activity/sync
router.post('/sync', auth, async (req, res) => {
  const { steps, calories, activeMinutes, source, date } = req.body;
  const userId = req.user.uid;
  const syncDate = date || getISTDateString();
  const docId = `${userId}_${syncDate}`; // one doc per user per day, overwrite on resync

  try {
    await db.collection('physicalActivity').doc(docId).set({
      userId,
      date: syncDate,
      steps,
      calories,
      activeMinutes,
      source,
    }, { merge: true });
    res.json({ success: true, message: 'Activity updated' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Activity sync failed' });
  }
});

// @route   GET api/activity/today
router.get('/today', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = getISTDateString();
  const docId = `${userId}_${today}`;

  try {
    const doc = await db.collection('physicalActivity').doc(docId).get();
    res.json({ success: true, activity: doc.exists ? doc.data() : { steps: 0, calories: 0, activeMinutes: 0 } });
  } catch (err) {
    res.status(500).json({ success: false });
  }
});

module.exports = router;
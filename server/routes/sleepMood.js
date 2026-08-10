const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { getLocalDateString, normalizeDateValue } = require('../utils/dateUtils');
const { selectCanonicalSleepLog, sortByRecencyDesc, hasValidDetectedSleep } = require('../utils/sleepSelection');

// @route   POST api/sleep-mood/log
router.post('/log', auth, async (req, res) => {
  const { sleepDuration, sleepQuality, mood, moodScore, notes, date } = req.body;
  const userId = req.user.uid;
  const logDate = normalizeDateValue(date || new Date());
  const timestamp = new Date().toISOString();

  // Explicit source, trusted when the caller sets it. Older app builds that
  // don't send `source` yet fall back to the same sleepStart/sleepEnd-presence
  // heuristic sleepSelection.js already used before this change — so this is
  // not a behavior change for existing clients, only an upgrade path for new
  // ones. Any unrecognized value is treated the same as "not provided".
  const requestedSource = req.body.source === 'automatic' || req.body.source === 'manual'
    ? req.body.source
    : null;
  const source = requestedSource || (hasValidDetectedSleep(req.body) ? 'automatic' : 'manual');

  try {
    const docRef = await db.collection('sleepMoodLogs').add({
      userId,
      date: logDate,
      createdAt: timestamp,
      updatedAt: timestamp,
      sleepDuration: sleepDuration ?? null,
      sleepQuality: sleepQuality ?? null,
      mood: mood ?? null,
      moodScore: moodScore ?? null,
      notes: notes ?? null,
      sleepStart: req.body.sleepStart ?? null,
      sleepEnd: req.body.sleepEnd ?? null,
      awakeningCount: req.body.awakeningCount ?? null,
      disturbanceScore: req.body.disturbanceScore ?? null,
      source,
    });

    const doc = await docRef.get();
    res.json({ success: true, log: { id: doc.id, ...doc.data() } });
  } catch (err) {
    console.error('Error saving sleep/mood log:', err.message);
    res.status(500).json({ success: false, message: 'Error saving log' });
  }
});

// @route   GET api/sleep-mood/logs
router.get('/logs', auth, async (req, res) => {
  const userId = req.user.uid;
  const limit = parseInt(req.query.limit) || 7;

  try {
    const snapshot = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .get();

    const allLogs = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));

    // `logs` keeps its existing contract exactly: newest-first, same fields,
    // same limit — manual mood logging and any other consumer are unaffected.
    const logs = sortByRecencyDesc(allLogs).slice(0, limit);

    // `canonical` is additive: the newest record that actually contains
    // Android's detected sleepStart/sleepEnd. Selected from ALL logs, not just
    // the limited slice, so a newer manual mood entry can no longer hide it.
    // null when Android has not synced a detected session.
    const canonical = selectCanonicalSleepLog(allLogs);

    res.json({
      success: true,
      logs,
      canonical,
      canonicalAvailable: canonical !== null
    });
  } catch (err) {
    console.error('Error fetching logs:', err.message);
    res.status(500).json({ success: false, message: 'Error fetching logs' });
  }
});

// @route   GET api/sleep-mood/trends/sleep
router.get('/trends/sleep', auth, async (req, res) => {
  const userId = req.user.uid;
  const days = parseInt(req.query.days) || 30;

  try {
    const snapshot = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .get();

    const trends = snapshot.docs
      .map(d => { const data = d.data(); return { date: normalizeDateValue(data.date || data.createdAt), sleepDuration: data.sleepDuration, sleepQuality: data.sleepQuality }; })
      .sort((a, b) => a.date.localeCompare(b.date))
      .slice(0, days);

    res.json({ success: true, trends });
  } catch (err) {
    console.error('Error fetching trends:', err.message);
    res.status(500).json({ success: false });
  }
});

module.exports = router;
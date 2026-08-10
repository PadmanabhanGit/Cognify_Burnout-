const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { getLocalDateString, normalizeDateValue } = require('../utils/dateUtils');
const { selectCanonicalSleepLog, sortByRecencyDesc, hasValidDetectedSleep, isAutomaticSource } = require('../utils/sleepSelection');

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

  const payload = {
    userId,
    date: logDate,
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
  };

  try {
    let docRef;

    if (source === 'automatic') {
      // ── Idempotent automatic writes ──────────────────────────────────────
      // One detected night = one document. The identity is (userId, sleep
      // date, automatic), so a re-analysis of the same night updates the same
      // record instead of appending another one. Before this, `.add()` created
      // a new document on every call, and because SleepMonitoringEngine POSTs
      // once per Room insert, the Room duplicate race replicated itself into
      // Firestore.
      //
      // Deliberately NOT applied to manual logs: a user may legitimately log
      // mood more than once in a day, and collapsing those onto one id would
      // silently destroy entries. Manual logs keep `.add()` exactly as before.
      const docId = `${userId}_${logDate}_automatic`;
      docRef = db.collection('sleepMoodLogs').doc(docId);

      // createdAt is preserved on re-write so the record keeps the timestamp of
      // the night it was first detected — sleepSelection.js's recencyOf() reads
      // createdAt first, so overwriting it would reshuffle canonical selection.
      const existing = await docRef.get();
      await docRef.set(
        { ...payload, createdAt: existing.exists ? (existing.data().createdAt || timestamp) : timestamp },
        { merge: true }
      );
    } else {
      docRef = await db.collection('sleepMoodLogs').add({ ...payload, createdAt: timestamp });
    }

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

    const allLogs = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));

    // ── 1. Automatic detected sleep only ────────────────────────────────────
    // Same predicate the canonical selector uses, so Web history and the Web
    // Sleep Analysis page can never disagree about what counts as a real night.
    // Manual mood/sleep entries are excluded here and can never become
    // automatic history.
    const detected = allLogs.filter(log => isAutomaticSource(log) && hasValidDetectedSleep(log));

    // ── 2. A real trailing date window ──────────────────────────────────────
    // Previously this sorted ASCENDING and then sliced, so `days=30` returned
    // the OLDEST 30 records ever recorded and labelled them "30-Day". `days` is
    // now an actual calendar window ending today, inclusive.
    const cutoff = new Date();
    cutoff.setHours(0, 0, 0, 0);
    cutoff.setDate(cutoff.getDate() - (days - 1));
    const cutoffDate = normalizeDateValue(cutoff);

    const inWindow = detected.filter(log => {
      const d = normalizeDateValue(log.date || log.createdAt);
      return typeof d === 'string' && d >= cutoffDate;
    });

    // ── 3. One entry per DISTINCT night ─────────────────────────────────────
    // Duplicate automatic documents for the same night (written before the
    // deterministic-id fix) must not become separate trend points. Newest write
    // wins per date; nothing is averaged or invented.
    const byDate = new Map();
    for (const log of sortByRecencyDesc(inWindow)) {
      const d = normalizeDateValue(log.date || log.createdAt);
      if (!byDate.has(d)) {
        byDate.set(d, {
          date: d,
          // Never coerced to 0 — a missing metric stays null so the client can
          // render an honest gap instead of a measured zero.
          sleepQuality: log.sleepQuality ?? null,
          sleepDuration: log.sleepDuration ?? null,
        });
      }
    }

    // Chronological (oldest -> newest) for left-to-right plotting.
    const trends = [...byDate.values()].sort((a, b) => a.date.localeCompare(b.date));

    res.json({
      success: true,
      trends,
      windowDays: days,
      // Distinct real nights actually found — the client must label from this,
      // never from `days`.
      nightCount: trends.length,
    });
  } catch (err) {
    console.error('Error fetching trends:', err.message);
    res.status(500).json({ success: false });
  }
});

module.exports = router;
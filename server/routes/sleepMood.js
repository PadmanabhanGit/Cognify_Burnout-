const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { getLocalDateString, normalizeDateValue } = require('../utils/dateUtils');

// @route   POST api/sleep-mood/log
router.post('/log', auth, async (req, res) => {
  const { sleepDuration, sleepQuality, mood, moodScore, notes, date } = req.body;
  const userId = req.user.uid;
  const logDate = normalizeDateValue(date || new Date());
  const timestamp = new Date().toISOString();

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
    const logs = snapshot.docs
      .map(d => ({ id: d.id, ...d.data() }))
      .sort((a, b) => {
        const aTime = new Date(a.createdAt || a.updatedAt || a.date || 0).getTime();
        const bTime = new Date(b.createdAt || b.updatedAt || b.date || 0).getTime();
        return bTime - aTime;
      })
      .slice(0, limit);
    res.json({ success: true, logs });
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
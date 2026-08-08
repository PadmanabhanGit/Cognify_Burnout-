const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

function getLocalDateString(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function normalizeDateValue(value) {
  if (!value) return getLocalDateString();
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
    const parsed = new Date(trimmed);
    if (!Number.isNaN(parsed.getTime())) return getLocalDateString(parsed);
    return getLocalDateString();
  }
  if (value instanceof Date) return getLocalDateString(value);
  return getLocalDateString();
}

// @route   POST api/productivity/log
router.post('/log', auth, async (req, res) => {
  const { productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes, date } = req.body;
  const userId = req.user.uid;
  const logDate = date || new Date().toISOString();

  try {
    const docRef = await db.collection('productivityLogs').add({
      userId,
      date: logDate,
      productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes,
    });
    const doc = await docRef.get();
    res.json({ success: true, log: { id: doc.id, ...doc.data() } });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Error saving log' });
  }
});

// @route   GET api/productivity/today
router.get('/today', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = getLocalDateString();

  try {
    const snapshot = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .get();

    const matchingLog = snapshot.docs
      .map(doc => ({ id: doc.id, ...doc.data() }))
      .find(log => normalizeDateValue(log.date) === today) || null;

    res.json({ success: true, log: matchingLog });
  } catch (err) {
    res.status(500).json({ success: false });
  }
});

module.exports = router;
const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

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
  const today = new Date().toISOString().split('T')[0];

  try {
    const snapshot = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .where('date', '>=', today)
      .where('date', '<', today + '\uf8ff')
      .limit(1)
      .get();

    res.json({ success: true, log: snapshot.empty ? null : { id: snapshot.docs[0].id, ...snapshot.docs[0].data() } });
  } catch (err) {
    res.status(500).json({ success: false });
  }
});

module.exports = router;
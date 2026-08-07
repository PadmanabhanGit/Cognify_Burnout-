const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

// @route   POST api/activity/sync
router.post('/sync', auth, async (req, res) => {
  const { steps, calories, activeMinutes, source, date } = req.body;
  const userId = req.user.uid;
  const syncDate = date || new Date().toISOString().split('T')[0];
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
  const today = new Date().toISOString().split('T')[0];
  const docId = `${userId}_${today}`;

  try {
    const doc = await db.collection('physicalActivity').doc(docId).get();
    res.json({ success: true, activity: doc.exists ? doc.data() : { steps: 0, calories: 0, activeMinutes: 0 } });
  } catch (err) {
    res.status(500).json({ success: false });
  }
});

module.exports = router;
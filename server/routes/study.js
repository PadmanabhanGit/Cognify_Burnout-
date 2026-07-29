const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

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
      startTime,
      endTime: null,
      duration: null,
      isActive: true,
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

    const duration = Math.round((new Date(endTime) - new Date(session.startTime)) / 60000);
    await docRef.update({ endTime, duration, isActive: false });
    const updatedDoc = await docRef.get();
    res.json({ success: true, session: { id: updatedDoc.id, ...updatedDoc.data() } });
  } catch (err) {
    console.error('Error stopping study session:', err.message);
    res.status(500).json({ success: false, message: 'Database error' });
  }
});

module.exports = router;
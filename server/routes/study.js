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

// @route   GET api/study/stats/weekly
router.get('/stats/weekly', auth, async (req, res) => {
  const userId = req.user.uid;
  const weekAgo = new Date();
  weekAgo.setDate(weekAgo.getDate() - 7);

  try {
    const snapshot = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekAgo.toISOString())
      .get();

    const sessions = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
    const totalMinutes = sessions.reduce((sum, s) => sum + (s.duration || 0), 0);
    const totalHours = Math.round((totalMinutes / 60) * 10) / 10;

    const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    const dailyMap = {};
    let todayMinutes = 0;
    const todayStr = new Date().toISOString().split('T')[0];

    sessions.forEach(s => {
      if (!s.startTime) return;
      const sDate = new Date(s.startTime);
      const dayName = dayNames[sDate.getDay()];
      
      dailyMap[dayName] = (dailyMap[dayName] || 0) + (s.duration || 0);
      
      if (s.startTime.split('T')[0] === todayStr) {
        todayMinutes += (s.duration || 0);
      }
    });

    const activeSession = sessions.find(s => s.isActive === true) || null;

    res.json({
      success: true,
      stats: {
        totalHours,
        totalMinutes,
        todayMinutes,
        sessionCount: sessions.size || sessions.length,
        dailyBreakdown: dailyMap,
        recentSessions: sessions.slice(0, 5),
        activeSession
      }
    });
  } catch (err) {
    console.error('Error fetching weekly stats:', err.message);
    res.status(500).json({ success: false });
  }
});

module.exports = router;
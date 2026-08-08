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
      .get();

    const sessions = snapshot.docs
      .map(d => ({ id: d.id, ...d.data() }))
      .filter(s => {
        if (!s.startTime) return false;
        const startTime = new Date(s.startTime);
        return !Number.isNaN(startTime.getTime()) && startTime >= weekAgo;
      });
    const totalMinutes = sessions.reduce((sum, s) => sum + (s.duration || 0), 0);
    const totalHours = Math.round((totalMinutes / 60) * 10) / 10;

    const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    const dailyMap = {};
    const subjectMap = {};
    let todayMinutes = 0;
    const todayStr = getLocalDateString();

    sessions.forEach(s => {
      if (!s.startTime) return;
      const sDate = new Date(s.startTime);
      const dayName = dayNames[sDate.getDay()];
      
      dailyMap[dayName] = (dailyMap[dayName] || 0) + (s.duration || 0);
      
      if (s.subject) {
        subjectMap[s.subject] = (subjectMap[s.subject] || 0) + (s.duration || 0);
      }
      
      if (normalizeDateValue(s.startTime) === todayStr) {
        todayMinutes += (s.duration || 0);
      }
    });

    const activeSession = sessions
      .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
      .find(s => {
         if (s.isActive !== true) return false;
         // Ignore zombie sessions older than 24 hours
         const ageHours = (new Date() - new Date(s.startTime)) / 3600000;
         return ageHours < 24;
      }) || null;

    res.json({
      success: true,
      stats: {
        totalHours,
        totalMinutes,
        todayMinutes,
        sessionCount: sessions.size || sessions.length,
        sessionsCount: sessions.size || sessions.length,
        dailyBreakdown: dailyMap,
        dailyTotals: dailyMap,
        subjectBreakdown: subjectMap,
        recentSessions: sessions.slice(0, 5),
        activeSession
      }
    });
  } catch (err) {
    console.error('Error fetching weekly stats:', err.message);
    res.status(500).json({ success: false });
  }
});

// @route   POST api/study/log-offline
// @desc    Log a completed session from offline queue
router.post('/log-offline', auth, async (req, res) => {
  const { subject, duration, startTime } = req.body;
  const userId = req.user.uid;
  const actualStartTime = startTime || new Date().toISOString();
  // Calculate end time
  const start = new Date(actualStartTime);
  const end = new Date(start.getTime() + (duration || 0) * 60000);
  const endTime = end.toISOString();

  try {
    // Attempt to clean up any zombie active sessions that match this start time closely
    const zombieSnapshot = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('isActive', '==', true)
      .get();
      
    const batch = db.batch();
    zombieSnapshot.docs.forEach(doc => {
       const zData = doc.data();
       // If zombie started within 2 minutes of this offline log, it's the same session
       if (Math.abs(new Date(zData.startTime) - start) < 120000) {
           batch.delete(doc.ref);
       }
    });
    
    // Add the completed session
    const docRef = db.collection('studySessions').doc();
    batch.set(docRef, {
      userId,
      subject: subject || null,
      notes: null,
      startTime: actualStartTime,
      endTime,
      duration: duration || 0,
      isActive: false,
    });
    
    await batch.commit();
    res.json({ success: true, message: 'Offline session logged' });
  } catch (err) {
    console.error('Error logging offline session:', err.message);
    res.status(500).json({ success: false, message: 'Database error' });
  }
});

module.exports = router;
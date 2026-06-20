const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const auth = require('../middleware/auth');
const db = require('../database');

// @route   POST api/study/start
// @desc    Start a study session
router.post('/start', auth, (req, res) => {
  const { subject, notes } = req.body;
  const userId = req.user.id;
  const id = uuidv4();
  const startTime = new Date().toISOString();

  db.run(
    'INSERT INTO study_sessions (id, userId, subject, startTime, notes, isActive) VALUES (?, ?, ?, ?, ?, 1)',
    [id, userId, subject, startTime, notes],
    function(err) {
      if (err) return res.status(500).json({ success: false, message: 'Database error' });

      db.get('SELECT * FROM study_sessions WHERE id = ?', [id], (err, session) => {
        res.json({ success: true, session });
      });
    }
  );
});

// @route   PATCH api/study/stop/:sessionId
// @desc    Stop a study session
router.patch('/stop/:sessionId', auth, (req, res) => {
  const { sessionId } = req.params;
  const endTime = new Date().toISOString();

  db.get('SELECT * FROM study_sessions WHERE id = ?', [sessionId], (err, session) => {
    if (err || !session) return res.status(404).json({ success: false, message: 'Session not found' });

    const start = new Date(session.startTime);
    const end = new Date(endTime);
    const duration = Math.round((end - start) / 60000); // in minutes

    db.run(
      'UPDATE study_sessions SET endTime = ?, duration = ?, isActive = 0 WHERE id = ?',
      [endTime, duration, sessionId],
      function(err) {
        if (err) return res.status(500).json({ success: false, message: 'Database error' });

        db.get('SELECT * FROM study_sessions WHERE id = ?', [sessionId], (err, updatedSession) => {
          res.json({ success: true, session: updatedSession });
        });
      }
    );
  });
});

module.exports = router;

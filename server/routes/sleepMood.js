const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const auth = require('../middleware/auth');
const db = require('../database');

// @route   POST api/sleep-mood/log
// @desc    Save sleep and mood log
router.post('/log', auth, (req, res) => {
  const { sleepDuration, sleepQuality, mood, moodScore, notes, date } = req.body;
  const userId = req.user.id;
  const id = uuidv4();
  const logDate = date || new Date().toISOString();

  db.run(
    'INSERT INTO sleep_mood_logs (id, userId, date, sleepDuration, sleepQuality, mood, moodScore, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
    [id, userId, logDate, sleepDuration, sleepQuality, mood, moodScore, notes],
    function(err) {
      if (err) return res.status(500).json({ success: false, message: 'Error saving log' });

      db.get('SELECT * FROM sleep_mood_logs WHERE id = ?', [id], (err, log) => {
        res.json({ success: true, log });
      });
    }
  );
});

// @route   GET api/sleep-mood/logs
// @desc    Get recent sleep and mood logs
router.get('/logs', auth, (req, res) => {
  const userId = req.user.id;
  const limit = req.query.limit || 7;

  db.all(
    'SELECT * FROM sleep_mood_logs WHERE userId = ? ORDER BY date DESC LIMIT ?',
    [userId, limit],
    (err, logs) => {
      if (err) return res.status(500).json({ success: false, message: 'Error fetching logs' });
      res.json({ success: true, logs });
    }
  );
});

// @route   GET api/sleep-mood/trends/sleep
router.get('/trends/sleep', auth, (req, res) => {
    const userId = req.user.id;
    const days = req.query.days || 30;

    db.all(
        'SELECT date, sleepDuration, sleepQuality FROM sleep_mood_logs WHERE userId = ? ORDER BY date ASC LIMIT ?',
        [userId, days],
        (err, trends) => {
            if (err) return res.status(500).json({ success: false });
            res.json({ success: true, trends });
        }
    );
});

module.exports = router;

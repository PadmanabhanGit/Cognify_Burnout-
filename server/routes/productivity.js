const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const auth = require('../middleware/auth');
const db = require('../database');

// @route   POST api/productivity/log
// @desc    Log productivity details
router.post('/log', auth, (req, res) => {
  const { productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes, date } = req.body;
  const userId = req.user.id;
  const id = uuidv4();
  const logDate = date || new Date().toISOString();

  db.run(
    'INSERT INTO productivity_logs (id, userId, date, productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
    [id, userId, logDate, productivityScore, focusHours, breakHours, tasksCompleted, tasksPlanned, distractions, notes],
    function(err) {
      if (err) return res.status(500).json({ success: false, message: 'Error saving log' });

      db.get('SELECT * FROM productivity_logs WHERE id = ?', [id], (err, log) => {
        res.json({ success: true, log });
      });
    }
  );
});

// @route   GET api/productivity/today
router.get('/today', auth, (req, res) => {
    const userId = req.user.id;
    const today = new Date().toISOString().split('T')[0];

    db.get('SELECT * FROM productivity_logs WHERE userId = ? AND date LIKE ?', [userId, `${today}%`], (err, log) => {
        if (err) return res.status(500).json({ success: false });
        res.json({ success: true, log: log || null });
    });
});

module.exports = router;

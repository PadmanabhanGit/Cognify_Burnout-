const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const auth = require('../middleware/auth');
const db = require('../database');

// @route   POST api/activity/sync
// @desc    Sync steps and activity from health provider
router.post('/sync', auth, (req, res) => {
  const { steps, calories, activeMinutes, source, date } = req.body;
  const userId = req.user.id;
  const syncDate = date || new Date().toISOString().split('T')[0];
  const id = uuidv4();

  db.run(
    `INSERT INTO physical_activity (id, userId, date, steps, calories, activeMinutes, source)
     VALUES (?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(userId, date) DO UPDATE SET
     steps = excluded.steps,
     calories = excluded.calories,
     activeMinutes = excluded.activeMinutes,
     source = excluded.source`,
    [id, userId, syncDate, steps, calories, activeMinutes, source],
    function(err) {
      if (err) return res.status(500).json({ success: false, message: 'Activity sync failed' });
      res.json({ success: true, message: 'Activity updated' });
    }
  );
});

// @route   GET api/activity/today
router.get('/today', auth, (req, res) => {
    const userId = req.user.id;
    const today = new Date().toISOString().split('T')[0];

    db.get('SELECT * FROM physical_activity WHERE userId = ? AND date = ?', [userId, today], (err, row) => {
        if (err) return res.status(500).json({ success: false });
        res.json({ success: true, activity: row || { steps: 0, calories: 0, activeMinutes: 0 } });
    });
});

module.exports = router;

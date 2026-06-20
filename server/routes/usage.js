const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const auth = require('../middleware/auth');
const db = require('../database');
const { resolveCategory } = require('../services/appDiscoveryService');

// @route   POST api/usage/sync
// @desc    Sync and resolve categories for app usage statistics
router.post('/sync', auth, async (req, res) => {
  const { usageData, date } = req.body; // usageData: [{ packageName: 'com.mangazone', duration: 120 }, ...]
  const userId = req.user.id;
  const syncDate = date || new Date().toISOString().split('T')[0];

  try {
    for (const item of usageData) {
      const category = await resolveCategory(item.packageName);
      const id = uuidv4();

      // Upsert logic for usage on the same day/category
      db.run(
        `INSERT INTO app_usage (id, userId, date, category, duration)
         VALUES (?, ?, ?, ?, ?)
         ON CONFLICT(userId, date, category) DO UPDATE SET duration = duration + ?`,
        [id, userId, syncDate, category, item.duration, item.duration]
      );
    }
    res.json({ success: true, message: 'Usage stats synced and categorized' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Sync failed' });
  }
});

// @route   GET api/usage/today
router.get('/today', auth, (req, res) => {
  const userId = req.user.id;
  const today = new Date().toISOString().split('T')[0];

  db.all('SELECT category, SUM(duration) as duration FROM app_usage WHERE userId = ? AND date = ? GROUP BY category',
    [userId, today], (err, rows) => {
      if (err) return res.status(500).json({ success: false });

      const formatted = rows.map(r => ({
        category: r.category,
        time: `${Math.floor(r.duration / 60)}h ${r.duration % 60}m`,
        progress: Math.min(r.duration / 480, 1.0), // Progress relative to 8h
        color: getColorForCategory(r.category)
      }));

      res.json({ success: true, usage: formatted });
  });
});

function getColorForCategory(cat) {
  const colors = {
    'Entertainment': '#3B82F6',
    'Social Media': '#EC4899',
    'Productivity': '#10B981',
    'Gaming': '#F97316'
  };
  return colors[cat] || '#6B7280';
}

module.exports = router;

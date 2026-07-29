const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db, admin } = require('../firebase');
const { resolveCategory } = require('../services/appDiscoveryService');

// @route   POST api/usage/sync
router.post('/sync', auth, async (req, res) => {
  const { usageData, date } = req.body;
  const userId = req.user.uid;
  const syncDate = date || new Date().toISOString().split('T')[0];

  try {
    const batch = db.batch();
    for (const item of usageData) {
      const category = await resolveCategory(item.packageName);
      // Deterministic ID = same doc gets reused/merged instead of duplicated
      const docId = `${userId}_${syncDate}_${category}`;
      const docRef = db.collection('appUsage').doc(docId);
      batch.set(docRef, {
        userId,
        date: syncDate,
        category,
        duration: admin.firestore.FieldValue.increment(item.duration),
      }, { merge: true });
    }
    await batch.commit();
    res.json({ success: true, message: 'Usage stats synced and categorized' });
  } catch (err) {
    console.error('Sync failed:', err.message);
    res.status(500).json({ success: false, message: 'Sync failed' });
  }
});

// @route   GET api/usage/today
router.get('/today', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = new Date().toISOString().split('T')[0];

  try {
    const snapshot = await db.collection('appUsage')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .get();

    const formatted = snapshot.docs.map(doc => {
      const r = doc.data();
      return {
        category: r.category,
        time: `${Math.floor(r.duration / 60)}h ${r.duration % 60}m`,
        progress: Math.min(r.duration / 480, 1.0),
        color: getColorForCategory(r.category)
      };
    });

    res.json({ success: true, usage: formatted });
  } catch (err) {
    res.status(500).json({ success: false });
  }
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
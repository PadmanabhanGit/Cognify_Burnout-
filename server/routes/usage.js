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
      // Doc for the specific app (e.g., whatsapp)
      const appDocId = `${userId}_${syncDate}_app_${item.packageName.replace(/\./g, '_')}`;
      const appRef = db.collection('appUsageDetails').doc(appDocId);

      batch.set(appRef, {
        userId,
        date: syncDate,
        appName: item.packageName,
        category,
        duration: item.duration,
      }, { merge: true });

      // Aggregate into category doc for the dashboard bars
      const catDocId = `${userId}_${syncDate}_${category}`;
      const catRef = db.collection('appUsage').doc(catDocId);
      batch.set(catRef, {
        userId,
        date: syncDate,
        category,
        totalDuration: admin.firestore.FieldValue.increment(0), // placeholder so merge works
      }, { merge: true });
    }
    await batch.commit();

    // Now update category totals by re-aggregating from appUsageDetails
    const allApps = await db.collection('appUsageDetails')
      .where('userId', '==', userId).where('date', '==', syncDate).get();

    const categoryTotals = {};
    allApps.docs.forEach(doc => {
      const d = doc.data();
      categoryTotals[d.category] = (categoryTotals[d.category] || 0) + d.duration;
    });

    const updateBatch = db.batch();
    for (const [cat, total] of Object.entries(categoryTotals)) {
      const catRef = db.collection('appUsage').doc(`${userId}_${syncDate}_${cat}`);
      updateBatch.set(catRef, { userId, date: syncDate, category: cat, totalDuration: total }, { merge: true });
    }
    await updateBatch.commit();

    res.json({ success: true, message: 'Usage stats synced and aggregated' });
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
    const snapshot = await db.collection('appUsageDetails')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .get();

    const apps = snapshot.docs.map(doc => doc.data());

    // Group by category for the bars
    const categories = {};
    apps.forEach(app => {
      if (!categories[app.category]) categories[app.category] = 0;
      categories[app.category] += app.duration;
    });

    const formattedUsage = Object.keys(categories).map(cat => ({
      category: cat,
      time: `${Math.floor(categories[cat] / 60)}h ${categories[cat] % 60}m`,
      progress: Math.min(categories[cat] / 480, 1.0),
      color: getColorForCategory(cat)
    }));

    res.json({
      success: true,
      usage: formattedUsage,
      topApps: apps.map(app => ({
        name: app.appName,
        category: app.category,
        time: `${Math.floor(app.duration / 60)}h ${app.duration % 60}m`,
        duration: app.duration
      })).sort((a, b) => b.duration - a.duration).slice(0, 10)
    });
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
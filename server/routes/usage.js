const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db, FieldValue } = require('../firebase');
const { resolveCategory } = require('../services/appDiscoveryService');

// @route   POST api/usage/sync
router.post('/sync', auth, async (req, res) => {
  const { usageData, date } = req.body;
  const userId = req.user.uid;
  const syncDate = date || new Date().toISOString().split('T')[0];

  try {
    const batch = db.batch();
    for (const item of usageData) {
      // Trust device category first, fallback to resolver
      const category = item.category || await resolveCategory(item.packageName);

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
        totalDuration: FieldValue.increment(0),
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
    const snapshot = await db.collection('appUsage')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .get();

    const formattedUsage = snapshot.docs.map(doc => {
      const r = doc.data();
      return {
        category: r.category,
        time: `${Math.floor(r.totalDuration / 60)}h ${r.totalDuration % 60}m`,
        progress: Math.min(r.totalDuration / 480, 1.0),
        color: getColorForCategory(r.category)
      };
    });

    // Also fetch top apps from appUsageDetails
    const detailsSnapshot = await db.collection('appUsageDetails')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .orderBy('duration', 'desc')
      .limit(10)
      .get();

    const topApps = detailsSnapshot.docs.map(doc => {
      const data = doc.data();
      return {
        name: data.appName,
        category: data.category,
        time: `${Math.floor(data.duration / 60)}h ${data.duration % 60}m`,
        hours: data.duration / 60.0,
        color: getColorForCategory(data.category)
      };
    });

    res.json({ success: true, usage: formattedUsage, topApps });
  } catch (err) {
    console.error('Fetch failed:', err.message);
    res.status(500).json({ success: false });
  }
});

function getColorForCategory(cat) {
  const colors = {
    'Entertainment': '#3B82F6',
    'Streaming': '#3B82F6',
    'Social Media': '#EC4899',
    'Productivity': '#10B981',
    'Gaming': '#F97316'
  };
  return colors[cat] || '#6B7280';
}

module.exports = router;
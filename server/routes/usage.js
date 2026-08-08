const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db, FieldValue } = require('../firebase');
const { resolveCategory } = require('../services/appDiscoveryService');
const { getLocalDateString, normalizeDateValue } = require('../utils/dateUtils');

// @route   POST api/usage/sync
router.post('/sync', auth, async (req, res) => {
  const { usageData, date } = req.body;
  const userId = req.user.uid;
  const syncDate = normalizeDateValue(date || new Date());
  const timestamp = new Date().toISOString();

  console.log(`[usage/sync] Received data from user ${userId} for date ${syncDate}:`, req.body);

  try {
    const batch = db.batch();
    for (const item of usageData || []) {
      const rawDuration = Number(item.duration || 0);
      const category = (item.category || await resolveCategory(item.packageName) || 'Others').trim();

      const appDocId = `${userId}_${syncDate}_app_${(item.packageName || 'unknown').replace(/\./g, '_')}`;
      const appRef = db.collection('appUsageDetails').doc(appDocId);

      batch.set(appRef, {
        userId,
        date: syncDate,
        appName: item.packageName || 'Unknown',
        category,
        duration: rawDuration,
        updatedAt: timestamp,
      }, { merge: true });

      const catDocId = `${userId}_${syncDate}_${category}`;
      const catRef = db.collection('appUsage').doc(catDocId);
      batch.set(catRef, {
        userId,
        date: syncDate,
        category,
        totalDuration: FieldValue.increment(rawDuration),
        updatedAt: timestamp,
      }, { merge: true });
    }
    await batch.commit();

    const allApps = await db.collection('appUsageDetails').get();
    const categoryTotals = {};

    allApps.docs.forEach(doc => {
      const d = doc.data();
      if (!d || d.userId !== userId || normalizeDateValue(d.date) !== syncDate) return;
      const category = d.category || 'Others';
      categoryTotals[category] = (categoryTotals[category] || 0) + Number(d.duration || 0);
    });

    const updateBatch = db.batch();
    for (const [cat, total] of Object.entries(categoryTotals)) {
      const catRef = db.collection('appUsage').doc(`${userId}_${syncDate}_${cat}`);
      updateBatch.set(catRef, { userId, date: syncDate, category: cat, totalDuration: total, updatedAt: timestamp }, { merge: true });
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
  const today = getLocalDateString();

  try {
    const snapshot = await db.collection('appUsage').get();

    const formattedUsage = snapshot.docs
      .map(doc => doc.data())
      .filter(data => data && data.userId === userId && normalizeDateValue(data.date) === today)
      .map(data => ({
        category: data.category,
        time: `${Math.floor((data.totalDuration || 0) / 60)}h ${(data.totalDuration || 0) % 60}m`,
        duration: data.totalDuration || 0,
        progress: Math.min((data.totalDuration || 0) / 480, 1.0),
        color: getColorForCategory(data.category)
      }));

    const detailsSnapshot = await db.collection('appUsageDetails').get();

    const topApps = detailsSnapshot.docs
      .map(doc => doc.data())
      .filter(data => data && data.userId === userId && normalizeDateValue(data.date) === today)
      .sort((a, b) => (b.duration || 0) - (a.duration || 0))
      .slice(0, 10)
      .map(data => ({
        name: data.appName || data.packageName || 'Unknown',
        category: data.category,
        time: `${Math.floor((data.duration || 0) / 60)}h ${(data.duration || 0) % 60}m`,
        hours: (data.duration || 0) / 60.0,
        duration: data.duration || 0,
        color: getColorForCategory(data.category)
      }));

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
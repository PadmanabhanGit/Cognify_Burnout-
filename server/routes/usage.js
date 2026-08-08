const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
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
    // The device sends a full, current-day snapshot. Replacing the previous
    // snapshot prevents repeated syncs from accumulating stale app durations.
    const itemsByPackage = new Map();
    for (const item of usageData || []) {
      const packageName = String(item.packageName || 'unknown');
      const durationSeconds = Math.max(0, Math.round(Number(item.durationSeconds ?? (Number(item.duration || 0) * 60))));
      const duration = Math.floor(durationSeconds / 60);
      const category = String(item.category || await resolveCategory(packageName) || 'Others').trim();
      itemsByPackage.set(packageName, { packageName, category, duration, durationSeconds });
    }

    const [existingDetailsSnap, existingCategoriesSnap] = await Promise.all([
      db.collection('appUsageDetails').where('userId', '==', userId).get(),
      db.collection('appUsage').where('userId', '==', userId).get()
    ]);

    const batch = db.batch();

    existingDetailsSnap.docs.forEach(doc => {
      const data = doc.data();
      if (data && normalizeDateValue(data.date) === syncDate) batch.delete(doc.ref);
    });
    existingCategoriesSnap.docs.forEach(doc => {
      const data = doc.data();
      if (data && normalizeDateValue(data.date) === syncDate) batch.delete(doc.ref);
    });

    const categoryTotals = {};
    for (const item of itemsByPackage.values()) {
      const appDocId = `${userId}_${syncDate}_app_${item.packageName.replace(/\./g, '_')}`;
      const appRef = db.collection('appUsageDetails').doc(appDocId);

      batch.set(appRef, {
        userId,
        date: syncDate,
        appName: item.packageName || 'Unknown',
        category: item.category,
        duration: item.duration,
        durationSeconds: item.durationSeconds,
        updatedAt: timestamp,
      });

      categoryTotals[item.category] = (categoryTotals[item.category] || 0) + item.durationSeconds;
    }

    for (const [cat, totalDurationSeconds] of Object.entries(categoryTotals)) {
      const catRef = db.collection('appUsage').doc(`${userId}_${syncDate}_${cat}`);
      batch.set(catRef, {
        userId,
        date: syncDate,
        category: cat,
        totalDuration: Math.floor(totalDurationSeconds / 60),
        totalDurationSeconds,
        updatedAt: timestamp
      });
    }

    await batch.commit();

    res.json({ success: true, message: 'Usage snapshot synced', syncedApps: itemsByPackage.size });
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
      .map(data => {
        const durationSeconds = Number(data.totalDurationSeconds ?? (data.totalDuration || 0) * 60);
        return {
          category: data.category,
          time: formatDuration(durationSeconds),
          duration: data.totalDuration || 0,
          durationSeconds,
          progress: Math.min(durationSeconds / (8 * 60 * 60), 1.0),
          color: getColorForCategory(data.category)
        };
      });

    const detailsSnapshot = await db.collection('appUsageDetails').get();

    const topApps = detailsSnapshot.docs
      .map(doc => doc.data())
      .filter(data => data && data.userId === userId && normalizeDateValue(data.date) === today)
      .sort((a, b) => (b.duration || 0) - (a.duration || 0))
      .slice(0, 10)
      .map(data => {
        const durationSeconds = Number(data.durationSeconds ?? (data.duration || 0) * 60);
        return {
          name: data.appName || data.packageName || 'Unknown',
          category: data.category,
          time: formatDuration(durationSeconds),
          hours: durationSeconds / 3600.0,
          duration: data.duration || 0,
          durationSeconds,
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

function formatDuration(totalSeconds) {
  const seconds = Math.max(0, Math.floor(totalSeconds || 0));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;
  return [hours > 0 ? `${hours}h` : null, minutes > 0 ? `${minutes}m` : null, remainingSeconds > 0 ? `${remainingSeconds}s` : null]
    .filter(Boolean)
    .join(' ') || '0s';
}

module.exports = router;

const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { resolveCategory } = require('../services/appDiscoveryService');
const { normalizeDateValue } = require('../utils/dateUtils');

// ─── IST helpers (must match dashboard.js / study.js exactly) ─────────────────
// Android device timezone = IST (UTC+05:30). The server has no TZ override and
// defaults to UTC, so getLocalDateString() (server clock) disagreed with the
// device's own IST-based date for the first ~5.5 hours of every IST day
// (00:00-05:30 IST is still 18:30-24:00 UTC the previous day) — GET /today
// looked for yesterday's date while the device had already synced under
// today's, returning stale/empty data at that boundary every day. Every other
// date-boundary-sensitive route already carries this fix; this file was the
// one still using the server-local date for "today".
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

function getISTDateString(dateOrString) {
  const ist = new Date(new Date(dateOrString).getTime() + IST_OFFSET_MS);
  const y = ist.getUTCFullYear();
  const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const d = String(ist.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

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
      const lastUsedAt = Number(item.lastUsedAt) > 0 ? Number(item.lastUsedAt) : null;
      const name = String(item.name || packageName).trim();
      itemsByPackage.set(packageName, { packageName, name, category, duration, durationSeconds, lastUsedAt });
    }

    const [existingDetailsSnap, existingCategoriesSnap] = await Promise.all([
      db.collection('appUsageDetails').where('userId', '==', userId).where('date', '==', syncDate).get(),
      db.collection('appUsage').where('userId', '==', userId).where('date', '==', syncDate).get()
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
        appName: item.name || item.packageName || 'Unknown',
        packageName: item.packageName,
        category: item.category,
        duration: item.duration,
        durationSeconds: item.durationSeconds,
        lastUsedAt: item.lastUsedAt,
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
    console.error(`Sync failed for user ${userId} on date ${syncDate}. Code: ${err.code}. Message: ${err.message}`);
    if (err.code === 8 || err.code === 'RESOURCE_EXHAUSTED' || (err.message && err.message.includes('Quota exceeded'))) {
      return res.status(429).json({ success: false, message: 'Firestore quota exhausted' });
    }
    res.status(500).json({ success: false, message: 'Sync failed' });
  }
});

// @route   GET api/usage/today
router.get('/today', auth, async (req, res) => {
  const userId = req.user.uid;
  const today = getISTDateString(new Date());

  try {
    // Filtered server-side rather than fetching the whole collection and
    // filtering in JS: the previous version read every appUsage/appUsageDetails
    // document ever written, for every user, on every page load — every other
    // route in this file (and every other route file) scopes its query with
    // .where('userId', ...); this was the one place that didn't.
    const snapshot = await db.collection('appUsage')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .get();

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

    const detailsSnapshot = await db.collection('appUsageDetails')
      .where('userId', '==', userId)
      .where('date', '==', today)
      .get();

    const todaysDetails = detailsSnapshot.docs
      .map(doc => doc.data())
      .filter(data => data && data.userId === userId && normalizeDateValue(data.date) === today);

    const topApps = todaysDetails
      .slice()
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

    // Ordered by most recent foreground open, not accumulated time, so a
    // just-opened app with little usage today still shows up. Apps synced
    // before lastUsedAt existed (or without a fresh-enough client) have no
    // timestamp and are excluded rather than sorted arbitrarily.
    const recentApps = todaysDetails
      .filter(data => Number(data.lastUsedAt) > 0)
      .slice()
      .sort((a, b) => Number(b.lastUsedAt) - Number(a.lastUsedAt))
      .slice(0, 8)
      .map(data => ({
        name: data.appName || data.packageName || 'Unknown',
        category: data.category,
        lastUsedAt: Number(data.lastUsedAt),
        color: getColorForCategory(data.category)
      }));

    res.json({ success: true, usage: formattedUsage, topApps, recentApps });
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

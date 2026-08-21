const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { normalizeDateValue } = require('../utils/dateUtils');

// ─── IST helpers (must match dashboard.js / study.js / burnout.js / usage.js
// exactly) ───────────────────────────────────────────────────────────────────
// Every record this endpoint reads (sleepMoodLogs, studySessions,
// productivityLogs) is stamped with an IST calendar date by the Android
// client. This endpoint was computing its own 7-day window and per-day
// buckets with `.toISOString()`, which is always UTC — the server has no TZ
// override. For the first ~5.5 hours of every IST day the window boundary
// and every trailing-date bucket landed one day early relative to how the
// records were actually stored, so a day's data could silently bucket under
// the wrong day of the week, or the whole week's `from`/`to` could be off by
// one. Weekly Report is exactly the feature where that kind of boundary
// mismatch is most visible, so this must agree with the same IST date every
// other module already uses.
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

function getISTDateString(dateOrString = new Date()) {
  const ist = new Date(new Date(dateOrString).getTime() + IST_OFFSET_MS);
  const y = ist.getUTCFullYear();
  const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const d = String(ist.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

router.get('/weekly', auth, async (req, res) => {
  const userId = req.user.uid;

  try {
    const now = new Date();
    const weekAgo = new Date();
    weekAgo.setDate(now.getDate() - 7);
    const fromStr = getISTDateString(weekAgo);
    const toStr = getISTDateString(now);

    // Same 7 dates the summary averages are already computed over — used to
    // group the same records into a real per-day breakdown for the charts,
    // instead of Android/Web drawing a fabricated pattern because this
    // endpoint never gave them anything to plot.
    const trailingDates = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(now.getDate() - i);
      trailingDates.push(getISTDateString(d));
    }

    const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .get();
    const sleepLogs = sleepSnap.docs
      .map(d => d.data())
      .filter(item => {
        const value = normalizeDateValue(item.date);
        return value >= fromStr && value <= toStr;
      });
    // A mood-only manual entry (no detected sleep) or a night the phone never
    // synced has sleepDuration/moodScore absent, not zero. Averaging with
    // `|| 0` over the whole window silently reported "no data" as "measured
    // zero" and dragged both averages down. Each average now excludes only
    // the records missing that specific field, instead of excluding nothing
    // and corrupting the number.
    const withSleep = sleepLogs.filter(l => Number.isFinite(l.sleepDuration));
    const withMood = sleepLogs.filter(l => Number.isFinite(l.moodScore));
    const avgSleep = withSleep.length ? withSleep.reduce((s, l) => s + l.sleepDuration, 0) / withSleep.length : 0;
    const avgMood = withMood.length ? withMood.reduce((s, l) => s + l.moodScore, 0) / withMood.length : 0;

    const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .get();
    const studySessions = studySnap.docs
      .map(d => d.data())
      .filter(item => {
        const value = normalizeDateValue(item.startTime);
        return value >= fromStr && value <= toStr;
      });
    const totalStudyHours = Math.round((studySessions
      .reduce((s, d) => s + (d.duration || 0), 0) / 60) * 10) / 10;

    const prodSnap = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .get();
    const prodLogs = prodSnap.docs
      .map(d => d.data())
      .filter(item => {
        const value = normalizeDateValue(item.date);
        return value >= fromStr && value <= toStr;
      });
    const avgProductivity = prodLogs.length ? prodLogs.reduce((s, l) => s + (l.productivityScore || 0), 0) / prodLogs.length : 0;

    // ── Per-day breakdown for the Daily Activity and Mood vs Productivity
    // charts — grouped from the exact same records the summary averages
    // above were computed from. No new Firestore reads. A date with no
    // matching record stays null (a gap the chart skips), never a
    // fabricated zero.
    const dailyActivity = trailingDates.map(date => {
      const dayStudy = studySessions.filter(s => normalizeDateValue(s.startTime) === date);
      const studyMinutes = dayStudy.length
        ? dayStudy.reduce((sum, s) => sum + (s.duration || 0), 0)
        : null;

      const daySleep = withSleep.filter(l => normalizeDateValue(l.date) === date);
      const sleepHours = daySleep.length
        ? Math.round((daySleep.reduce((sum, l) => sum + l.sleepDuration, 0) / daySleep.length) * 10) / 10
        : null;

      return { date, studyMinutes, sleepHours };
    });

    const moodVsProductivity = trailingDates.map(date => {
      const prodForDay = prodLogs.find(p => normalizeDateValue(p.date) === date);
      const dayMood = withMood.filter(l => normalizeDateValue(l.date) === date);
      const moodScore = dayMood.length
        ? Math.round(dayMood.reduce((sum, l) => sum + l.moodScore, 0) / dayMood.length)
        : null;

      return {
        date,
        productivityScore: prodForDay ? (prodForDay.productivityScore ?? null) : null,
        moodScore,
      };
    });

    const wellnessRadar = {
      sleep: Math.round(Math.min((avgSleep / 8) * 100, 100)),
      mood: Math.round(Math.min((avgMood / 10) * 100, 100)),
      study: Math.round(Math.min((totalStudyHours / 40) * 100, 100)),
      productivity: Math.round(avgProductivity),
      balance: Math.round((Math.min((avgSleep / 8) * 100, 100) + Math.min((avgMood / 10) * 100, 100)) / 2)
    };

    const achievements = [];
    const concerns = [];
    const recommendations = [];

    if (totalStudyHours > 20) achievements.push(`Logged ${totalStudyHours} hours of focused study this week.`);
    if (avgSleep >= 7) achievements.push("Maintained healthy average sleep duration.");
    if (avgMood >= 7) achievements.push("Mood scores stayed positive through the week.");

    if (avgSleep < 6.5) concerns.push("Average sleep duration is below the recommended range.");
    if (avgMood < 6) concerns.push("Mood scores trended low this week.");
    if (prodLogs.length === 0) concerns.push("No productivity logs recorded this week.");

    if (avgSleep < 7) recommendations.push("Aim for at least 7 hours of sleep per night.");
    if (avgProductivity < 60) recommendations.push("Consider breaking work into shorter, focused sessions.");

    res.json({
      success: true,
      report: {
        period: { from: fromStr, to: toStr },
        summary: {
          avgSleep: Math.round(avgSleep * 10) / 10,
          avgMood: Math.round(avgMood * 10) / 10,
          totalStudyHours,
          avgProductivity: Math.round(avgProductivity)
        },
        dailyActivity,
        moodVsProductivity,
        wellnessRadar,
        achievements: achievements.length ? achievements : ["No standout achievements logged this week yet."],
        concerns: concerns.length ? concerns : ["No major concerns detected this week."],
        recommendations: recommendations.length ? recommendations : ["Keep up your current habits."]
      }
    });
  } catch (err) {
    console.error('Weekly report aggregation failed:', err.message);
    res.status(500).json({ success: false, message: 'Error generating weekly report' });
  }
});

module.exports = router;
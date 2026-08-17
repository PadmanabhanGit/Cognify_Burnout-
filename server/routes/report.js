const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { normalizeDateValue } = require('../utils/dateUtils');

router.get('/weekly', auth, async (req, res) => {
  const userId = req.user.uid;

  try {
    const now = new Date();
    const weekAgo = new Date();
    weekAgo.setDate(now.getDate() - 7);

    const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .get();
    const sleepLogs = sleepSnap.docs
      .map(d => d.data())
      .filter(item => {
        const value = normalizeDateValue(item.date);
        return value >= weekAgo.toISOString().slice(0, 10) && value <= now.toISOString().slice(0, 10);
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
    const totalStudyHours = Math.round((studySnap.docs
      .map(d => d.data())
      .filter(item => {
        const value = normalizeDateValue(item.startTime);
        return value >= weekAgo.toISOString().slice(0, 10) && value <= now.toISOString().slice(0, 10);
      })
      .reduce((s, d) => s + (d.duration || 0), 0) / 60) * 10) / 10;

    const prodSnap = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .get();
    const prodLogs = prodSnap.docs
      .map(d => d.data())
      .filter(item => {
        const value = normalizeDateValue(item.date);
        return value >= weekAgo.toISOString().slice(0, 10) && value <= now.toISOString().slice(0, 10);
      });
    const avgProductivity = prodLogs.length ? prodLogs.reduce((s, l) => s + (l.productivityScore || 0), 0) / prodLogs.length : 0;

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
        period: { from: weekAgo.toISOString().split('T')[0], to: now.toISOString().split('T')[0] },
        summary: {
          avgSleep: Math.round(avgSleep * 10) / 10,
          avgMood: Math.round(avgMood * 10) / 10,
          totalStudyHours,
          avgProductivity: Math.round(avgProductivity)
        },
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
const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');

router.get('/weekly', auth, async (req, res) => {
  const userId = req.user.uid;

  try {
    const now = new Date();
    const weekAgo = new Date();
    weekAgo.setDate(now.getDate() - 7);
    const fromIso = weekAgo.toISOString();
    const toIso = now.toISOString();

    const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .where('date', '>=', fromIso)
      .where('date', '<=', toIso)
      .get();
    const sleepLogs = sleepSnap.docs.map(d => d.data());
    const avgSleep = sleepLogs.length ? sleepLogs.reduce((s, l) => s + (l.sleepDuration || 0), 0) / sleepLogs.length : 0;
    const avgMood = sleepLogs.length ? sleepLogs.reduce((s, l) => s + (l.moodScore || 0), 0) / sleepLogs.length : 0;

    const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', fromIso)
      .where('startTime', '<=', toIso)
      .get();
    const totalStudyHours = Math.round((studySnap.docs.reduce((s, d) => s + (d.data().duration || 0), 0) / 60) * 10) / 10;

    const prodSnap = await db.collection('productivityLogs')
      .where('userId', '==', userId)
      .where('date', '>=', fromIso)
      .where('date', '<=', toIso)
      .get();
    const prodLogs = prodSnap.docs.map(d => d.data());
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
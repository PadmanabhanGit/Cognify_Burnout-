import { Response } from 'express';
import SleepMoodLog from '../models/SleepMoodLog';
import StudySession from '../models/StudySession';
import ProductivityLog from '../models/ProductivityLog';
import { AuthRequest } from '../middleware/auth';

// ─── GET WEEKLY REPORT ────────────────────────────────────────────────────────
// Powers the full WeeklyReportScreen: summary metrics, daily bar chart,
// mood vs productivity line chart, radar comparison, achievements, concerns
export const getWeeklyReport = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const [sleepLogs, studySessions, productivityLogs] = await Promise.all([
      SleepMoodLog.find({ userId: req.userId, date: { $gte: sevenDaysAgo } }).sort({ date: 1 }),
      StudySession.find({ userId: req.userId, startTime: { $gte: sevenDaysAgo }, isActive: false }),
      ProductivityLog.find({ userId: req.userId, date: { $gte: sevenDaysAgo } }).sort({ date: 1 }),
    ]);

    // ── Summary Metrics ──────────────────────────────────────────────────────
    const avgSleep = sleepLogs.length
      ? +(sleepLogs.reduce((s, l) => s + l.sleepDuration, 0) / sleepLogs.length).toFixed(1)
      : 0;
    const avgMood = sleepLogs.length
      ? +(sleepLogs.reduce((s, l) => s + l.moodScore, 0) / sleepLogs.length).toFixed(1)
      : 0;
    const totalStudyHours = +(studySessions.reduce((s, l) => s + l.duration, 0) / 60).toFixed(1);
    const avgProductivity = productivityLogs.length
      ? +(productivityLogs.reduce((s, l) => s + l.productivityScore, 0) / productivityLogs.length).toFixed(1)
      : 0;

    // ── Daily Activity (bar chart) ───────────────────────────────────────────
    const dailyActivity: Record<string, { studyMinutes: number; moodScore: number; productivityScore: number }> = {};
    studySessions.forEach((s) => {
      const d = s.startTime.toISOString().split('T')[0];
      if (!dailyActivity[d]) dailyActivity[d] = { studyMinutes: 0, moodScore: 0, productivityScore: 0 };
      dailyActivity[d].studyMinutes += s.duration;
    });
    sleepLogs.forEach((l) => {
      const d = l.date.toISOString().split('T')[0];
      if (!dailyActivity[d]) dailyActivity[d] = { studyMinutes: 0, moodScore: 0, productivityScore: 0 };
      dailyActivity[d].moodScore = l.moodScore;
    });
    productivityLogs.forEach((l) => {
      const d = l.date.toISOString().split('T')[0];
      if (!dailyActivity[d]) dailyActivity[d] = { studyMinutes: 0, moodScore: 0, productivityScore: 0 };
      dailyActivity[d].productivityScore = l.productivityScore;
    });

    // ── Mood vs Productivity (line chart) ────────────────────────────────────
    const moodVsProductivity = productivityLogs.map((pl) => {
      const date = pl.date.toISOString().split('T')[0];
      const mood = sleepLogs.find((sl) => sl.date.toISOString().split('T')[0] === date);
      return { date, productivityScore: pl.productivityScore, moodScore: mood?.moodScore ?? null };
    });

    // ── Wellness Radar ───────────────────────────────────────────────────────
    const wellnessRadar = {
      sleep: Math.min(10, Math.round(avgSleep / 0.9)),
      mood: Math.round(avgMood),
      study: Math.min(10, Math.round((totalStudyHours / 40) * 10)),
      productivity: Math.round(avgProductivity / 10),
      balance: Math.round((avgSleep / 8 + avgMood / 10 + avgProductivity / 100) * 3.33),
    };

    // ── Achievements ─────────────────────────────────────────────────────────
    const achievements: string[] = [];
    if (totalStudyHours >= 20) achievements.push('🎯 Studied 20+ hours this week');
    if (avgSleep >= 7) achievements.push('😴 Achieved healthy sleep average (7h+)');
    if (avgMood >= 7) achievements.push('😊 Maintained high mood score');
    if (avgProductivity >= 70) achievements.push('⚡ Productivity above 70%');
    if (studySessions.length >= 5) achievements.push('📚 5+ study sessions completed');

    // ── Areas of Concern ─────────────────────────────────────────────────────
    const concerns: string[] = [];
    if (avgSleep < 6) concerns.push('Sleep average is below 6 hours — risk of fatigue');
    if (avgMood < 4) concerns.push('Mood has been consistently low this week');
    if (totalStudyHours < 5) concerns.push('Study hours are low — consider scheduling dedicated sessions');
    if (avgProductivity < 40) concerns.push('Productivity score is below 40% — possible burnout risk');

    // ── Personalised Recommendations ─────────────────────────────────────────
    const recommendations: string[] = [
      avgSleep < 7 ? 'Prioritise getting 7–9 hours of sleep nightly' : 'Keep up your great sleep routine',
      avgMood < 6 ? 'Try mindfulness or short breaks to improve mood' : 'Your mood is great — keep it up',
      totalStudyHours < 15 ? 'Aim for at least 15 focused study hours next week' : 'Excellent study discipline!',
      avgProductivity < 60 ? 'Use the Pomodoro technique to boost focus sessions' : 'High productivity — great work!',
    ];

    // ── Next Week Goals ───────────────────────────────────────────────────────
    const nextWeekGoals = [
      { goal: `Sleep at least 7 hours per night`, completed: false },
      { goal: `Log mood daily`, completed: false },
      { goal: `Study ${Math.max(15, Math.round(totalStudyHours * 1.1))} hours`, completed: false },
      { goal: `Maintain productivity above ${Math.min(90, Math.round(avgProductivity + 10))}%`, completed: false },
    ];

    res.status(200).json({
      success: true,
      report: {
        period: { from: sevenDaysAgo.toISOString(), to: new Date().toISOString() },
        summary: { avgSleep, avgMood, totalStudyHours, avgProductivity },
        dailyActivity,
        moodVsProductivity,
        wellnessRadar,
        achievements,
        concerns,
        recommendations,
        nextWeekGoals,
      },
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error generating weekly report', error });
  }
};

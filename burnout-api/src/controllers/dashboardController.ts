import { Response } from 'express';
import SleepMoodLog from '../models/SleepMoodLog';
import StudySession from '../models/StudySession';
import ProductivityLog from '../models/ProductivityLog';
import BurnoutAssessment from '../models/BurnoutAssessment';
import { AuthRequest } from '../middleware/auth';

// ─── GET DASHBOARD DATA ───────────────────────────────────────────────────────
// Single endpoint that powers the full DashboardScreen:
// greeting, quick stats, burnout alert card, feature card data
export const getDashboard = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const [latestSleep, latestProductivity, latestBurnout, weekStudy] = await Promise.all([
      SleepMoodLog.findOne({ userId: req.userId }).sort({ date: -1 }),
      ProductivityLog.findOne({ userId: req.userId }).sort({ date: -1 }),
      BurnoutAssessment.findOne({ userId: req.userId }).sort({ date: -1 }),
      StudySession.find({ userId: req.userId, startTime: { $gte: sevenDaysAgo }, isActive: false }),
    ]);

    const weeklyStudyMinutes = weekStudy.reduce((s, l) => s + l.duration, 0);
    const weeklyStudyHours = +(weeklyStudyMinutes / 60).toFixed(1);
    const todayStudyMinutes = weekStudy
      .filter(s => s.startTime >= today)
      .reduce((sum, s) => sum + s.duration, 0);

    res.status(200).json({
      success: true,
      dashboard: {
        quickStats: {
          lastSleepHours: latestSleep?.sleepDuration ?? null,
          lastSleepQuality: latestSleep?.sleepQuality ?? null,
          lastMood: latestSleep?.mood ?? null,
          lastMoodScore: latestSleep?.moodScore ?? null,
          lastProductivityScore: latestProductivity?.productivityScore ?? null,
          weeklyStudyHours,
          weeklyStudyMinutes,
          todayStudyMinutes,
        },
        burnoutAlert: {
          riskScore: latestBurnout?.riskScore ?? null,
          riskLevel: latestBurnout?.riskLevel ?? 'unknown',
          topWarning: latestBurnout?.warnings?.[0] ?? null,
        },
        featureCards: {
          study: { weeklyHours: weeklyStudyHours, sessionCount: weekStudy.length },
          sleep: { lastDuration: latestSleep?.sleepDuration ?? null, lastQuality: latestSleep?.sleepQuality ?? null },
          burnout: { riskScore: latestBurnout?.riskScore ?? null, riskLevel: latestBurnout?.riskLevel ?? null },
          productivity: { score: latestProductivity?.productivityScore ?? null },
        },
      },
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching dashboard data', error });
  }
};

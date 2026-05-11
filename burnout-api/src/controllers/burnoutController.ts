import { Response } from 'express';
import BurnoutAssessment from '../models/BurnoutAssessment';
import SleepMoodLog from '../models/SleepMoodLog';
import StudySession from '../models/StudySession';
import ProductivityLog from '../models/ProductivityLog';
import { AuthRequest } from '../middleware/auth';

// ─── SAVE BURNOUT ASSESSMENT ─────────────────────────────────────────────────
export const saveAssessment = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { riskScore, riskLevel, factors, wellbeingDimensions, warnings, recommendations } = req.body;

    if (riskScore === undefined || !riskLevel) {
      res.status(400).json({ success: false, message: 'riskScore and riskLevel are required' });
      return;
    }

    const assessment = await BurnoutAssessment.create({
      userId: req.userId,
      date: new Date(),
      riskScore, riskLevel,
      factors: factors || [],
      wellbeingDimensions: wellbeingDimensions || {},
      warnings: warnings || [],
      recommendations: recommendations || [],
    });

    res.status(201).json({ success: true, message: 'Burnout assessment saved', assessment });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error saving burnout assessment', error });
  }
};

// ─── GET LATEST ASSESSMENT ────────────────────────────────────────────────────
// Returns data for BurnoutPredictionScreen (pie chart, radar, recommendations)
export const getLatestAssessment = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const assessment = await BurnoutAssessment.findOne({ userId: req.userId }).sort({ date: -1 });
    res.status(200).json({ success: true, assessment: assessment || null });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching burnout assessment', error });
  }
};

// ─── COMPUTE BURNOUT RISK (AUTO-CALCULATED) ───────────────────────────────────
// On-demand calculation based on the user's recent logs (no manual input required)
export const computeBurnoutRisk = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const [sleepLogs, studyLogs, productivityLogs] = await Promise.all([
      SleepMoodLog.find({ userId: req.userId, date: { $gte: sevenDaysAgo } }),
      StudySession.find({ userId: req.userId, startTime: { $gte: sevenDaysAgo }, isActive: false }),
      ProductivityLog.find({ userId: req.userId, date: { $gte: sevenDaysAgo } }),
    ]);

    // Compute factor scores (0-100, higher = worse)
    const avgSleep = sleepLogs.length
      ? sleepLogs.reduce((s, l) => s + l.sleepDuration, 0) / sleepLogs.length
      : 7;
    const avgMood = sleepLogs.length
      ? sleepLogs.reduce((s, l) => s + l.moodScore, 0) / sleepLogs.length
      : 5;
    const totalStudyHours = studyLogs.reduce((s, l) => s + l.duration, 0) / 60;
    const avgProductivity = productivityLogs.length
      ? productivityLogs.reduce((s, l) => s + l.productivityScore, 0) / productivityLogs.length
      : 50;

    const sleepFactor = Math.max(0, Math.min(100, ((7 - avgSleep) / 7) * 100));
    const moodFactor = Math.max(0, Math.min(100, ((10 - avgMood) / 10) * 100));
    const overworkFactor = Math.max(0, Math.min(100, ((totalStudyHours - 40) / 20) * 100));
    const productivityFactor = Math.max(0, Math.min(100, 100 - avgProductivity));

    const riskScore = Math.round((sleepFactor * 0.3 + moodFactor * 0.3 + overworkFactor * 0.2 + productivityFactor * 0.2));

    const riskLevel =
      riskScore < 25 ? 'low' :
      riskScore < 50 ? 'moderate' :
      riskScore < 75 ? 'high' : 'critical';

    const factors = [
      { name: 'Sleep Deprivation', score: Math.round(sleepFactor) },
      { name: 'Mood Decline', score: Math.round(moodFactor) },
      { name: 'Overwork', score: Math.round(overworkFactor) },
      { name: 'Low Productivity', score: Math.round(productivityFactor) },
    ];

    const wellbeingDimensions = {
      physical: Math.max(1, Math.round(10 - sleepFactor / 10)),
      emotional: Math.max(1, Math.round(avgMood)),
      social: 5, // Placeholder — extend with social tracking later
      intellectual: Math.max(1, Math.round(avgProductivity / 10)),
      occupational: Math.max(1, Math.round(10 - overworkFactor / 10)),
    };

    const warnings: string[] = [];
    if (avgSleep < 6) warnings.push('Consistently sleeping less than 6 hours');
    if (avgMood < 4) warnings.push('Mood has been consistently low this week');
    if (totalStudyHours > 50) warnings.push('Study/work hours are very high this week');

    const recommendations: string[] = [
      riskScore > 50 ? 'Take a full rest day this week' : 'Maintain your current healthy habits',
      avgSleep < 7 ? 'Aim for 7–9 hours of sleep each night' : 'Your sleep schedule looks good',
      avgMood < 5 ? 'Try journaling or a 10-minute walk to boost mood' : 'Keep up the positive mood',
    ];

    res.status(200).json({
      success: true,
      computed: { riskScore, riskLevel, factors, wellbeingDimensions, warnings, recommendations },
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error computing burnout risk', error });
  }
};

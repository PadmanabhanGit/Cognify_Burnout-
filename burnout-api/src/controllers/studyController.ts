import { Response } from 'express';
import StudySession from '../models/StudySession';
import { AuthRequest } from '../middleware/auth';

// ─── START SESSION ────────────────────────────────────────────────────────────
export const startSession = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { subject, notes } = req.body;

    if (!subject) {
      res.status(400).json({ success: false, message: 'Subject is required to start a session' });
      return;
    }

    // Stop any active session before starting a new one
    await StudySession.updateMany(
      { userId: req.userId, isActive: true },
      { isActive: false, endTime: new Date() }
    );

    const session = await StudySession.create({
      userId: req.userId,
      subject,
      notes,
      startTime: new Date(),
      isActive: true,
    });

    res.status(201).json({ success: true, message: 'Study session started', session });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error starting study session', error });
  }
};

// ─── STOP SESSION ─────────────────────────────────────────────────────────────
export const stopSession = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { sessionId } = req.params;

    const session = await StudySession.findOne({ _id: sessionId, userId: req.userId, isActive: true });
    if (!session) {
      res.status(404).json({ success: false, message: 'Active session not found' });
      return;
    }

    const endTime = new Date();
    const durationMs = endTime.getTime() - session.startTime.getTime();
    const durationMinutes = Math.round(durationMs / 60000);

    session.endTime = endTime;
    session.duration = durationMinutes;
    session.isActive = false;
    await session.save();

    res.status(200).json({ success: true, message: 'Study session stopped', session });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error stopping study session', error });
  }
};

// ─── LOG OFFLINE SESSION ──────────────────────────────────────────────────────
export const logOfflineSession = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { subject, duration, startTime, notes } = req.body;

    if (!subject || duration === undefined || !startTime) {
      res.status(400).json({ success: false, message: 'Missing required fields for offline session' });
      return;
    }

    const session = await StudySession.create({
      userId: req.userId,
      subject,
      notes: notes || '',
      startTime: new Date(startTime),
      endTime: new Date(new Date(startTime).getTime() + duration * 60000),
      duration,
      isActive: false,
    });

    res.status(201).json({ success: true, message: 'Offline session synced', session });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error syncing offline session', error });
  }
};

// ─── GET WEEKLY STATS ─────────────────────────────────────────────────────────
// Returns data for the weekly bar chart and subject breakdown on StudyTrackingScreen
export const getWeeklyStats = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const now = new Date();
    const sevenDaysAgo = new Date(now);
    sevenDaysAgo.setDate(now.getDate() - 7);

    const sessions = await StudySession.find({
      userId: req.userId,
      startTime: { $gte: sevenDaysAgo },
      isActive: false,
    });

    // Group by day of week
    const dailyTotals: Record<string, number> = {};
    const subjectTotals: Record<string, number> = {};

    sessions.forEach((s) => {
      const day = s.startTime.toLocaleDateString('en-US', { weekday: 'short' });
      dailyTotals[day] = (dailyTotals[day] || 0) + s.duration;
      subjectTotals[s.subject] = (subjectTotals[s.subject] || 0) + s.duration;
    });

    const totalMinutes = sessions.reduce((sum, s) => sum + s.duration, 0);

    res.status(200).json({
      success: true,
      stats: {
        totalMinutes,
        totalHours: Math.round((totalMinutes / 60) * 10) / 10,
        sessionsCount: sessions.length,
        dailyTotals,
        subjectBreakdown: subjectTotals,
      },
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching weekly stats', error });
  }
};

// ─── GET MONTHLY STATS ────────────────────────────────────────────────────────
// Returns data for the monthly line chart
export const getMonthlyStats = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const now = new Date();
    const thirtyDaysAgo = new Date(now);
    thirtyDaysAgo.setDate(now.getDate() - 30);

    const sessions = await StudySession.find({
      userId: req.userId,
      startTime: { $gte: thirtyDaysAgo },
      isActive: false,
    });

    const dailyTotals: Record<string, number> = {};
    sessions.forEach((s) => {
      const date = s.startTime.toISOString().split('T')[0];
      dailyTotals[date] = (dailyTotals[date] || 0) + s.duration;
    });

    res.status(200).json({ success: true, stats: { dailyTotals } });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching monthly stats', error });
  }
};

import { Response } from 'express';
import SleepMoodLog from '../models/SleepMoodLog';
import { AuthRequest } from '../middleware/auth';

// ─── SAVE LOG ENTRY ───────────────────────────────────────────────────────────
// Called when user taps "Save Entry" on SleepMoodScreen
export const saveLog = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { sleepDuration, sleepQuality, mood, moodScore, notes, date } = req.body;

    if (!sleepDuration || !sleepQuality || !mood || !moodScore) {
      res.status(400).json({ success: false, message: 'sleepDuration, sleepQuality, mood, and moodScore are required' });
      return;
    }

    const log = await SleepMoodLog.create({
      userId: req.userId,
      date: date ? new Date(date) : new Date(),
      sleepDuration,
      sleepQuality,
      mood,
      moodScore,
      notes,
    });

    res.status(201).json({ success: true, message: 'Sleep & mood entry saved', log });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error saving sleep/mood log', error });
  }
};

// ─── GET RECENT LOGS ──────────────────────────────────────────────────────────
// Returns the last N entries for the "Recent logs" list
export const getRecentLogs = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const limit = parseInt(req.query.limit as string) || 7;
    const logs = await SleepMoodLog.find({ userId: req.userId })
      .sort({ date: -1 })
      .limit(limit);

    res.status(200).json({ success: true, logs });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching recent logs', error });
  }
};

// ─── GET SLEEP TRENDS ─────────────────────────────────────────────────────────
// Returns data for the sleep area chart on SleepMoodScreen
export const getSleepTrends = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const days = parseInt(req.query.days as string) || 30;
    const since = new Date();
    since.setDate(since.getDate() - days);

    const logs = await SleepMoodLog.find({ userId: req.userId, date: { $gte: since } })
      .sort({ date: 1 })
      .select('date sleepDuration sleepQuality');

    const trends = logs.map((log) => ({
      date: log.date.toISOString().split('T')[0],
      sleepDuration: log.sleepDuration,
      sleepQuality: log.sleepQuality,
    }));

    res.status(200).json({ success: true, trends });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching sleep trends', error });
  }
};

// ─── GET MOOD TRENDS ──────────────────────────────────────────────────────────
// Returns data for the mood line chart
export const getMoodTrends = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const days = parseInt(req.query.days as string) || 30;
    const since = new Date();
    since.setDate(since.getDate() - days);

    const logs = await SleepMoodLog.find({ userId: req.userId, date: { $gte: since } })
      .sort({ date: 1 })
      .select('date mood moodScore');

    const trends = logs.map((log) => ({
      date: log.date.toISOString().split('T')[0],
      mood: log.mood,
      moodScore: log.moodScore,
    }));

    res.status(200).json({ success: true, trends });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching mood trends', error });
  }
};

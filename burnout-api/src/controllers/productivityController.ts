import { Response } from 'express';
import ProductivityLog from '../models/ProductivityLog';
import { AuthRequest } from '../middleware/auth';

// ─── LOG PRODUCTIVITY ─────────────────────────────────────────────────────────
export const logProductivity = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const {
      productivityScore, focusHours, breakHours,
      tasksCompleted, tasksPlanned, peakHourStart,
      peakHourEnd, distractions, categories, notes, date,
    } = req.body;

    if (productivityScore === undefined) {
      res.status(400).json({ success: false, message: 'productivityScore is required' });
      return;
    }

    const log = await ProductivityLog.create({
      userId: req.userId,
      date: date ? new Date(date) : new Date(),
      productivityScore, focusHours, breakHours,
      tasksCompleted, tasksPlanned, peakHourStart,
      peakHourEnd, distractions,
      categories: categories || [],
      notes,
    });

    res.status(201).json({ success: true, message: 'Productivity logged', log });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error logging productivity', error });
  }
};

// ─── GET TODAY'S PRODUCTIVITY ─────────────────────────────────────────────────
// Returns data for the circular score indicator on ProductivityScreen
export const getTodayProductivity = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const log = await ProductivityLog.findOne({
      userId: req.userId,
      date: { $gte: today, $lt: tomorrow },
    });

    res.status(200).json({ success: true, log: log || null });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching today productivity', error });
  }
};

// ─── GET WEEKLY TREND ─────────────────────────────────────────────────────────
// Returns data for the weekly trend line chart on ProductivityScreen
export const getWeeklyProductivity = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const logs = await ProductivityLog.find({
      userId: req.userId,
      date: { $gte: sevenDaysAgo },
    }).sort({ date: 1 }).select('date productivityScore focusHours tasksCompleted');

    res.status(200).json({ success: true, trend: logs });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Error fetching weekly productivity trend', error });
  }
};

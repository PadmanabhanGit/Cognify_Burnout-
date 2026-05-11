import { Request, Response } from 'express';
import UsageLog from '../models/UsageLog';

// Helper to determine category, color, and icon based on package name or keywords
const categorizeApp = (packageName: string) => {
  const lowerName = packageName.toLowerCase();
  
  if (lowerName.includes('youtube') || lowerName.includes('netflix') || lowerName.includes('spotify') || lowerName.includes('tiktok') || lowerName.includes('video')) {
    return { category: 'Entertainment', color: '#3B82F6' };
  }
  if (lowerName.includes('facebook') || lowerName.includes('instagram') || lowerName.includes('twitter') || lowerName.includes('whatsapp') || lowerName.includes('snapchat')) {
    return { category: 'Social Media', color: '#EC4899' };
  }
  if (lowerName.includes('gmail') || lowerName.includes('docs') || lowerName.includes('drive') || lowerName.includes('slack') || lowerName.includes('teams')) {
    return { category: 'Productivity', color: '#10B981' };
  }
  if (lowerName.includes('game') || lowerName.includes('pubg') || lowerName.includes('candy') || lowerName.includes('clash')) {
    return { category: 'Gaming', color: '#F97316' };
  }
  return { category: 'Other', color: '#6B7280' };
};

// Helper to format duration in minutes to "Xh Ym" or "Ym"
const formatDuration = (minutes: number) => {
  const hours = Math.floor(minutes / 60);
  const mins = Math.floor(minutes % 60);
  if (hours > 0) return `${hours}h ${mins}m`;
  return `${mins}m`;
};

// @desc    Sync daily app usage data from device
// @route   POST /api/usage/sync
// @access  Private
export const syncUsageData = async (req: Request, res: Response) => {
  try {
    const { usageData, date } = req.body;

    if (!usageData || !Array.isArray(usageData)) {
      return res.status(400).json({ success: false, message: 'Invalid usage data provided' });
    }

    const logDate = date ? new Date(date) : new Date();
    // Normalize date to start of day to avoid multiple logs for the same day
    logDate.setHours(0, 0, 0, 0);

    let usageLog = await UsageLog.findOne({
      userId: req.user?._id,
      date: {
        $gte: logDate,
        $lt: new Date(logDate.getTime() + 24 * 60 * 60 * 1000), // Within the same day
      },
    });

    if (usageLog) {
      // Update existing log
      usageLog.usageData = usageData;
      await usageLog.save();
    } else {
      // Create new log
      usageLog = await UsageLog.create({
        userId: req.user?._id,
        date: logDate,
        usageData,
      });
    }

    res.status(200).json({
      success: true,
      message: 'Usage data synced successfully',
    });
  } catch (error) {
    console.error('Error syncing usage data:', error);
    res.status(500).json({ success: false, message: 'Server error while syncing usage data' });
  }
};

// @desc    Get today's app usage data formatted for the frontend
// @route   GET /api/usage/today
// @access  Private
export const getTodayUsage = async (req: Request, res: Response) => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const usageLog = await UsageLog.findOne({
      userId: req.user?._id,
      date: {
        $gte: today,
        $lt: new Date(today.getTime() + 24 * 60 * 60 * 1000),
      },
    });

    if (!usageLog || usageLog.usageData.length === 0) {
      return res.status(200).json({
        success: true,
        usage: [],
      });
    }

    // Group usage by category
    const categoryTotals: Record<string, { duration: number; color: string }> = {};
    let totalMinutes = 0;

    usageLog.usageData.forEach((item) => {
      const { category, color } = categorizeApp(item.packageName);
      if (!categoryTotals[category]) {
        categoryTotals[category] = { duration: 0, color };
      }
      categoryTotals[category].duration += item.duration;
      totalMinutes += item.duration;
    });

    // Format for response
    const usageResponse = Object.keys(categoryTotals).map((category) => {
      const { duration, color } = categoryTotals[category];
      const progress = totalMinutes > 0 ? duration / totalMinutes : 0;
      
      return {
        category,
        time: formatDuration(duration),
        progress: progress,
        color,
      };
    });

    // Sort by duration descending
    usageResponse.sort((a, b) => {
      const durationA = categoryTotals[a.category].duration;
      const durationB = categoryTotals[b.category].duration;
      return durationB - durationA;
    });

    res.status(200).json({
      success: true,
      usage: usageResponse,
    });
  } catch (error) {
    console.error('Error fetching today usage:', error);
    res.status(500).json({ success: false, message: 'Server error while fetching today usage' });
  }
};

import mongoose, { Schema, Document } from 'mongoose';

export interface ISleepMoodLog extends Document {
  userId: mongoose.Types.ObjectId;
  date: Date;
  sleepDuration: number; // in hours (e.g. 7.5)
  sleepQuality: number; // 1-10 scale (matches the slider in SleepMoodScreen)
  mood: string; // e.g. 'happy', 'sad', 'anxious', 'calm', 'energetic', 'tired'
  moodScore: number; // 1-10 numeric representation
  notes?: string;
  createdAt: Date;
  updatedAt: Date;
}

const SleepMoodLogSchema: Schema = new Schema(
  {
    userId: {
      type: Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    date: {
      type: Date,
      required: true,
      default: Date.now,
    },
    sleepDuration: {
      type: Number,
      required: [true, 'Sleep duration is required'],
      min: [0, 'Sleep duration cannot be negative'],
      max: [24, 'Sleep duration cannot exceed 24 hours'],
    },
    sleepQuality: {
      type: Number,
      required: [true, 'Sleep quality is required'],
      min: [1, 'Sleep quality must be at least 1'],
      max: [10, 'Sleep quality cannot exceed 10'],
    },
    mood: {
      type: String,
      required: [true, 'Mood is required'],
      enum: ['happy', 'sad', 'anxious', 'calm', 'energetic', 'tired', 'stressed', 'neutral', 'excited', 'angry'],
    },
    moodScore: {
      type: Number,
      required: true,
      min: [1, 'Mood score must be at least 1'],
      max: [10, 'Mood score cannot exceed 10'],
    },
    notes: {
      type: String,
      trim: true,
      default: '',
    },
  },
  {
    timestamps: true,
  }
);

// Index for efficient date-range queries (sleep trends, mood trends)
SleepMoodLogSchema.index({ userId: 1, date: -1 });

export default mongoose.model<ISleepMoodLog>('SleepMoodLog', SleepMoodLogSchema);

import mongoose, { Schema, Document } from 'mongoose';

export interface IProductivityLog extends Document {
  userId: mongoose.Types.ObjectId;
  date: Date;
  productivityScore: number; // 0-100 (circular indicator on ProductivityScreen)
  focusHours: number; // hours of focused work
  breakHours: number; // hours of breaks
  tasksCompleted: number;
  tasksPlanned: number;
  peakHourStart: number; // 0-23 (hour of day)
  peakHourEnd: number; // 0-23
  distractions: number; // count of distractions
  categories: {
    name: string;
    hours: number; // time distribution
  }[];
  notes?: string;
  createdAt: Date;
  updatedAt: Date;
}

const ProductivityLogSchema: Schema = new Schema(
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
    productivityScore: {
      type: Number,
      required: true,
      min: [0, 'Productivity score cannot be negative'],
      max: [100, 'Productivity score cannot exceed 100'],
    },
    focusHours: {
      type: Number,
      default: 0,
      min: 0,
    },
    breakHours: {
      type: Number,
      default: 0,
      min: 0,
    },
    tasksCompleted: {
      type: Number,
      default: 0,
      min: 0,
    },
    tasksPlanned: {
      type: Number,
      default: 0,
      min: 0,
    },
    peakHourStart: {
      type: Number,
      default: 9,
      min: 0,
      max: 23,
    },
    peakHourEnd: {
      type: Number,
      default: 12,
      min: 0,
      max: 23,
    },
    distractions: {
      type: Number,
      default: 0,
      min: 0,
    },
    categories: [
      {
        name: { type: String, required: true },
        hours: { type: Number, required: true, min: 0 },
      },
    ],
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

ProductivityLogSchema.index({ userId: 1, date: -1 });

export default mongoose.model<IProductivityLog>('ProductivityLog', ProductivityLogSchema);

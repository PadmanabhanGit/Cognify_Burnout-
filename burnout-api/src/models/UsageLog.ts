import mongoose, { Schema, Document } from 'mongoose';

export interface IUsageItem {
  packageName: string;
  duration: number; // in minutes
}

export interface IUsageLog extends Document {
  userId: mongoose.Types.ObjectId;
  date: Date;
  usageData: IUsageItem[];
  createdAt: Date;
  updatedAt: Date;
}

const UsageLogSchema: Schema = new Schema(
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
    usageData: [
      {
        packageName: { type: String, required: true },
        duration: { type: Number, required: true, min: 0 },
      },
    ],
  },
  {
    timestamps: true,
  }
);

// Index to quickly find usage log for a specific user on a specific date
UsageLogSchema.index({ userId: 1, date: -1 });

export default mongoose.model<IUsageLog>('UsageLog', UsageLogSchema);

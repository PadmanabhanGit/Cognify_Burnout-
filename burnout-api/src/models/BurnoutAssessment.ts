import mongoose, { Schema, Document } from 'mongoose';

export interface IBurnoutAssessment extends Document {
  userId: mongoose.Types.ObjectId;
  date: Date;
  riskScore: number; // 0-100 (pie chart on BurnoutPredictionScreen)
  riskLevel: string; // 'low', 'moderate', 'high', 'critical'
  factors: {
    name: string;
    score: number; // 0-100 progress bars
  }[];
  wellbeingDimensions: {
    physical: number; // 0-10 (radar chart)
    emotional: number;
    social: number;
    intellectual: number;
    occupational: number;
  };
  warnings: string[];
  recommendations: string[];
  createdAt: Date;
  updatedAt: Date;
}

const BurnoutAssessmentSchema: Schema = new Schema(
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
    riskScore: {
      type: Number,
      required: true,
      min: [0, 'Risk score cannot be negative'],
      max: [100, 'Risk score cannot exceed 100'],
    },
    riskLevel: {
      type: String,
      required: true,
      enum: ['low', 'moderate', 'high', 'critical'],
    },
    factors: [
      {
        name: { type: String, required: true },
        score: { type: Number, required: true, min: 0, max: 100 },
      },
    ],
    wellbeingDimensions: {
      physical: { type: Number, default: 5, min: 0, max: 10 },
      emotional: { type: Number, default: 5, min: 0, max: 10 },
      social: { type: Number, default: 5, min: 0, max: 10 },
      intellectual: { type: Number, default: 5, min: 0, max: 10 },
      occupational: { type: Number, default: 5, min: 0, max: 10 },
    },
    warnings: [{ type: String }],
    recommendations: [{ type: String }],
  },
  {
    timestamps: true,
  }
);

BurnoutAssessmentSchema.index({ userId: 1, date: -1 });

export default mongoose.model<IBurnoutAssessment>('BurnoutAssessment', BurnoutAssessmentSchema);

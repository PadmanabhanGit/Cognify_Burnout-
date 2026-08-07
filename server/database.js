const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.resolve(__dirname, 'cognify.db');
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Error opening database', err.message);
  } else {
    console.log('Connected to the SQLite database.');
    createTables();
  }
});

function createTables() {
  db.serialize(() => {
    // Users Table
    db.run(`CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      fullName TEXT NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      avatarUrl TEXT
    )`);

    // User Profiles Table
    db.run(`CREATE TABLE IF NOT EXISTS user_profiles (
      userId TEXT PRIMARY KEY,
      firstName TEXT,
      lastName TEXT,
      age TEXT,
      location TEXT,
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);

    // Study Sessions Table
    db.run(`CREATE TABLE IF NOT EXISTS study_sessions (
      id TEXT PRIMARY KEY,
      userId TEXT NOT NULL,
      subject TEXT NOT NULL,
      duration INTEGER DEFAULT 0,
      startTime TEXT NOT NULL,
      endTime TEXT,
      isActive INTEGER DEFAULT 1,
      notes TEXT,
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);

    // Sleep & Mood Logs Table
    db.run(`CREATE TABLE IF NOT EXISTS sleep_mood_logs (
      id TEXT PRIMARY KEY,
      userId TEXT NOT NULL,
      date TEXT NOT NULL,
      sleepDuration REAL,
      sleepQuality INTEGER,
      mood TEXT,
      moodScore INTEGER,
      notes TEXT,
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);

    // Productivity Logs Table
    db.run(`CREATE TABLE IF NOT EXISTS productivity_logs (
      id TEXT PRIMARY KEY,
      userId TEXT NOT NULL,
      date TEXT NOT NULL,
      productivityScore INTEGER,
      focusHours REAL,
      breakHours REAL,
      tasksCompleted INTEGER,
      tasksPlanned INTEGER,
      peakHourStart INTEGER,
      peakHourEnd INTEGER,
      distractions INTEGER,
      notes TEXT,
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);

    // Burnout Assessments Table
    db.run(`CREATE TABLE IF NOT EXISTS burnout_assessments (
      id TEXT PRIMARY KEY,
      userId TEXT NOT NULL,
      date TEXT NOT NULL,
      riskScore INTEGER,
      riskLevel TEXT,
      factors TEXT, -- JSON string
      wellbeingDimensions TEXT, -- JSON string
      warnings TEXT, -- JSON string
      recommendations TEXT, -- JSON string
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);

    // App Usage Table (with unique constraint for sync logic)
    db.run(`CREATE TABLE IF NOT EXISTS app_usage (
      id TEXT PRIMARY KEY,
      userId TEXT NOT NULL,
      date TEXT NOT NULL,
      category TEXT NOT NULL,
      duration INTEGER NOT NULL,
      UNIQUE(userId, date, category),
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);

    // Physical Activity Table (for Samsung Health / Google Fit sync)
    db.run(`CREATE TABLE IF NOT EXISTS physical_activity (
      id TEXT PRIMARY KEY,
      userId TEXT NOT NULL,
      date TEXT NOT NULL,
      steps INTEGER DEFAULT 0,
      calories INTEGER DEFAULT 0,
      activeMinutes INTEGER DEFAULT 0,
      source TEXT, -- 'SamsungHealth', 'GoogleFit', 'Manual'
      UNIQUE(userId, date),
      FOREIGN KEY (userId) REFERENCES users (id)
    )`);
  });
}

module.exports = db;

require('dotenv').config();
const express = require('express');
const cors = require('cors');
//const db = require('./database');
const { db } = require('./firebase');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Routes
app.use('/api/auth', require('./routes/auth'));
app.use('/api/study', require('./routes/study'));
app.use('/api/dashboard', require('./routes/dashboard'));
app.use('/api/usage', require('./routes/usage'));
app.use('/api/activity', require('./routes/physicalActivity'));
app.use('/api/burnout', require('./routes/burnout'));
app.use('/api/sleep-mood', require('./routes/sleepMood'));
app.use('/api/productivity', require('./routes/productivity'));
app.use('/api/report', require('./routes/report'));
// app.use('/api/productivity', require('./routes/productivity'));
// app.use('/api/burnout', require('./routes/burnout'));
// app.use('/api/report', require('./routes/report'));

// Root endpoint
app.get('/', (req, res) => {
  res.send('Cognify Backend API is running...');
});

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

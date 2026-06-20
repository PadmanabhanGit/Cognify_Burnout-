const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const db = require('../database');

const JWT_SECRET = process.env.JWT_SECRET || 'your_super_secret_key';

// @route   POST api/auth/register
// @desc    Register a user
router.post('/register', async (req, res) => {
  const { fullName, email, password } = req.body;

  if (!fullName || !email || !password) {
    return res.status(400).json({ success: false, message: 'Please enter all fields' });
  }

  // Check if user exists
  db.get('SELECT * FROM users WHERE email = ?', [email], async (err, user) => {
    if (err) return res.status(500).json({ success: false, message: 'Database error' });
    if (user) return res.status(400).json({ success: false, message: 'User already exists' });

    const id = uuidv4();
    const hashedPassword = await bcrypt.hash(password, 10);

    db.run(
      'INSERT INTO users (id, fullName, email, password) VALUES (?, ?, ?, ?)',
      [id, fullName, email, hashedPassword],
      function(err) {
        if (err) return res.status(500).json({ success: false, message: 'Error creating user' });

        const token = jwt.sign({ id }, JWT_SECRET, { expiresIn: '7d' });

        res.json({
          success: true,
          message: 'User registered successfully',
          token,
          user: { id, fullName, email }
        });
      }
    );
  });
});

// @route   POST api/auth/login
// @desc    Login user
router.post('/login', (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ success: false, message: 'Please enter all fields' });
  }

  db.get('SELECT * FROM users WHERE email = ?', [email], async (err, user) => {
    if (err) return res.status(500).json({ success: false, message: 'Database error' });
    if (!user) return res.status(400).json({ success: false, message: 'Invalid credentials' });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(400).json({ success: false, message: 'Invalid credentials' });

    const token = jwt.sign({ id: user.id }, JWT_SECRET, { expiresIn: '7d' });

    res.json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        fullName: user.fullName,
        email: user.email,
        avatarUrl: user.avatarUrl
      }
    });
  });
});

module.exports = router;

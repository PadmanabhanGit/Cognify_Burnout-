const express = require('express');
const router = express.Router();
const db = require('../database');
const { verifyToken } = require('../middleware/authMiddleware');

// Get User Profile
router.get('/', verifyToken, (req, res) => {
  const userId = req.user.uid;
  
  db.get(`SELECT * FROM user_profiles WHERE userId = ?`, [userId], (err, row) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ error: 'Database error' });
    }
    if (!row) {
      return res.json({
        firstName: '',
        lastName: '',
        age: '',
        location: ''
      });
    }
    res.json(row);
  });
});

// Update User Profile
router.post('/', verifyToken, (req, res) => {
  const userId = req.user.uid;
  const { firstName, lastName, age, location } = req.body;

  const query = `
    INSERT INTO user_profiles (userId, firstName, lastName, age, location)
    VALUES (?, ?, ?, ?, ?)
    ON CONFLICT(userId) DO UPDATE SET
      firstName = excluded.firstName,
      lastName = excluded.lastName,
      age = excluded.age,
      location = excluded.location
  `;

  db.run(query, [userId, firstName, lastName, age, location], function(err) {
    if (err) {
      console.error(err);
      return res.status(500).json({ error: 'Database error' });
    }
    res.json({ success: true, message: 'Profile updated successfully' });
  });
});

module.exports = router;

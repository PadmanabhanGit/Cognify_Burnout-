const express = require('express');
const router = express.Router();
const { db } = require('../firebase');
const authMiddleware = require('../middleware/auth');

// @route   POST api/auth/register
// @desc    Called once, right after the client registers with Firebase Auth SDK.
//          Client sends its Firebase ID token; we verify it and create the profile doc.
router.post('/register', authMiddleware, async (req, res) => {
  const { fullName } = req.body;
  const uid = req.user.uid;
  const email = req.user.email;

  try {
    await db.collection('users').doc(uid).set({
      fullName: fullName || '',
      email: email || null,
      createdAt: new Date().toISOString(),
    });
    res.json({ success: true, message: 'Profile created' });
  } catch (err) {
    console.error('Profile creation failed:', err.message);
    res.status(500).json({ success: false, message: 'Error creating profile' });
  }
});

// @route   GET api/auth/me
// @desc    Fetch the logged-in user's profile
router.get('/me', authMiddleware, async (req, res) => {
  try {
    const doc = await db.collection('users').doc(req.user.uid).get();
    if (!doc.exists) return res.status(404).json({ success: false, message: 'Profile not found' });
    res.json({ success: true, user: doc.data() });
  } catch (err) {
    console.error('Profile fetch failed:', err.message);
    res.status(500).json({ success: false, message: 'Error fetching profile' });
  }
});

module.exports = router;
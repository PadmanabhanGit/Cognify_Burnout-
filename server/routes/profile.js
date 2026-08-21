const express = require('express');
const router = express.Router();
const { db } = require('../firebase');
const authMiddleware = require('../middleware/auth'); // ensure correct auth middleware import

// Get User Profile
router.get('/', authMiddleware, async (req, res) => {
  const userId = req.user.uid;
  
  try {
    const doc = await db.collection('users').doc(userId).get();
    if (!doc.exists) {
      return res.json({
        firstName: '',
        lastName: '',
        age: '',
        location: '',
        linkedAccounts: []
      });
    }
    res.json(doc.data());
  } catch (err) {
    console.error('Error fetching profile from Firestore:', err);
    res.status(500).json({ error: 'Database error' });
  }
});

// Update User Profile
router.post('/', authMiddleware, async (req, res) => {
  const userId = req.user.uid;

  // Extract all fields that could be in ProfileData
  const { firstName, lastName, age, location, linkedAccounts } = req.body;

  // Only write fields the caller actually sent. Each screen (Personal Info,
  // Privacy & Data, ...) only sends the subset it edits; defaulting the rest
  // to '' / [] / false here would blank out every field the request omitted.
  const updates = { updatedAt: new Date().toISOString() };
  if (firstName !== undefined) updates.firstName = firstName;
  if (lastName !== undefined) updates.lastName = lastName;
  if (age !== undefined) updates.age = age;
  if (location !== undefined) updates.location = location;
  if (linkedAccounts !== undefined) updates.linkedAccounts = linkedAccounts;

  try {
    await db.collection('users').doc(userId).set(updates, { merge: true });

    res.json({ success: true, message: 'Profile updated successfully in Firestore' });
  } catch (err) {
    console.error('Error updating profile in Firestore:', err);
    res.status(500).json({ error: 'Database error' });
  }
});

module.exports = router;

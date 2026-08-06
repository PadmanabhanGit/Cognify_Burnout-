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
        linkedAccounts: [],
        syncHealth: false,
        anonymousAnalytics: false,
        personalizedInsights: true
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
  const { 
    firstName, lastName, age, location, 
    linkedAccounts, syncHealth, 
    anonymousAnalytics, personalizedInsights 
  } = req.body;

  try {
    await db.collection('users').doc(userId).set({
      firstName: firstName || '',
      lastName: lastName || '',
      age: age || '',
      location: location || '',
      linkedAccounts: linkedAccounts || [],
      syncHealth: syncHealth ?? false,
      anonymousAnalytics: anonymousAnalytics ?? false,
      personalizedInsights: personalizedInsights ?? true,
      updatedAt: new Date().toISOString()
    }, { merge: true }); // Use merge: true to avoid overwriting email/fullName from auth/register

    res.json({ success: true, message: 'Profile updated successfully in Firestore' });
  } catch (err) {
    console.error('Error updating profile in Firestore:', err);
    res.status(500).json({ error: 'Database error' });
  }
});

module.exports = router;

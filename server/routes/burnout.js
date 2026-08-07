const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { computeBurnoutRisk } = require('../services/burnoutService');

router.get('/compute', auth, async (req, res) => {
    const userId = req.user.uid;
    const today = new Date().toISOString().split('T')[0];
    try {
        const computed = await computeBurnoutRisk(userId, today);
        res.json({ success: true, computed });
    } catch (err) {
        console.error('Error computing burnout risk:', err.message);
        res.status(500).json({ success: false, message: "Error computing burnout risk" });
    }
});

module.exports = router;
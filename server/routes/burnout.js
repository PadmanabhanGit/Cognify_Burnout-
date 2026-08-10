const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { db } = require('../firebase');
const { computeBurnoutRisk } = require('../services/burnoutService');

// ─── Canonical Timezone ───────────────────────────────────────────────────────
// Matches study.js and dashboard.js exactly. Android's device timezone is IST
// (UTC+05:30), so the calendar day used for the document id must be IST too —
// otherwise a 00:30 IST assessment would be filed under the previous day.
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

function getISTDateString(dateOrString = new Date()) {
    const ist = new Date(new Date(dateOrString).getTime() + IST_OFFSET_MS);
    const y = ist.getUTCFullYear();
    const m = String(ist.getUTCMonth() + 1).padStart(2, '0');
    const d = String(ist.getUTCDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

// Coerce a 0-100 integer, or null if absent/unusable. Never substitutes a default.
function toScore(value) {
    const n = Number(value);
    if (!Number.isFinite(n)) return null;
    return Math.max(0, Math.min(100, Math.round(n)));
}

// @route   POST api/burnout/assessment
// @desc    Persist the burnout result ALREADY CALCULATED BY ANDROID.
//          This endpoint stores; it never computes. Android's TFLite prediction,
//          InsightGenerator factors and WellbeingGenerator radar values are the
//          single source of truth for every consumer, including the Web app.
router.post('/assessment', auth, async (req, res) => {
    const userId = req.user.uid;
    const {
        riskScore,
        riskLevel,
        assessment,
        warnings,
        factors,
        wellbeing,
        recommendations
    } = req.body || {};

    // ── Validation ───────────────────────────────────────────────────────────
    const score = toScore(riskScore);
    if (score === null) {
        return res.status(400).json({ success: false, message: 'riskScore must be a number between 0 and 100' });
    }
    if (typeof riskLevel !== 'string' || riskLevel.trim() === '') {
        return res.status(400).json({ success: false, message: 'riskLevel is required' });
    }

    // Keep only well-formed factors; drop anything malformed rather than defaulting it.
    const cleanFactors = Array.isArray(factors)
        ? factors
            .filter(f => f && typeof f.name === 'string' && toScore(f.score) !== null)
            .map(f => ({ name: f.name, score: toScore(f.score) }))
        : [];

    // Wellbeing is stored only if all six Android axes are present. A partial
    // radar would render as a misleading shape, so it is stored as null instead.
    const WELLBEING_AXES = ['focus', 'stress', 'mood', 'energy', 'sleep', 'study'];
    let cleanWellbeing = null;
    if (wellbeing && typeof wellbeing === 'object') {
        const axes = {};
        const complete = WELLBEING_AXES.every(axis => {
            const v = toScore(wellbeing[axis]);
            if (v === null) return false;
            axes[axis] = v;
            return true;
        });
        if (complete) cleanWellbeing = axes;
    }

    const date = getISTDateString();
    const docId = `${userId}_${date}`;   // deterministic → upsert, never duplicates
    const now = new Date().toISOString();

    try {
        const docRef = db.collection('burnoutAssessments').doc(docId);
        const existing = await docRef.get();

        const payload = {
            userId,
            date,
            riskScore: score,
            riskLevel: riskLevel.trim(),
            assessment: typeof assessment === 'string' && assessment.trim() !== '' ? assessment.trim() : null,
            warnings: Array.isArray(warnings) ? warnings.filter(w => typeof w === 'string') : [],
            factors: cleanFactors,
            wellbeing: cleanWellbeing,
            recommendations: Array.isArray(recommendations) ? recommendations.filter(r => typeof r === 'string') : [],
            updatedAt: now,
            createdAt: existing.exists ? (existing.data().createdAt || now) : now
        };

        // merge:true keeps this to one document per user per IST day — one write
        // per Android refresh cycle, preserving the existing quota profile.
        await docRef.set(payload, { merge: true });

        const saved = await docRef.get();
        res.json({ success: true, assessment: { id: saved.id, ...saved.data() } });
    } catch (err) {
        console.error('Error saving burnout assessment:', err.message);
        res.status(500).json({ success: false, message: 'Database error' });
    }
});

// @route   GET api/burnout/assessment
// @desc    Return the latest persisted Android assessment for this user.
//          Returns an explicit empty result when none exists — it does NOT fall
//          back to computeBurnoutRisk(), because that is a different algorithm
//          and would silently disagree with the Android app.
router.get('/assessment', auth, async (req, res) => {
    const userId = req.user.uid;
    try {
        const snapshot = await db.collection('burnoutAssessments')
            .where('userId', '==', userId)
            .orderBy('date', 'desc')
            .limit(1)
            .get();

        if (snapshot.empty) {
            return res.json({ success: true, available: false, assessment: null });
        }

        const doc = snapshot.docs[0];
        res.json({ success: true, available: true, assessment: { id: doc.id, ...doc.data() } });
    } catch (err) {
        console.error('Error fetching burnout assessment:', err.message);
        res.status(500).json({ success: false, message: 'Database error' });
    }
});

// @route   GET api/burnout/compute
// @desc    Legacy server-side heuristic. Retained for backward compatibility with
//          any existing caller, but it is NO LONGER used by /api/dashboard and must
//          not be used as a substitute for the Android assessment.
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

const { db } = require('../firebase');
const { normalizeDateValue } = require('../utils/dateUtils');

async function getTodaySleep(userId, date) {
    const snapshot = await db.collection('sleepMoodLogs')
        .where('userId', '==', userId)
        .get();

    const matchingLogs = snapshot.docs
        .map(doc => doc.data())
        .filter(item => normalizeDateValue(item.date) === date)
        .sort((a, b) => (b.date || '').localeCompare(a.date || ''));

    return matchingLogs.length > 0 ? matchingLogs[0] : null;
}

async function getTodayActivity(userId, date) {
    const doc = await db.collection('physicalActivity').doc(`${userId}_${date}`).get();
    return doc.exists ? doc.data() : null;
}

async function getTodayUsage(userId, date) {
    const snapshot = await db.collection('appUsage')
        .where('userId', '==', userId)
        .where('date', '==', date)
        .get();
    return snapshot.docs.map(doc => doc.data());
}

async function getTodayStudyMinutes(userId, date) {
    const snapshot = await db.collection('studySessions')
        .where('userId', '==', userId)
        .get();

    return snapshot.docs
        .map(doc => doc.data())
        .filter(item => normalizeDateValue(item.startTime) === date)
        .reduce((sum, item) => sum + (item.duration || 0), 0);
}

async function computeBurnoutRisk(userId, date) {
    const sleepData = await getTodaySleep(userId, date);
    const activityData = await getTodayActivity(userId, date);
    const usageData = await getTodayUsage(userId, date);
    const studyMins = await getTodayStudyMinutes(userId, date);

    let riskScore = 0;
    let warnings = [];
    let recommendations = [];

    const sleepHrs = sleepData ? sleepData.sleepDuration : 7;
    if (sleepHrs < 6) {
        riskScore += 25;
        warnings.push("Severely low sleep duration detected.");
        recommendations.push("Prioritize getting at least 7 hours of sleep tonight.");
    } else if (sleepHrs < 7) {
        riskScore += 10;
        warnings.push("Sleep is slightly below optimal levels.");
    }

    const steps = activityData ? activityData.steps : 0;
    if (steps < 3000) {
        riskScore += 20;
        warnings.push("Sedentary behavior increases cognitive fatigue.");
        recommendations.push("Try a 15-minute walk to clear your mind.");
    } else if (steps < 6000) {
        riskScore += 10;
    }

    const entertainmentMins = usageData
        .filter(u => u.category === 'Entertainment' || u.category === 'Social Media')
        .reduce((sum, u) => sum + u.duration, 0);

    if (entertainmentMins > 240) {
        riskScore += 15;
        warnings.push("High screen time detected in entertainment apps.");
        recommendations.push("Set app limits for social media.");
    }

    if (studyMins > 0 && (entertainmentMins / studyMins) > 2) {
        riskScore += 10;
        warnings.push("Focus ratio is skewed toward escapism.");
    }

    const moodScore = sleepData ? sleepData.moodScore : 7;
    if (moodScore < 5) {
        riskScore += 30;
        warnings.push("Low mood score reported. Stress levels are high.");
        recommendations.push("Consider a mindfulness session or talking to a friend.");
    } else if (moodScore < 7) {
        riskScore += 15;
    }

    let riskLevel = "Low";
    if (riskScore > 75) riskLevel = "Critical";
    else if (riskScore > 50) riskLevel = "High";
    else if (riskScore > 25) riskLevel = "Moderate";

    return {
        riskScore: Math.min(riskScore, 100),
        riskLevel,
        factors: [
            { name: "Recovery (Sleep)", score: sleepData ? (sleepHrs / 8) * 100 : 70 },
            { name: "Activity (Steps)", score: Math.min((steps / 10000) * 100, 100) },
            { name: "Focus Balance", score: studyMins > 0 ? (studyMins / (studyMins + entertainmentMins)) * 100 : 50 }
        ],
        wellbeingDimensions: {
            physical: steps > 8000 ? 9 : (steps > 4000 ? 6 : 3),
            emotional: moodScore,
            social: 7,
            intellectual: studyMins > 120 ? 8 : 5,
            occupational: 6
        },
        warnings,
        recommendations
    };
}

module.exports = { computeBurnoutRisk };
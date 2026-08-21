process.env.NODE_ENV = 'production';
const { db } = require('./firebase');
const { getLocalDateString, normalizeDateValue } = require('./utils/dateUtils');

async function run() {
  const usersSnap = await db.collection('appUsage').orderBy('date', 'desc').limit(10).get();
  let userId = null;
  if (!usersSnap.empty) {
    userId = usersSnap.docs[0].data().userId;
  }
  if (!userId) {
    console.log("No usage data found to identify a user");
    return;
  }
  console.log("Found active userId:", userId);
  const today = getLocalDateString();
  console.log("today (getLocalDateString):", today);

  // appUsage (Optimized Query)
  const usageSnap = await db.collection('appUsage')
    .where('userId', '==', userId)
    .where('date', '==', today)
    .get();
  
  let todayAppUsageSeconds = 0;
  console.log("appUsage doc count for today:", usageSnap.size);
  usageSnap.docs.forEach(doc => {
    const data = doc.data();
    console.log(`  - doc: date=${data.date}, category=${data.category}, totalDurationSeconds=${data.totalDurationSeconds}, totalDuration=${data.totalDuration}`);
  });

  // Check AppUsage (Original Unoptimized Query for verification)
  const oldUsageSnap = await db.collection('appUsage')
    .where('userId', '==', userId)
    .get();
  console.log("appUsage total doc count (all dates):", oldUsageSnap.size);
  oldUsageSnap.docs.forEach(doc => {
    const data = doc.data();
    if (data.date !== today) {
       console.log(`  - non-today doc: date=${data.date} (vs today=${today})`);
    }
  });

  // sleepMoodLogs
  const sleepSnap = await db.collection('sleepMoodLogs')
      .where('userId', '==', userId)
      .orderBy('createdAt', 'desc')
      .limit(1)
      .get();
  console.log("sleepMoodLogs recent doc exists:", !sleepSnap.empty);
  if (!sleepSnap.empty) {
     console.log("  - sleep doc data:", sleepSnap.docs[0].data());
  }

  // studySessions
  const weekAgo = new Date();
  weekAgo.setDate(weekAgo.getDate() - 7);
  const studySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .where('startTime', '>=', weekAgo.toISOString())
      .get();
  console.log("studySessions doc count (last 7 days):", studySnap.size);
  if (!studySnap.empty) {
     console.log("  - recent study doc data:", studySnap.docs[0].data());
  }

  const oldStudySnap = await db.collection('studySessions')
      .where('userId', '==', userId)
      .get();
  console.log("studySessions total doc count (all dates):", oldStudySnap.size);
  
  process.exit(0);
}
run().catch(console.error);

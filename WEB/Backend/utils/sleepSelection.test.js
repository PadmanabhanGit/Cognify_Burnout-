const test = require('node:test');
const assert = require('node:assert/strict');
const {
  hasValidDetectedSleep,
  isAutomaticSource,
  nightOf,
  selectCanonicalSleepLog,
  sortByRecencyDesc,
} = require('./sleepSelection');

/** Epoch ms for a local wall-clock time, matching nightOf's local convention. */
const at = (s) => new Date(s).getTime();

/** A detected night: 23:00 -> 02:00 the next day is 3h. */
const night = ({ date, start, end, hours, createdAt }) => ({
  userId: 'u1',
  date,
  source: 'automatic',
  sleepStart: at(start),
  sleepEnd: at(end),
  sleepDuration: hours,
  createdAt,
});

test('canonical selection follows the night, not the write time', () => {
  // The Aug 14 night is the most recent night, but it was written FIRST.
  // The Aug 12 night was backfilled afterwards, so it has a newer createdAt.
  // Android ranks these by sleepStart DESC and shows Aug 14 (3h); ranking by
  // createdAt would show Aug 12 (7.5h) on the web for the same account.
  const logs = [
    night({ date: '2026-08-14', start: '2026-08-14T23:00:00', end: '2026-08-15T02:00:00', hours: 3, createdAt: '2026-08-15T06:00:00Z' }),
    night({ date: '2026-08-12', start: '2026-08-12T23:00:00', end: '2026-08-13T06:30:00', hours: 7.5, createdAt: '2026-08-15T09:00:00Z' }),
  ];

  assert.equal(selectCanonicalSleepLog(logs).sleepDuration, 3);
  // Guard the regression directly: write-time order really does disagree here.
  assert.equal(sortByRecencyDesc(logs)[0].sleepDuration, 7.5);
});

test('a re-synced correction wins for the same night despite preserved createdAt', () => {
  // routes/sleepMood.js preserves createdAt when re-writing the deterministic
  // automatic id, so a corrected record cannot rely on createdAt to rank.
  const first = night({ date: '2026-08-14', start: '2026-08-14T23:00:00', end: '2026-08-15T02:00:00', hours: 3, createdAt: '2026-08-15T06:00:00Z' });
  const corrected = { ...first, sleepDuration: 3.25, updatedAt: '2026-08-15T11:00:00Z' };

  assert.equal(selectCanonicalSleepLog([first, corrected]).sleepDuration, 3.25);
});

test('manual logs never become canonical, however recent', () => {
  const logs = [
    night({ date: '2026-08-14', start: '2026-08-14T23:00:00', end: '2026-08-15T02:00:00', hours: 3, createdAt: '2026-08-15T06:00:00Z' }),
    { userId: 'u1', date: '2026-08-15', source: 'manual', sleepDuration: 9, sleepStart: null, sleepEnd: null, createdAt: '2026-08-15T20:00:00Z' },
  ];

  assert.equal(selectCanonicalSleepLog(logs).sleepDuration, 3);
});

test('legacy records without a source field still rank by night', () => {
  const legacy = night({ date: '2026-08-10', start: '2026-08-10T23:00:00', end: '2026-08-11T07:00:00', hours: 8, createdAt: '2026-08-15T10:00:00Z' });
  delete legacy.source;
  const recent = night({ date: '2026-08-14', start: '2026-08-14T23:00:00', end: '2026-08-15T02:00:00', hours: 3, createdAt: '2026-08-15T06:00:00Z' });

  assert.equal(isAutomaticSource(legacy), true);
  assert.equal(selectCanonicalSleepLog([legacy, recent]).sleepDuration, 3);
});

test('nightOf falls back to the date when sleepEnd is absent', () => {
  assert.equal(nightOf({ date: '2026-08-14' }), at('2026-08-14T00:00:00'));
  assert.equal(nightOf({ date: 'not-a-date' }), 0);
  assert.equal(nightOf(null), 0);
});

test('no detected sleep yields null rather than a substituted manual log', () => {
  assert.equal(selectCanonicalSleepLog([{ source: 'manual', sleepDuration: 9 }]), null);
  assert.equal(selectCanonicalSleepLog([]), null);
  assert.equal(selectCanonicalSleepLog(null), null);
  assert.equal(hasValidDetectedSleep({ sleepStart: 5, sleepEnd: 5 }), false);
});

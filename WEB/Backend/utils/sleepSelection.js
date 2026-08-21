/**
 * Canonical sleep-record selection — ONE definition, shared by
 * routes/sleepMood.js (Web Sleep page) and routes/dashboard.js (Sleep card),
 * so the two endpoints can never select different records.
 *
 * WHY THIS IS DONE IN JS AND NOT AS A FIRESTORE QUERY
 * ---------------------------------------------------
 * The obvious query, `.where('sleepStart', '!=', null)`, is not usable here:
 *
 *   1. Firestore requires the FIRST orderBy() to be on the inequality field.
 *      Combining `where('userId','==')` + `where('sleepStart','!=')` +
 *      `orderBy('createdAt','desc')` is rejected at runtime.
 *   2. It would need a new composite index (userId ASC, sleepStart ASC) that
 *      does not exist in firestore.indexes.json — the query would fail in
 *      production until that index was built.
 *   3. Inequality filters skip documents where the field is ABSENT. Records
 *      written before sleepStart existed have no such field at all, so the
 *      behaviour would differ between old and new documents.
 *
 * routes/sleepMood.js already fetches this collection with a single
 * `where('userId','==')` and sorts in JS, so filtering here adds no extra
 * Firestore reads to that endpoint and requires no new index.
 */

/**
 * A record counts as canonical detected sleep only if it carries a usable
 * start/end pair from SleepMonitoringEngine. Manual mood logs store these as
 * null and are therefore never selected.
 */
function hasValidDetectedSleep(log) {
  const start = Number(log && log.sleepStart);
  const end = Number(log && log.sleepEnd);
  return Number.isFinite(start) && Number.isFinite(end) && start > 0 && end > start;
}

/**
 * True when a record should be treated as an automatic sleep-recognition
 * session for canonical selection.
 *
 * Prefers the explicit `source` field written by routes/sleepMood.js's POST
 * handler. Records written before that field existed have no `source` at
 * all (undefined) — those fall back to the original structural check
 * (hasValidDetectedSleep) so historical documents keep working unchanged,
 * per the backward-compatibility requirement for this pass.
 *
 * A record explicitly tagged `source: "manual"` is NEVER treated as
 * automatic here, even if it happened to carry a start/end pair — the
 * explicit tag takes precedence over the structural heuristic once present.
 */
function isAutomaticSource(log) {
  if (!log) return false;
  if (log.source === 'manual') return false;
  if (log.source === 'automatic') return true;
  return true; // no `source` field at all: legacy record, fall through to hasValidDetectedSleep below
}

/** Recency key, mirroring the existing fallback chain in routes/sleepMood.js. */
function recencyOf(log) {
  const t = new Date((log && (log.createdAt || log.updatedAt || log.date)) || 0).getTime();
  return Number.isFinite(t) ? t : 0;
}

function sortByRecencyDesc(logs) {
  return [...logs].sort((a, b) => recencyOf(b) - recencyOf(a));
}

/**
 * WHICH NIGHT this record describes, as an epoch-ms key.
 *
 * Distinct from [recencyOf], which answers "when was this row WRITTEN". The two
 * are not interchangeable, and conflating them is what made Android and Web
 * disagree about the same account's most recent sleep:
 *
 *   - Android picks the latest night with `ORDER BY sleepStart DESC`
 *     (SleepDao.getAllSessions) — it ranks by the night itself.
 *   - Canonical selection here used to rank by createdAt. That is write order,
 *     which does not track night order. A night detected late (a backfill, a
 *     re-analysis, a phone that was offline and synced days afterwards) gets a
 *     createdAt newer than a night that actually happened later.
 *
 * The deterministic-id write in routes/sleepMood.js makes this strictly worse
 * rather than better: re-writing `${userId}_${date}_automatic` deliberately
 * PRESERVES the original createdAt, so a corrected duration keeps the rank of
 * the first time that night was ever seen. The record's ordering key therefore
 * stops tracking its content entirely.
 *
 * `sleepEnd` is the honest key — it is the moment the night ended, written by
 * SleepMonitoringEngine from the same session object Room holds, so ordering by
 * it reproduces Android's `sleepStart DESC` ranking for the detected records
 * canonical selection considers. `date` is the fallback for records that
 * somehow lack it; parsed as local midnight to stay consistent with
 * normalizeDateValue's local-date convention elsewhere.
 */
function nightOf(log) {
  if (!log) return 0;
  const end = Number(log.sleepEnd);
  if (Number.isFinite(end) && end > 0) return end;
  const d = new Date(`${log.date}T00:00:00`).getTime();
  return Number.isFinite(d) ? d : 0;
}

/**
 * The most recent time this record was WRITTEN.
 *
 * Not the same as [recencyOf], which reads createdAt first and so reports when
 * a record was first created. That is the wrong key for "which of these two is
 * fresher": the automatic write path preserves createdAt across re-writes, so a
 * corrected record and the original it replaced report an identical recencyOf.
 * Taking the max of both timestamps makes a correction visibly newer.
 */
function lastWriteOf(log) {
  if (!log) return 0;
  const created = new Date(log.createdAt || 0).getTime();
  const updated = new Date(log.updatedAt || 0).getTime();
  return Math.max(Number.isFinite(created) ? created : 0, Number.isFinite(updated) ? updated : 0);
}

/**
 * Newest NIGHT first, breaking ties on last write so that when two records
 * describe the same night — a legacy duplicate and the deterministic-id
 * document, say — the most recently written one wins.
 */
function sortByNightDesc(logs) {
  return [...logs].sort((a, b) => (nightOf(b) - nightOf(a)) || (lastWriteOf(b) - lastWriteOf(a)));
}

/**
 * The most recent NIGHT that actually contains detected sleep.
 * Returns null when Android has not synced one — callers must render an
 * explicit unavailable state rather than substituting a manual log.
 *
 * Ordered by [nightOf], not [recencyOf], so this returns the same night the
 * Android app shows as latest. See [nightOf] for why write time was wrong.
 */
function selectCanonicalSleepLog(logs) {
  if (!Array.isArray(logs)) return null;
  const detected = logs.filter(log => isAutomaticSource(log) && hasValidDetectedSleep(log));
  if (detected.length === 0) return null;
  return sortByNightDesc(detected)[0];
}

module.exports = {
  hasValidDetectedSleep,
  isAutomaticSource,
  recencyOf,
  sortByRecencyDesc,
  lastWriteOf,
  nightOf,
  sortByNightDesc,
  selectCanonicalSleepLog,
};

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
 * The newest record that actually contains detected sleep.
 * Returns null when Android has not synced one — callers must render an
 * explicit unavailable state rather than substituting a manual log.
 */
function selectCanonicalSleepLog(logs) {
  if (!Array.isArray(logs)) return null;
  const detected = logs.filter(log => isAutomaticSource(log) && hasValidDetectedSleep(log));
  if (detected.length === 0) return null;
  return sortByRecencyDesc(detected)[0];
}

module.exports = {
  hasValidDetectedSleep,
  isAutomaticSource,
  recencyOf,
  sortByRecencyDesc,
  selectCanonicalSleepLog,
};

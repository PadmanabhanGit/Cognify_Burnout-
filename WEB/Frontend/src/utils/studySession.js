/**
 * Durable, account-scoped study-session state for the web client.
 *
 * The web had no durable session state at all: the active session lived in a
 * React `useState`, so a refresh, a tab close, or a crash lost the id while the
 * Firestore document stayed `isActive: true` forever — invisible to every total
 * and impossible to close from the UI. This is the browser-side counterpart of
 * StudySessionStore on Android, and it exists for the same reason.
 *
 * ACCOUNT SCOPING
 * Android gets isolation from per-uid SharedPreferences files. localStorage has
 * no such namespacing, so the uid is part of the KEY and is also stamped INSIDE
 * each record. Both are needed: the key keeps two accounts' data apart on a
 * shared browser, and the stamp lets every read re-verify ownership against the
 * live Firebase user — which matters in a SPA, where signing out and back in as
 * someone else never reloads the page.
 *
 * The state machine mirrors StudySessionLifecycle.kt exactly:
 *
 *   ACTIVE -> STOPPING -> STOPPED        (terminal)
 *                  \-> FAILED -> STOPPING -> STOPPED
 *
 * Only a confirmed server response may produce STOPPED.
 */

const ACTIVE_PREFIX = 'study_active__u_';
const STOPS_PREFIX = 'study_pending_stops__u_';

export const StudySessionState = {
  ACTIVE: 'ACTIVE',
  STOPPING: 'STOPPING',
  STOPPED: 'STOPPED',
  FAILED: 'FAILED',
};

/** A blank uid is the signed-out sentinel and owns nothing. */
export function mayStop(ownerUid, authenticatedUid) {
  return Boolean(ownerUid) && Boolean(authenticatedUid) && ownerUid === authenticatedUid;
}

function readJson(key, fallback) {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    // Corrupt or unavailable storage (private mode, quota) must degrade to
    // "nothing stored" rather than throwing inside a render path.
    return fallback;
  }
}

function writeJson(key, value) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* storage unavailable — the session simply is not durable in this browser */
  }
}

function removeKey(key) {
  try {
    window.localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}

// ── active session ─────────────────────────────────────────────────────────

/**
 * The persisted active session for `uid`, or null.
 *
 * Returns null when the record's stamped owner disagrees with `uid`, even though
 * the key already scopes by uid. The stamp is the check that survives a
 * same-tab account switch, where the key changes but stale component state does
 * not.
 */
export function readActiveSession(uid) {
  if (!uid) return null;
  const record = readJson(ACTIVE_PREFIX + uid, null);
  if (!record || !record.sessionId) return null;
  return record.ownerUid === uid ? record : null;
}

export function writeActiveSession(uid, { sessionId, subject, startedAt }) {
  if (!uid || !sessionId) return;
  writeJson(ACTIVE_PREFIX + uid, { sessionId, ownerUid: uid, subject: subject || '', startedAt });
}

/**
 * Releases the active slot.
 *
 * Safe only after a confirmed stop, or after `handOffStop` has taken durable
 * custody. Clearing before the PATCH resolves is the defect this module exists
 * to prevent.
 */
export function clearActiveSession(uid) {
  if (!uid) return;
  removeKey(ACTIVE_PREFIX + uid);
}

// ── pending stops ──────────────────────────────────────────────────────────

export function readPendingStops(uid) {
  if (!uid) return [];
  const queue = readJson(STOPS_PREFIX + uid, []);
  return Array.isArray(queue) ? queue.filter(s => s && s.sessionId && s.ownerUid === uid) : [];
}

function writePendingStops(uid, queue) {
  writeJson(STOPS_PREFIX + uid, queue);
}

/**
 * THE HANDOFF: enqueue the stop, THEN release the active slot.
 *
 * The session id is held durably and continuously — active record, then pending
 * stop — with no window in which neither holds it. A tab closed between the two
 * writes leaves a recoverable active session; closed after them, a retryable
 * pending stop. Nothing is dropped because a request was merely dispatched.
 *
 * Idempotent per session id, so a double click or a refresh mid-stop cannot
 * enqueue the same stop twice.
 */
export function handOffStop(uid, { sessionId, subject, startedAt }) {
  if (!uid || !sessionId) return;
  const queue = readPendingStops(uid);
  if (!queue.some(s => s.sessionId === sessionId)) {
    queue.push({
      sessionId,
      ownerUid: uid,
      subject: subject || '',
      startedAt: startedAt || Date.now(),
      queuedAt: Date.now(),
      state: StudySessionState.STOPPING,
      lastAttemptAt: 0,
      lastError: null,
    });
    writePendingStops(uid, queue);
  }
  clearActiveSession(uid);
}

export function markStopFailed(uid, sessionId, detail) {
  writePendingStops(
    uid,
    readPendingStops(uid).map(s =>
      s.sessionId === sessionId && s.state !== StudySessionState.STOPPED
        ? { ...s, state: StudySessionState.FAILED, lastAttemptAt: Date.now(), lastError: String(detail).slice(0, 300) }
        : s
    )
  );
}

/** Drops a confirmed stop. STOPPED is the only state that may leave the queue. */
export function removeStop(uid, sessionId) {
  writePendingStops(uid, readPendingStops(uid).filter(s => s.sessionId !== sessionId));
}

/**
 * Retry every stop this account owns.
 *
 * `api` is injected rather than imported so this module stays testable and free
 * of a circular dependency with the axios instance.
 *
 * Retrying is safe because PATCH /api/study/stop/:id is now idempotent: an
 * already-stopped session is returned unchanged instead of having its duration
 * re-derived from the moment of the retry.
 */
export async function flushPendingStops(api, uid) {
  const queue = readPendingStops(uid);
  if (!queue.length) return 0;

  let confirmed = 0;
  for (const stop of queue.slice().sort((a, b) => a.queuedAt - b.queuedAt)) {
    if (!mayStop(stop.ownerUid, uid)) continue;
    try {
      const res = await api.patch(`/api/study/stop/${stop.sessionId}`);
      if (res.data && res.data.success) {
        removeStop(uid, stop.sessionId);
        confirmed += 1;
      } else {
        markStopFailed(uid, stop.sessionId, (res.data && res.data.message) || 'stop rejected by server');
      }
    } catch (err) {
      const status = err && err.response && err.response.status;
      if (status === 404 || status === 403) {
        // Terminal, not retryable: the session does not exist, or is not ours.
        // Retrying forever would achieve nothing, and nothing is fabricated to
        // replace it — the entry is dropped and the reason logged.
        console.warn(`[STUDY] stop ${stop.sessionId} rejected (${status}); dropping from queue.`);
        removeStop(uid, stop.sessionId);
      } else {
        markStopFailed(uid, stop.sessionId, err && err.message ? err.message : 'network error');
      }
    }
  }
  return confirmed;
}

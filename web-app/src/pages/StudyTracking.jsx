import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TimerIcon from '@mui/icons-material/Timer';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import api from '../services/api';
import BottomNavigation from '../components/BottomNavigation';
import { auth } from '../firebase';
import {
  readActiveSession,
  writeActiveSession,
  handOffStop,
  removeStop,
  markStopFailed,
  flushPendingStops,
  mayStop,
} from '../utils/studySession';

export default function StudyTracking() {
  const navigate = useNavigate();

  // Whether the web UI itself started an active session this page-load
  const [webIsActive, setWebIsActive] = useState(false);
  // The session object from the server (id, startTime, etc.)
  const [session, setSession] = useState(null);
  // Live elapsed seconds for the web-started session
  const [elapsed, setElapsed] = useState(0);

  const [error, setError] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  // Study stats from backend — all computed server-side from Firestore
  const [stats, setStats] = useState({
    weekMinutes: 0,
    todayMinutes: 0,
    sessionCount: 0,
    dailyBreakdown: {},
    subjectBreakdown: {},
    activeSession: null,
  });

  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  // Derive weekly chart data from dailyBreakdown (minutes per day, Mon–Sun)
  const weeklyData = days.map(d => stats.dailyBreakdown[d] || 0);
  const maxMinutes = Math.max(...weeklyData, 1);

  // --- Live timer for the web-side active session ---
  useEffect(() => {
    let interval = null;
    if (webIsActive) {
      interval = setInterval(() => setElapsed(e => e + 1), 1000);
    } else {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [webIsActive]);

  // --- Recover, flush, then fetch, in that order ---
  //
  // Order matters for the same reason it does on Android: a stop that never
  // landed leaves its Firestore document isActive, and every total below
  // excludes it until the retry succeeds. Fetching first would render a figure
  // we are about to invalidate.
  useEffect(() => {
    (async () => {
      const uid = auth.currentUser?.uid || '';
      if (uid) {
        // Retry anything promised but unconfirmed — a closed tab, a refresh
        // mid-stop, or a dropped connection on a previous visit.
        await flushPendingStops(api, uid);

        // Adopt a session this account left running. Without this the id was
        // lost on refresh and the session could never be closed from the web.
        const saved = readActiveSession(uid);
        if (saved) {
          setSession({ id: saved.sessionId, startTime: saved.startedAt, subject: saved.subject });
          setWebIsActive(true);
          setElapsed(Math.max(0, Math.floor((Date.now() - saved.startedAt) / 1000)));
        }
      }
      await fetchStats();
    })();
  }, []);

  const fetchStats = async () => {
    try {
      const res = await api.get('/api/study/stats/weekly');
      if (res.data.success) {
        const s = res.data.stats;
        const newStats = {
          weekMinutes: s.weekMinutes ?? s.totalMinutes ?? 0,
          todayMinutes: s.todayMinutes ?? 0,
          sessionCount: s.sessionCount ?? 0,
          dailyBreakdown: s.dailyBreakdown || s.dailyTotals || {},
          subjectBreakdown: s.subjectBreakdown || {},
          activeSession: s.activeSession || null,
        };
        setStats(newStats);

        // [STUDY WEB TIMER DEBUG]
        // webIsActive: only true if THIS browser tab called handleStart()
        // webOwnsSession: same — session state from this component instance
        // Android-owned active sessions must NOT start a web timer.
        // The timer useEffect depends only on webIsActive (set only in handleStart).
        console.log('[STUDY WEB TIMER DEBUG]', {
          activeSessionFromBackend: s.activeSession
            ? { id: s.activeSession.id, startTime: s.activeSession.startTime }
            : null,
          webOwnsSession: false, // fetchStats never sets webIsActive — only handleStart() does
          timerStarted: false,   // timer only starts if webIsActive was set by handleStart
          backendTodayMinutes: newStats.todayMinutes,
          backendWeekMinutes: newStats.weekMinutes,
          localElapsedWillAdd: 0, // elapsed is only added if webIsActive — see todaysDisplay
          note: 'If Android owns the active session: webOwnsSession=false, timer=false, todayMinutes=backend snapshot',
        });

        // NOTE: We intentionally do NOT set webIsActive here.
        // Even if the backend reports an active session (Android-owned),
        // the web must not start a local elapsed timer for it.
        setLastSyncedAt(Date.now());
        setError(false);
      } else {
        setError(true);
      }
    } catch (err) {
      console.error(err);
      setError(true);
    }
  };

  const handleStart = async () => {
    try {
      const uid = auth.currentUser?.uid || '';
      if (!uid) return;

      // Refuse to open a second concurrent session. Previously the button read
      // "Start Session" whenever the web itself had not started one — including
      // while Android had a session running — so pressing it created a second
      // active document and both were counted.
      if (stats.activeSession || readActiveSession(uid)) {
        alert('A study session is already running on this account. Stop it before starting another.');
        return;
      }

      const subject = prompt('What are you working on?', 'e.g. Mathematics, Project Research');
      if (!subject) return;
      const res = await api.post('/api/study/start', { subject, notes: '' });
      if (res.data.success && res.data.session?.id) {
        const startedAt = Date.now();
        // Durable and owner-stamped at the one moment ownership is unambiguous,
        // so a refresh can recover it and a later stop can verify it.
        writeActiveSession(uid, { sessionId: res.data.session.id, subject, startedAt });
        setSession(res.data.session);
        setWebIsActive(true);
        setElapsed(0);
        // [STUDY WEB TIMER DEBUG] — Web now owns this session
        console.log('[STUDY WEB TIMER DEBUG]', {
          webOwnsSession: true,
          timerStarted: true,
          sessionId: res.data.session?.id,
          note: 'Web started this session — local timer is permitted',
        });
        // Refresh stats to pick up the new active session from backend
        await fetchStats();
      }
    } catch (err) {
      console.error(err);
    }
  };


  const handleStop = async () => {
    if (!session) return;
    const uid = auth.currentUser?.uid || '';
    const saved = readActiveSession(uid);
    const ownerUid = saved?.ownerUid || uid;

    // The account that started this session must be the one signed in now.
    // In a SPA, signing out and back in as someone else never reloads the page,
    // so stale component state can otherwise outlive the account that created it.
    if (!mayStop(ownerUid, uid)) {
      console.warn(`[STUDY] stop refused: owner=${ownerUid} authenticated=${uid}`);
      return;
    }

    // Durable custody BEFORE the UI releases anything, so a failed or
    // interrupted request is retried on the next visit instead of stranding the
    // Firestore document as isActive forever.
    handOffStop(uid, {
      sessionId: session.id,
      subject: saved?.subject || session.subject,
      startedAt: saved?.startedAt || Date.now(),
    });
    setWebIsActive(false);
    setSession(null);
    setElapsed(0);

    try {
      const res = await api.patch(`/api/study/stop/${session.id}`);
      // Only a confirmed response clears the record. `success` is checked
      // explicitly rather than inferred from "did not throw".
      if (res.data && res.data.success) {
        removeStop(uid, session.id);
      } else {
        markStopFailed(uid, session.id, (res.data && res.data.message) || 'stop rejected by server');
      }
    } catch (err) {
      console.error(err);
      const status = err?.response?.status;
      if (status === 404 || status === 403) {
        // Terminal: unknown session, or not ours. Not retryable, and no
        // duration is invented to stand in for it.
        removeStop(uid, session.id);
      } else {
        markStopFailed(uid, session.id, err?.message || 'network error');
      }
    }
    await fetchStats();
  };

  // Format HH:MM:SS for the live timer display
  const formatTime = (secs) => {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return h > 0 ? `${h}:${m}:${s}` : `${m}:${s}`;
  };

  // Format a duration in minutes + extra seconds as a human-readable string
  const getFormattedDuration = (baseMins, addedSecs = 0) => {
    const totalSecs = (baseMins || 0) * 60 + (addedSecs || 0);
    if (!totalSecs) return '0s';
    if (totalSecs < 3600) {
      const m = Math.floor(totalSecs / 60);
      return `${m}m`;
    }
    const h = (totalSecs / 3600).toFixed(1);
    return `${h}H`;
  };

  // Today's display:
  //   - Backend todayMinutes already includes active session elapsed (server computed it)
  //   - If WEB started this session, add local elapsed since todayMinutes was last fetched
  //   - If ANDROID started the session (stats.activeSession but webIsActive=false), the
  //     backend todayMinutes already has its elapsed baked in — do not double-count
  const webElapsedToAdd = webIsActive ? elapsed : 0;
  const todaysDisplay = error ? '--' : getFormattedDuration(stats.todayMinutes, webElapsedToAdd);

  // This Week:
  //   - Backend weekMinutes = sum of completed sessions this Mon–Sun (active not included)
  //   - If WEB started this session add elapsed; Android sessions: already completed,
  //     their duration is in weekMinutes
  const weeklyDisplay = error ? '--' : getFormattedDuration(stats.weekMinutes, webElapsedToAdd);

  // Determine whether to show "IN PROGRESS" status
  // True if web has an active session OR if Android has an active session (from backend)
  const isAnySessionActive = webIsActive || Boolean(stats.activeSession);

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: 'var(--bg-primary)' }}>
      {/* Header Section */}
      <div style={{
        background: 'linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%)',
        borderBottomLeftRadius: '32px',
        borderBottomRightRadius: '32px',
        width: '100%',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{ position: 'absolute', top: '-20%', right: '-10%', width: '200px', height: '200px', background: 'rgba(255,255,255,0.1)', borderRadius: '50%', filter: 'blur(30px)' }}></div>
        <div className="desktop-padding" style={{ padding: '50px 24px 60px 24px', height: '200px', position: 'relative', zIndex: 1 }}>
          <div style={{ cursor: 'pointer', display: 'inline-flex', padding: '8px', background: 'rgba(255,255,255,0.2)', borderRadius: '50%', backdropFilter: 'blur(10px)' }} onClick={() => navigate('/dashboard')}>
            <ArrowBackIcon style={{ color: 'white' }} />
          </div>
          <div style={{ marginTop: '24px' }}>
            <div style={{ color: 'white', fontSize: '32px', fontWeight: 800, letterSpacing: '-0.5px' }}>Study Tracking</div>
            <div style={{ color: 'rgba(255,255,255,0.85)', fontSize: '15px', fontWeight: 500, marginTop: '4px' }}>Monitor your focus and analytics</div>
          </div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-35px', display: 'flex', flexDirection: 'column', gap: '24px' }}>

        {/* Current Session Card */}
        <div className="glass-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{ background: 'rgba(99, 102, 241, 0.1)', padding: '8px', borderRadius: '12px', display: 'flex', marginRight: '12px' }}>
                <TimerIcon style={{ color: '#6366F1', fontSize: '20px' }} />
              </div>
              <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)' }}>Current Session</div>
            </div>
            <div style={{
              backgroundColor: isAnySessionActive ? 'rgba(234, 179, 8, 0.15)' : 'rgba(34, 197, 94, 0.15)',
              padding: '6px 12px',
              borderRadius: '20px',
              fontSize: '11px',
              fontWeight: 800,
              color: isAnySessionActive ? '#EAB308' : '#22C55E',
              letterSpacing: '0.5px'
            }}>
              {isAnySessionActive ? 'IN PROGRESS' : 'READY'}
            </div>
          </div>

          {/* Live timer — only shown when WEB started the session */}
          <div style={{ marginTop: '36px', marginBottom: '36px', fontSize: '72px', fontWeight: 800, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums', textShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
            {webIsActive
              ? formatTime(elapsed)
              : (stats.activeSession
                ? <span style={{ fontSize: '20px', color: 'var(--text-secondary)', fontWeight: 600 }}>📱 Session active on Android</span>
                : formatTime(0)
              )
            }
          </div>

          <button
            onClick={webIsActive ? handleStop : handleStart}
            style={{
              width: '80%', height: '60px', borderRadius: '30px',
              background: webIsActive ? 'linear-gradient(135deg, #EF4444, #F43F5E)' : 'linear-gradient(135deg, #4F46E5, #7C3AED)',
              color: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', border: 'none', cursor: 'pointer',
              boxShadow: webIsActive ? '0 8px 25px -5px rgba(239, 68, 68, 0.5)' : '0 8px 25px -5px rgba(99, 102, 241, 0.5)',
              transition: 'all 0.3s ease',
              transform: webIsActive ? 'scale(1.02)' : 'scale(1)'
            }}
            onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
            onMouseOut={(e) => e.currentTarget.style.transform = webIsActive ? 'scale(1.02)' : 'scale(1)'}
          >
            {webIsActive ? <StopIcon style={{ marginRight: '8px' }} /> : <PlayArrowIcon style={{ marginRight: '8px' }} />}
            <span style={{ fontSize: '18px', fontWeight: 700, letterSpacing: '0.5px' }}>{webIsActive ? 'End Session' : 'Start Session'}</span>
          </button>

          <div style={{ display: 'flex', width: '100%', gap: '16px', marginTop: '32px' }}>
            <div style={{ flex: 1, backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border-color)', borderRadius: '20px', padding: '20px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 4px 15px var(--accent-glow)' }}>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Today's Total</div>
              <div style={{ fontSize: '24px', fontWeight: 800, color: '#3B82F6', marginTop: '8px' }}>{todaysDisplay}</div>
            </div>
            <div style={{ flex: 1, backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border-color)', borderRadius: '20px', padding: '20px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 4px 15px var(--accent-glow)' }}>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>This Week</div>
              <div style={{ fontSize: '24px', fontWeight: 800, color: '#8B5CF6', marginTop: '8px' }}>{weeklyDisplay}</div>
            </div>
          </div>
        </div>

        {/* Weekly Overview Card — shows Mon–Sun of current week only */}
        <div className="glass-card" style={{ padding: '24px', marginBottom: '40px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '8px', borderRadius: '12px', display: 'flex', marginRight: '12px' }}>
                <CalendarMonthIcon style={{ color: '#8B5CF6', fontSize: '20px' }} />
              </div>
              <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Weekly Overview</div>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>This week (Mon–Sun)</div>
          </div>

          {/* Bar Chart — each bar = actual minutes from Firestore for that day */}
          <div style={{ display: 'flex', height: '180px', justifyContent: 'space-between', alignItems: 'flex-end', padding: '0 4px', gap: '12px' }}>
            {weeklyData.map((val, idx) => {
              const heightPercentage = val > 0 ? Math.max((val / maxMinutes) * 100, 8) : 4;
              const isToday = (() => {
                // Must match backend IST (UTC+05:30) day index: 0=Mon … 6=Sun
                const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;
                const istDate = new Date(Date.now() + IST_OFFSET_MS);
                const utcDay = istDate.getUTCDay(); // 0=Sun … 6=Sat in IST wall-clock
                const mondayBased = utcDay === 0 ? 6 : utcDay - 1; // 0=Mon … 6=Sun
                return idx === mondayBased;
              })();
              return (
                <div key={idx} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', height: '100%' }}>
                  <div style={{
                    width: '100%',
                    height: `${heightPercentage}%`,
                    background: isToday
                      ? 'linear-gradient(to top, #F59E0B, #FBBF24)'
                      : (val > 0 ? 'linear-gradient(to top, #6366F1, #8B5CF6)' : 'var(--border-color)'),
                    borderRadius: '8px',
                    boxShadow: val > 0 ? '0 4px 10px rgba(99, 102, 241, 0.3)' : 'none',
                    transition: 'height 0.5s cubic-bezier(0.4, 0, 0.2, 1)',
                    opacity: val > 0 ? 1 : 0.3,
                  }}></div>
                  {val > 0 && (
                    <div style={{ fontSize: '9px', color: 'var(--text-secondary)', marginTop: '4px', fontWeight: 600 }}>
                      {val >= 60 ? `${(val / 60).toFixed(1)}H` : `${val}m`}
                    </div>
                  )}
                  <div style={{ fontSize: '11px', fontWeight: isToday ? 700 : 600, color: isToday ? '#F59E0B' : 'var(--text-secondary)', marginTop: '4px' }}>{days[idx]}</div>
                </div>
              );
            })}
          </div>

          {/* Subject Breakdown */}
          {Object.keys(stats.subjectBreakdown).length > 0 && (
            <div style={{ marginTop: '24px', borderTop: '1px solid var(--border-color)', paddingTop: '16px' }}>
              <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>Study Breakdown</div>
              {Object.entries(stats.subjectBreakdown)
                .sort((a, b) => b[1] - a[1])
                .map(([subject, mins]) => {
                  const totalMins = Object.values(stats.subjectBreakdown).reduce((s, v) => s + v, 0) || 1;
                  const pct = Math.round((mins / totalMins) * 100);
                  const displayDur = mins >= 60
                    ? `${(mins / 60).toFixed(1)}H`
                    : `${mins}m`;
                  return (
                    <div key={subject} style={{ marginBottom: '12px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>{subject || 'Uncategorized'}</span>
                        <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{displayDur} ({pct}%)</span>
                      </div>
                      <div style={{ height: '6px', backgroundColor: 'var(--border-color)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${pct}%`, background: 'linear-gradient(90deg, #6366F1, #8B5CF6)', borderRadius: '3px', transition: 'width 0.5s ease' }} />
                      </div>
                    </div>
                  );
                })}
            </div>
          )}
        </div>

        <div style={{ textAlign: 'center', marginTop: '8px', marginBottom: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
          {error ? '⚠️ Unable to sync data. Retry.' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
        </div>

      </div>

      <BottomNavigation activeTab="tracker" />
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AddIcon from '@mui/icons-material/Add';
import BedtimeIcon from '@mui/icons-material/Bedtime';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import WarningIcon from '@mui/icons-material/Warning';
import NightlightIcon from '@mui/icons-material/Nightlight';
import WbSunnyIcon from '@mui/icons-material/WbSunny';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import api from '../services/api';

export default function SleepMoodDashboard() {
  const navigate = useNavigate();
  const [canonical, setCanonical] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);
  const [logs, setLogs] = useState([]);

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const res = await api.get('/api/sleep-mood/logs');
        if (res.data.success) {
          // `canonical` is the newest record containing Android's detected
          // sleepStart/sleepEnd. It is null when no detected session has synced —
          // in that case every value below stays null and the page says so.
          setCanonical(res.data.canonical ?? null);
          // `logs` was previously fetched and discarded, so a manually entered
          // night reached Firestore and came back in this very response but was
          // never rendered anywhere — it looked like the sync had failed when it
          // had not. Kept so manual entries are visible as the user's own
          // records, clearly distinguished from detected ones.
          setLogs(Array.isArray(res.data.logs) ? res.data.logs : []);
          setLastSyncedAt(Date.now());
          setError(false);
        } else {
          setError(true);
        }
      } catch (err) {
        console.error("Failed to load sleep logs", err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchLogs();
  }, []);

  // Sleep History / Average Quality / Quality Trend deliberately do NOT live on
  // this page. /sleep/analytics is the single canonical implementation.
  //
  // The duplicate here also produced a contradiction: it sliced the trend
  // endpoint's response to the last 3 nights before averaging, while
  // /sleep/analytics averages every night the endpoint returns. With four stored
  // nights that is (0+75+75)/3 = 50% here versus (0+0+75+75)/4 = 37.5 -> 38%
  // there — two different numbers for "average quality" from one dataset.
  // Removing this section, rather than duplicating the calculation correctly,
  // means there is only one average to keep right.
  //
  // The "View Sleep History" button below is retained as the route into it.

  const latestSession = error ? null : canonical;
  const available = latestSession !== null;

  const toNumber = (v) => {
    const n = Number(v);
    return Number.isFinite(n) ? n : null;
  };

  const displayQuality = available ? toNumber(latestSession.sleepQuality) : null;
  const displayDisturbance = available ? toNumber(latestSession.disturbanceScore) : null;
  const awakeningCount = available ? toNumber(latestSession.awakeningCount) : null;
  const sleepDurationHours = available ? toNumber(latestSession.sleepDuration) : null;

  // ── Derived session details — mirrors Android SleepMoodDashboardScreen ──────
  // Android computes these from the same two persisted timestamps:
  //   timeInBed = sleepEnd - sleepStart
  //   awake     = timeInBed - countedAsSleep
  // No independent sleep calculation happens here: the browser never inspects
  // its own clock, and sleepStart/sleepEnd/sleepDuration all come straight from
  // the record Android produced. Nulls stay null so older records that predate a
  // field degrade to "--" instead of rendering a fabricated 0.
  const sleepStartMs = available ? toNumber(latestSession.sleepStart) : null;
  const sleepEndMs = available ? toNumber(latestSession.sleepEnd) : null;

  const timeInBedMinutes =
    sleepStartMs !== null && sleepEndMs !== null && sleepEndMs > sleepStartMs
      ? Math.round((sleepEndMs - sleepStartMs) / 60000)
      : null;
  const countedAsSleepMinutes =
    sleepDurationHours !== null ? Math.round(sleepDurationHours * 60) : null;
  const awakeMinutes =
    timeInBedMinutes !== null && countedAsSleepMinutes !== null
      ? Math.max(0, timeInBedMinutes - countedAsSleepMinutes)
      : null;

  // "2026-08-11" -> "Aug 11, 2026". Parsed from parts, never `new Date(iso)`,
  // which treats a bare date as UTC midnight and can render the previous day.
  const sessionDateLabel = (isoDate) => {
    const parts = String(isoDate || '').split('-');
    if (parts.length !== 3) return isoDate || '--';
    const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    const mi = Number(parts[1]) - 1;
    if (!(mi >= 0 && mi < 12)) return isoDate;
    return `${months[mi]} ${Number(parts[2]) || parts[2]}, ${parts[0]}`;
  };

  // Records that describe a NIGHT, in the order the API returned them (newest
  // first). Mood-only logs are excluded: they carry no sleepDuration, so listing
  // them under sleep would show "--" rows that mean "this is not a sleep record"
  // rather than "this value is missing".
  //
  // `isManual` mirrors the server's own rule in sleepSelection.isAutomaticSource:
  // an explicit `source: "manual"` is manual; anything else — including legacy
  // records written before the field existed — is treated as detected. Computed
  // here rather than assumed from the absence of sleepStart, so the label always
  // agrees with what the backend used for canonical selection.
  const sleepEntries = (Array.isArray(logs) ? logs : [])
    .filter(l => l && toNumber(l.sleepDuration) !== null)
    .map(l => ({ ...l, isManual: l.source === 'manual' }));

  // Local calendar date, built from parts rather than toISOString(), which is UTC
  // and would name the previous day for anyone east of UTC.
  const todayIso = (() => {
    const d = new Date();
    const p = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  })();

  // Today's MANUAL entry, and whether the canonical (detected) record is even
  // from today.
  //
  // These matter because canonical is by definition the most recent DETECTED
  // night, which can be days old. On a night the phone detected nothing and the
  // user logged their sleep by hand, every panel below still described the older
  // detected night — complete with "Detected sleep start" and "Detected wake
  // time" — so the screen confidently presented a stale night as the current one
  // and showed detection fields for a record that has none.
  const todayManualEntry = sleepEntries.find(e => e.date === todayIso && e.isManual) || null;
  const canonicalIsToday = available && latestSession.date === todayIso;
  const showManualToday = todayManualEntry !== null && !canonicalIsToday;

  const formatMinutes = (mins) => {
    if (mins === null) return '--';
    const h = Math.floor(mins / 60);
    return h > 0 ? `${h}h ${mins % 60}m` : `${mins}m`;
  };

  const getQualityColor = (score) => {
    if (score === null) return '#9CA3AF';
    if (score >= 75) return '#10B981';
    if (score >= 60) return '#F59E0B';
    return '#EF4444';
  };

  const getQualityLevel = (score) => {
    if (score >= 90) return 'Excellent';
    if (score >= 75) return 'Good';
    if (score >= 60) return 'Moderate';
    if (score >= 40) return 'Poor';
    return 'Very Poor';
  };

  // Epoch millis → wall-clock time. Absolute instant, so this matches the Android
  // clock for a viewer in the same timezone. Never derived from the `date` field:
  // `new Date("2026-08-10")` parses as UTC midnight and rendered 05:30 AM in IST.
  const formatTimeMillis = (millis) => {
    const n = Number(millis);
    if (!Number.isFinite(n) || n <= 0) return '--:--';
    return new Date(n).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  // Hours (float) → "11h 0m", matching Android's formatMinutes output. Avoids
  // rendering raw floats such as 1.8999999767158142h.
  const formatDurationHours = (hours) => {
    if (hours === null) return '--';
    const totalMinutes = Math.max(0, Math.round(hours * 60));
    return `${Math.floor(totalMinutes / 60)}h ${totalMinutes % 60}m`;
  };

  const sleepStartDisplay = available ? formatTimeMillis(latestSession.sleepStart) : '--:--';
  const sleepEndDisplay = available ? formatTimeMillis(latestSession.sleepEnd) : '--:--';

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      {/* Header Section */}
      <div style={{ 
        background: 'linear-gradient(to bottom, #4F46E5, #9333EA)', 
        borderBottomLeftRadius: '32px', 
        borderBottomRightRadius: '32px',
        padding: '40px 24px 60px 24px',
        height: '200px'
      }}>
        <div style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          <ArrowBackIcon style={{ color: 'white' }} />
        </div>
        <div style={{ marginTop: '24px' }}>
          <div style={{ color: 'white', fontSize: '28px', fontWeight: 700 }}>Sleep Analysis</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px' }}>Detailed breakdown of your automatically detected sleep</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Sleep Quality Score Card */}
        <div className="white-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          
          <div style={{ position: 'relative', width: '120px', height: '120px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <svg viewBox="0 0 36 36" style={{ width: '120px', height: '120px' }}>
              <path
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                fill="none" stroke="#F3F4F6" strokeWidth="3"
              />
              {/* Progress arc is drawn ONLY when a real quality value exists.
                  No `?? 0` fallback: an unavailable night must render as no arc
                  at all, never as a 0% reading that looks like a measurement. */}
              {displayQuality !== null && (
                <path
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  fill="none" stroke={getQualityColor(displayQuality)} strokeWidth="3"
                  strokeDasharray={`${displayQuality}, 100`}
                  strokeLinecap="round"
                />
              )}
            </svg>
            <div style={{ position: 'absolute', textAlign: 'center' }}>
              <div style={{ fontSize: '28px', fontWeight: 800, color: '#1F2937' }}>
                {displayQuality === null ? '--' : `${displayQuality}%`}
              </div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: getQualityColor(displayQuality) }}>
                {displayQuality === null ? 'Unavailable' : getQualityLevel(displayQuality)}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', width: '100%', marginTop: '24px', gap: '12px' }}>
            <MetricCard label="Total Sleep" value={formatDurationHours(sleepDurationHours)} icon={<BedtimeIcon />} color="#6366F1" />
            <MetricCard label="Awakenings" value={awakeningCount === null ? '--' : awakeningCount} icon={<NotificationsActiveIcon />} color="#F59E0B" />
            <MetricCard label="Disturbance" value={displayDisturbance === null ? '--' : displayDisturbance} icon={<WarningIcon />} color="#EF4444" />
          </div>
        </div>

        {/* Sleep Start & Wake Times — straight from Android's persisted epoch millis */}
        <div className="white-card" style={{ padding: '20px', display: 'flex', justifyContent: 'space-evenly', alignItems: 'center' }}>
          <TimeInfo label="Sleep Start" time={sleepStartDisplay} icon={<NightlightIcon />} />
          <div style={{ width: '1px', height: '40px', backgroundColor: '#F3F4F6' }}></div>
          <TimeInfo label="Wake Up" time={sleepEndDisplay} icon={<WbSunnyIcon />} />
        </div>

        {/* ── Today's manually recorded sleep ──────────────────────────────────
            Shown ABOVE Session Details, and only when today has a manual entry
            that no detected session supersedes. Manual records carry a duration
            but no sleepStart/sleepEnd, so none of the detection rows below apply
            to them — presenting them there would invent precision the record
            does not have. */}
        {showManualToday && (
          <>
            <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>Today</div>
            <div className="white-card" style={{ padding: '20px', borderLeft: '4px solid #F59E0B' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontSize: '26px', fontWeight: 800, color: '#1F2937' }}>
                    {formatDurationHours(toNumber(todayManualEntry.sleepDuration))}
                  </div>
                  <div style={{ fontSize: '13px', color: '#6B7280', marginTop: '2px' }}>
                    Manually recorded for {sessionDateLabel(todayManualEntry.date)}
                  </div>
                </div>
                <span style={{
                  fontSize: '10px', fontWeight: 800, letterSpacing: '0.4px',
                  padding: '4px 10px', borderRadius: '10px',
                  backgroundColor: '#FEF3C7', color: '#B45309'
                }}>
                  MANUAL
                </span>
              </div>
              <div style={{ fontSize: '12px', color: '#6B7280', marginTop: '12px', lineHeight: 1.5 }}>
                No sleep was automatically detected for this night, so start and wake
                times, awakenings and disturbance are not available for it.
              </div>
            </div>
          </>
        )}

        {/* ── Session Details — mirrors Android's compact detail rows ────────── */}
        <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>
          {showManualToday ? 'Last Detected Session' : 'Session Details'}
        </div>
        {showManualToday && (
          <div style={{ fontSize: '12px', color: '#6B7280', marginTop: '-8px' }}>
            Automatically detected on an earlier night — not today's entry.
          </div>
        )}
        <div className="white-card" style={{ padding: '6px 0' }}>
          {available ? (
            <>
              <DetailRow label="Night of" value={sessionDateLabel(latestSession.date)} />
              <DetailRow label="Detected sleep start" value={sleepStartDisplay} />
              <DetailRow label="Detected wake time" value={sleepEndDisplay} />
              <DetailRow label="Time in bed" value={formatMinutes(timeInBedMinutes)} />
              <DetailRow label="Counted as sleep" value={formatMinutes(countedAsSleepMinutes)} />
              <DetailRow label="Awake during session" value={formatMinutes(awakeMinutes)} />
              <DetailRow
                label="Awakenings detected"
                value={awakeningCount === null ? '--' : String(awakeningCount)}
                isLast
              />
            </>
          ) : (
            <div style={{ padding: '18px', color: '#6B7280', fontSize: '14px' }}>
              No detected sleep session yet.
            </div>
          )}
        </div>

        {/* ── Recent sleep entries — detected AND manual ──────────────────────
            Everything above describes the CANONICAL record, which by design is
            the most recent automatically DETECTED night: selectCanonicalSleepLog
            excludes `source: "manual"` so a hand-entered figure can never be
            presented as something the phone measured.

            That rule is correct and is left untouched — but it meant a manual
            entry was invisible on the web entirely, which read as a broken sync.
            Manual records are the user's own data and belong in their history;
            they are listed here, labelled, so the distinction stays explicit
            rather than being enforced by hiding them. */}
        {sleepEntries.length > 0 && (
          <>
            <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>Recent Entries</div>
            <div className="white-card" style={{ padding: '6px 0' }}>
              {sleepEntries.map((entry, i) => (
                <div
                  key={entry.id || i}
                  style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '12px 18px',
                    borderBottom: i === sleepEntries.length - 1 ? 'none' : '1px solid #F3F4F6'
                  }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span style={{ fontSize: '14px', fontWeight: 600, color: '#1F2937' }}>
                      {sessionDateLabel(entry.date)}
                    </span>
                    <span style={{ fontSize: '12px', color: '#6B7280' }}>
                      {entry.mood ? `Mood: ${entry.mood}` : 'No mood recorded'}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span style={{ fontSize: '15px', fontWeight: 700, color: '#1F2937' }}>
                      {formatDurationHours(toNumber(entry.sleepDuration))}
                    </span>
                    <span
                      style={{
                        fontSize: '10px', fontWeight: 800, letterSpacing: '0.4px',
                        padding: '3px 8px', borderRadius: '10px',
                        backgroundColor: entry.isManual ? '#FEF3C7' : '#DBEAFE',
                        color: entry.isManual ? '#B45309' : '#1D4ED8'
                      }}
                    >
                      {entry.isManual ? 'MANUAL' : 'DETECTED'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {/* Timeline Section */}
        <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>Sleep Timeline</div>

        <div className="white-card" style={{ padding: '24px' }}>
          {available ? (
            <>
              {/* "Monitoring Started" row removed entirely, matching Android's
                  SleepMoodDashboardScreen. No monitoring-window start timestamp
                  is persisted anywhere in the data model, so the row had nothing
                  real to represent — it was a hardcoded "10:00 PM", then an empty
                  "--:--" placeholder. The timeline now contains only real events. */}
              <TimelineItem
                time={sleepStartDisplay}
                title="Sleep Started"
                subtitle="Detected from a sustained inactivity gap"
                icon={<BedtimeIcon />}
                color="#4F46E5"
              />
              {/* Individual WakeEvent rows are NOT synced to Firestore — the
                  Android engine POSTs only the session summary, of which
                  awakeningCount is the one awakening field that crosses the
                  wire. So the count is stated honestly rather than inventing
                  per-event rows. When the count is 0 (as tonight) the timeline
                  is complete and this says so explicitly. */}
              {awakeningCount !== null && awakeningCount > 0 ? (
                <TimelineItem
                  time="--"
                  title={`${awakeningCount} awakening${awakeningCount === 1 ? '' : 's'} detected`}
                  subtitle="Per-event times and apps are recorded on the device; only the count is synced."
                  icon={<NotificationsActiveIcon />}
                  color="#F59E0B"
                />
              ) : awakeningCount === 0 ? (
                <div style={{ padding: '0 0 20px 48px', fontSize: '13px', color: '#6B7280' }}>
                  No detected awakenings during this session.
                </div>
              ) : null}
              <TimelineItem
                time={sleepEndDisplay}
                title="Final Wake Up"
                subtitle="Detected from a sustained activity cluster"
                icon={<WbSunnyIcon />}
                color="#10B981"
                isLast={true}
              />
            </>
          ) : (
            <div style={{ color: 'gray', fontSize: '14px' }}>
              {error
                ? 'Unable to load sleep data. Retry.'
                : 'No detected sleep session yet — open the Android app to sync your latest night.'}
            </div>
          )}
        </div>

        {/* ── Sleep Quality — same stored value, no web-side recalculation ──── */}
        <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>Sleep Quality</div>
        <div className="white-card" style={{ padding: '20px' }}>
          {displayQuality === null ? (
            <div style={{ color: '#6B7280', fontSize: '14px' }}>Sleep quality unavailable for this record.</div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <div style={{ fontSize: '24px', fontWeight: 700, color: getQualityColor(displayQuality) }}>{displayQuality}%</div>
                <div style={{ fontSize: '13px', fontWeight: 700, color: getQualityColor(displayQuality) }}>{getQualityLevel(displayQuality)}</div>
              </div>
              <div style={{ height: '8px', borderRadius: '4px', backgroundColor: '#F3F4F6', marginTop: '10px', overflow: 'hidden' }}>
                <div style={{ width: `${Math.max(0, Math.min(100, displayQuality))}%`, height: '100%', backgroundColor: getQualityColor(displayQuality), borderRadius: '4px' }} />
              </div>
              <div style={{ fontSize: '12px', color: '#6B7280', marginTop: '12px', lineHeight: 1.4 }}>
                Measured by the Android sleep monitor for this session. The engine does not persist a
                per-factor breakdown of this score, so it is shown as a single measured result.
              </div>
            </>
          )}
        </div>

        {/* ── Disturbance ───────────────────────────────────────────────────── */}
        <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>Disturbance</div>
        <div className="white-card" style={{ padding: '20px' }}>
          {displayDisturbance === null ? (
            <div style={{ color: '#6B7280', fontSize: '14px' }}>Disturbance score unavailable for this record.</div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontSize: '14px', fontWeight: 500, color: '#1F2937' }}>Disturbance score</div>
                <div style={{ fontSize: '22px', fontWeight: 700, color: '#EF4444' }}>{displayDisturbance}</div>
              </div>
              <div style={{ height: '1px', backgroundColor: '#F3F4F6', margin: '16px 0' }} />
              <div style={{ fontSize: '12px', color: '#6B7280', lineHeight: 1.4 }}>
                {/* Android can show a per-category breakdown because it reads the
                    local app_usage_logs table. Those rows are not synced, so the
                    web states the limitation instead of guessing at factors. */}
                Per-app activity behind this score is recorded on the device and is not synced,
                so detailed factor attribution is unavailable here. Open Sleep Analysis in the
                Android app for the per-category breakdown.
              </div>
            </>
          )}
        </div>

        {/* Sleep History / Average Quality / Quality Trend removed from this page.
            /sleep/analytics is the single canonical implementation; this button
            is the route to it. */}

        <button
          onClick={() => navigate('/sleep/analytics')}
          style={{ width: '100%', height: '56px', backgroundColor: '#4F46E5', color: 'white', borderRadius: '16px', fontWeight: 700, display: 'flex', justifyContent: 'center', alignItems: 'center', border: 'none', cursor: 'pointer' }}
        >
          View Sleep History
          <ArrowForwardIcon style={{ marginLeft: '8px' }} />
        </button>


        <div style={{ textAlign: 'center', marginTop: '8px', marginBottom: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
          {error ? '⚠️ Unable to sync data. Retry.' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
        </div>

      </div>
      
      <button 
        onClick={() => navigate('/sleep/log')} 
        style={{ position: 'fixed', bottom: '80px', right: '24px', width: '56px', height: '56px', borderRadius: '28px', backgroundColor: '#6366F1', display: 'flex', justifyContent: 'center', alignItems: 'center', boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)', border: 'none', cursor: 'pointer' }}
      >
        <AddIcon style={{ color: 'white' }} />
      </button>
    </div>
  );
}

/** One compact label/value row, matching Android's Session Details rows. */
function DetailRow({ label, value, isLast = false }) {
  return (
    <div>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        padding: '13px 18px', gap: '12px'
      }}>
        <div style={{ fontSize: '13px', color: '#6B7280' }}>{label}</div>
        <div style={{ fontSize: '14px', fontWeight: 700, color: '#1F2937', textAlign: 'right' }}>{value}</div>
      </div>
      {!isLast && <div style={{ height: '1px', backgroundColor: '#F3F4F6', margin: '0 18px' }} />}
    </div>
  );
}


function MetricCard({ label, value, icon, color }) {
  return (
    <div style={{ flex: 1, backgroundColor: `${color}1A`, borderRadius: '16px', padding: '12px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ color, marginBottom: '8px' }}>{icon}</div>
      <div style={{ fontSize: '18px', fontWeight: 800, color: '#1F2937' }}>{value}</div>
      <div style={{ fontSize: '10px', color: '#6B7280', fontWeight: 600, textAlign: 'center' }}>{label}</div>
    </div>
  );
}

function TimeInfo({ label, time, icon }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '4px' }}>
        <div style={{ color: '#6366F1', marginRight: '4px', display: 'flex', alignItems: 'center', '& svg': { fontSize: '16px' } }}>{icon}</div>
        <div style={{ fontSize: '12px', color: 'gray' }}>{label}</div>
      </div>
      <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937' }}>{time}</div>
    </div>
  );
}

function TimelineItem({ time, title, subtitle, icon, color, isLast = false }) {
  return (
    <div style={{ display: 'flex', marginBottom: isLast ? '0' : '20px' }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginRight: '16px' }}>
        <div style={{ width: '32px', height: '32px', borderRadius: '16px', backgroundColor: `${color}1A`, display: 'flex', justifyContent: 'center', alignItems: 'center', color }}>
          {React.cloneElement(icon, { style: { fontSize: '16px' } })}
        </div>
        {!isLast && <div style={{ width: '2px', flex: 1, backgroundColor: '#F3F4F6', marginTop: '4px', minHeight: '30px' }}></div>}
      </div>
      <div>
        <div style={{ fontSize: '12px', color: '#6B7280', fontWeight: 600 }}>{time}</div>
        <div style={{ fontSize: '16px', fontWeight: 700, color: '#374151', margin: '2px 0' }}>{title}</div>
        {subtitle && <div style={{ fontSize: '12px', color: '#6B7280' }}>{subtitle}</div>}
      </div>
    </div>
  );
}

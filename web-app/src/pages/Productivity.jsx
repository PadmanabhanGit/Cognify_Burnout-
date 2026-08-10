import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import FlashOnIcon from '@mui/icons-material/FlashOn';
import BottomNavigation from '../components/BottomNavigation';
import api from '../services/api';

export default function Productivity() {
  const navigate = useNavigate();
  // 'loading' | 'loaded' | 'error' — a failed request must never render as a
  // real score of 0. 'loaded' with a null score means "no record for today
  // yet", which is also distinct from a request failure.
  const [loadState, setLoadState] = useState('loading');
  const [todayLog, setTodayLog] = useState(null);
  const [weeklyDays, setWeeklyDays] = useState([]);
  const [weeklyLoaded, setWeeklyLoaded] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  // Single fetch on mount — no polling.
  useEffect(() => {
    const fetchProd = async () => {
      try {
        const res = await api.get('/api/productivity/today');
        if (res.data.success) {
          setTodayLog(res.data.log ?? null);
          setLastSyncedAt(Date.now());
          setLoadState('loaded');
        } else {
          setLoadState('error');
        }
      } catch (err) {
        console.error('Failed to load productivity data', err);
        setLoadState('error');
      }
    };

    const fetchWeekly = async () => {
      try {
        const res = await api.get('/api/productivity/weekly');
        if (res.data.success) {
          setWeeklyDays(res.data.days ?? []);
        }
      } catch (err) {
        console.error('Failed to load weekly productivity data', err);
      } finally {
        setWeeklyLoaded(true);
      }
    };

    fetchProd();
    fetchWeekly();
  }, []);

  const productivityScore = todayLog?.productivityScore ?? null;
  const focusHours = todayLog?.focusHours ?? null;

  // Genuine day-over-day comparison from two real persisted values only.
  const todayIndex = todayLog?.date ? weeklyDays.findIndex(d => d.date === todayLog.date) : -1;
  const yesterday = todayIndex > 0 ? weeklyDays[todayIndex - 1] : null;
  const yesterdayScore = yesterday?.available ? (yesterday.productivityScore ?? null) : null;
  const dayOverDayChange = (productivityScore != null && yesterdayScore != null)
    ? productivityScore - yesterdayScore
    : null;
  const changeText = dayOverDayChange != null
    ? `${dayOverDayChange >= 0 ? '+' : ''}${dayOverDayChange}`
    : 'Insufficient data';

  const availableDays = weeklyDays.filter(d => d.available && d.productivityScore != null);

  const gaugeCenterText = () => {
    if (loadState === 'loading') return { big: '…', small: 'LOADING' };
    if (loadState === 'error') return { big: '--', small: 'UNAVAILABLE (request failed)' };
    if (productivityScore == null) return { big: '--', small: 'NO DATA TODAY YET' };
    return { big: String(productivityScore), small: 'PRODUCTIVITY SCORE' };
  };
  const gauge = gaugeCenterText();
  const gaugeFraction = (loadState === 'loaded' && productivityScore != null) ? productivityScore / 100 : 0;

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>

      {/* Header matching Android ProductivityScreen */}
      <div style={{
        width: '100%',
        background: 'linear-gradient(to bottom, #10B981, #059669)',
        borderBottomLeftRadius: '32px',
        borderBottomRightRadius: '32px',
        padding: '40px 24px 60px 24px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div onClick={() => navigate('/dashboard')} style={{ cursor: 'pointer', display: 'flex', justifyContent: 'center', alignItems: 'center', width: '36px', height: '36px' }}>
            <ArrowBackIcon style={{ color: 'white' }} />
          </div>
        </div>
        <div style={{ marginTop: '24px' }}>
          <div style={{ color: 'white', fontSize: '24px', fontWeight: 700 }}>Productivity Analysis</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '4px' }}>Track and optimize your performance</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>

        {/* Today's Productivity Card — driven by the persisted backend record */}
        <div className="white-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>Today's Productivity</div>
            <TrendingUpIcon style={{ color: '#10B981', fontSize: '20px' }} />
          </div>

          <div style={{ marginTop: '30px', position: 'relative', width: '160px', height: '160px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <svg viewBox="0 0 160 160" style={{ position: 'absolute', transform: 'rotate(135deg)', width: '100%', height: '100%' }}>
              <circle cx="80" cy="80" r="70" fill="none" stroke="#F3F4F6" strokeWidth="12" strokeDasharray="440" strokeDashoffset="110" strokeLinecap="round" />
              <circle cx="80" cy="80" r="70" fill="none" stroke="#0F172A" strokeWidth="12" strokeDasharray="440" strokeDashoffset={440 - (gaugeFraction * 330)} strokeLinecap="round" />
            </svg>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ fontSize: gauge.big.length > 2 ? '32px' : '48px', fontWeight: 800, color: '#111827' }}>{gauge.big}</div>
              <div style={{ fontSize: '9px', fontWeight: 700, color: '#9CA3AF', textAlign: 'center', maxWidth: '110px' }}>{gauge.small}</div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px', width: '100%', marginTop: '30px' }}>
            <ChangeBox label="vs Yesterday" value={changeText} color="#DCFCE7" textColor="#16A34A" />
            <ChangeBox label="This Month" value="Insufficient data" color="#EFF6FF" textColor="#2563EB" />
          </div>
        </div>

        {/* 7-Day Trend — Monday..Sunday from the backend's real /weekly days[].
            Missing days render as gaps, never as a measured zero. */}
        <div className="white-card" style={{ padding: '20px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '24px' }}>7-Day Trend</div>
          {!weeklyLoaded ? (
            <div style={{ fontSize: '12px', color: '#9CA3AF' }}>Loading…</div>
          ) : availableDays.length < 2 ? (
            <div style={{ fontSize: '12px', color: '#9CA3AF' }}>
              Insufficient history — need at least 2 days of real productivity data this week to show a trend.
            </div>
          ) : (
            <>
              <WeeklyTrendChart days={weeklyDays} />
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px' }}>
                {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(d => (
                  <div key={d} style={{ fontSize: '9px', color: 'gray' }}>{d}</div>
                ))}
              </div>
            </>
          )}
        </div>

        {/* Key Insights — only the one metric with a real persisted source.
            Goal Hit / Start Time / Ranking removed: no legitimate backend
            source exists for any of them (see the Productivity audit).
            Label is honest that focusHours is a proxy (avg session length x2),
            not a measured "peak" span. */}
        <InsightMiniCard
          icon={<FlashOnIcon />}
          value={loadState === 'loaded' && focusHours != null ? `${Math.round(focusHours * 10) / 10}h` : '--'}
          label="Avg Focus Span"
          sub="Proxy from average session length, not a measured peak"
        />

      </div>

      <div style={{ textAlign: 'center', marginTop: '24px', marginBottom: '24px', fontSize: '12px', color: loadState === 'error' ? '#EF4444' : 'var(--text-secondary)' }}>
        {loadState === 'error' ? 'Unable to sync data. Retry.' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
      </div>

      <BottomNavigation activeTab="analytics" />
    </div>
  );
}

function ChangeBox({ label, value, color, textColor }) {
  return (
    <div style={{ backgroundColor: color, borderRadius: '16px', padding: '12px', flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ fontSize: value === 'Insufficient data' ? '11px' : '18px', fontWeight: 700, color: textColor, textAlign: 'center' }}>{value}</div>
      <div style={{ fontSize: '10px', color: textColor, opacity: 0.7 }}>{label}</div>
    </div>
  );
}

function InsightMiniCard({ icon, value, label, sub }) {
  return (
    <div style={{ backgroundColor: 'white', borderRadius: '20px', padding: '16px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
      <div style={{ width: '32px', height: '32px', borderRadius: '16px', backgroundColor: '#F5F3FF', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        {React.cloneElement(icon, { style: { color: '#8B5CF6', fontSize: '16px' } })}
      </div>
      <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937', marginTop: '12px' }}>{value}</div>
      <div style={{ fontSize: '13px', fontWeight: 700, color: '#111827' }}>{label}</div>
      <div style={{ fontSize: '10px', color: '#6B7280', lineHeight: '14px' }}>{sub}</div>
    </div>
  );
}

/** Renders only the real, available days from /weekly — connects consecutive
 *  available days with a line and leaves a gap where a day is unavailable,
 *  rather than interpolating or treating a missing day as zero. */
function WeeklyTrendChart({ days }) {
  const width = 300;
  const height = 140;
  const n = days.length;
  if (n < 2) return null;
  const spacing = width / (n - 1);
  const yFor = (score) => height - (score / 100) * height;

  const segments = [];
  let prevIndex = null;
  days.forEach((day, i) => {
    if (day.available && day.productivityScore != null) {
      if (prevIndex !== null && prevIndex === i - 1) {
        const prevDay = days[prevIndex];
        if (prevDay.productivityScore != null) {
          segments.push({
            key: `seg-${i}`,
            x1: prevIndex * spacing, y1: yFor(prevDay.productivityScore),
            x2: i * spacing, y2: yFor(day.productivityScore)
          });
        }
      }
      prevIndex = i;
    }
  });

  const points = days
    .map((day, i) => (day.available && day.productivityScore != null) ? { x: i * spacing, y: yFor(day.productivityScore) } : null)
    .filter(Boolean);

  return (
    <div style={{ width: '100%', height: '140px', position: 'relative' }}>
      <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height: '100%', overflow: 'visible' }}>
        {segments.map(s => (
          <line key={s.key} x1={s.x1} y1={s.y1} x2={s.x2} y2={s.y2} stroke="#10B981" strokeWidth="2.5" strokeLinecap="round" />
        ))}
        {points.map((p, i) => (
          <g key={i}>
            <circle cx={p.x} cy={p.y} r="4" fill="#10B981" />
            <circle cx={p.x} cy={p.y} r="2" fill="white" />
          </g>
        ))}
      </svg>
    </div>
  );
}

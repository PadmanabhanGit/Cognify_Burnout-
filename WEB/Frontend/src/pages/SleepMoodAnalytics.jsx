import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api from '../services/api';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';

// Registered here so this page renders correctly on a direct load of
// /sleep/analytics. Previously it relied on WeeklyReport.jsx's module-level
// registration side effect. ChartJS.register is idempotent, so this does not
// conflict with that existing call.
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

/**
 * Sleep History / Analytics — the Web mirror of Android's
 * SleepMoodAnalyticsScreen (route sleep_mood_analytics).
 *
 * Primary metric is sleep QUALITY, matching Android. The page previously
 * plotted duration under a "30-Day Trend" heading while the backend returned
 * the oldest 30 records ever recorded, so both the metric and the label
 * disagreed with Android. The backend now returns automatic-only, deduplicated,
 * chronological nights inside a real trailing window (see
 * server/routes/sleepMood.js), and every count/label below is derived from the
 * distinct nights actually returned — never from the requested window size.
 */
const WINDOW_DAYS = 30;

export default function SleepMoodAnalytics() {
  const navigate = useNavigate();
  const [nights, setNights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  useEffect(() => {
    const fetchTrends = async () => {
      try {
        const res = await api.get(`/api/sleep-mood/trends/sleep?days=${WINDOW_DAYS}`);
        if (res.data.success) {
          // Only nights that carry a real quality reading can be plotted.
          // A night with a null quality is dropped rather than zero-filled.
          setNights((res.data.trends || []).filter(t => typeof t.sleepQuality === 'number'));
          setLastSyncedAt(Date.now());
          setError(false);
        } else {
          setError(true);
        }
      } catch (err) {
        console.error('Failed to load sleep history', err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchTrends();
  }, []);

  const nightCount = nights.length;
  const hasTrend = nightCount >= 2;

  // Averaged across distinct nights only. Null (not 0) when nothing real exists.
  const averageQuality = nightCount > 0
    ? Math.round(nights.reduce((sum, n) => sum + n.sleepQuality, 0) / nightCount)
    : null;

  // "2026-08-10" -> "Aug 10". Parsed as parts, never `new Date("YYYY-MM-DD")`,
  // which is treated as UTC midnight and can render as the previous day.
  const shortDateLabel = (isoDate) => {
    const parts = String(isoDate || '').split('-');
    if (parts.length !== 3) return isoDate;
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const monthIndex = Number(parts[1]) - 1;
    if (!(monthIndex >= 0 && monthIndex < 12)) return isoDate;
    return `${months[monthIndex]} ${Number(parts[2]) || parts[2]}`;
  };

  const chartData = {
    labels: nights.map(n => shortDateLabel(n.date)),
    datasets: [
      {
        label: 'Sleep Quality (%)',
        data: nights.map(n => n.sleepQuality),
        borderColor: '#4F46E5',
        backgroundColor: 'rgba(79, 70, 229, 0.1)',
        fill: true,
        tension: 0.4,
      },
    ],
  };

  return (
    <div style={{ paddingBottom: '20px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      {/* Purple header, matching Android's Sleep History screen */}
      <div style={{
        background: 'linear-gradient(to bottom, #4F46E5, #9333EA)',
        borderBottomLeftRadius: '32px',
        borderBottomRightRadius: '32px',
        padding: '40px 24px 48px 24px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/sleep')}>
          <ArrowBackIcon style={{ color: 'white', marginRight: '16px' }} />
          <div style={{ fontSize: '24px', fontWeight: 700, color: 'white' }}>Sleep History</div>
        </div>
        <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '6px' }}>
          Your automatically detected nights
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>

        {/* Average Quality — labelled with the real number of distinct nights */}
        <div className="white-card" style={{ padding: '20px', textAlign: 'center' }}>
          {loading ? (
            <div style={{ color: '#6B7280', fontSize: '14px' }}>Loading…</div>
          ) : error ? (
            <div style={{ color: '#EF4444', fontSize: '14px' }}>⚠️ Unable to load sleep history. Retry.</div>
          ) : averageQuality === null ? (
            <>
              <div style={{ fontSize: '12px', color: '#6B7280' }}>Average Quality</div>
              <div style={{ fontSize: '14px', color: '#6B7280', marginTop: '4px' }}>No detected sleep sessions yet</div>
            </>
          ) : (
            <>
              <div style={{ fontSize: '12px', color: '#6B7280' }}>
                {nightCount === 1 ? 'Quality — 1 night' : `Average Quality — Last ${nightCount} Nights`}
              </div>
              <div style={{ fontSize: '28px', fontWeight: 700, color: '#4F46E5', marginTop: '4px' }}>
                {averageQuality}%
              </div>
            </>
          )}
        </div>

        {/* Quality Trend — real nights only, minimum 2 distinct nights */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937' }}>Quality Trend</div>
            {hasTrend && (
              <div style={{ fontSize: '10px', color: '#6B7280', backgroundColor: '#F3F4F6', borderRadius: '8px', padding: '4px 8px' }}>
                Last {nightCount} nights
              </div>
            )}
          </div>

          {loading ? (
            <div style={{ color: '#6B7280' }}>Loading trends…</div>
          ) : error ? (
            <div style={{ color: '#EF4444' }}>⚠️ Unable to sync data. Retry.</div>
          ) : nightCount === 0 ? (
            <div style={{ color: '#6B7280', fontSize: '13px' }}>
              No detected sleep sessions yet. A trend will appear once the Android sleep monitor has recorded a few nights.
            </div>
          ) : !hasTrend ? (
            <div style={{ color: '#6B7280', fontSize: '13px' }}>
              Only one detected night so far ({shortDateLabel(nights[0].date)} — {nights[0].sleepQuality}%). A trend needs at least two distinct nights.
            </div>
          ) : (
            <Line
              data={chartData}
              options={{ scales: { y: { min: 0, max: 100 } } }}
            />
          )}
        </div>

        <div style={{ textAlign: 'center', marginTop: '8px', marginBottom: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
          {error ? '⚠️ Sync failed' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing…')}
        </div>
      </div>
    </div>
  );
}

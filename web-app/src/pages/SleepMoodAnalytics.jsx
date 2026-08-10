import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api from '../services/api';
import { Line } from 'react-chartjs-2';

export default function SleepMoodAnalytics() {
  const navigate = useNavigate();
  const [trends, setTrends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  useEffect(() => {
    const fetchTrends = async () => {
      try {
        const res = await api.get('/api/sleep-mood/trends/sleep?days=30');
        if (res.data.success) {
          setTrends(res.data.trends || []);
          setLastSyncedAt(Date.now());
          setError(false);
        } else {
          setError(true);
        }
      } catch (err) {
        console.error("Failed to load trends", err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchTrends();
  }, []);

  const chartData = {
    labels: trends.map(t => new Date(t.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })),
    datasets: [
      {
        label: 'Sleep Duration (hrs)',
        data: trends.map(t => t.sleepDuration),
        borderColor: '#6366F1',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        fill: true,
        tension: 0.4
      }
    ]
  };

  return (
    <div style={{ paddingBottom: '20px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      <div style={{ padding: '24px', backgroundColor: 'white', display: 'flex', alignItems: 'center', borderBottom: '1px solid #E5E7EB' }}>
        <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/sleep')}>
          <ArrowBackIcon style={{ color: '#1F2937', marginRight: '16px' }} />
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937' }}>Sleep Analytics</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '24px' }}>
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#1F2937' }}>30-Day Trend</div>
          {loading ? (
            <div>Loading trends...</div>
          ) : error ? (
            <div style={{ color: '#EF4444' }}>⚠️ Unable to sync data. Retry.</div>
          ) : trends.length === 0 ? (
            <div style={{ color: '#6B7280' }}>Not enough data for analytics.</div>
          ) : (
            <Line data={chartData} options={{ scales: { y: { min: 0, max: 14 } } }} />
          )}
        </div>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
          {error ? '⚠️ Sync failed' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
        </div>
      </div>
    </div>
  );
}

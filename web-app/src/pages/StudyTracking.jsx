import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api from '../services/api';

export default function StudyTracking() {
  const navigate = useNavigate();
  const [isActive, setIsActive] = useState(false);
  const [session, setSession] = useState(null);
  const [elapsed, setElapsed] = useState(0);

  const [stats, setStats] = useState({ weeklyHours: 0, sessionCount: 0 });

  useEffect(() => {
    let interval = null;
    if (isActive) {
      interval = setInterval(() => setElapsed(e => e + 1), 1000);
    } else {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [isActive]);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await api.get('/api/dashboard');
        if (res.data.success) {
          setStats(res.data.dashboard.featureCards.study);
        }
      } catch (err) {
        console.error(err);
      }
    };
    fetchStats();
  }, []);

  const handleStart = async () => {
    try {
      const res = await api.post('/api/study/start', { subject: 'General', notes: '' });
      if (res.data.success) {
        setSession(res.data.session);
        setIsActive(true);
        setElapsed(0);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleStop = async () => {
    if (!session) return;
    try {
      await api.patch(`/api/study/stop/${session.id}`);
      setIsActive(false);
      setSession(null);
      alert('Session logged successfully!');
    } catch (err) {
      console.error(err);
    }
  };

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  return (
    <div style={{ paddingBottom: '20px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      <div style={{ padding: '24px', backgroundColor: 'white', display: 'flex', alignItems: 'center', borderBottom: '1px solid #E5E7EB' }}>
        <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          <ArrowBackIcon style={{ color: '#1F2937', marginRight: '16px' }} />
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937' }}>Study Tracking</div>
        </div>
      </div>

      <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: '64px' }}>
        <div style={{ fontSize: '72px', fontWeight: 800, color: '#1E293B', fontVariantNumeric: 'tabular-nums' }}>
          {formatTime(elapsed)}
        </div>
        
        {isActive ? (
          <button onClick={handleStop} style={{ marginTop: '48px', backgroundColor: '#EF4444', color: 'white', padding: '16px 48px', borderRadius: '32px', fontSize: '20px', fontWeight: 700 }}>
            Stop Session
          </button>
        ) : (
          <button onClick={handleStart} style={{ marginTop: '48px', backgroundColor: '#10B981', color: 'white', padding: '16px 48px', borderRadius: '32px', fontSize: '20px', fontWeight: 700 }}>
            Start Session
          </button>
        )}

        <div style={{ display: 'flex', gap: '16px', width: '100%', marginTop: '48px' }}>
          <div style={{ flex: 1, backgroundColor: '#EFF6FF', padding: '24px', borderRadius: '16px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{ fontSize: '14px', color: '#3B82F6', fontWeight: 700, marginBottom: '8px' }}>Weekly Hours</div>
            <div style={{ fontSize: '28px', color: '#1D4ED8', fontWeight: 800 }}>{stats.weeklyHours}h</div>
          </div>
          <div style={{ flex: 1, backgroundColor: '#FAF5FF', padding: '24px', borderRadius: '16px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{ fontSize: '14px', color: '#9333EA', fontWeight: 700, marginBottom: '8px' }}>Sessions</div>
            <div style={{ fontSize: '28px', color: '#7E22CE', fontWeight: 800 }}>{stats.sessionCount}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

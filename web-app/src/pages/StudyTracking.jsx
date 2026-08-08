import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TimerIcon from '@mui/icons-material/Timer';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import api from '../services/api';
import BottomNavigation from '../components/BottomNavigation';

export default function StudyTracking() {
  const navigate = useNavigate();
  const [isActive, setIsActive] = useState(false);
  const [session, setSession] = useState(null);
  const [elapsed, setElapsed] = useState(0);

  const [stats, setStats] = useState({ weeklyHours: 0, sessionCount: 0 });
  const [weeklyData, setWeeklyData] = useState([0, 0, 0, 0, 0, 0, 0]); // Sun-Sat or Mon-Sun depending on backend
  
  const maxHours = Math.max(...weeklyData, 1);
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

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
        const res = await api.get('/api/study/stats/weekly');
        if (res.data.success) {
          setStats({
            weeklyHours: res.data.stats.totalHours,
            sessionCount: res.data.stats.sessionCount,
            todayMinutes: res.data.stats.todayMinutes,
            totalMinutes: res.data.stats.totalMinutes,
          });
          const breakdown = res.data.stats.dailyTotals || res.data.stats.dailyBreakdown;
          if (breakdown) {
            if (Array.isArray(breakdown)) {
              setWeeklyData(breakdown);
            } else if (typeof breakdown === 'object') {
              const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
              const arr = days.map(d => breakdown[d] || 0);
              setWeeklyData(arr);
            }
          }
          if (res.data.stats.activeSession) {
            setSession(res.data.stats.activeSession);
            setIsActive(true);
            const elapsedSeconds = Math.floor((new Date() - new Date(res.data.stats.activeSession.startTime)) / 1000);
            setElapsed(elapsedSeconds);
          }
        }
      } catch (err) {
        console.error(err);
      }
    };

    fetchStats();
    const intervalId = window.setInterval(fetchStats, 10000);
    const handleFocus = () => fetchStats();
    window.addEventListener('focus', handleFocus);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('focus', handleFocus);
    };
  }, []);

  const handleStart = async () => {
    try {
      const subject = prompt("What are you working on?", "e.g. Mathematics, Project Research");
      if (!subject) return;
      
      const res = await api.post('/api/study/start', { subject, notes: '' });
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
      // Refresh stats
      const res = await api.get('/api/study/stats/weekly');
      if (res.data.success) {
        setStats({
          weeklyHours: res.data.stats.totalHours,
          sessionCount: res.data.stats.sessionCount,
          todayMinutes: res.data.stats.todayMinutes,
          totalMinutes: res.data.stats.totalMinutes,
        });
        const breakdown = res.data.stats.dailyTotals || res.data.stats.dailyBreakdown;
        if (breakdown) {
          if (Array.isArray(breakdown)) {
            setWeeklyData(breakdown);
          } else if (typeof breakdown === 'object') {
            const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
            const arr = days.map(d => breakdown[d] || 0);
            setWeeklyData(arr);
          }
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  const formatTime = (secs) => {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return h > 0 ? `${h}:${m}:${s}` : `${m}:${s}`;
  };

  const getFormattedDuration = (baseMins, addedSecs) => {
    const totalSecs = (baseMins || 0) * 60 + addedSecs;
    if (!totalSecs) return '0s';
    const h = Math.floor(totalSecs / 3600);
    const m = Math.floor((totalSecs % 3600) / 60);
    const s = totalSecs % 60;
    
    let parts = [];
    if (h > 0) parts.push(`${h}h`);
    if (m > 0) parts.push(`${m}m`);
    if (s > 0) parts.push(`${s}s`);
    return parts.length > 0 ? parts.join(' ') : '0s';
  };

  const todaysDisplay = getFormattedDuration(stats.todayMinutes, elapsed);
  const weeklyDisplay = getFormattedDuration(stats.totalMinutes, elapsed);

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
              backgroundColor: isActive ? 'rgba(234, 179, 8, 0.15)' : 'rgba(34, 197, 94, 0.15)', 
              padding: '6px 12px', 
              borderRadius: '20px', 
              fontSize: '11px', 
              fontWeight: 800, 
              color: isActive ? '#EAB308' : '#22C55E',
              letterSpacing: '0.5px'
            }}>
              {isActive ? "IN PROGRESS" : "READY"}
            </div>
          </div>

          <div style={{ marginTop: '36px', marginBottom: '36px', fontSize: '72px', fontWeight: 800, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums', textShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
            {formatTime(elapsed)}
          </div>

          <button 
            onClick={isActive ? handleStop : handleStart}
            style={{ 
              width: '80%', height: '60px', borderRadius: '30px', 
              background: isActive ? 'linear-gradient(135deg, #EF4444, #F43F5E)' : 'linear-gradient(135deg, #4F46E5, #7C3AED)', 
              color: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', border: 'none', cursor: 'pointer',
              boxShadow: isActive ? '0 8px 25px -5px rgba(239, 68, 68, 0.5)' : '0 8px 25px -5px rgba(99, 102, 241, 0.5)',
              transition: 'all 0.3s ease',
              transform: isActive ? 'scale(1.02)' : 'scale(1)'
            }}
            onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
            onMouseOut={(e) => e.currentTarget.style.transform = isActive ? 'scale(1.02)' : 'scale(1)'}
          >
            {isActive ? <StopIcon style={{ marginRight: '8px' }} /> : <PlayArrowIcon style={{ marginRight: '8px' }} />}
            <span style={{ fontSize: '18px', fontWeight: 700, letterSpacing: '0.5px' }}>{isActive ? 'End Session' : 'Start Session'}</span>
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

        {/* Weekly Overview Card */}
        <div className="glass-card" style={{ padding: '24px', marginBottom: '40px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '8px', borderRadius: '12px', display: 'flex', marginRight: '12px' }}>
                 <CalendarMonthIcon style={{ color: '#8B5CF6', fontSize: '20px' }} />
              </div>
              <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Weekly Overview</div>
            </div>
          </div>
          
          {/* Bar Chart */}
          <div style={{ display: 'flex', height: '180px', justifyContent: 'space-between', alignItems: 'flex-end', padding: '0 4px', gap: '12px' }}>
            {weeklyData.map((val, idx) => {
              const heightPercentage = Math.max((val / maxHours) * 100, 5); // min height 5%
              return (
                <div key={idx} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', height: '100%' }}>
                  <div style={{ 
                    width: '100%', 
                    height: `${heightPercentage}%`, 
                    background: 'linear-gradient(to top, #6366F1, #8B5CF6)', 
                    borderRadius: '8px',
                    boxShadow: '0 4px 10px rgba(99, 102, 241, 0.3)',
                    transition: 'height 0.5s cubic-bezier(0.4, 0, 0.2, 1)'
                  }}></div>
                  <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-secondary)', marginTop: '12px' }}>{days[idx]}</div>
                </div>
              );
            })}
          </div>
        </div>

      </div>
      
      <BottomNavigation activeTab="tracker" />
    </div>
  );
}

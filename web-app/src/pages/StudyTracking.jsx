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
          });
          // Assuming backend might send an array of 7 days, else fallback
          if (res.data.stats.dailyBreakdown) {
            setWeeklyData(res.data.stats.dailyBreakdown);
          }
        }
      } catch (err) {
        console.error(err);
      }
    };
    fetchStats();
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
        });
        if (res.data.stats.dailyBreakdown) {
          setWeeklyData(res.data.stats.dailyBreakdown);
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const todaysHours = (elapsed / 3600).toFixed(1);

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      {/* Header Section */}
      <div style={{ 
        background: 'linear-gradient(to bottom, #6366F1, #8B5CF6)', 
        borderBottomLeftRadius: '32px', 
        borderBottomRightRadius: '32px',
        padding: '40px 24px 60px 24px',
        height: '180px'
      }}>
        <div style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          <ArrowBackIcon style={{ color: 'white' }} />
        </div>
        <div style={{ marginTop: '16px' }}>
          <div style={{ color: 'white', fontSize: '28px', fontWeight: 700 }}>Study Time Tracking</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px' }}>Monitor your study sessions and analytics</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Current Session Card */}
        <div className="white-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 8px 16px rgba(0,0,0,0.05)' }}>
          <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <TimerIcon style={{ color: '#4F46E5', fontSize: '20px', marginRight: '8px' }} />
              <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>Current Session</div>
            </div>
            <div style={{ 
              backgroundColor: isActive ? '#FEF08A' : '#DCFCE7', 
              padding: '4px 8px', 
              borderRadius: '8px', 
              fontSize: '10px', 
              fontWeight: 700, 
              color: isActive ? '#A16207' : '#16A34A' 
            }}>
              {isActive ? "IN PROGRESS" : "READY"}
            </div>
          </div>

          <div style={{ marginTop: '32px', marginBottom: '32px', fontSize: '64px', fontWeight: 800, color: '#111827', fontVariantNumeric: 'tabular-nums' }}>
            {formatTime(elapsed)}
          </div>

          <button 
            onClick={isActive ? handleStop : handleStart}
            style={{ 
              width: '100%', height: '56px', borderRadius: '12px', 
              backgroundColor: isActive ? '#EF4444' : '#2563EB', 
              color: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', border: 'none', cursor: 'pointer' 
            }}
          >
            {isActive ? <StopIcon style={{ marginRight: '8px' }} /> : <PlayArrowIcon style={{ marginRight: '8px' }} />}
            <span style={{ fontSize: '16px', fontWeight: 700 }}>{isActive ? 'End Session' : 'Start Session'}</span>
          </button>

          <div style={{ display: 'flex', width: '100%', gap: '12px', marginTop: '24px' }}>
            <div style={{ flex: 1, backgroundColor: '#EFF6FF', borderRadius: '16px', padding: '16px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ fontSize: '12px', color: 'rgba(37, 99, 235, 0.7)', fontWeight: 600 }}>Today's Total</div>
              <div style={{ fontSize: '22px', fontWeight: 800, color: '#2563EB' }}>{todaysHours}h</div>
            </div>
            <div style={{ flex: 1, backgroundColor: '#FAF5FF', borderRadius: '16px', padding: '16px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ fontSize: '12px', color: 'rgba(147, 51, 234, 0.7)', fontWeight: 600 }}>This Week</div>
              <div style={{ fontSize: '22px', fontWeight: 800, color: '#9333EA' }}>{stats.weeklyHours}h</div>
            </div>
          </div>
        </div>

        {/* Weekly Overview Card */}
        <div className="white-card" style={{ padding: '20px', marginBottom: '40px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <CalendarMonthIcon style={{ color: '#4F46E5', fontSize: '20px', marginRight: '8px' }} />
              <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>Weekly Overview</div>
            </div>
          </div>
          
          {/* Bar Chart */}
          <div style={{ display: 'flex', height: '150px', justifyContent: 'space-between', alignItems: 'flex-end', padding: '0 4px', gap: '8px' }}>
            {weeklyData.map((val, idx) => (
              <div key={idx} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', height: '100%' }}>
                <div style={{ width: '100%', height: `${(val / maxHours) * 120}px`, backgroundColor: '#3B82F6', borderTopLeftRadius: '6px', borderTopRightRadius: '6px' }}></div>
                <div style={{ fontSize: '10px', color: 'gray', marginTop: '8px' }}>{days[idx]}</div>
              </div>
            ))}
          </div>

          <div style={{ marginTop: '24px' }}>
            {/* Detailed trends button removed as it was static and unimplemented in Web App */}
          </div>
        </div>

      </div>
      
      <BottomNavigation activeTab="tracker" />
    </div>
  );
}

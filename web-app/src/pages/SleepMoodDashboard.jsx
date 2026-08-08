import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AddIcon from '@mui/icons-material/Add';
import BedtimeIcon from '@mui/icons-material/Bedtime';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import WarningIcon from '@mui/icons-material/Warning';
import NightlightIcon from '@mui/icons-material/Nightlight';
import WbSunnyIcon from '@mui/icons-material/WbSunny';
import RadioButtonCheckedIcon from '@mui/icons-material/RadioButtonChecked';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import api from '../services/api';

export default function SleepMoodDashboard() {
  const navigate = useNavigate();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const res = await api.get('/api/sleep-mood/logs');
        if (res.data.success) {
          setLogs(res.data.logs);
        }
      } catch (err) {
        console.error("Failed to load sleep logs", err);
      } finally {
        setLoading(false);
      }
    };

    fetchLogs();
    const intervalId = window.setInterval(fetchLogs, 10000);
    const handleFocus = () => fetchLogs();
    window.addEventListener('focus', handleFocus);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('focus', handleFocus);
    };
  }, []);

  const latestSession = logs.length > 0 ? logs[0] : null;
  const displayQuality = Number(latestSession?.sleepQuality ?? 0);
  const displayDisturbance = Number(latestSession?.disturbanceScore ?? 0);
  const sleepDuration = Number(latestSession?.sleepDuration ?? 0);
  const awakeningCount = Number(latestSession?.awakeningCount ?? 0);

  const getQualityColor = (score) => {
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

  const formatTimestamp = (dateString) => {
    if (!dateString) return '--:--';
    return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

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
          <div style={{ color: 'white', fontSize: '28px', fontWeight: 700 }}>Sleep Monitoring</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px' }}>Scientific analysis of your night rest</div>
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
              <path
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                fill="none" stroke={getQualityColor(displayQuality)} strokeWidth="3"
                strokeDasharray={`${displayQuality}, 100`}
                strokeLinecap="round"
              />
            </svg>
            <div style={{ position: 'absolute', textAlign: 'center' }}>
              <div style={{ fontSize: '28px', fontWeight: 800, color: '#1F2937' }}>{displayQuality}%</div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: getQualityColor(displayQuality) }}>{getQualityLevel(displayQuality)}</div>
            </div>
          </div>

          <div style={{ display: 'flex', width: '100%', marginTop: '24px', gap: '12px' }}>
            <MetricCard label="Total Sleep" value={`${sleepDuration}h`} icon={<BedtimeIcon />} color="#6366F1" />
            <MetricCard label="Awakenings" value={awakeningCount} icon={<NotificationsActiveIcon />} color="#F59E0B" />
            <MetricCard label="Disturbance" value={displayDisturbance} icon={<WarningIcon />} color="#EF4444" />
          </div>
        </div>

        {/* Sleep Start & Wake Times */}
        <div className="white-card" style={{ padding: '20px', display: 'flex', justifyContent: 'space-evenly', alignItems: 'center' }}>
          <TimeInfo label="Sleep Start" time={formatTimestamp(latestSession?.date)} icon={<NightlightIcon />} />
          <div style={{ width: '1px', height: '40px', backgroundColor: '#F3F4F6' }}></div>
          <TimeInfo label="Wake Up" time={latestSession ? "07:30 AM" : "--:--"} icon={<WbSunnyIcon />} />
        </div>

        {/* Timeline Section */}
        <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginTop: '8px' }}>Sleep Timeline</div>

        <div className="white-card" style={{ padding: '24px' }}>
          <TimelineItem time="10:00 PM" title="Monitoring Started" icon={<RadioButtonCheckedIcon />} color="#6366F1" />
          {latestSession ? (
            <>
              <TimelineItem 
                time={formatTimestamp(latestSession.date)} 
                title="Sleep Started" 
                subtitle="User became inactive for 20+ mins" 
                icon={<BedtimeIcon />} 
                color="#4F46E5" 
              />
              <TimelineItem 
                time="07:30 AM" 
                title="Final Wake Up" 
                subtitle="Monitoring successfully completed" 
                icon={<WbSunnyIcon />} 
                color="#10B981" 
                isLast={true} 
              />
            </>
          ) : (
            <div style={{ color: 'gray', fontSize: '14px' }}>No timeline data for today.</div>
          )}
        </div>

        <button 
          onClick={() => navigate('/sleep/analytics')}
          style={{ width: '100%', height: '56px', backgroundColor: '#4F46E5', color: 'white', borderRadius: '16px', fontWeight: 700, display: 'flex', justifyContent: 'center', alignItems: 'center', border: 'none', cursor: 'pointer' }}
        >
          View Full Analytics
          <ArrowForwardIcon style={{ marginLeft: '8px' }} />
        </button>

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

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import GroupsIcon from '@mui/icons-material/Groups';
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import TvIcon from '@mui/icons-material/Tv';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import WarningIcon from '@mui/icons-material/Warning';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import api from '../services/api';

import BottomNavigation from '../components/BottomNavigation';

export default function AppUsage() {
  const navigate = useNavigate();
  const [usage, setUsage] = useState([]);
  const [topApps, setTopApps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeSeconds, setActiveSeconds] = useState(0);

  useEffect(() => {
    const fetchUsage = async () => {
      try {
        const res = await api.get('/api/usage/today');
        if (res.data.success) {
          setUsage(res.data.usage || []);
          setTopApps(res.data.topApps || []);
          setActiveSeconds(0); // Reset optimistic ticks on fresh data
        }
      } catch (err) {
        console.error("Failed to load usage data", err);
      } finally {
        setLoading(false);
      }
    };
    
    fetchUsage(); // initial fetch
  }, []);

  // Optimistic UI Ticking
  useEffect(() => {
    if (loading) return;
    const tickId = setInterval(() => {
      setActiveSeconds(prev => prev + 1);
    }, 1000);
    return () => clearInterval(tickId);
  }, [loading]);

  // Normalize categories from backend — classifier may return "Entertainment", "Others", etc.
  const normalizeCategory = (cat) => {
    if (!cat) return 'Others';
    const c = cat.toLowerCase();
    if (c.includes('social')) return 'Social Media';
    if (c.includes('gaming') || c.includes('game')) return 'Gaming';
    if (c.includes('stream') || c.includes('entertainment') || c.includes('video') || c.includes('media')) return 'Streaming';
    if (c.includes('product') || c.includes('work') || c.includes('study') || c.includes('edu')) return 'Productivity';
    return 'Others';
  };

  // The current API preserves seconds; older records fall back to stored minutes.
  const buckets = { 'Social Media': 0, 'Gaming': 0, 'Streaming': 0, 'Productivity': 0 };
  usage.forEach(u => {
    const norm = normalizeCategory(u.category);
    if (norm in buckets) {
      buckets[norm] += Number(u.durationSeconds ?? (u.duration || 0) * 60);
    }
  });

  // Apply optimistic ticking to Productivity
  buckets['Productivity'] += activeSeconds;

  const totalSeconds = Object.values(buckets).reduce((a, b) => a + b, 0);

  const optimisticTopApps = topApps.map(app => {
    const isCognify = app.name.toLowerCase().includes('cognify') || app.name.toLowerCase().includes('burnout') || (app.packageName && app.packageName.includes('simats'));
    if (isCognify) {
      return { ...app, durationSeconds: (app.durationSeconds ?? (app.duration || 0) * 60) + activeSeconds };
    }
    return app;
  });
  
  const formatDuration = (totalSeconds) => {
    const safeSeconds = Math.max(0, Math.floor(Number(totalSeconds) || 0));
    const h = Math.floor(safeSeconds / 3600);
    const m = Math.floor((safeSeconds % 3600) / 60);
    const s = safeSeconds % 60;

    let parts = [];
    if (h > 0) parts.push(`${h}h`);
    if (m > 0) parts.push(`${m}m`);
    if (s > 0 || parts.length === 0) parts.push(`${s}s`);
    return parts.join(' ');
  };

  const totalHoursStr = formatDuration(totalSeconds);


  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      
      {/* Header matching Android EntertainmentAppUsageScreen */}
      <div style={{ 
        width: '100%', 
        background: 'linear-gradient(to right, #8B5CF6, #3B82F6)', 
        borderBottomLeftRadius: '32px', 
        borderBottomRightRadius: '32px',
        padding: '40px 24px 60px 24px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <IconButton onClick={() => navigate('/dashboard')} icon={<ArrowBackIcon style={{ color: 'white' }}/>} />
        </div>
        <div style={{ marginTop: '24px' }}>
          <div style={{ color: 'white', fontSize: '28px', fontWeight: 700, lineHeight: 1.2 }}>App Usage</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '8px' }}>Track your leisure time and its impact</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Daily Entertainment Usage Card */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Daily Usage</div>
            <div style={{ backgroundColor: 'var(--bg-primary)', padding: '4px 8px', borderRadius: '8px', fontSize: '12px', color: 'var(--text-secondary)' }}>Today</div>
          </div>

          {loading ? (
            <div>Syncing with device...</div>
          ) : (
            <>
              {['Social Media', 'Gaming', 'Streaming', 'Productivity'].map((cat, index) => {
                const secs = buckets[cat] || 0;
                const durationStr = formatDuration(secs);
                const progress = Math.min(secs / (8 * 60 * 60), 1.0);
                const colors = { 'Social Media': '#F43F5E', 'Gaming': '#F59E0B', 'Streaming': '#3B82F6', 'Productivity': '#10B981' };
                return (
                  <UsageItem key={index} label={cat} duration={durationStr} progress={progress}
                    color={colors[cat] || '#6B7280'} icon={getIconForCategory(cat)} />
                );
              })}
            </>
          )}

          <div style={{ height: '1px', backgroundColor: 'var(--border-color, #F3F4F6)', margin: '20px 0' }}></div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-secondary)' }}>Total App Usage</div>
            <div style={{ fontSize: '16px', fontWeight: 800, color: 'var(--text-primary)' }}>{totalHoursStr}</div>
          </div>
        </div>

        {/* Top Used Apps Today */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '16px' }}>Top Used Apps Today</div>
          {loading ? (
            <div>Syncing...</div>
          ) : optimisticTopApps.length === 0 ? (
            <div style={{ color: '#6B7280', fontSize: '14px' }}>No specific apps recorded today.</div>
          ) : (
            optimisticTopApps.map((item, index) => {
              return (
                <div key={index} style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
                  <div style={{ width: '100%', maxWidth: '300px', display: 'flex', alignItems: 'center' }}>
                    <div style={{ width: '10px', height: '10px', borderRadius: '5px', backgroundColor: item.color || '#6B7280', marginRight: '12px' }}></div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)', textTransform: 'capitalize' }}>{item.name}</div>
                      <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>{item.category}</div>
                    </div>
                  </div>
                  <div style={{ flex: 1, textAlign: 'right', fontSize: '14px', fontWeight: 800, color: 'var(--text-primary)' }}>{formatDuration(item.durationSeconds ?? (item.duration || 0) * 60)}</div>
                </div>
              );
            })
          )}
        </div>

        {/* Burnout Risk Visualization Insight */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '4px' }}>Burnout Risk Impact</div>
          <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>Correlation between app type and burnout score</div>
          
          {totalSeconds > 4 * 60 * 60 ? (
            <div style={{ backgroundColor: '#FFF1F2', borderRadius: '12px', padding: '12px', display: 'flex', alignItems: 'center' }}>
              <WarningIcon style={{ color: '#E11D48', fontSize: '16px', marginRight: '12px' }} />
              <div style={{ fontSize: '11px', color: '#9F1239' }}>Your entertainment usage is high. This can reduce focus by <span style={{ fontWeight: 700, color: '#E11D48' }}>12%</span>.</div>
            </div>
          ) : (
            <div style={{ backgroundColor: '#F0FDF4', borderRadius: '12px', padding: '12px', display: 'flex', alignItems: 'center' }}>
              <CheckCircleIcon style={{ color: '#22C55E', fontSize: '16px', marginRight: '12px' }} />
              <div style={{ fontSize: '11px', color: '#166534' }}>Great job! Your entertainment usage is within healthy limits.</div>
            </div>
          )}
        </div>



      </div>
      
      <BottomNavigation activeTab="analytics" />
    </div>
  );
}

function IconButton({ onClick, icon }) {
  return (
    <div onClick={onClick} style={{ cursor: 'pointer', display: 'flex', justifyContent: 'center', alignItems: 'center', width: '36px', height: '36px' }}>
      {icon}
    </div>
  );
}

function UsageItem({ label, duration, progress, color, icon }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
      <div style={{ width: '36px', height: '36px', borderRadius: '10px', backgroundColor: `${color}1A`, display: 'flex', justifyContent: 'center', alignItems: 'center', marginRight: '16px' }}>
        {React.cloneElement(icon, { style: { color, fontSize: '18px' }})}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
          <div style={{ fontWeight: 600, fontSize: '14px', color: 'var(--text-primary)' }}>{label}</div>
          <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--text-primary)' }}>{duration}</div>
        </div>
        <div style={{ width: '100%', height: '6px', backgroundColor: 'var(--bg-primary, #F3F4F6)', borderRadius: '3px', overflow: 'hidden' }}>
          <div style={{ width: `${progress * 100}%`, height: '100%', backgroundColor: color, borderRadius: '3px' }}></div>
        </div>
      </div>
    </div>
  );
}

function getIconForCategory(cat) {
  if (cat === 'Social Media') return <GroupsIcon />;
  if (cat === 'Gaming') return <SportsEsportsIcon />;
  if (cat === 'Streaming' || cat === 'Entertainment') return <TvIcon />;
  if (cat === 'Productivity') return <MenuBookIcon />;
  return <TvIcon />;
}

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

  useEffect(() => {
    const fetchUsage = async () => {
      try {
        const res = await api.get('/api/usage/today');
        if (res.data.success) {
          setUsage(res.data.usage);
          setTopApps(res.data.topApps || []);
        }
      } catch (err) {
        console.error("Failed to load usage data", err);
      } finally {
        setLoading(false);
      }
    };
    fetchUsage();
  }, []);

  const totalMinutes = usage.reduce((acc, curr) => {
    if (!curr || !curr.time) return acc;
    const timeParts = String(curr.time).match(/(\d+)h\s*(\d+)m/);
    if (timeParts) return acc + (parseInt(timeParts[1]) * 60) + parseInt(timeParts[2]);
    return acc;
  }, 0);
  
  const totalHoursStr = `${Math.floor(totalMinutes / 60)}h ${totalMinutes % 60}m`;

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
          <div style={{ color: 'white', fontSize: '28px', fontWeight: 700, lineHeight: 1.2 }}>Entertainment &<br/>App Usage</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '8px' }}>Track your leisure time and its impact</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Daily Entertainment Usage Card */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937' }}>Daily Usage</div>
            <div style={{ backgroundColor: '#F3F4F6', padding: '4px 8px', borderRadius: '8px', fontSize: '12px', color: '#6B7280' }}>Today</div>
          </div>

          {loading ? (
            <div>Syncing with device...</div>
          ) : (
            <>
              {['Social Media', 'Gaming', 'Streaming', 'Productivity'].map((cat, index) => {
                const found = usage.find(u => u.category === cat);
                const durationStr = found ? found.time : '0h 00m';
                const progress = found ? found.progress : 0;
                const colors = {
                  'Social Media': '#F43F5E',
                  'Gaming': '#F59E0B',
                  'Streaming': '#3B82F6',
                  'Productivity': '#10B981'
                };
                return (
                  <UsageItem 
                    key={index} 
                    label={cat} 
                    duration={durationStr} 
                    progress={progress} 
                    color={colors[cat] || '#6B7280'} 
                    icon={getIconForCategory(cat)} 
                  />
                );
              })}
            </>
          )}

          <div style={{ height: '1px', backgroundColor: '#F3F4F6', margin: '20px 0' }}></div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#4B5563' }}>Total App Usage</div>
            <div style={{ fontSize: '16px', fontWeight: 800, color: '#111827' }}>{totalHoursStr}</div>
          </div>
        </div>

        {/* Top Used Apps Today */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginBottom: '16px' }}>Top Used Apps Today</div>
          {loading ? (
            <div>Syncing...</div>
          ) : topApps.length === 0 ? (
            <div style={{ color: '#6B7280', fontSize: '14px' }}>No specific apps recorded today.</div>
          ) : (
            topApps.map((item, index) => {
              const colors = {
                'Social Media': '#F43F5E',
                'Gaming': '#F59E0B',
                'Streaming': '#3B82F6',
                'Productivity': '#10B981'
              };
              return (
                <div key={index} style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
                  <div style={{ width: '10px', height: '10px', borderRadius: '5px', backgroundColor: colors[item.category] || '#6B7280', marginRight: '12px' }}></div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: '14px', fontWeight: 700, color: '#374151', textTransform: 'capitalize' }}>{item.name}</div>
                    <div style={{ fontSize: '11px', color: 'gray' }}>{item.category}</div>
                  </div>
                  <div style={{ fontSize: '14px', fontWeight: 800, color: '#111827' }}>{item.time}</div>
                </div>
              );
            })
          )}
        </div>

        {/* Burnout Risk Visualization Insight */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, color: '#1F2937', marginBottom: '4px' }}>Burnout Risk Impact</div>
          <div style={{ fontSize: '12px', color: '#6B7280', marginBottom: '16px' }}>Correlation between app type and burnout score</div>
          
          {totalMinutes > 240 ? (
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

        {/* AI Recommendations */}
        <div style={{ background: 'linear-gradient(to bottom, #8B5CF6, #3B82F6)', borderRadius: '24px', padding: '20px', marginBottom: '40px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
            <AutoAwesomeIcon style={{ color: 'white', fontSize: '20px', marginRight: '12px' }} />
            <div style={{ fontWeight: 700, fontSize: '16px', color: 'white' }}>AI Recommendations</div>
          </div>
          
          <div style={{ backgroundColor: 'rgba(255,255,255,0.15)', borderRadius: '16px', padding: '16px', display: 'flex', alignItems: 'center' }}>
            <TvIcon style={{ color: 'white', fontSize: '18px', marginRight: '16px' }} />
            <div>
              <div style={{ color: 'white', fontWeight: 700, fontSize: '14px' }}>Digital Detox</div>
              <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '11px', marginTop: '2px' }}>Try to stay off screens 30 mins before bed for better sleep quality.</div>
            </div>
          </div>
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
          <div style={{ fontWeight: 600, fontSize: '14px', color: '#374151' }}>{label}</div>
          <div style={{ fontWeight: 700, fontSize: '14px', color: '#111827' }}>{duration}</div>
        </div>
        <div style={{ width: '100%', height: '6px', backgroundColor: '#F3F4F6', borderRadius: '3px', overflow: 'hidden' }}>
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

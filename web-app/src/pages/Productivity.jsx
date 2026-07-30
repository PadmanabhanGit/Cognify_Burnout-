import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import FlashOnIcon from '@mui/icons-material/FlashOn';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import BottomNavigation from '../components/BottomNavigation';
import api from '../services/api';

export default function Productivity() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProd = async () => {
      try {
        const res = await api.get('/api/productivity/today');
        if (res.data.success && res.data.log) {
          setData(res.data.log);
        }
      } catch (err) {
        console.error("Failed to load productivity data", err);
      } finally {
        setLoading(false);
      }
    };
    fetchProd();
  }, []);
  
  // Mapping Android AppData states to real backend data, with fallbacks
  const productivityScore = data?.productivityScore ?? 0;
  const peakFocusHours = data?.focusHours ?? 0;
  const goalHitRate = data?.tasksPlanned ? Math.round((data.tasksCompleted / data.tasksPlanned) * 100) : 0;
  const averageStartTime = "09:00 AM"; // Not yet provided by backend
  const userGlobalRanking = "Top 15%"; // Not yet provided by backend
  
  const activeHours = peakFocusHours + (data?.breakHours ?? 0);

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
        
        {/* Today's Productivity Card */}
        <div className="white-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>Today's Productivity</div>
            <TrendingUpIcon style={{ color: '#10B981', fontSize: '20px' }} />
          </div>

          <div style={{ marginTop: '30px', position: 'relative', width: '160px', height: '160px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            {/* SVG implementation of the Android Canvas arc */}
            <svg width="160" height="160" style={{ position: 'absolute', transform: 'rotate(-135deg)' }}>
              <circle cx="80" cy="80" r="70" fill="none" stroke="#F3F4F6" strokeWidth="12" strokeDasharray="440" strokeDashoffset="120" strokeLinecap="round" />
              <circle cx="80" cy="80" r="70" fill="none" stroke="#0F172A" strokeWidth="12" strokeDasharray="440" strokeDashoffset={440 - ((productivityScore / 100) * 320)} strokeLinecap="round" />
            </svg>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ fontSize: '48px', fontWeight: 800, color: '#111827' }}>{productivityScore}</div>
              <div style={{ fontSize: '9px', fontWeight: 700, color: '#9CA3AF', textAlign: 'center' }}>PRODUCTIVITY<br/>SCORE</div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px', width: '100%', marginTop: '30px' }}>
            <ChangeBox label="Weekly Change" value="+12%" color="#DCFCE7" textColor="#16A34A" />
            <ChangeBox label="This Month" value="+6%" color="#EFF6FF" textColor="#2563EB" />
          </div>
        </div>

        {/* Key Insights Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <InsightMiniCard icon={<FlashOnIcon/>} value={`${peakFocusHours}h`} label="Peak Focus" sub="Highest continuous span" />
          <InsightMiniCard icon={<CheckCircleIcon/>} value={`${goalHitRate}%`} label="Goal Hit" sub="Daily target completion" />
          <InsightMiniCard icon={<AccessTimeIcon/>} value={averageStartTime} label="Start Time" sub="Consistent start habit" />
          <InsightMiniCard icon={<EmojiEventsIcon/>} value={userGlobalRanking} label="Ranking" sub="Compared to peers" />
        </div>

        {/* Time Distribution Card */}
        <div className="white-card" style={{ padding: '24px', marginBottom: '40px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '20px' }}>Time Distribution</div>
          
          <DistributionItem label="Work/Study" time={`${peakFocusHours}h`} progress={activeHours > 0 ? peakFocusHours / activeHours : 0} color="#3B82F6" />
          <DistributionItem label="Breaks" time={`${data?.breakHours ?? 0}h`} progress={activeHours > 0 ? (data?.breakHours ?? 0) / activeHours : 0} color="#10B981" />
          <DistributionItem label="Distractions" time={`${data?.distractions ?? 0}`} progress={0.12} color="#EF4444" />

          <div style={{ backgroundColor: '#F0FDF4', borderRadius: '16px', padding: '16px', marginTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontSize: '10px', fontWeight: 700, color: '#166534' }}>TOTAL ACTIVE TIME</div>
              <div style={{ fontSize: '20px', fontWeight: 700, color: '#14532D' }}>{activeHours} hours</div>
            </div>
            <AccessTimeIcon style={{ color: '#16A34A', fontSize: '24px' }} />
          </div>
        </div>

      </div>
      
      <BottomNavigation activeTab="analytics" />
    </div>
  );
}

function ChangeBox({ label, value, color, textColor }) {
  return (
    <div style={{ backgroundColor: color, borderRadius: '16px', padding: '12px', flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ fontSize: '18px', fontWeight: 700, color: textColor }}>{value}</div>
      <div style={{ fontSize: '10px', color: textColor, opacity: 0.7 }}>{label}</div>
    </div>
  );
}

function InsightMiniCard({ icon, value, label, sub }) {
  return (
    <div style={{ backgroundColor: 'white', borderRadius: '20px', padding: '16px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
      <div style={{ width: '32px', height: '32px', borderRadius: '16px', backgroundColor: '#F5F3FF', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        {React.cloneElement(icon, { style: { color: '#8B5CF6', fontSize: '16px' }})}
      </div>
      <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937', marginTop: '12px' }}>{value}</div>
      <div style={{ fontSize: '13px', fontWeight: 700, color: '#111827' }}>{label}</div>
      <div style={{ fontSize: '10px', color: '#6B7280', lineHeight: '14px' }}>{sub}</div>
    </div>
  );
}

function DistributionItem({ label, time, progress, color }) {
  return (
    <div style={{ marginBottom: '16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
        <div style={{ fontSize: '13px', color: '#6B7280' }}>{label}</div>
        <div style={{ fontSize: '13px', fontWeight: 700, color: '#1F2937' }}>{time}</div>
      </div>
      <div style={{ width: '100%', height: '8px', backgroundColor: '#F3F4F6', borderRadius: '4px', overflow: 'hidden' }}>
        <div style={{ width: `${progress * 100}%`, height: '100%', backgroundColor: color, borderRadius: '4px' }}></div>
      </div>
    </div>
  );
}

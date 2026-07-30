import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import FlashOnIcon from '@mui/icons-material/FlashOn';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import TimerIcon from '@mui/icons-material/Timer';
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
  
  const productivityScore = data?.productivityScore ?? 85;
  const peakFocusHours = data?.focusHours ?? 4.2;
  const goalHitRate = data?.tasksPlanned ? Math.round((data.tasksCompleted / data.tasksPlanned) * 100) : 80;
  const averageStartTime = "09:00 AM";
  const userGlobalRanking = "Top 15%";
  
  const activeHours = peakFocusHours + (data?.breakHours ?? 1.5);

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
            <svg viewBox="0 0 160 160" style={{ position: 'absolute', transform: 'rotate(135deg)', width: '100%', height: '100%' }}>
              <circle cx="80" cy="80" r="70" fill="none" stroke="#F3F4F6" strokeWidth="12" strokeDasharray="440" strokeDashoffset="110" strokeLinecap="round" />
              <circle cx="80" cy="80" r="70" fill="none" stroke="#0F172A" strokeWidth="12" strokeDasharray="440" strokeDashoffset={440 - ((productivityScore / 100) * 330)} strokeLinecap="round" />
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

        {/* 7-Day Trend Analysis Card */}
        <div className="white-card" style={{ padding: '20px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '24px' }}>7-Day Trend Analysis</div>
          <div style={{ width: '100%', height: '140px', position: 'relative' }}>
            <svg viewBox="0 0 300 140" style={{ width: '100%', height: '100%', overflow: 'visible' }}>
              {/* Overall (Green) */}
              <polyline points="0,84 50,91 100,63 150,70 200,49 250,35 300,45" fill="none" stroke="#10B981" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
              {/* Focus (Blue, Dashed) */}
              <polyline points="0,98 50,105 100,77 150,84 200,63 250,49 300,56" fill="none" stroke="#3B82F6" strokeWidth="2.5" strokeDasharray="8,6" strokeLinecap="round" strokeLinejoin="round" />
              {/* Efficiency (Purple) */}
              <polyline points="0,70 50,77 100,49 150,56 200,35 250,21 300,28" fill="none" stroke="#8B5CF6" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
              
              {/* Points for overall */}
              {[
                {x: 0, y: 84}, {x: 50, y: 91}, {x: 100, y: 63}, {x: 150, y: 70}, 
                {x: 200, y: 49}, {x: 250, y: 35}, {x: 300, y: 45}
              ].map((p, i) => (
                <g key={i}>
                  <circle cx={p.x} cy={p.y} r="5" fill="#10B981" />
                  <circle cx={p.x} cy={p.y} r="2.5" fill="white" />
                </g>
              ))}
            </svg>
          </div>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', marginTop: '16px' }}>
            <ProductivityLegendItem color="#10B981" text="Overall" />
            <ProductivityLegendItem color="#3B82F6" text="Focus" />
            <ProductivityLegendItem color="#8B5CF6" text="Efficiency" />
          </div>
        </div>

        {/* Key Insights Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <InsightMiniCard icon={<FlashOnIcon/>} value={`${peakFocusHours}h`} label="Peak Focus" sub="Highest continuous span" />
          <InsightMiniCard icon={<CheckCircleIcon/>} value={`${goalHitRate}%`} label="Goal Hit" sub="Daily target completion" />
          <InsightMiniCard icon={<AccessTimeIcon/>} value={averageStartTime} label="Start Time" sub="Consistent start habit" />
          <InsightMiniCard icon={<EmojiEventsIcon/>} value={userGlobalRanking} label="Ranking" sub="Compared to peers" />
        </div>

        {/* Peak Performance Hours Card */}
        <div className="white-card" style={{ padding: '20px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '24px' }}>Peak Performance Hours</div>
          <div style={{ width: '100%', height: '100px', position: 'relative' }}>
            <svg viewBox="0 0 300 100" style={{ width: '100%', height: '100%', overflow: 'visible' }}>
              <defs>
                <linearGradient id="peakGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10B981" stopOpacity="0.3" />
                  <stop offset="100%" stopColor="#10B981" stopOpacity="0" />
                </linearGradient>
              </defs>
              <polygon points="0,100 0,80 60,30 120,60 180,70 240,50 300,80 300,100" fill="url(#peakGrad)" />
              <polyline points="0,80 60,30 120,60 180,70 240,50 300,80" fill="none" stroke="#10B981" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px' }}>
            {['06AM', '09AM', '12PM', '03PM', '06PM', '09PM'].map(time => (
              <div key={time} style={{ fontSize: '9px', color: 'gray' }}>{time}</div>
            ))}
          </div>
        </div>

        {/* Time Distribution Card */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '20px' }}>Time Distribution</div>
          
          <DistributionItem label="Work/Study" time="4h 15m" progress={0.65} color="#3B82F6" />
          <DistributionItem label="Breaks" time="1h 30m" progress={0.23} color="#10B981" />
          <DistributionItem label="Distractions" time="45m" progress={0.12} color="#EF4444" />

          <div style={{ backgroundColor: '#F0FDF4', borderRadius: '16px', padding: '16px', marginTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontSize: '10px', fontWeight: 700, color: '#166534' }}>TOTAL ACTIVE TIME</div>
              <div style={{ fontSize: '20px', fontWeight: 700, color: '#14532D' }}>{activeHours} hours</div>
            </div>
            <TimerIcon style={{ color: '#16A34A', fontSize: '24px' }} />
          </div>
        </div>

        {/* Optimization Suggestions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>Optimization Suggestions</div>
            <div style={{ marginLeft: '8px', fontSize: '16px' }}>✨</div>
          </div>
          <SuggestionCard 
            title="Increase Break Frequency" 
            sub="Your focus drops significantly after 90 minutes. Try the Pomodoro technique (25m work, 5m break)." 
            tag="HIGH IMPACT" tagColor="#EF4444" tagBg="#FEE2E2" 
          />
          <SuggestionCard 
            title="Morning Peak alignment" 
            sub="You are most productive between 10AM - 11AM. Schedule your hardest tasks for this window." 
            tag="MEDIUM IMPACT" tagColor="#F59E0B" tagBg="#FEF3C7" 
          />
        </div>

        {/* Footer Button */}
        <button 
          onClick={() => navigate('/burnout-risk')}
          style={{ 
            width: '100%', height: '60px', borderRadius: '16px', border: 'none', cursor: 'pointer',
            background: 'linear-gradient(to right, #6366F1, #A855F7)',
            display: 'flex', justifyContent: 'center', alignItems: 'center',
            marginBottom: '40px'
          }}
        >
          <span style={{ color: 'white', fontWeight: 700, fontSize: '16px' }}>View Burnout</span>
        </button>

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

function ProductivityLegendItem({ color, text }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center' }}>
      <div style={{ width: '8px', height: '8px', borderRadius: '4px', backgroundColor: color, marginRight: '6px' }}></div>
      <div style={{ fontSize: '10px', color: 'gray' }}>{text}</div>
    </div>
  );
}

function SuggestionCard({ title, sub, tag, tagColor, tagBg }) {
  return (
    <div style={{ backgroundColor: 'white', borderRadius: '16px', padding: '16px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
        <div style={{ fontSize: '14px', fontWeight: 700, color: '#1F2937', flex: 1 }}>{title}</div>
        <div style={{ backgroundColor: tagBg, borderRadius: '4px', padding: '2px 6px', fontSize: '8px', fontWeight: 800, color: tagColor }}>{tag}</div>
      </div>
      <div style={{ fontSize: '12px', color: 'gray', lineHeight: '1.5' }}>{sub}</div>
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import WarningIcon from '@mui/icons-material/Warning';
import InfoIcon from '@mui/icons-material/Info';
import AssignmentIcon from '@mui/icons-material/Assignment';
import NotificationsIcon from '@mui/icons-material/Notifications';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import BedtimeIcon from '@mui/icons-material/Bedtime';
import FavoriteIcon from '@mui/icons-material/Favorite';
import RestoreIcon from '@mui/icons-material/Restore';
import SpaIcon from '@mui/icons-material/Spa';
import SelfImprovementIcon from '@mui/icons-material/SelfImprovement';
import api from '../services/api';
import BottomNavigation from '../components/BottomNavigation';

export default function BurnoutRisk() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await api.get('/api/dashboard');
        if (res.data.success) {
          setData(res.data.dashboard);
        }
      } catch (err) {
        console.error(err);
      }
    };

    fetchData();
    const intervalId = window.setInterval(fetchData, 10000);
    const handleFocus = () => fetchData();
    window.addEventListener('focus', handleFocus);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('focus', handleFocus);
    };
  }, []);

  const riskScore = data?.burnoutAlert?.riskScore ?? 45;
  const riskLevel = data?.burnoutAlert?.riskLevel ?? "MODERATE";

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      
      {/* Header Section */}
      <div style={{ 
        width: '100%', 
        background: 'linear-gradient(to bottom, #FFFF7E3D, #F97316)', 
        background: 'linear-gradient(to bottom, #fb923c, #ea580c)', 
        borderBottomLeftRadius: '32px', 
        borderBottomRightRadius: '32px',
        padding: '40px 24px 60px 24px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <div onClick={() => navigate('/dashboard')} style={{ cursor: 'pointer', display: 'flex', justifyContent: 'center', alignItems: 'center', width: '36px', height: '36px' }}>
            <ArrowBackIcon style={{ color: 'white' }} />
          </div>
        </div>
        <div style={{ marginTop: '24px' }}>
          <div style={{ color: 'white', fontSize: '24px', fontWeight: 700 }}>Burnout Risk Analysis</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '4px' }}>AI-powered mental fatigue prediction</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Risk Gauge Card */}
        <div className="white-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>Current Risk Level</div>
              <WarningIcon style={{ color: '#F97316', fontSize: '18px' }} />
            </div>
            <InfoIcon style={{ color: '#9CA3AF', fontSize: '20px' }} />
          </div>

          <div style={{ marginTop: '30px', position: 'relative', width: '150px', height: '150px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <svg width="150" height="150" style={{ position: 'absolute', transform: 'rotate(-135deg)' }}>
              <circle cx="75" cy="75" r="65" fill="none" stroke="#F3F4F6" strokeWidth="12" strokeDasharray="408" strokeDashoffset="120" strokeLinecap="round" />
              <circle cx="75" cy="75" r="65" fill="none" stroke="#F97316" strokeWidth="12" strokeDasharray="408" strokeDashoffset={408 - ((riskScore / 100) * 288)} strokeLinecap="round" />
            </svg>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ fontSize: '36px', fontWeight: 800, color: '#111827' }}>{riskScore}%</div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#F97316' }}>{riskLevel}</div>
            </div>
          </div>

          <div style={{ backgroundColor: '#FFF7ED', borderRadius: '16px', padding: '16px', width: '100%', marginTop: '30px', display: 'flex', alignItems: 'flex-start' }}>
            <AssignmentIcon style={{ color: '#9A3412', fontSize: '20px', marginRight: '12px', marginTop: '2px' }} />
            <div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#9A3412' }}>Assessment</div>
              <div style={{ fontSize: '12px', color: 'rgba(154,52,18,0.8)', marginTop: '4px' }}>Your burnout risk is {riskLevel.toLowerCase()}. Immediate action is recommended to prevent escalation.</div>
            </div>
          </div>
        </div>

        {/* Warning Indicators */}
        <div style={{ background: 'linear-gradient(to bottom, #F97316, #EA580C)', borderRadius: '24px', padding: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
            <NotificationsIcon style={{ color: 'white', fontSize: '20px', marginRight: '12px' }} />
            <div style={{ fontSize: '16px', fontWeight: 700, color: 'white' }}>Warning Indicators</div>
          </div>
          <WarningItem text="Increased study hours (>15%)" />
          <WarningItem text="Sleep deficit detected" />
          {riskScore > 60 && <WarningItem text="Elevated stress levels" />}
        </div>

        {/* Contributing Factors */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '20px' }}>Contributing Factors</div>
          
          <FactorItem name="Study Load" value={75} color="#EF4444" icon={<MenuBookIcon />} />
          <FactorItem name="Sleep Quality" value={60} color="#3B82F6" icon={<BedtimeIcon />} />
          <FactorItem name="Stress Level" value={80} color="#EF4444" icon={<FavoriteIcon />} />
          <FactorItem name="Recovery Time" value={40} color="#3B82F6" icon={<RestoreIcon />} />
        </div>

        {/* Action Plan Button */}
        <div 
          onClick={() => navigate('/action-plan')}
          style={{ 
            background: 'linear-gradient(to right, #4F46E5, #9333EA)', 
            borderRadius: '16px', 
            height: '64px', 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center',
            cursor: 'pointer',
            marginTop: '12px',
            marginBottom: '40px'
          }}
        >
          <AutoAwesomeIcon style={{ color: 'white', fontSize: '20px', marginRight: '12px' }} />
          <div style={{ color: 'white', fontSize: '16px', fontWeight: 700 }}>Generate Personalized Action Plan</div>
        </div>

      </div>

      <BottomNavigation activeTab="home" />
    </div>
  );
}

function WarningItem({ text }) {
  return (
    <div style={{ backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: '12px', padding: '12px', marginBottom: '8px', display: 'flex', alignItems: 'center' }}>
      <WarningIcon style={{ color: 'white', fontSize: '16px', marginRight: '12px' }} />
      <div style={{ color: 'white', fontSize: '13px', fontWeight: 500 }}>{text}</div>
    </div>
  );
}

function FactorItem({ name, value, color, icon }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
      <div style={{ color: '#6B7280', marginRight: '12px', display: 'flex' }}>
        {React.cloneElement(icon, { style: { fontSize: '18px' }})}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
          <div style={{ fontSize: '14px', fontWeight: 500, color: '#4B5563' }}>{name}</div>
          <div style={{ fontSize: '14px', fontWeight: 700, color: color }}>{value}%</div>
        </div>
        <div style={{ width: '100%', height: '6px', backgroundColor: '#F3F4F6', borderRadius: '3px', overflow: 'hidden' }}>
          <div style={{ width: `${value}%`, height: '100%', backgroundColor: color, borderRadius: '3px' }}></div>
        </div>
      </div>
    </div>
  );
}

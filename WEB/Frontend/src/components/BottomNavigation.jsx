import React from 'react';
import { useNavigate } from 'react-router-dom';
import HomeIcon from '@mui/icons-material/Home';
import InsertChartIcon from '@mui/icons-material/InsertChart';
import PersonIcon from '@mui/icons-material/Person';
import TimerIcon from '@mui/icons-material/Timer';

export default function BottomNavigation({ activeTab = 'home' }) {
  const navigate = useNavigate();

  const getStyle = (tabId) => ({
    display: 'flex', 
    flexDirection: 'column', 
    alignItems: 'center', 
    color: activeTab === tabId ? '#6366F1' : '#9CA3AF',
    cursor: 'pointer'
  });

  return (
    <div className="bottom-nav">
      <div style={getStyle('home')} onClick={() => navigate('/dashboard')}>
        <HomeIcon />
        <span style={{ fontSize: '10px', marginTop: '4px', fontWeight: activeTab === 'home' ? 600 : 400 }}>Home</span>
      </div>
      <div style={getStyle('tracker')} onClick={() => navigate('/study')}>
        <TimerIcon />
        <span style={{ fontSize: '10px', marginTop: '4px', fontWeight: activeTab === 'tracker' ? 600 : 400 }}>Tracker</span>
      </div>
      <div style={getStyle('analytics')} onClick={() => navigate('/report')}>
        <InsertChartIcon />
        <span style={{ fontSize: '10px', marginTop: '4px', fontWeight: activeTab === 'analytics' ? 600 : 400 }}>Stats</span>
      </div>
      <div style={getStyle('profile')} onClick={() => navigate('/profile')}>
        <PersonIcon />
        <span style={{ fontSize: '10px', marginTop: '4px', fontWeight: activeTab === 'profile' ? 600 : 400 }}>Profile</span>
      </div>
    </div>
  );
}

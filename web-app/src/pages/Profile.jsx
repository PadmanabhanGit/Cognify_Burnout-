import React from 'react';
import { useNavigate } from 'react-router-dom';
import BottomNavigation from '../components/BottomNavigation';
import { auth } from '../firebase';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonIcon from '@mui/icons-material/Person';
import EmailIcon from '@mui/icons-material/Email';

export default function Profile() {
  const navigate = useNavigate();
  const user = auth.currentUser;
  
  const handleLogout = async () => {
    try {
      await auth.signOut();
      navigate('/login');
    } catch (err) {
      console.error('Failed to log out', err);
    }
  };

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      <div style={{ background: 'linear-gradient(to right, #6366f1, #3b82f6)', borderBottomLeftRadius: '32px', borderBottomRightRadius: '32px', padding: '60px 24px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ width: '80px', height: '80px', borderRadius: '40px', backgroundColor: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px' }}>
            <PersonIcon style={{ fontSize: '48px', color: '#6366f1' }} />
          </div>
          <div style={{ color: 'white', fontSize: '24px', fontWeight: 700 }}>{user?.email?.split('@')[0] || "User"}</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '4px' }}>{user?.email}</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '24px', marginTop: '20px' }}>
        <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937', marginBottom: '16px' }}>Settings</div>
        
        <div className="white-card" style={{ padding: '0' }}>
          <div style={{ display: 'flex', alignItems: 'center', padding: '16px', borderBottom: '1px solid #F3F4F6' }}>
            <EmailIcon style={{ color: '#6B7280', marginRight: '16px' }} />
            <div style={{ flex: 1, fontSize: '16px', color: '#374151', fontWeight: 500 }}>Email Address</div>
            <div style={{ fontSize: '14px', color: '#9CA3AF' }}>{user?.email}</div>
          </div>
          
          <div onClick={handleLogout} style={{ display: 'flex', alignItems: 'center', padding: '16px', cursor: 'pointer' }}>
            <LogoutIcon style={{ color: '#EF4444', marginRight: '16px' }} />
            <div style={{ flex: 1, fontSize: '16px', color: '#EF4444', fontWeight: 600 }}>Log Out</div>
          </div>
        </div>
      </div>

      <BottomNavigation activeTab="profile" />
    </div>
  );
}

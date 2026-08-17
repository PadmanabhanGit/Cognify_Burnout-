import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import BottomNavigation from '../components/BottomNavigation';
import { auth } from '../firebase';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonIcon from '@mui/icons-material/Person';
import EmailIcon from '@mui/icons-material/Email';
import SaveIcon from '@mui/icons-material/Save';
import api from '../services/api';

export default function Profile() {
  const navigate = useNavigate();
  const user = auth.currentUser;
  
  const [profile, setProfile] = useState({
    firstName: '',
    lastName: '',
    age: '',
    location: ''
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await api.get('/api/profile');
        if (res.data) {
          setProfile(res.data);
        }
      } catch (err) {
        console.error('Failed to fetch profile', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      await api.post('/api/profile', profile);
      alert('Profile updated successfully!');
    } catch (err) {
      console.error('Failed to save profile', err);
      alert('Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };
  
  const handleLogout = async () => {
    try {
      await auth.signOut();
      navigate('/login');
    } catch (err) {
      console.error('Failed to log out', err);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setProfile(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div style={{ paddingBottom: '90px', minHeight: '100vh', backgroundColor: 'var(--bg-primary)' }}>
      <div style={{ background: 'linear-gradient(to right, #6366f1, #3b82f6)', borderBottomLeftRadius: '32px', borderBottomRightRadius: '32px', padding: '60px 24px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ width: '80px', height: '80px', borderRadius: '40px', backgroundColor: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: '16px', overflow: 'hidden' }}>
            {user?.photoURL ? (
              <img
                src={user.photoURL}
                alt="Profile"
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              <PersonIcon style={{ fontSize: '48px', color: '#6366f1' }} />
            )}
          </div>
          <div style={{ color: 'white', fontSize: '24px', fontWeight: 700 }}>
            {profile.firstName || profile.lastName ? `${profile.firstName} ${profile.lastName}` : (user?.email?.split('@')[0] || "User")}
          </div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '4px' }}>{user?.email}</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '24px', marginTop: '20px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        
        <div>
          <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '16px', display: 'flex', alignItems: 'center' }}>
            <PersonIcon style={{ color: '#3B82F6', marginRight: '8px' }}/> Personal Information
          </div>
          
          {loading ? (
            <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-secondary)' }}>Loading profile...</div>
          ) : (
            <div className="white-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
              
              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>FIRST NAME</label>
                  <input 
                    type="text" 
                    name="firstName"
                    value={profile.firstName || ''}
                    onChange={handleChange}
                    style={{ width: '100%', padding: '12px 16px', borderRadius: '12px', border: '1px solid var(--border-color)', outline: 'none', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)', fontWeight: 500 }} 
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>LAST NAME</label>
                  <input 
                    type="text" 
                    name="lastName"
                    value={profile.lastName || ''}
                    onChange={handleChange}
                    style={{ width: '100%', padding: '12px 16px', borderRadius: '12px', border: '1px solid var(--border-color)', outline: 'none', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)', fontWeight: 500 }} 
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>AGE</label>
                  <input 
                    type="number" 
                    name="age"
                    value={profile.age || ''}
                    onChange={handleChange}
                    style={{ width: '100%', padding: '12px 16px', borderRadius: '12px', border: '1px solid var(--border-color)', outline: 'none', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)', fontWeight: 500 }} 
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>LOCATION</label>
                  <input 
                    type="text" 
                    name="location"
                    value={profile.location || ''}
                    onChange={handleChange}
                    style={{ width: '100%', padding: '12px 16px', borderRadius: '12px', border: '1px solid var(--border-color)', outline: 'none', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)', fontWeight: 500 }} 
                  />
                </div>
              </div>

              <button 
                onClick={handleSave}
                disabled={saving}
                className="btn-primary"
                style={{
                  marginTop: '12px', width: '100%', padding: '16px', borderRadius: '16px', border: 'none',
                  color: 'white', fontWeight: 700, cursor: 'pointer',
                  display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px',
                  opacity: saving ? 0.7 : 1, fontSize: '16px'
                }}
              >
                <SaveIcon style={{ fontSize: '20px' }} />
                {saving ? 'Saving Changes...' : 'Save Profile'}
              </button>
            </div>
          )}
        </div>

        <div>
          <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '16px', display: 'flex', alignItems: 'center' }}>
            <EmailIcon style={{ color: '#10B981', marginRight: '8px' }}/> Account Settings
          </div>
          <div className="white-card" style={{ padding: '0', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '20px', borderBottom: '1px solid var(--border-color)' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '20px', backgroundColor: '#E0F2FE', display: 'flex', justifyContent: 'center', alignItems: 'center', marginRight: '16px' }}>
                <EmailIcon style={{ color: '#0284C7' }} />
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '16px', color: 'var(--text-primary)', fontWeight: 700 }}>Email Address</div>
                <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{user?.email}</div>
              </div>
            </div>
            
            <div onClick={handleLogout} style={{ display: 'flex', alignItems: 'center', padding: '20px', cursor: 'pointer', backgroundColor: '#FEF2F2' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '20px', backgroundColor: '#FEE2E2', display: 'flex', justifyContent: 'center', alignItems: 'center', marginRight: '16px' }}>
                <LogoutIcon style={{ color: '#EF4444' }} />
              </div>
              <div style={{ flex: 1, fontSize: '16px', color: '#EF4444', fontWeight: 700 }}>Log Out of Account</div>
            </div>
          </div>
        </div>
      </div>

      <BottomNavigation activeTab="profile" />
    </div>
  );
}

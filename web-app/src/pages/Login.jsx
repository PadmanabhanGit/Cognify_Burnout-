import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PsychologyIcon from '@mui/icons-material/Psychology';
import MailRoundedIcon from '@mui/icons-material/MailRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '../firebase';

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSignIn = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please enter email and password');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
      await signInWithEmailAndPassword(auth, email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-bg" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minHeight: '100vh' }}>
      <div style={{ marginTop: '64px', width: '80px', height: '80px', background: 'rgba(255,255,255,0.2)', borderRadius: '24px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <PsychologyIcon style={{ color: 'white', fontSize: '40px' }} />
      </div>
      
      <div style={{ marginTop: '16px', color: 'white', fontSize: '36px', fontWeight: 700 }}>Cognify</div>
      <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px' }}>Mental Health & Burnout Detection</div>
      
      <div className="white-card-lg" style={{ marginTop: '32px', width: '90%', maxWidth: '500px', padding: '28px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ fontSize: '32px', fontWeight: 700, color: '#1E293B' }}>Welcome</div>
        
        {error && <div style={{ color: 'red', fontSize: '14px', marginTop: '8px' }}>{error}</div>}
        
        <form onSubmit={handleSignIn} style={{ width: '100%', marginTop: '32px' }}>
          <div style={{ width: '100%' }}>
            <div style={{ fontSize: '14px', fontWeight: 700, color: '#475569', marginBottom: '8px' }}>Email Address</div>
            <div style={{ display: 'flex', alignItems: 'center', border: '1px solid #E2E8F0', borderRadius: '12px', padding: '12px' }}>
              <MailRoundedIcon style={{ color: '#94A3B8', fontSize: '20px', marginRight: '8px' }} />
              <input 
                type="email" 
                value={email} 
                onChange={e => setEmail(e.target.value)} 
                placeholder="student@example.com"
                style={{ border: 'none', outline: 'none', width: '100%', fontSize: '16px', color: '#1E293B' }}
              />
            </div>
          </div>
          
          <div style={{ width: '100%', marginTop: '24px' }}>
            <div style={{ fontSize: '14px', fontWeight: 700, color: '#475569', marginBottom: '8px' }}>Password</div>
            <div style={{ display: 'flex', alignItems: 'center', border: '1px solid #E2E8F0', borderRadius: '12px', padding: '12px' }}>
              <LockRoundedIcon style={{ color: '#94A3B8', fontSize: '20px', marginRight: '8px' }} />
              <input 
                type={passwordVisible ? "text" : "password"} 
                value={password} 
                onChange={e => setPassword(e.target.value)} 
                placeholder="********"
                style={{ border: 'none', outline: 'none', width: '100%', fontSize: '16px', color: '#1E293B' }}
              />
              <div onClick={() => setPasswordVisible(!passwordVisible)} style={{ cursor: 'pointer', display: 'flex' }}>
                {passwordVisible ? <VisibilityIcon style={{ color: '#94A3B8', fontSize: '20px' }} /> : <VisibilityOffIcon style={{ color: '#94A3B8', fontSize: '20px' }} />}
              </div>
            </div>
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
            <span 
              onClick={() => alert('Forgot password flow not implemented yet.')}
              style={{ color: '#9333EA', fontWeight: 700, fontSize: '14px', cursor: 'pointer' }}
            >
              Forgot Password?
            </span>
          </div>
          
          <button type="submit" disabled={isLoading} className="btn-primary" style={{ width: '100%', height: '60px', marginTop: '24px', fontSize: '18px' }}>
            {isLoading ? 'Loading...' : 'Sign In'}
          </button>
        </form>
        
        <div style={{ display: 'flex', alignItems: 'center', width: '100%', marginTop: '32px' }}>
          <div style={{ flex: 1, height: '1px', backgroundColor: '#E2E8F0' }}></div>
          <div style={{ margin: '0 16px', color: '#94A3B8', fontSize: '12px', fontWeight: 700 }}>OR CONTINUE WITH</div>
          <div style={{ flex: 1, height: '1px', backgroundColor: '#E2E8F0' }}></div>
        </div>
        
        <div style={{ display: 'flex', width: '100%', gap: '16px', marginTop: '24px' }}>
          <button 
            type="button"
            onClick={async () => {
              try {
                const { GoogleAuthProvider, signInWithPopup } = await import('firebase/auth');
                const provider = new GoogleAuthProvider();
                await signInWithPopup(auth, provider);
                navigate('/dashboard');
              } catch (err) {
                setError(err.message);
              }
            }}
            style={{ flex: 1, height: '56px', border: '1px solid #E2E8F0', borderRadius: '12px', display: 'flex', justifyContent: 'center', alignItems: 'center', fontWeight: 700, color: '#1F2937' }}
          >
            Google
          </button>
          <button 
            type="button"
            onClick={() => alert('Facebook login not configured yet.')}
            style={{ flex: 1, height: '56px', border: '1px solid #E2E8F0', borderRadius: '12px', display: 'flex', justifyContent: 'center', alignItems: 'center', fontWeight: 700, color: '#1877F2' }}
          >
            <span style={{ fontSize: '20px', marginRight: '8px' }}>f</span> Facebook
          </button>
        </div>
      </div>
      
      <div style={{ marginTop: '32px', display: 'flex', paddingBottom: '48px' }}>
        <span style={{ color: 'rgba(255,255,255,0.8)' }}>Don't have an account? </span>
        <span 
          onClick={() => navigate('/signup')}
          style={{ color: 'white', fontWeight: 700, marginLeft: '4px', cursor: 'pointer' }}
        >
          Sign Up
        </span>
      </div>
    </div>
  );
}

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PsychologyIcon from '@mui/icons-material/Psychology';
import MailRoundedIcon from '@mui/icons-material/MailRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import PersonIcon from '@mui/icons-material/Person';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { createUserWithEmailAndPassword, updateProfile } from 'firebase/auth';
import { auth } from '../firebase';
import api from '../services/api';

export default function SignUp() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  // Password strength checks — computed live as user types
  const hasMinLength = password.length >= 8;
  const hasUppercase = /[A-Z]/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasDigit = /[0-9]/.test(password);
  const hasSpecial = /[^A-Za-z0-9]/.test(password);
  const isPasswordStrong = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial;

  const handleSignUp = async (e) => {
    e.preventDefault();
    if (!name || !email || !password) {
      setError('Please fill in all fields');
      return;
    }
    if (!isPasswordStrong) {
      setError('Password does not meet all requirements below');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      await updateProfile(userCredential.user, { displayName: name });
      
      // Attempt to register with our backend to sync the user doc in Firestore
      try {
        await api.post('/api/auth/register', { email, password, fullName: name });
      } catch (apiErr) {
        console.error("Backend sync issue:", apiErr);
      }
      
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const reqStyle = (met) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    fontSize: '11px',
    color: met ? '#10B981' : '#6B7280',
    marginBottom: '2px',
  });

  return (
    <div className="login-bg desktop-padding" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minHeight: '100vh' }}>
      <div style={{ marginTop: '64px', width: '80px', height: '80px', background: 'rgba(255,255,255,0.2)', borderRadius: '24px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <PsychologyIcon style={{ color: 'white', fontSize: '40px' }} />
      </div>
      
      <div style={{ marginTop: '16px', color: 'white', fontSize: '36px', fontWeight: 700 }}>Cognify</div>
      <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px' }}>Mental Health &amp; Burnout Detection</div>
      
      <div className="white-card-lg" style={{ marginTop: '32px', width: '90%', maxWidth: '500px', padding: '28px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ fontSize: '32px', fontWeight: 700, color: '#1E293B' }}>Create Account</div>
        
        {error && <div style={{ color: 'red', fontSize: '14px', marginTop: '8px', textAlign: 'center' }}>{error}</div>}
        
        <form onSubmit={handleSignUp} style={{ width: '100%', marginTop: '32px' }}>
          
          {/* Full Name */}
          <div style={{ width: '100%', marginBottom: '24px' }}>
            <div style={{ fontSize: '14px', fontWeight: 700, color: '#475569', marginBottom: '8px' }}>Full Name</div>
            <div style={{ display: 'flex', alignItems: 'center', border: '1px solid #E2E8F0', borderRadius: '12px', padding: '12px' }}>
              <PersonIcon style={{ color: '#94A3B8', fontSize: '20px', marginRight: '8px' }} />
              <input 
                type="text" 
                value={name} 
                onChange={e => setName(e.target.value)} 
                placeholder="John Doe"
                style={{ border: 'none', outline: 'none', width: '100%', fontSize: '16px', color: '#1E293B' }}
              />
            </div>
          </div>

          {/* Email */}
          <div style={{ width: '100%', marginBottom: '24px' }}>
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
          
          {/* Password with strength indicator */}
          <div style={{ width: '100%', marginBottom: '8px' }}>
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
                {passwordVisible
                  ? <VisibilityIcon style={{ color: '#94A3B8', fontSize: '20px' }} />
                  : <VisibilityOffIcon style={{ color: '#94A3B8', fontSize: '20px' }} />
                }
              </div>
            </div>

            {/* Real-time strength indicator — only shown when user has started typing */}
            {password.length > 0 && (
              <div style={{ marginTop: '10px', padding: '10px 12px', background: '#F8FAFC', borderRadius: '10px', border: '1px solid #E2E8F0' }}>
                <div style={{ fontSize: '11px', fontWeight: 700, color: '#475569', marginBottom: '6px' }}>Password requirements:</div>
                <div style={reqStyle(hasMinLength)}>
                  {hasMinLength
                    ? <CheckCircleIcon style={{ fontSize: '13px' }} />
                    : <CancelIcon style={{ fontSize: '13px', color: '#EF4444' }} />}
                  8+ characters
                </div>
                <div style={reqStyle(hasUppercase)}>
                  {hasUppercase
                    ? <CheckCircleIcon style={{ fontSize: '13px' }} />
                    : <CancelIcon style={{ fontSize: '13px', color: '#EF4444' }} />}
                  Uppercase letter (A-Z)
                </div>
                <div style={reqStyle(hasLowercase)}>
                  {hasLowercase
                    ? <CheckCircleIcon style={{ fontSize: '13px' }} />
                    : <CancelIcon style={{ fontSize: '13px', color: '#EF4444' }} />}
                  Lowercase letter (a-z)
                </div>
                <div style={reqStyle(hasDigit)}>
                  {hasDigit
                    ? <CheckCircleIcon style={{ fontSize: '13px' }} />
                    : <CancelIcon style={{ fontSize: '13px', color: '#EF4444' }} />}
                  Number (0-9)
                </div>
                <div style={reqStyle(hasSpecial)}>
                  {hasSpecial
                    ? <CheckCircleIcon style={{ fontSize: '13px' }} />
                    : <CancelIcon style={{ fontSize: '13px', color: '#EF4444' }} />}
                  Special character (!@#$...)
                </div>
              </div>
            )}
          </div>
          
          <button type="submit" disabled={isLoading} className="btn-primary" style={{ width: '100%', height: '60px', marginTop: '24px', fontSize: '18px' }}>
            {isLoading ? 'Loading...' : 'Sign Up'}
          </button>
        </form>
      </div>
      
      <div style={{ marginTop: '32px', display: 'flex', paddingBottom: '48px' }}>
        <span style={{ color: 'rgba(255,255,255,0.8)' }}>Already have an account? </span>
        <span onClick={() => navigate('/login')} style={{ color: 'white', fontWeight: 700, marginLeft: '4px', cursor: 'pointer' }}>Sign In</span>
      </div>
    </div>
  );
}

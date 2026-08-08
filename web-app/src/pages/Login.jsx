import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PsychologyIcon from '@mui/icons-material/Psychology';
import MailRoundedIcon from '@mui/icons-material/MailRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { sendPasswordResetEmail, signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '../firebase';

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPasswordReset, setShowPasswordReset] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetMessage, setResetMessage] = useState('');
  const [resetError, setResetError] = useState('');
  const [isResetting, setIsResetting] = useState(false);

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

  const openPasswordReset = () => {
    setResetEmail(email);
    setResetMessage('');
    setResetError('');
    setShowPasswordReset(true);
  };

  const handlePasswordReset = async (event) => {
    event.preventDefault();
    const normalizedEmail = resetEmail.trim();
    if (!normalizedEmail) {
      setResetError('Enter the email address for your account.');
      return;
    }

    setIsResetting(true);
    setResetError('');
    try {
      await sendPasswordResetEmail(auth, normalizedEmail, {
        url: `${window.location.origin}/login`,
        handleCodeInApp: false,
      });
      setResetMessage('If an account exists for this address, a reset link has been sent. Check your inbox and spam folder.');
    } catch (err) {
      console.error('Password reset request failed', err);
      setResetError('We could not send the reset email. Please check the address and try again.');
    } finally {
      setIsResetting(false);
    }
  };

  return (
    <div className="login-bg" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minHeight: '100vh' }}>
      <div style={{ marginTop: '64px', width: '80px', height: '80px', background: 'rgba(255,255,255,0.2)', borderRadius: '24px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <PsychologyIcon style={{ color: 'white', fontSize: '40px' }} />
      </div>
      
      <div style={{ marginTop: '16px', color: 'white', fontSize: '36px', fontWeight: 700 }}>Cognify</div>
      <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px' }}>Mental Health & Burnout Detection</div>
      
      <div className="white-card-lg" style={{ marginTop: '32px', width: '90%', maxWidth: '500px', padding: '28px', display: 'flex', flexDirection: 'column', alignItems: 'center', backgroundColor: 'var(--bg-secondary)' }}>
        <div style={{ fontSize: '32px', fontWeight: 700, color: 'var(--text-primary)' }}>Welcome</div>
        
        {error && <div className="error-message" style={{ color: 'red', fontSize: '14px', marginTop: '8px' }}>{error}</div>}
        
        <form onSubmit={handleSignIn} style={{ width: '100%', marginTop: '32px' }}>
          <div style={{ width: '100%' }}>
            <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px' }}>Email Address</div>
            <div style={{ display: 'flex', alignItems: 'center', border: '1px solid var(--border-color)', borderRadius: '12px', padding: '12px', backgroundColor: 'var(--input-bg)' }}>
              <MailRoundedIcon style={{ color: 'var(--text-secondary)', fontSize: '20px', marginRight: '8px' }} />
              <input 
                type="email" 
                value={email} 
                onChange={e => setEmail(e.target.value)} 
                placeholder="student@example.com"
                style={{ border: 'none', outline: 'none', width: '100%', fontSize: '16px', color: 'var(--text-primary)', backgroundColor: 'transparent' }}
              />
            </div>
          </div>
          
          <div style={{ width: '100%', marginTop: '24px' }}>
            <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px' }}>Password</div>
            <div style={{ display: 'flex', alignItems: 'center', border: '1px solid var(--border-color)', borderRadius: '12px', padding: '12px', backgroundColor: 'var(--input-bg)' }}>
              <LockRoundedIcon style={{ color: 'var(--text-secondary)', fontSize: '20px', marginRight: '8px' }} />
              <input 
                type={passwordVisible ? "text" : "password"} 
                value={password} 
                onChange={e => setPassword(e.target.value)} 
                placeholder="********"
                style={{ border: 'none', outline: 'none', width: '100%', fontSize: '16px', color: 'var(--text-primary)', backgroundColor: 'transparent' }}
              />
              <div onClick={() => setPasswordVisible(!passwordVisible)} style={{ cursor: 'pointer', display: 'flex' }}>
                {passwordVisible ? <VisibilityIcon style={{ color: 'var(--text-secondary)', fontSize: '20px' }} /> : <VisibilityOffIcon style={{ color: 'var(--text-secondary)', fontSize: '20px' }} />}
              </div>
            </div>
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
            <button
              type="button"
              onClick={openPasswordReset}
              style={{ color: '#9333EA', fontWeight: 700, fontSize: '14px', cursor: 'pointer' }}
            >
              Forgot Password?
            </button>
          </div>
          
          <button type="submit" disabled={isLoading} className="btn-primary" style={{ width: '100%', height: '60px', marginTop: '24px', fontSize: '18px' }}>
            {isLoading ? 'Loading...' : 'Sign In'}
          </button>
        </form>
        
        <div style={{ display: 'flex', alignItems: 'center', width: '100%', marginTop: '32px' }}>
          <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--border-color)' }}></div>
          <div style={{ margin: '0 16px', color: 'var(--text-secondary)', fontSize: '12px', fontWeight: 700 }}>OR CONTINUE WITH</div>
          <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--border-color)' }}></div>
        </div>
        
        <div style={{ display: 'flex', width: '100%', gap: '16px', marginTop: '24px' }}>
          <button 
            type="button"
            onClick={async () => {
              try {
                const { GoogleAuthProvider, signInWithPopup } = await import('firebase/auth');
                const provider = new GoogleAuthProvider();
                provider.setCustomParameters({ prompt: 'consent select_account' });
                await signInWithPopup(auth, provider);
                navigate('/dashboard');
              } catch (err) {
                setError(err.message);
              }
            }}
            style={{ flex: 1, height: '56px', border: '1px solid var(--border-color)', borderRadius: '12px', display: 'flex', justifyContent: 'center', alignItems: 'center', fontWeight: 700, color: 'var(--text-primary)', backgroundColor: 'transparent' }}
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

      {showPasswordReset && (
        <div
          role="presentation"
          onMouseDown={() => setShowPasswordReset(false)}
          style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px', background: 'rgba(15, 23, 42, 0.58)' }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="password-reset-title"
            onMouseDown={(event) => event.stopPropagation()}
            className="white-card"
            style={{ width: '100%', maxWidth: '440px', padding: '28px', background: 'var(--bg-secondary)' }}
          >
            <div id="password-reset-title" style={{ color: 'var(--text-primary)', fontSize: '24px', fontWeight: 800 }}>Reset your password</div>
            <p style={{ marginTop: '8px', color: 'var(--text-secondary)', fontSize: '14px', lineHeight: '20px' }}>
              Enter your email and we’ll send you a secure password-reset link.
            </p>

            {resetMessage ? (
              <div style={{ marginTop: '20px', padding: '12px', borderRadius: '12px', color: '#047857', background: '#D1FAE5', fontSize: '14px', lineHeight: '20px' }}>
                {resetMessage}
              </div>
            ) : (
              <form onSubmit={handlePasswordReset} style={{ marginTop: '24px' }}>
                <label htmlFor="reset-email" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '14px', fontWeight: 700, marginBottom: '8px' }}>Email address</label>
                <input
                  id="reset-email"
                  type="email"
                  value={resetEmail}
                  onChange={(event) => setResetEmail(event.target.value)}
                  autoComplete="email"
                  placeholder="student@example.com"
                  style={{ width: '100%', border: '1px solid var(--border-color)', borderRadius: '12px', padding: '14px', fontSize: '16px', color: 'var(--text-primary)', background: 'var(--input-bg)' }}
                />
                {resetError && <div style={{ color: '#DC2626', fontSize: '13px', marginTop: '10px' }}>{resetError}</div>}
                <button type="submit" disabled={isResetting} className="btn-primary" style={{ width: '100%', height: '52px', marginTop: '20px', fontSize: '16px' }}>
                  {isResetting ? 'Sending link...' : 'Send reset link'}
                </button>
              </form>
            )}

            <button type="button" onClick={() => setShowPasswordReset(false)} style={{ width: '100%', marginTop: '16px', color: '#6366F1', fontWeight: 700, fontSize: '14px' }}>
              {resetMessage ? 'Back to sign in' : 'Cancel'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

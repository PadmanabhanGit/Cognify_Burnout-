import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api from '../services/api';

export default function SleepMoodLogger() {
  const navigate = useNavigate();
  const [sleepHours, setSleepHours] = useState(8);
  const [moodScore, setMoodScore] = useState(8);
  const [loading, setLoading] = useState(false);

  const handleSave = async () => {
    setLoading(true);
    try {
      await api.post('/api/sleep-mood/log', {
        sleepDuration: sleepHours,
        sleepQuality: Math.max(1, Math.min(10, Math.round((sleepHours / 8) * 10))),
        mood: moodScore > 7 ? 'Happy' : 'Neutral',
        moodScore: moodScore,
        notes: 'Logged from Web App'
      });
      alert('Logged successfully!');
      navigate('/sleep');
    } catch (err) {
      console.error(err);
      alert('Failed to log');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ paddingBottom: '20px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      <div style={{ padding: '24px', backgroundColor: 'white', display: 'flex', alignItems: 'center', borderBottom: '1px solid #E5E7EB' }}>
        <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/sleep')}>
          <ArrowBackIcon style={{ color: '#1F2937', marginRight: '16px' }} />
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937' }}>Log Sleep & Mood</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '24px' }}>
        <div className="white-card" style={{ padding: '24px', marginBottom: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#1F2937' }}>Sleep Duration (Hours)</div>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <input 
              type="range" 
              min="0" max="14" step="0.5" 
              value={sleepHours} 
              onChange={e => setSleepHours(Number(e.target.value))}
              style={{ flex: 1, marginRight: '16px' }}
            />
            <div style={{ fontSize: '24px', fontWeight: 800, color: '#6366F1' }}>{sleepHours}h</div>
          </div>
        </div>

        <div className="white-card" style={{ padding: '24px', marginBottom: '32px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#1F2937' }}>Mood Score (1-10)</div>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <input 
              type="range" 
              min="1" max="10" step="1" 
              value={moodScore} 
              onChange={e => setMoodScore(Number(e.target.value))}
              style={{ flex: 1, marginRight: '16px' }}
            />
            <div style={{ fontSize: '24px', fontWeight: 800, color: '#10B981' }}>{moodScore}</div>
          </div>
        </div>

        <button 
          onClick={handleSave} 
          disabled={loading}
          className="btn-primary" 
          style={{ width: '100%', padding: '16px', fontSize: '18px' }}
        >
          {loading ? 'Saving...' : 'Save Log'}
        </button>
      </div>
    </div>
  );
}

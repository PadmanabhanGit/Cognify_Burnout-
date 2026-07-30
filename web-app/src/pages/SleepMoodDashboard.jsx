import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AddIcon from '@mui/icons-material/Add';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import api from '../services/api';

export default function SleepMoodDashboard() {
  const navigate = useNavigate();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const res = await api.get('/api/sleep-mood/logs');
        if (res.data.success) {
          setLogs(res.data.logs);
        }
      } catch (err) {
        console.error("Failed to load sleep logs", err);
      } finally {
        setLoading(false);
      }
    };
    fetchLogs();
  }, []);

  return (
    <div style={{ paddingBottom: '20px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      <div style={{ padding: '24px', backgroundColor: 'white', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid #E5E7EB' }}>
        <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          <ArrowBackIcon style={{ color: '#1F2937', marginRight: '16px' }} />
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937' }}>Sleep & Mood</div>
        </div>
        <button onClick={() => navigate('/sleep/analytics')} style={{ backgroundColor: '#EEF2FF', padding: '8px', borderRadius: '8px' }}>
          <TrendingUpIcon style={{ color: '#6366F1' }} />
        </button>
      </div>

      <div className="desktop-padding" style={{ padding: '24px' }}>
        <div style={{ fontSize: '22px', fontWeight: 800, color: '#1F2937', marginBottom: '16px' }}>Recent Logs</div>
        
        {loading ? (
          <div>Loading logs...</div>
        ) : logs.length === 0 ? (
          <div style={{ color: '#6B7280' }}>No sleep logs recorded yet.</div>
        ) : (
          <div className="responsive-grid">
            {logs.map(log => (
              <div key={log.id} className="white-card" style={{ padding: '16px', marginBottom: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <div style={{ fontWeight: 700, color: '#1F2937' }}>{new Date(log.date).toLocaleDateString()}</div>
                  <div style={{ color: '#10B981', fontWeight: 700 }}>Mood: {log.moodScore}/10</div>
                </div>
                <div style={{ color: '#4B5563' }}>Sleep Duration: {log.sleepDuration} hours</div>
                <div style={{ color: '#6B7280', fontSize: '12px', marginTop: '4px' }}>Quality: {log.sleepQuality}</div>
              </div>
            ))}
          </div>
        )}
        
        <button 
          onClick={() => navigate('/sleep/log')} 
          className="btn-primary" 
          style={{ position: 'fixed', bottom: '80px', right: '24px', width: '56px', height: '56px', borderRadius: '28px', display: 'flex', justifyContent: 'center', alignItems: 'center', boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)' }}
        >
          <AddIcon style={{ color: 'white' }} />
        </button>
      </div>
    </div>
  );
}

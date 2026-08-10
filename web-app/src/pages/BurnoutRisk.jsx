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
import {
  Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, ResponsiveContainer
} from 'recharts';
import api from '../services/api';
import BottomNavigation from '../components/BottomNavigation';

// Axis order matches the Android WellbeingAnalysisCard exactly.
const WELLBEING_AXES = [
  { key: 'focus', label: 'Focus' },
  { key: 'stress', label: 'Stress' },
  { key: 'mood', label: 'Mood' },
  { key: 'energy', label: 'Energy' },
  { key: 'sleep', label: 'Sleep' },
  { key: 'study', label: 'Study' },
];

export default function BurnoutRisk() {
  const navigate = useNavigate();
  const [assessment, setAssessment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Reads the assessment Android persisted to Firestore. The Web never
        // computes a burnout score of its own.
        const res = await api.get('/api/burnout/assessment');
        if (res.data.success) {
          setAssessment(res.data.available ? res.data.assessment : null);
          setLastSyncedAt(Date.now());
          setError(false);
        } else {
          setError(true);
        }
      } catch (err) {
        console.error('Failed to load burnout assessment', err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // No fallbacks. If Android has not synced an assessment, every value stays null
  // and the page renders an explicit unavailable state instead of inventing one.
  const available = !error && assessment !== null;
  const riskScore = available ? Number(assessment.riskScore) : null;
  const riskLevel = available && assessment.riskLevel
    ? String(assessment.riskLevel).toUpperCase()
    : null;
  const factors = available ? (assessment.factors || []) : [];
  const warnings = available ? (assessment.warnings || []) : [];
  const wellbeing = available ? assessment.wellbeing : null;

  const radarData = wellbeing
    ? WELLBEING_AXES.map(({ key, label }) => ({
        axis: label,
        value: Number(wellbeing[key]),
      })).filter(d => Number.isFinite(d.value))
    : [];

  const accent = riskScore === null ? '#9CA3AF'
    : riskScore > 75 ? '#EF4444'
    : riskScore > 40 ? '#F97316'
    : '#10B981';

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        Loading...
      </div>
    );
  }

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
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '4px' }}>ML-powered mental fatigue prediction</div>
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
              {riskScore !== null && (
                <circle cx="75" cy="75" r="65" fill="none" stroke={accent} strokeWidth="12" strokeDasharray="408" strokeDashoffset={408 - ((riskScore / 100) * 288)} strokeLinecap="round" />
              )}
            </svg>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ fontSize: '36px', fontWeight: 800, color: riskScore === null ? '#9CA3AF' : '#111827' }}>
                {riskScore === null ? '--' : `${riskScore}%`}
              </div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: accent }}>
                {riskLevel ?? 'UNAVAILABLE'}
              </div>
            </div>
          </div>

          <div style={{ backgroundColor: available ? '#FFF7ED' : '#F3F4F6', borderRadius: '16px', padding: '16px', width: '100%', marginTop: '30px', display: 'flex', alignItems: 'flex-start' }}>
            <AssignmentIcon style={{ color: available ? '#9A3412' : '#6B7280', fontSize: '20px', marginRight: '12px', marginTop: '2px' }} />
            <div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: available ? '#9A3412' : '#6B7280' }}>Assessment</div>
              <div style={{ fontSize: '12px', color: available ? 'rgba(154,52,18,0.8)' : '#6B7280', marginTop: '4px' }}>
                {available
                  ? (assessment.assessment ?? 'No assessment text was recorded for this reading.')
                  : 'Assessment unavailable — open the Android app to sync your latest burnout reading.'}
              </div>
            </div>
          </div>
        </div>

        {/* Warning Indicators */}
        <div style={{ background: 'linear-gradient(to bottom, #F97316, #EA580C)', borderRadius: '24px', padding: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
            <NotificationsIcon style={{ color: 'white', fontSize: '20px', marginRight: '12px' }} />
            <div style={{ fontSize: '16px', fontWeight: 700, color: 'white' }}>Warning Indicators</div>
          </div>
          {warnings.length > 0 ? (
            warnings.map((warning, index) => (
              <WarningItem key={index} text={warning} />
            ))
          ) : (
            <div style={{ color: 'white', fontSize: '13px', fontWeight: 500, padding: '12px' }}>
              {available ? 'No warnings' : 'Unavailable until Android syncs'}
            </div>
          )}
        </div>

        {/* Contributing Factors */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '20px' }}>Contributing Factors</div>

          {factors.length > 0 ? (
            factors.map((factor, index) => {
              let icon = <InfoIcon />;
              let color = factor.score > 60 ? "#EF4444" : "#3B82F6";
              if (factor.name.toLowerCase().includes('study')) icon = <MenuBookIcon />;
              else if (factor.name.toLowerCase().includes('sleep')) icon = <BedtimeIcon />;
              else if (factor.name.toLowerCase().includes('stress') || factor.name.toLowerCase().includes('burnout')) icon = <FavoriteIcon />;
              else if (factor.name.toLowerCase().includes('recovery') || factor.name.toLowerCase().includes('mood')) icon = <RestoreIcon />;

              return (
                <FactorItem key={index} name={factor.name} value={factor.score} color={color} icon={icon} />
              );
            })
          ) : (
            <div style={{ color: '#6B7280', fontSize: '14px', fontWeight: 500 }}>
              {available ? 'No contributing factors recorded' : 'Unavailable until Android syncs'}
            </div>
          )}
        </div>

        {/* Wellbeing Analysis — six axes, values persisted by Android */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937', marginBottom: '20px' }}>Wellbeing Analysis</div>

          {radarData.length === WELLBEING_AXES.length ? (
            <>
              <div style={{ width: '100%', height: '260px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart data={radarData} outerRadius="72%">
                    <PolarGrid stroke="#E5E7EB" />
                    <PolarAngleAxis dataKey="axis" tick={{ fontSize: 12, fill: '#4B5563', fontWeight: 600 }} />
                    <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
                    <Radar
                      name="Wellbeing"
                      dataKey="value"
                      stroke="#F97316"
                      strokeWidth={2}
                      fill="#F97316"
                      fillOpacity={0.25}
                    />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '8px', justifyContent: 'center' }}>
                {radarData.map(d => (
                  <div key={d.axis} style={{ fontSize: '12px', color: '#4B5563', background: '#F9FAFB', border: '1px solid #E5E7EB', borderRadius: '999px', padding: '4px 10px' }}>
                    {d.axis} <strong style={{ color: '#111827' }}>{d.value}%</strong>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div style={{ color: '#6B7280', fontSize: '14px', fontWeight: 500 }}>
              {available
                ? 'Wellbeing data was not recorded for this reading.'
                : 'Unavailable until Android syncs'}
            </div>
          )}
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

      <div style={{ textAlign: 'center', marginTop: '8px', marginBottom: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
        {error ? '⚠️ Unable to sync data. Retry.' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
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

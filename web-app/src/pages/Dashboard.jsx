import React, { useEffect, useState } from 'react';
import PsychologyIcon from '@mui/icons-material/Psychology';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import BedtimeIcon from '@mui/icons-material/Bedtime';
import SentimentSatisfiedAltIcon from '@mui/icons-material/SentimentSatisfiedAlt';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import BarChartIcon from '@mui/icons-material/BarChart';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import DescriptionIcon from '@mui/icons-material/Description';

import FeatureCard from '../components/FeatureCard';
import SummaryCard from '../components/SummaryCard';
import BurnoutAlertBox from '../components/BurnoutAlertBox';
import BottomNavigation from '../components/BottomNavigation';
import api from '../services/api';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  const firstName = dashboardData?.user?.firstName || auth.currentUser?.email?.split('@')[0] || "Student";
  const currentDate = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });

  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged((currentUser) => {
      if (!currentUser) {
        setDashboardData(null);
        setLoading(false);
        return;
      }

      const fetchDashboard = async () => {
        try {
          const response = await api.get('/api/dashboard');
          if (response.data.success) {
            setDashboardData(response.data.dashboard);
            setLastSyncedAt(Date.now());
            setError(false);
          } else {
            setError(true);
          }
        } catch (err) {
          console.error('Failed to load dashboard data', err);
          setError(true);
          setDashboardData(null);
        } finally {
          setLoading(false);
        }
      };

      fetchDashboard();
    });

    return () => unsubscribe();
  }, []);


  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Loading...</div>;
  }

  // Map data exactly from the backend response, providing safe fallbacks
  const alertScore = dashboardData?.burnoutAlert?.riskScore ?? 0;
  const alertLevel = dashboardData?.burnoutAlert?.riskLevel ?? 'Low';
  
  const formatDuration = (seconds) => {
    const safeSeconds = Math.max(0, Math.floor(Number(seconds) || 0));
    const h = Math.floor(safeSeconds / 3600);
    const m = Math.floor((safeSeconds % 3600) / 60);
    const s = safeSeconds % 60;
    const parts = [];
    if (h > 0) parts.push(`${h}h`);
    if (m > 0) parts.push(`${m}m`);
    if (s > 0 || parts.length === 0) parts.push(`${s}s`);
    return parts.join(' ');
  };

  const quickStats = dashboardData?.quickStats ?? {};

  // IMPORTANT: The backend already includes active-session elapsed in todayStudySeconds
  // (computed at fetch time in dashboard.js). The Web must NOT add local elapsed on top,
  // because:
  //   1. The Dashboard has no way to know if the session is still running after the fetch.
  //   2. Android may have stopped the session seconds after the fetch.
  //   3. Adding (currentTime - lastSyncedAt) causes the displayed value to keep growing
  //      indefinitely even after Android has stopped — the exact bug reported.
  //
  // Rule: Dashboard always displays the backend snapshot value as-is.
  // The value updates only when the page re-mounts / re-fetches.
  const todayStudySeconds = Math.max(0, Number(quickStats.todayStudySeconds ?? (quickStats.todayStudyMinutes || 0) * 60));
  const hasActiveStudySession = quickStats.hasActiveStudySession === true;
  const todayStudyDisplay = error ? '--' : formatDuration(todayStudySeconds);



  const appUsageSeconds = Number(quickStats.todayAppUsageSeconds ?? (quickStats.todayAppUsageMinutes || 0) * 60);
  const appUsageDisplay = error ? '--' : formatDuration(appUsageSeconds);
  const appUsageProgress = error ? 0 : Math.min(appUsageSeconds / (10 * 60 * 60), 1);
  const sleepDurationMinutes = Math.round(Number(quickStats.lastSleepHours ?? 0) * 60);
  const sleepDisplay = error ? '--' : formatDuration(sleepDurationMinutes * 60);
  const moodScore = Number(dashboardData?.quickStats?.lastMoodScore ?? 0);
  const sleepQuality = Number(quickStats.lastSleepQuality);
  const sleepProgress = Number.isFinite(sleepQuality)
    ? Math.min(sleepQuality > 10 ? sleepQuality / 100 : sleepQuality / 10, 1)
    : 0;
  const sleepStatus = sleepQuality >= 8 || moodScore >= 8 ? 'Excellent'
    : sleepQuality >= 6 || moodScore >= 6 ? 'Good'
    : quickStats.lastMood ? 'Needs care' : 'Log Today';
  const productivityScore = quickStats.lastProductivityScore;
  const moodEmoji = error ? '--' : (moodScore >= 7 ? '😊' : moodScore >= 4 ? '😐' : (moodScore > 0 ? '😔' : '--'));

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: 'var(--bg-primary)' }}>
      <div className="dashboard-header-bg" style={{ width: '100%' }}>
        <div className="desktop-padding" style={{ padding: '40px 24px 100px 24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ color: 'white', fontSize: '28px', fontWeight: 700 }}>Hello, {firstName}!</div>
              <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: '14px' }}>{currentDate}</div>
            </div>
            <div onClick={() => navigate('/profile')} style={{ width: '48px', height: '48px', borderRadius: '24px', backgroundColor: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', cursor: 'pointer' }}>
              <PsychologyIcon style={{ color: '#6B21A8', fontSize: '28px' }} />
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px', marginTop: '32px' }}>
            <div style={{ flex: 1, height: '110px', backgroundColor: 'rgba(255,255,255,0.12)', borderRadius: '20px', padding: '12px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', border: '1px solid rgba(255,255,255,0.2)' }}>
              <AccessTimeIcon style={{ color: 'white', fontSize: '20px', marginBottom: '8px' }} />
              <div style={{ color: 'white', fontSize: '20px', fontWeight: 700 }}>{todayStudyDisplay}</div>
              <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: '11px' }}>Study Today</div>
            </div>
            <div style={{ flex: 1, height: '110px', backgroundColor: 'rgba(255,255,255,0.12)', borderRadius: '20px', padding: '12px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', border: '1px solid rgba(255,255,255,0.2)' }}>
              <BedtimeIcon style={{ color: 'white', fontSize: '20px', marginBottom: '8px' }} />
              <div style={{ color: 'white', fontSize: '20px', fontWeight: 700 }}>{sleepDisplay}</div>
              <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: '11px' }}>Sleep</div>
            </div>
            <div style={{ flex: 1, height: '110px', backgroundColor: 'rgba(255,255,255,0.12)', borderRadius: '20px', padding: '12px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', border: '1px solid rgba(255,255,255,0.2)' }}>
              <SentimentSatisfiedAltIcon style={{ color: 'white', fontSize: '20px', marginBottom: '8px' }} />
              <div style={{ color: 'white', fontSize: '20px', fontWeight: 700 }}>{moodEmoji}</div>
              <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: '11px' }}>Mood</div>
            </div>
          </div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px' }}>
        <BurnoutAlertBox riskLevel={alertLevel} riskScore={alertScore} onClick={() => navigate('/burnout')} />

        <div style={{ fontSize: '22px', fontWeight: 800, color: 'var(--text-primary)', marginTop: '16px', marginBottom: '16px' }}>Features</div>

        <div className="responsive-grid">
          <FeatureCard 
            icon={MenuBookIcon} title="Study Tracking" subtitle="Daily goal progress" 
            trailing={todayStudyDisplay} progress={Math.min(todayStudySeconds / (8 * 60 * 60), 1)}
            color="#E0F2FE" iconColor="#0284C7" onClick={() => navigate('/study')} 
          />
          <FeatureCard 
            icon={BedtimeIcon} title="Sleep & Mood" subtitle="Wellness analysis" 
            trailing={sleepStatus} progress={sleepProgress}
            color="#EEF2FF" iconColor="#6366F1" onClick={() => navigate('/sleep')} 
          />
          <FeatureCard 
            icon={BarChartIcon} title="App Usage" subtitle="Leisure time impact" 
            trailing={appUsageSeconds > 0 ? appUsageDisplay : 'Today'} progress={appUsageProgress}
            color="#F5F3FF" iconColor="#8B5CF6" onClick={() => navigate('/usage')} 
          />
          <FeatureCard 
            icon={TrendingUpIcon} title="Productivity" subtitle="Weekly trends" 
            trailing={Number.isFinite(Number(productivityScore)) ? `${productivityScore}%` : 'View'} color="#DCFCE7" iconColor="#10B981" onClick={() => navigate('/productivity')}
          />
          <FeatureCard 
            icon={DescriptionIcon} title="Weekly Report" subtitle="Download PDF" 
            color="#FCE7F3" iconColor="#EC4899" onClick={() => navigate('/report')} 
          />
        </div>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
          {error ? '⚠️ Unable to sync data. Retry.' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
        </div>

      </div>

      <BottomNavigation />
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import PsychologyIcon from '@mui/icons-material/Psychology';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import BedtimeIcon from '@mui/icons-material/Bedtime';
import SentimentSatisfiedAltIcon from '@mui/icons-material/SentimentSatisfiedAlt';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import BarChartIcon from '@mui/icons-material/BarChart';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import DescriptionIcon from '@mui/icons-material/Description';
import TimelineIcon from '@mui/icons-material/Timeline';
import WarningIcon from '@mui/icons-material/Warning';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';

import FeatureCard from '../components/FeatureCard';
import SummaryCard from '../components/SummaryCard';
import BottomNavigation from '../components/BottomNavigation';
import api from '../services/api';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip } from 'recharts';

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

  // Burnout comes from the assessment Android persisted to Firestore. There is no
  // fallback: when `available` is false the alert renders an explicit unavailable
  // state rather than a substituted score (which previously read as a real "Low").
  const burnoutAlert = dashboardData?.burnoutAlert ?? null;
  const burnoutAvailable = !error && burnoutAlert?.available === true
    && Number.isFinite(Number(burnoutAlert?.riskScore));
  const alertScore = burnoutAvailable ? Number(burnoutAlert.riskScore) : null;
  const alertLevel = burnoutAvailable ? burnoutAlert.riskLevel : null;
  // Descriptive text comes from the persisted Android assessment — never derived
  // from riskScore thresholds on the Web.
  const alertAssessment = burnoutAvailable ? (burnoutAlert.assessment ?? null) : null;
  const alertTopWarning = burnoutAvailable ? (burnoutAlert.topWarning ?? null) : null;
  // Severity styling keys off the PERSISTED risk level string, not a local threshold.
  const alertLevelKey = String(alertLevel ?? '').trim().toLowerCase();
  const alertClass = alertLevelKey === 'high' || alertLevelKey === 'critical'
    ? 'burnout-alert-high'
    : alertLevelKey === 'moderate'
      ? 'burnout-alert-moderate'
      : 'burnout-alert-low';
  const AlertIcon = alertLevelKey === 'low' ? CheckCircleIcon : WarningIcon;

  const formatDuration = (seconds) => {
    const safeSeconds = Math.max(0, Math.floor(Number(seconds) || 0));
    if (safeSeconds < 3600) {
      const m = Math.floor(safeSeconds / 60);
      return `${m}m`;
    }
    const h = (safeSeconds / 3600).toFixed(1);
    return `${h}H`;
  };

  const formatProgressLabel = (seconds) => {
    return null;
  }

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
  // Same canonical detected-sleep record as the Sleep page (see
  // server/utils/sleepSelection.js). null means Android has not synced one —
  // show '--' rather than '0m', which reads as "slept zero minutes".
  const sleepHoursRaw = Number(quickStats.lastSleepHours);
  const sleepAvailable = !error && Number.isFinite(sleepHoursRaw);
  const sleepDurationMinutes = sleepAvailable ? Math.round(sleepHoursRaw * 60) : null;
  const sleepDisplay = sleepAvailable ? formatDuration(sleepDurationMinutes * 60) : '--';
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

  // Monthly trend mock data based on backend stats if available, else standard points
  const trendData = [
    { name: 'Week 1', value: 0.4 },
    { name: 'Week 2', value: 0.6 },
    { name: 'Week 3', value: 0.5 },
    { name: 'Week 4', value: (todayStudySeconds / (40 * 3600)).toFixed(2) }
  ];

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
        {burnoutAvailable ? (
          <div
            onClick={() => navigate('/burnout')}
            style={{ width: '100%', borderRadius: '24px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)', overflow: 'hidden', cursor: 'pointer', marginBottom: '16px' }}
          >
            <div className={alertClass} style={{ padding: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <div style={{ width: '40px', height: '40px', borderRadius: '12px', background: 'rgba(255,255,255,0.2)', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                  <AlertIcon style={{ color: 'white', fontSize: '22px' }} />
                </div>
                <div style={{ marginLeft: '16px', flex: 1 }}>
                  <div style={{ color: 'white', fontWeight: 700, fontSize: '18px' }}>Burnout Alert</div>
                  <div style={{ color: 'rgba(255,255,255,0.9)', fontSize: '12px' }}>
                    Risk Level: {alertLevel} · {alertScore}%
                  </div>
                </div>
                <KeyboardArrowRightIcon style={{ color: 'white' }} />
              </div>
              {alertAssessment && (
                <div style={{ marginTop: '20px', color: 'white', fontSize: '14px', lineHeight: '20px' }}>
                  {alertAssessment}
                </div>
              )}
              {alertTopWarning && (
                <div style={{ marginTop: '12px', background: 'rgba(255,255,255,0.2)', borderRadius: '12px', padding: '10px 12px', color: 'white', fontSize: '13px', fontWeight: 500 }}>
                  {alertTopWarning}
                </div>
              )}
            </div>
          </div>
        ) : (
          <div
            onClick={() => navigate('/burnout')}
            style={{ width: '100%', borderRadius: '24px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', overflow: 'hidden', cursor: 'pointer', marginBottom: '16px', background: '#E5E7EB', padding: '20px' }}
          >
            <div style={{ fontWeight: 700, fontSize: '18px', color: '#374151' }}>Burnout Alert</div>
            <div style={{ fontSize: '13px', color: '#6B7280', marginTop: '8px' }}>
              Assessment unavailable — open the Android app to sync your latest burnout reading.
            </div>
          </div>
        )}

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

        {/* Monthly Trend Section */}
        <div style={{ marginTop: '24px', marginBottom: '16px' }}>
          <div className="white-card" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '20px' }}>
              <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '8px', borderRadius: '12px', display: 'flex', marginRight: '12px' }}>
                <TimelineIcon style={{ color: '#8B5CF6', fontSize: '20px' }} />
              </div>
              <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Monthly Trend</div>
            </div>

            <div style={{ width: '100%', height: '200px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trendData}>
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#64748b' }} />
                  <Tooltip
                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                    itemStyle={{ fontSize: '12px', fontWeight: 700 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="value"
                    stroke="#8B5CF6"
                    strokeWidth={3}
                    dot={{ r: 6, fill: '#fff', stroke: '#8B5CF6', strokeWidth: 2 }}
                    activeDot={{ r: 8, fill: '#8B5CF6', stroke: '#fff', strokeWidth: 2 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '12px', color: error ? '#EF4444' : 'var(--text-secondary)' }}>
          {error ? '⚠️ Unable to sync data. Retry.' : (lastSyncedAt ? `Synced just now (${new Date(lastSyncedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : 'Syncing...')}
        </div>

      </div>

      <BottomNavigation />
    </div>
  );
}

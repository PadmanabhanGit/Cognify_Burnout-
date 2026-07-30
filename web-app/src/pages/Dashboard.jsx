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
  const firstName = auth.currentUser?.email?.split('@')[0] || "Student";
  const currentDate = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await api.get('/api/dashboard');
        if (response.data.success) {
          setDashboardData(response.data.dashboard);
        }
      } catch (err) {
        console.error("Failed to load dashboard data", err);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Loading...</div>;
  }

  // Map data exactly from the backend response, providing safe fallbacks
  const alertScore = dashboardData?.burnoutAlert?.riskScore ?? 0;
  const alertLevel = dashboardData?.burnoutAlert?.riskLevel ?? 'Low';
  
  const studyHours = dashboardData?.quickStats?.weeklyStudyHours ?? 0;
  const sleepHours = dashboardData?.quickStats?.lastSleepHours ?? 0;
  const moodScore = dashboardData?.quickStats?.lastMoodScore ?? 5;
  const moodEmoji = moodScore >= 7 ? '😊' : moodScore >= 4 ? '😐' : '😔';

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh' }}>
      <div className="dashboard-header-bg" style={{ padding: '40px 24px 60px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ color: 'white', fontSize: '28px', fontWeight: 700 }}>Hello, {firstName}!</div>
            <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: '14px' }}>{currentDate}</div>
          </div>
          <div style={{ width: '48px', height: '48px', borderRadius: '24px', backgroundColor: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <PsychologyIcon style={{ color: '#6B21A8', fontSize: '28px' }} />
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px', marginTop: '32px' }}>
          <SummaryCard icon={AccessTimeIcon} value={`${studyHours}h`} label="Study Today" />
          <SummaryCard icon={BedtimeIcon} value={`${sleepHours}h`} label="Sleep" />
          <SummaryCard icon={SentimentSatisfiedAltIcon} value={moodEmoji} label="Mood" />
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-30px' }}>
        <BurnoutAlertBox riskLevel={alertLevel} riskScore={alertScore} onClick={() => alert('Navigating to Burnout Alert Details')} />

        <div style={{ fontSize: '22px', fontWeight: 800, color: '#1F2937', marginTop: '16px', marginBottom: '16px' }}>Features</div>

        <div className="responsive-grid">
          <FeatureCard 
            icon={MenuBookIcon} title="Study Tracking" subtitle="Daily goal progress" 
            trailing={`${studyHours}h`} progress={Math.min(studyHours / 8, 1)} 
            color="#E0F2FE" iconColor="#0284C7" onClick={() => navigate('/study')} 
          />
          <FeatureCard 
            icon={BedtimeIcon} title="Sleep & Mood" subtitle="Wellness analysis" 
            trailing="Log Today" progress={0.8} 
            color="#EEF2FF" iconColor="#6366F1" onClick={() => navigate('/sleep')} 
          />
          <FeatureCard 
            icon={BarChartIcon} title="App Usage" subtitle="Leisure time impact" 
            trailing="2.5h" progress={0.25} 
            color="#F5F3FF" iconColor="#8B5CF6" onClick={() => navigate('/usage')} 
          />
          <FeatureCard 
            icon={TrendingUpIcon} title="Productivity" subtitle="Weekly trends" 
            trailing="+12%" color="#DCFCE7" iconColor="#10B981" onClick={() => navigate('/productivity')} 
          />
          <FeatureCard 
            icon={DescriptionIcon} title="Weekly Report" subtitle="Download PDF" 
            color="#FCE7F3" iconColor="#EC4899" onClick={() => navigate('/report')} 
          />
        </div>
      </div>

      <BottomNavigation />
    </div>
  );
}

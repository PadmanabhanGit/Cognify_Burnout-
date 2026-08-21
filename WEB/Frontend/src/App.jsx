import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import SignUp from './pages/SignUp';
import Dashboard from './pages/Dashboard';
import { auth } from './firebase';

import StudyTracking from './pages/StudyTracking';
import WeeklyReport from './pages/WeeklyReport';
import BurnoutRisk from './pages/BurnoutRisk';
import ActionPlan from './pages/ActionPlan';
import SleepMoodDashboard from './pages/SleepMoodDashboard';
import SleepMoodLogger from './pages/SleepMoodLogger';
import SleepMoodAnalytics from './pages/SleepMoodAnalytics';
import AppUsage from './pages/AppUsage';
import Productivity from './pages/Productivity';
import Profile from './pages/Profile';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged((user) => {
      setUser(user);
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#8B5CF6' }}></div>;
  }

  return (
    <div className="app-container">
      <BrowserRouter basename={import.meta.env.BASE_URL}>
        <Routes>
          <Route path="/" element={user ? <Navigate to="/dashboard" /> : <Login />} />
          <Route path="/login" element={user ? <Navigate to="/dashboard" /> : <Login />} />
          <Route path="/signup" element={user ? <Navigate to="/dashboard" /> : <SignUp />} />
          <Route path="/dashboard" element={user ? <Dashboard /> : <Navigate to="/login" />} />
          <Route path="/study" element={user ? <StudyTracking /> : <Navigate to="/login" />} />
          <Route path="/sleep" element={user ? <SleepMoodDashboard /> : <Navigate to="/login" />} />
          <Route path="/sleep/log" element={user ? <SleepMoodLogger /> : <Navigate to="/login" />} />
          <Route path="/sleep/analytics" element={user ? <SleepMoodAnalytics /> : <Navigate to="/login" />} />
          <Route path="/usage" element={user ? <AppUsage /> : <Navigate to="/login" />} />
          <Route path="/productivity" element={user ? <Productivity /> : <Navigate to="/login" />} />
          <Route path="/report" element={user ? <WeeklyReport /> : <Navigate to="/login" />} />
          <Route path="/weekly-report" element={user ? <WeeklyReport /> : <Navigate to="/login" />} />
          <Route path="/burnout-risk" element={user ? <BurnoutRisk /> : <Navigate to="/login" />} />
          <Route path="/burnout" element={user ? <BurnoutRisk /> : <Navigate to="/login" />} />
          <Route path="/action-plan" element={user ? <ActionPlan /> : <Navigate to="/login" />} />
          <Route path="/profile" element={user ? <Profile /> : <Navigate to="/login" />} />
          <Route path="*" element={<Navigate to="/dashboard" />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;

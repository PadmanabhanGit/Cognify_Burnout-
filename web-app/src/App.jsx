import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import axios from 'axios';
import './App.css';

// Dashboard Component
const Dashboard = () => {
  const [data, setData] = useState(null);

  useEffect(() => {
    // Example call to your Node.js server
    // axios.get('http://localhost:5000/api/dashboard').then(res => setData(res.data));
  }, []);

  return (
    <div className="dashboard-container">
      <header className="glass-header">
        <h1>BurnOutTracker Dashboard</h1>
        <nav>
          <Link to="/analytics">Analytics</Link>
          <Link to="/profile">Profile</Link>
        </nav>
      </header>
      
      <main className="main-content">
        <section className="stats-card glass-panel fade-in">
          <h2>Daily Usage</h2>
          <p className="metric">4h 23m</p>
          <p className="subtitle">Screen time today</p>
        </section>
        
        <section className="stats-card glass-panel fade-in delay-1">
          <h2>Sleep Analytics</h2>
          <p className="metric">7h 15m</p>
          <p className="subtitle">Last night's rest</p>
        </section>
      </main>
    </div>
  );
};

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard />} />
      </Routes>
    </Router>
  );
}

export default App;

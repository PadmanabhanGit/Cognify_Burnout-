import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import api from '../services/api';
import { Radar } from 'react-chartjs-2';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  BarElement,
  CategoryScale,
  LinearScale,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar, Line } from 'react-chartjs-2';
import WarningIcon from '@mui/icons-material/Warning';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import BarChartIcon from '@mui/icons-material/BarChart';
import TimelineIcon from '@mui/icons-material/Timeline';
import DescriptionIcon from '@mui/icons-material/Description';

ChartJS.register(RadialLinearScale, PointElement, LineElement, BarElement, CategoryScale, LinearScale, Filler, Tooltip, Legend);

export default function WeeklyReport() {
  const navigate = useNavigate();
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(true);
  const reportRef = useRef(null);

  useEffect(() => {
    const fetchReport = async () => {
      try {
        const res = await api.get('/api/report/weekly');
        if (res.data.success) {
          setReportData(res.data.report);
        }
      } catch (err) {
        console.error("Failed to load report", err);
      } finally {
        setLoading(false);
      }
    };
    fetchReport();
  }, []);

  const downloadPDF = async () => {
    const element = reportRef.current;
    if (!element) return;
    
    try {
      const canvas = await html2canvas(element, { scale: 2 });
      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF('p', 'mm', 'a4');
      
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
      
      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
      pdf.save('BurnoutTracker_WeeklyReport.pdf');
    } catch (err) {
      console.error("Failed to generate PDF", err);
      alert('Failed to generate PDF');
    }
  };

  if (loading) return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Loading Report...</div>;

  const wr = reportData?.wellnessRadar || { sleep: 0, mood: 0, study: 0, productivity: 0, balance: 0 };
  
  const chartData = {
    labels: ['Physical', 'Mental', 'Social', 'Focus', 'Sleep'],
    datasets: [
      {
        label: 'This Week',
        data: [60, wr.mood * 10, 80, wr.productivity * 10, wr.sleep * 10], // scaled to 100
        backgroundColor: 'rgba(147, 51, 234, 0.2)',
        borderColor: '#9333EA',
        borderWidth: 2,
      },
    ],
  };

  const dates = Object.keys(reportData?.dailyActivity || {}).sort();
  const studyData = dates.map(d => (reportData.dailyActivity[d].studyMinutes / 60).toFixed(1));
  const sleepData = dates.map(d => reportData.dailyActivity[d].moodScore > 0 ? (reportData.summary.avgSleep).toFixed(1) : 0); // Approx sleep per day for chart

  const barChartData = {
    labels: dates.map(d => {
      const dt = new Date(d);
      return dt.toLocaleDateString('en-US', { weekday: 'short' });
    }),
    datasets: [
      {
        label: 'Study (hrs)',
        data: studyData,
        backgroundColor: '#3B82F6',
        borderRadius: 4,
      },
      {
        label: 'Sleep (hrs)',
        data: sleepData, // Normally we would have daily sleep, but we use avg as proxy if not available
        backgroundColor: '#A855F7',
        borderRadius: 4,
      }
    ]
  };

  const lineChartData = {
    labels: dates.map(d => {
      const dt = new Date(d);
      return dt.toLocaleDateString('en-US', { weekday: 'short' });
    }),
    datasets: [
      {
        label: 'Mood Score (1-10)',
        data: dates.map(d => reportData.dailyActivity[d].moodScore || 5),
        borderColor: '#EC4899',
        backgroundColor: '#EC4899',
        tension: 0.4,
      },
      {
        label: 'Productivity %',
        data: dates.map(d => (reportData.dailyActivity[d].productivityScore || 50) / 10),
        borderColor: '#10B981',
        backgroundColor: '#10B981',
        tension: 0.4,
      }
    ]
  };

  const riskScore = 100 - (reportData?.summary?.avgProductivity || 50);
  const riskLevel = riskScore > 70 ? 'High' : riskScore > 30 ? 'Moderate' : 'Low';
  const riskColor = riskLevel === 'High' ? '#EF4444' : riskLevel === 'Moderate' ? '#F97316' : '#10B981';

  return (
    <div style={{ paddingBottom: '90px', minHeight: '100vh', backgroundColor: 'var(--bg-primary)' }}>
      <div style={{ background: 'linear-gradient(to right, #9333EA, #DB2777)', borderBottomLeftRadius: '32px', borderBottomRightRadius: '32px', padding: '40px 24px 60px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div onClick={() => navigate('/dashboard')} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '36px', height: '36px' }}>
            <ArrowBackIcon style={{ color: 'white' }} />
          </div>
        </div>
        <div style={{ marginTop: '24px' }}>
          <div style={{ color: 'white', fontSize: '28px', fontWeight: 700 }}>Weekly Report</div>
          <div style={{ marginTop: '16px', display: 'flex', gap: '12px' }}>
            <button onClick={downloadPDF} style={{ display: 'flex', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.2)', color: 'white', padding: '8px 16px', borderRadius: '12px', fontWeight: 500, border: 'none' }}>
              <PictureAsPdfIcon style={{ fontSize: '18px', marginRight: '8px' }} /> Download PDF
            </button>
          </div>
        </div>
      </div>

      <div className="desktop-padding" ref={reportRef} style={{ padding: '0 24px', marginTop: '-30px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Executive Summary */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '20px' }}>
            <DescriptionIcon style={{ color: '#9333EA', marginRight: '8px' }} />
            <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Executive Summary</div>
          </div>
          
          <div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>TOTAL STUDY TIME</div>
              <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>{reportData?.summary?.totalStudyHours}h</div>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>AVG SLEEP</div>
              <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>{reportData?.summary?.avgSleep}h</div>
            </div>
          </div>
          
          <div style={{ display: 'flex', gap: '20px', marginBottom: '24px' }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>AVG MOOD</div>
              <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>{reportData?.summary?.avgMood}/10</div>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>PRODUCTIVITY</div>
              <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>{reportData?.summary?.avgProductivity}%</div>
            </div>
          </div>

          <div style={{ backgroundColor: `${riskColor}1A`, borderRadius: '16px', padding: '16px', display: 'flex', alignItems: 'center' }}>
            <WarningIcon style={{ color: riskColor, marginRight: '16px' }} />
            <div>
              <div style={{ fontSize: '11px', fontWeight: 700, color: riskColor }}>BURNOUT RISK LEVEL</div>
              <div style={{ fontSize: '18px', fontWeight: 800, color: riskColor }}>{riskLevel}</div>
              <div style={{ fontSize: '12px', color: riskColor, opacity: 0.8 }}>Based on your recent usage data.</div>
            </div>
          </div>
        </div>

        {/* Daily Activity Breakdown */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Daily Activity Breakdown</div>
            <BarChartIcon style={{ color: 'var(--text-secondary)' }} />
          </div>
          <div style={{ height: '200px' }}>
            <Bar data={barChartData} options={{ maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }} />
          </div>
        </div>

        {/* Mood & Productivity */}
        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>Mood & Productivity</div>
            <TimelineIcon style={{ color: 'var(--text-secondary)' }} />
          </div>
          <div style={{ height: '200px' }}>
            <Line data={lineChartData} options={{ maintainAspectRatio: false, scales: { y: { beginAtZero: true, max: 10 } } }} />
          </div>
        </div>

        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: 'var(--text-primary)' }}>Wellness Comparison</div>
          <div style={{ maxWidth: '400px', margin: '0 auto' }}>
            <Radar data={chartData} options={{ scales: { r: { min: 0, max: 100 } } }} />
          </div>
        </div>

        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#10B981', display: 'flex', alignItems: 'center' }}>
            <CheckCircleIcon style={{ marginRight: '8px' }}/> Achievements
          </div>
          <ul style={{ paddingLeft: '24px', color: 'var(--text-secondary)' }}>
            {reportData?.achievements?.map((ach, i) => <li key={i} style={{ marginBottom: '8px', fontWeight: 500 }}>{ach}</li>) || <li>No data</li>}
          </ul>
        </div>

        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#EF4444', display: 'flex', alignItems: 'center' }}>
            <WarningIcon style={{ marginRight: '8px' }}/> Areas of Concern
          </div>
          <ul style={{ paddingLeft: '24px', color: 'var(--text-secondary)' }}>
            {reportData?.concerns?.map((con, i) => <li key={i} style={{ marginBottom: '8px', fontWeight: 500 }}>{con}</li>) || <li>No data</li>}
          </ul>
        </div>

        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '12px', color: '#3B82F6' }}>Recommendations</div>
          <ul style={{ paddingLeft: '24px', color: 'var(--text-secondary)' }}>
            {reportData?.recommendations?.map((rec, i) => <li key={i} style={{ marginBottom: '8px', fontWeight: 500 }}>{rec}</li>) || <li>No data</li>}
          </ul>
        </div>
      </div>
    </div>
  );
}

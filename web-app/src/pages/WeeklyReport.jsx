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
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

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
    labels: ['Sleep', 'Mood', 'Study', 'Productivity', 'Balance'],
    datasets: [
      {
        label: 'Wellness Radar',
        data: [wr.sleep, wr.mood, wr.study, wr.productivity, wr.balance],
        backgroundColor: 'rgba(139, 92, 246, 0.2)',
        borderColor: 'rgba(139, 92, 246, 1)',
        borderWidth: 2,
      },
    ],
  };

  return (
    <div style={{ paddingBottom: '20px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      <div style={{ padding: '24px', backgroundColor: 'white', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid #E5E7EB' }}>
        <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          <ArrowBackIcon style={{ color: '#1F2937', marginRight: '16px' }} />
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#1F2937' }}>Weekly Report</div>
        </div>
        <button onClick={downloadPDF} style={{ display: 'flex', alignItems: 'center', backgroundColor: '#EC4899', color: 'white', padding: '8px 16px', borderRadius: '8px', fontWeight: 700 }}>
          <PictureAsPdfIcon style={{ fontSize: '18px', marginRight: '8px' }} /> PDF
        </button>
      </div>

      <div ref={reportRef} style={{ padding: '24px', backgroundColor: '#F9FAFB' }}>
        <div className="white-card" style={{ padding: '24px', marginBottom: '16px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#1F2937', textAlign: 'center' }}>Wellness Radar</div>
          <div style={{ maxWidth: '400px', margin: '0 auto' }}>
            <Radar data={chartData} options={{ scales: { r: { min: 0, max: 100 } } }} />
          </div>
        </div>

        <div className="white-card" style={{ padding: '24px', marginBottom: '16px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '12px', color: '#10B981' }}>Achievements</div>
          <ul style={{ paddingLeft: '20px', color: '#4B5563' }}>
            {reportData?.achievements?.map((ach, i) => <li key={i} style={{ marginBottom: '8px' }}>{ach}</li>) || <li>No data</li>}
          </ul>
        </div>

        <div className="white-card" style={{ padding: '24px', marginBottom: '16px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '12px', color: '#EF4444' }}>Concerns</div>
          <ul style={{ paddingLeft: '20px', color: '#4B5563' }}>
            {reportData?.concerns?.map((con, i) => <li key={i} style={{ marginBottom: '8px' }}>{con}</li>) || <li>No data</li>}
          </ul>
        </div>

        <div className="white-card" style={{ padding: '24px' }}>
          <div style={{ fontSize: '18px', fontWeight: 700, marginBottom: '12px', color: '#3B82F6' }}>Recommendations</div>
          <ul style={{ paddingLeft: '20px', color: '#4B5563' }}>
            {reportData?.recommendations?.map((rec, i) => <li key={i} style={{ marginBottom: '8px' }}>{rec}</li>) || <li>No data</li>}
          </ul>
        </div>
      </div>
    </div>
  );
}

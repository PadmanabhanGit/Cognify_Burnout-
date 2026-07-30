import React from 'react';

export default function SummaryCard({ icon: Icon, value, label }) {
  return (
    <div className="glass-card" style={{ flex: 1, height: '110px', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '12px' }}>
      <Icon style={{ color: 'white', fontSize: '20px' }} />
      <div style={{ marginTop: '8px', color: 'white', fontWeight: 700, fontSize: '20px' }}>{value}</div>
      <div style={{ color: 'rgba(255, 255, 255, 0.7)', fontSize: '11px', marginTop: '4px' }}>{label}</div>
    </div>
  );
}

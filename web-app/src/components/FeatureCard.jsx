import React from 'react';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';

export default function FeatureCard({ icon: Icon, title, subtitle, trailing, progress, progressLabel, color, iconColor, onClick }) {
  return (
    <div 
      className="white-card" 
      onClick={onClick} 
      style={{ display: 'flex', alignItems: 'center', padding: '16px', marginBottom: '16px', cursor: 'pointer' }}
    >
      <div 
        style={{ 
          width: '48px', height: '48px', borderRadius: '12px', 
          backgroundColor: color, display: 'flex', justifyContent: 'center', alignItems: 'center' 
        }}
      >
        <Icon style={{ color: iconColor }} />
      </div>
      
      <div style={{ marginLeft: '16px', flex: 1 }}>
        <div style={{ fontWeight: 700, fontSize: '16px', color: '#1F2937' }}>{title}</div>
        <div style={{ fontSize: '12px', color: '#6B7280' }}>{subtitle}</div>
        
        {progress !== undefined && (
          <div style={{ display: 'flex', alignItems: 'center', marginTop: '8px' }}>
            <div style={{ flex: 1, height: '4px', backgroundColor: `${iconColor}1A`, borderRadius: '2px' }}>
              <div style={{ width: `${progress * 100}%`, height: '100%', backgroundColor: iconColor, borderRadius: '2px' }} />
            </div>
            {progressLabel && (
              <div style={{ marginLeft: '8px', fontSize: '10px', fontWeight: 700, color: iconColor, whiteSpace: 'nowrap' }}>
                {progressLabel}
              </div>
            )}
          </div>
        )}
      </div>

      {trailing && (
        <div style={{ backgroundColor: `${iconColor}1A`, borderRadius: '8px', padding: '4px 8px', marginLeft: '16px' }}>
          <span style={{ fontSize: '12px', fontWeight: 700, color: iconColor }}>{trailing}</span>
        </div>
      )}

      <KeyboardArrowRightIcon style={{ color: '#D1D5DB', marginLeft: '8px' }} />
    </div>
  );
}

import React from 'react';
import WarningIcon from '@mui/icons-material/Warning';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';

export default function BurnoutAlertBox({ riskLevel, riskScore, onClick }) {
  let alertClass = 'burnout-alert-low';
  let message = 'Your mental balance is good! Maintain your current routine.';
  let Icon = CheckCircleIcon;

  if (riskScore > 75) {
    alertClass = 'burnout-alert-high';
    message = 'Immediate action required. Your burnout risk is critically high.';
    Icon = WarningIcon;
  } else if (riskScore > 40) {
    alertClass = 'burnout-alert-moderate';
    message = 'Your stress levels are elevated. Consider taking breaks and getting more sleep.';
    Icon = WarningIcon;
  }

  return (
    <div onClick={onClick} style={{ width: '100%', borderRadius: '24px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)', overflow: 'hidden', cursor: 'pointer', marginBottom: '16px' }}>
      <div className={alertClass} style={{ padding: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '12px', background: 'rgba(255,255,255,0.2)', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <Icon style={{ color: 'white', fontSize: '22px' }} />
          </div>
          <div style={{ marginLeft: '16px', flex: 1 }}>
            <div style={{ color: 'white', fontWeight: 700, fontSize: '18px' }}>Burnout Alert</div>
            <div style={{ color: 'rgba(255,255,255,0.9)', fontSize: '12px' }}>Risk Level: {riskLevel}</div>
          </div>
          <KeyboardArrowRightIcon style={{ color: 'white' }} />
        </div>
        <div style={{ marginTop: '20px', color: 'white', fontSize: '14px', lineHeight: '20px' }}>
          {message}
        </div>
      </div>
    </div>
  );
}

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TimerIcon from '@mui/icons-material/Timer';
import BlockIcon from '@mui/icons-material/Block';
import SpaIcon from '@mui/icons-material/Spa';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import BedtimeIcon from '@mui/icons-material/Bedtime';
import SelfImprovementIcon from '@mui/icons-material/SelfImprovement';
import WaterDropIcon from '@mui/icons-material/WaterDrop';

export default function ActionPlan() {
  const navigate = useNavigate();

  // State management for settings
  const [studyRemindersEnabled, setStudyRemindersEnabled] = useState(true);
  const [studyDuration, setStudyDuration] = useState("45 min");
  const [breakAlertsEnabled, setBreakAlertsEnabled] = useState(true);
  const [breakDuration, setBreakDuration] = useState("10 min");

  const [limitSocialEnabled, setLimitSocialEnabled] = useState(false);
  const [socialLimitMins, setSocialLimitMins] = useState(60);

  const [limitStreamingEnabled, setLimitStreamingEnabled] = useState(false);
  const [streamingLimitMins, setStreamingLimitMins] = useState(120);

  const [sleepReminderEnabled, setSleepReminderEnabled] = useState(true);
  const [mindfulnessEnabled, setMindfulnessEnabled] = useState(true);
  const [hydrationEnabled, setHydrationEnabled] = useState(true);

  const handleSave = () => {
    // Save to local storage or backend
    alert("Action plan saved successfully!");
    navigate('/burnout-risk');
  };

  return (
    <div style={{ paddingBottom: '70px', minHeight: '100vh', backgroundColor: '#F9FAFB' }}>
      
      {/* Header Section */}
      <div style={{ 
        width: '100%', 
        background: 'linear-gradient(to bottom, #10B981, #059669)', 
        borderBottomLeftRadius: '32px', 
        borderBottomRightRadius: '32px',
        padding: '40px 24px 60px 24px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <div onClick={() => navigate('/burnout-risk')} style={{ cursor: 'pointer', display: 'flex', justifyContent: 'center', alignItems: 'center', width: '36px', height: '36px' }}>
            <ArrowBackIcon style={{ color: 'white' }} />
          </div>
        </div>
        <div style={{ marginTop: '24px' }}>
          <div style={{ color: 'white', fontSize: '26px', fontWeight: 700, lineHeight: '32px' }}>Generalized Action<br />Plan</div>
          <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '14px', marginTop: '4px' }}>Set reminders and limits to stay balanced</div>
        </div>
      </div>

      <div className="desktop-padding" style={{ padding: '0 24px', marginTop: '-20px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Study & Break Notifications */}
        <ActionCard title="Study & Break Notifications" icon={<TimerIcon />} iconColor="#6366F1">
          <SettingToggleRow label="Enable Study Session Reminders" checked={studyRemindersEnabled} onChange={e => setStudyRemindersEnabled(e.target.checked)} />
          {studyRemindersEnabled && <DurationPicker label="STUDY DURATION" current={studyDuration} />}
          
          <div style={{ height: '16px' }}></div>
          
          <SettingToggleRow label="Enable Break Alerts" checked={breakAlertsEnabled} onChange={e => setBreakAlertsEnabled(e.target.checked)} />
          {breakAlertsEnabled && <DurationPicker label="BREAK DURATION" current={breakDuration} />}
        </ActionCard>

        {/* Entertainment Limits */}
        <ActionCard title="Entertainment Limits" icon={<BlockIcon />} iconColor="#EF4444">
          <div style={{ fontSize: '12px', color: 'gray', marginBottom: '16px' }}>Set daily boundaries for apps to maintain focus.</div>
          
          <LimitSlider label="Limit Social Media Apps" enabled={limitSocialEnabled} onToggle={e => setLimitSocialEnabled(e.target.checked)} value={socialLimitMins} max={120} onChange={e => setSocialLimitMins(e.target.value)} />
          <div style={{ height: '16px' }}></div>
          <LimitSlider label="Limit Streaming Apps" enabled={limitStreamingEnabled} onToggle={e => setLimitStreamingEnabled(e.target.checked)} value={streamingLimitMins} max={180} onChange={e => setStreamingLimitMins(e.target.value)} />
        </ActionCard>

        {/* Wellness Reminder */}
        <ActionCard title="Wellness Reminder" icon={<SpaIcon />} iconColor="#10B981">
          <WellnessItem label="Sleep Reminder" subtitle="10:00 PM" checked={sleepReminderEnabled} onChange={e => setSleepReminderEnabled(e.target.checked)} icon={<BedtimeIcon />} />
          <WellnessItem label="Mindfulness" subtitle="9:00 AM" checked={mindfulnessEnabled} onChange={e => setMindfulnessEnabled(e.target.checked)} icon={<SelfImprovementIcon />} />
          <WellnessItem label="Hydration" subtitle="Every 2 hours" checked={hydrationEnabled} onChange={e => setHydrationEnabled(e.target.checked)} icon={<WaterDropIcon />} />
        </ActionCard>

        {/* Save Button */}
        <div 
          onClick={handleSave}
          style={{ 
            background: 'linear-gradient(to right, #10B981, #3B82F6)', 
            borderRadius: '16px', 
            height: '60px', 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center',
            cursor: 'pointer',
            marginBottom: '40px'
          }}
        >
          <CheckCircleIcon style={{ color: 'white', fontSize: '20px', marginRight: '12px' }} />
          <div style={{ color: 'white', fontSize: '16px', fontWeight: 700 }}>Save Action Plan</div>
        </div>

      </div>
    </div>
  );
}

function ActionCard({ title, icon, iconColor, children }) {
  return (
    <div className="white-card" style={{ padding: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '20px' }}>
        <div style={{ width: '36px', height: '36px', borderRadius: '10px', backgroundColor: `${iconColor}1A`, display: 'flex', justifyContent: 'center', alignItems: 'center', marginRight: '12px' }}>
          {React.cloneElement(icon, { style: { color: iconColor, fontSize: '20px' }})}
        </div>
        <div style={{ fontSize: '16px', fontWeight: 700, color: '#1F2937' }}>{title}</div>
      </div>
      <div>{children}</div>
    </div>
  );
}

function SettingToggleRow({ label, checked, onChange }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', marginBottom: '8px' }}>
      <div style={{ fontSize: '14px', fontWeight: 500, color: '#374151' }}>{label}</div>
      <input type="checkbox" checked={checked} onChange={onChange} style={{ width: '18px', height: '18px', accentColor: '#10B981' }} />
    </div>
  );
}

function DurationPicker({ label, current }) {
  return (
    <div style={{ backgroundColor: '#F9FAFB', borderRadius: '12px', padding: '12px', marginTop: '12px', cursor: 'pointer' }}>
      <div style={{ fontSize: '10px', fontWeight: 700, color: 'gray', marginBottom: '4px' }}>{label}</div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontSize: '15px', fontWeight: 500 }}>{current}</div>
        <ArrowDropDownIcon />
      </div>
    </div>
  );
}

function LimitSlider({ label, enabled, onToggle, value, onChange, max }) {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontSize: '14px', fontWeight: 500, color: '#374151' }}>{label}</div>
        <input type="checkbox" checked={enabled} onChange={onToggle} style={{ width: '18px', height: '18px', accentColor: '#EF4444' }} />
      </div>
      {enabled && (
        <div style={{ marginTop: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <div style={{ fontSize: '11px', color: 'gray' }}>Daily Limit</div>
            <div style={{ fontSize: '11px', color: '#6366F1', fontWeight: 700 }}>{value} min</div>
          </div>
          <input type="range" min="0" max={max} value={value} onChange={onChange} style={{ width: '100%', accentColor: '#6366F1' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px' }}>
            <div style={{ fontSize: '10px', color: 'gray' }}>0m</div>
            <div style={{ fontSize: '10px', color: 'gray' }}>{max}m</div>
          </div>
        </div>
      )}
    </div>
  );
}

function WellnessItem({ label, subtitle, checked, onChange, icon }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', padding: '8px 0' }}>
      {React.cloneElement(icon, { style: { color: 'gray', fontSize: '18px', marginRight: '12px' }})}
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: '14px', fontWeight: 500, color: '#374151' }}>{label}</div>
        <div style={{ fontSize: '11px', color: '#10B981' }}>{subtitle}</div>
      </div>
      <input type="checkbox" checked={checked} onChange={onChange} style={{ width: '18px', height: '18px', accentColor: '#10B981' }} />
    </div>
  );
}

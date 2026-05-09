/* Primitives for CardClash UI kit. Theme tokens come via data-theme.   */
const { useState } = React;

const Logo = ({ size = 64 }) => (
  <svg viewBox="0 0 64 64" width={size} height={size} aria-hidden="true">
    <defs>
      <linearGradient id="lc-gold" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#E9C66A"/>
        <stop offset="100%" stopColor="#A87520"/>
      </linearGradient>
    </defs>
    <rect x="10" y="14" width="26" height="36" rx="4" fill="url(#lc-gold)" stroke="#7A4F12" strokeWidth="1.5" transform="rotate(-10 23 32)"/>
    <rect x="28" y="14" width="26" height="36" rx="4" fill="#1F4A2E" stroke="#0E2A1B" strokeWidth="1.5" transform="rotate(8 41 32)"/>
    <path d="M44 38 L44 26 M44 26 L40 30 M44 26 L48 30" stroke="#E9C66A" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" transform="rotate(8 41 32)"/>
  </svg>
);

const Phone = ({ children, landscape, theme }) => (
  <div className={`phone ${landscape ? 'landscape' : ''}`} data-theme={theme}>
    <div className="notch"></div>
    <div className="fake-status">
      <span>9:41</span>
      <span>•••• 5G</span>
    </div>
    {children}
  </div>
);

const Btn = ({ kind = 'primary', children, onClick, disabled, style }) => (
  <button className={`btn ${kind}`} onClick={onClick} disabled={disabled} style={style}>{children}</button>
);

const Field = ({ label, value, onChange, type = 'text', placeholder }) => (
  <label className="field">
    <span className="field-label">{label}</span>
    <input className="field-input" type={type} value={value || ''} onChange={(e)=>onChange?.(e.target.value)} placeholder={placeholder} />
  </label>
);

const TopBar = ({ room, blinds, onSettings }) => (
  <div className="topbar">
    <span className="room">ROOM {room}</span>
    <span>{blinds}</span>
    <span className="icons">
      <i data-lucide="circle-help" style={{width:18,height:18}}></i>
      <i data-lucide="info" style={{width:18,height:18}}></i>
      <i data-lucide="settings" style={{width:18,height:18}}></i>
    </span>
  </div>
);

const Suit = ({ s }) => {
  const path = {
    'S': 'M10 1 C12 6 18 9 18 13 C18 16 15 17 13 16 L13 18 H7 V16 C5 17 2 16 2 13 C2 9 8 6 10 1 Z',
    'H': 'M10 17 C7 14 1 11 1 6 C1 3 4 1 6 3 C7 4 8 5 10 7 C12 5 13 4 14 3 C16 1 19 3 19 6 C19 11 13 14 10 17 Z',
    'D': 'M10 1 L18 10 L10 19 L2 10 Z',
    'C': 'M10 2 A4 4 0 1 1 12 9 A4 4 0 1 1 16 14 A4 4 0 1 1 10 12 A4 4 0 1 1 4 14 A4 4 0 1 1 8 9 A4 4 0 1 1 10 2 Z M9 12 L8 18 H12 L11 12 Z'
  }[s];
  return <svg viewBox="0 0 20 20" width="1em" height="1em" fill="currentColor" aria-hidden="true"><path d={path}/></svg>;
};

const PCard = ({ rank, suit, selected, onClick }) => {
  const red = suit === 'H' || suit === 'D';
  const display = rank === 'T' ? '10' : rank;
  return (
    <div className={`pcard ${red ? 'red' : ''} ${selected ? 'selected' : ''}`} onClick={onClick}>
      <div style={{display:'flex',flexDirection:'column',alignItems:'center',lineHeight:1}}>
        <span>{display}</span>
        <span style={{fontSize:'0.9em'}}><Suit s={suit}/></span>
      </div>
      <div style={{alignSelf:'flex-end',transform:'rotate(180deg)',display:'flex',flexDirection:'column',alignItems:'center',lineHeight:1}}>
        <span>{display}</span>
        <span style={{fontSize:'0.9em'}}><Suit s={suit}/></span>
      </div>
    </div>
  );
};

const PlayerSlot = ({ name, chips, cards = 0, active, initials }) => (
  <div className={`player-slot ${active ? 'active' : ''}`}>
    <div className="avatar">{initials || name.slice(0,1).toUpperCase()}</div>
    <div className="meta">{name} · {cards}</div>
    <div className="mini-cards">
      {Array.from({length: Math.min(cards, 5)}).map((_,i) => <div key={i} className="mini-back"/>)}
    </div>
  </div>
);

Object.assign(window, { Logo, Phone, Btn, Field, TopBar, Suit, PCard, PlayerSlot });

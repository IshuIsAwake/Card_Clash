/* Five screens of CardClash. Game logic is mocked. */
const { useState: useStateS } = React;

const LoginScreen = ({ onLogin, onHotSeat }) => {
  const [email, setEmail] = useStateS('');
  const [pw, setPw] = useStateS('');
  return (
    <div className="screen">
      <div className="brand">
        <Logo size={72}/>
        <div className="name">CardClash</div>
        <div className="tagline">Pass · Scan · Play</div>
      </div>
      <div style={{display:'flex',flexDirection:'column',gap:14,marginTop:16}}>
        <Field label="Email" value={email} onChange={setEmail} placeholder="you@example.com"/>
        <Field label="Password" value={pw} onChange={setPw} type="password" placeholder="••••••••"/>
        <Btn kind="primary" onClick={onLogin}>Sign in to your table</Btn>
        <Btn kind="secondary" onClick={onHotSeat}>Hot Seat (Offline)</Btn>
      </div>
      <div className="footer-link">No account? <a href="#" onClick={(e)=>{e.preventDefault();onLogin();}}>Register</a></div>
    </div>
  );
};

const HomeScreen = ({ onCreate, onJoin, onHotSeat }) => (
  <div className="screen">
    <TopBar room="—" blinds="" />
    <div className="brand" style={{marginTop:8}}>
      <Logo size={48}/>
      <div className="name" style={{fontSize:24}}>CardClash</div>
    </div>
    <div className="section-title" style={{marginTop:16}}>Start a session</div>
    <div className="mode-list">
      <div className="mode-card" onClick={onCreate}>
        <div className="ic"><i data-lucide="qr-code" style={{width:22,height:22}}></i></div>
        <div className="body">
          <div className="title">Create Room</div>
          <div className="sub">Host a private table · share QR or 6-digit code</div>
        </div>
        <i data-lucide="chevron-right" style={{width:18,height:18}} className="arrow"></i>
      </div>
      <div className="mode-card" onClick={onJoin}>
        <div className="ic"><i data-lucide="users" style={{width:22,height:22}}></i></div>
        <div className="body">
          <div className="title">Join Room</div>
          <div className="sub">Scan QR or enter a 6-digit code</div>
        </div>
        <i data-lucide="chevron-right" style={{width:18,height:18}} className="arrow"></i>
      </div>
      <div className="mode-card" onClick={onHotSeat}>
        <div className="ic"><i data-lucide="circle" style={{width:22,height:22}}></i></div>
        <div className="body">
          <div className="title">Hot Seat (Offline)</div>
          <div className="sub">Single device · pass and play · no Firebase</div>
        </div>
        <i data-lucide="chevron-right" style={{width:18,height:18}} className="arrow"></i>
      </div>
    </div>
    <div className="spacer"></div>
    <div className="footer-link">Signed in as <a href="#">player@cardclash.app</a></div>
  </div>
);

const HotSeatSetupScreen = ({ onStart }) => {
  const [count, setCount] = useStateS(3);
  const [names, setNames] = useStateS(['Ishu','Ria','Ari','','']);
  const update = (i, v) => setNames(n => n.map((x,j) => j===i ? v : x));
  return (
    <div className="screen compact">
      <TopBar room="HOT SEAT" blinds="BLUFF · OPEN CALL"/>
      <div>
        <div className="section-title">BLUFF · HOT SEAT</div>
        <div style={{fontFamily:'var(--font-display)',fontSize:28,fontWeight:800,letterSpacing:'.01em',lineHeight:1.1}}>Single device.<br/>Pass and play.</div>
        <div style={{fontFamily:'var(--font-body)',fontSize:13,color:'var(--fg-2)',marginTop:8}}>No Firebase needed. Players take turns on this phone.</div>
      </div>
      <div>
        <div className="section-title">PLAYERS</div>
        <div className="count-pills">
          {[2,3,4,5].map(n => (
            <div key={n} className={`count-pill ${count===n?'active':''}`} onClick={()=>setCount(n)}>{n}</div>
          ))}
        </div>
      </div>
      <div>
        <div className="section-title">NAMES</div>
        <div className="player-rows">
          {Array.from({length: count}).map((_,i) => (
            <div className="player-row" key={i}>
              <span className="num">P{i+1}</span>
              <input value={names[i]||''} placeholder={`Player ${i+1}`} onChange={(e)=>update(i,e.target.value)}/>
            </div>
          ))}
        </div>
      </div>
      <div className="spacer"></div>
      <Btn kind="primary" onClick={()=>onStart(names.slice(0,count).map((n,i)=>n||`Player ${i+1}`))}>Deal · Pass to Player 1</Btn>
    </div>
  );
};

const PassGateScreen = ({ playerName, idx, onContinue }) => (
  <div className="screen no-pad">
    <div className="gate">
      <div className="eyebrow">PASS THE DEVICE</div>
      <div className="who">{playerName}<br/>— your turn</div>
      <div className="desc">Hand will appear after you continue. Hide the screen from other players first.</div>
      <Btn kind="primary" onClick={onContinue} style={{maxWidth:280}}>I'm {playerName} — Continue</Btn>
      <div className="footer-link" style={{marginTop:0}}>Player {idx + 1} of 3</div>
    </div>
  </div>
);

const HAND = [
  {rank:'K', suit:'S'}, {rank:'K', suit:'H'}, {rank:'9', suit:'D'},
  {rank:'7', suit:'C'}, {rank:'4', suit:'H'}, {rank:'2', suit:'S'},
  {rank:'J', suit:'D'},
];

const BluffTableScreen = ({ playerName, onBack }) => {
  const [sel, setSel] = useStateS(new Set());
  const toggle = (i) => setSel(s => { const n = new Set(s); n.has(i)?n.delete(i):n.add(i); return n; });
  return (
    <div className="screen no-pad" style={{padding:0}}>
      <div className="table-bg"></div>
      <div className="table-felt-rim"></div>
      <div className="table-content">
        <div className="opponents">
          <PlayerSlot name="Ria" chips={0} cards={17} initials="R"/>
          <PlayerSlot name="Ari" chips={0} cards={18} active initials="A"/>
        </div>
        <div className="center-pile">
          <div className="claim-chip">RIA CLAIMED · 3 KINGS</div>
          <div className="pile-stack">
            <div className="mini-back" style={{left:0,top:8,transform:'rotate(-6deg)'}}></div>
            <div className="mini-back" style={{left:8,top:4,transform:'rotate(3deg)'}}></div>
            <div className="mini-back" style={{left:16,top:0,transform:'rotate(-2deg)'}}></div>
          </div>
          <div style={{fontFamily:'var(--font-mono)',fontSize:11,color:'var(--fg-2)',letterSpacing:'.08em'}}>14 IN PILE · ROUND 3</div>
        </div>
        <div style={{display:'flex',flexDirection:'column',alignItems:'center',gap:6,paddingBottom:10}}>
          <div style={{fontFamily:'var(--font-heading)',fontSize:11,letterSpacing:'.16em',textTransform:'uppercase',color:'var(--accent)'}}>{playerName.toUpperCase()} — YOUR TURN · {sel.size} SEL</div>
          <div className="hand-strip">
            {HAND.map((c,i) => <PCard key={i} rank={c.rank} suit={c.suit} selected={sel.has(i)} onClick={()=>toggle(i)}/>)}
          </div>
        </div>
      </div>
      <div className="action-col">
        <Btn kind="primary" disabled={sel.size===0}>PLAY {sel.size||''}</Btn>
        <Btn kind="danger">CALL BLUFF</Btn>
        <Btn kind="secondary">PASS</Btn>
        <Btn kind="ghost" onClick={onBack}>EXIT</Btn>
      </div>
    </div>
  );
};

Object.assign(window, { LoginScreen, HomeScreen, HotSeatSetupScreen, PassGateScreen, BluffTableScreen });

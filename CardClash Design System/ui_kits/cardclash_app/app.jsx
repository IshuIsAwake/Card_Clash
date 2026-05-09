/* Tiny client-side router for the kit. */
const { useState: useStateA, useEffect: useEffectA } = React;

const SCREENS = ['login', 'home', 'setup', 'gate', 'table'];
const SCREEN_LABELS = { login: 'Login', home: 'Home', setup: 'Hot Seat Setup', gate: 'Pass Gate', table: 'Bluff Table' };
const THEMES = ['dev','royal-oak','neon-pulse','balatro'];
const THEME_LABELS = { 'dev':'Dev', 'royal-oak':'Royal Oak', 'neon-pulse':'Neon Pulse', 'balatro':'Balatro' };

function App() {
  const [screen, setScreen] = useStateA('login');
  const [theme, setTheme] = useStateA('royal-oak');
  const [players, setPlayers] = useStateA(['Ishu','Ria','Ari']);
  const [activeIdx, setActiveIdx] = useStateA(0);

  useEffectA(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [screen, theme]);

  const isLandscape = screen === 'table';
  const playerName = players[activeIdx] || 'Player 1';

  return (
    <>
      <div className="kit-toolbar" data-screen-label="Toolbar">
        <span style={{color:'#888',marginRight:4}}>SCREEN</span>
        {SCREENS.map(s => (
          <button key={s} className={screen===s?'active':''} onClick={()=>setScreen(s)}>{SCREEN_LABELS[s]}</button>
        ))}
        <div className="sep"></div>
        <span style={{color:'#888',marginRight:4}}>THEME</span>
        {THEMES.map(t => (
          <button key={t} className={theme===t?'active':''} onClick={()=>setTheme(t)}>{THEME_LABELS[t]}</button>
        ))}
      </div>
      <div data-screen-label={SCREEN_LABELS[screen]}>
        <Phone landscape={isLandscape} theme={theme}>
          {screen === 'login' && <LoginScreen onLogin={()=>setScreen('home')} onHotSeat={()=>setScreen('setup')}/>}
          {screen === 'home' && <HomeScreen onCreate={()=>setScreen('setup')} onJoin={()=>setScreen('setup')} onHotSeat={()=>setScreen('setup')}/>}
          {screen === 'setup' && <HotSeatSetupScreen onStart={(ps)=>{setPlayers(ps);setActiveIdx(0);setScreen('gate');}}/>}
          {screen === 'gate' && <PassGateScreen playerName={playerName} idx={activeIdx} onContinue={()=>setScreen('table')}/>}
          {screen === 'table' && <BluffTableScreen playerName={playerName} onBack={()=>setScreen('gate')}/>}
        </Phone>
      </div>
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);

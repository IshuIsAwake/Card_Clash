package com.example.cardclash.games.poker.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.hotseat.HotSeatConfig;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.games.poker.engine.PokerBotPolicy;
import com.example.cardclash.games.poker.engine.PokerEngine;
import com.example.cardclash.games.poker.engine.PokerHandRanker;
import com.example.cardclash.ui.common.CardView;
import com.example.cardclash.ui.common.ChipRackView;
import com.example.cardclash.ui.common.CollapsiblePanel;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.hotseat.PassTheDeviceActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PokerActivity extends ThemedActivity {

    public static final String EXTRA_ROOM_ID   = "room_id";
    public static final String EXTRA_LOCAL_UID = "local_uid";
    public static final String EXTRA_HOTSEAT   = "hotseat";
    private static final int   REQ_PASS = 5101;

    private PokerEngine engine;
    private String localUid = "you";
    private boolean hotSeat;
    private boolean pendingPass;
    private String pendingNextUid;

    private final Random botRng = new Random();
    private final Handler bg = new Handler(Looper.getMainLooper());

    private Theme theme;

    // -- views
    private LinearLayout root;
    private CollapsiblePanel leftDock;
    private LinearLayout opponentColumn;
    private LinearLayout communityRow;
    private LinearLayout potBlock, showdownBanner;
    private TextView roomCodeLabel, timerPill, phaseLabel, currentBetLabel, stackLabel;
    private TextView potAmount, potDetail;
    private TextView myName, myStrength;
    private LinearLayout myHoleRow;
    private ChipRackView myChipRack;
    private LinearLayout actionRow;
    private Button btnFold, btnCheckCall, btnRaise, btnAllIn, btnPostBlind, btnDealHand, btnHostMenu;

    // -- timer
    private CountDownTimer timer;
    private long timerSecs;

    // -- buy-in queue surfaced from engine events
    private final List<Map<String, Object>> pendingBuyInRequests = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyImmersive();
        theme = ThemePrefs.activeTheme(this);

        hotSeat = getIntent().getBooleanExtra(EXTRA_HOTSEAT, false) && HotSeatConfig.isActive();
        String roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        if (roomId == null) roomId = hotSeat ? "PASS-AND-PLAY" : "DEMO";

        List<Player> roster = new ArrayList<>();
        if (hotSeat) {
            roster.addAll(HotSeatConfig.get().players);
            localUid = roster.get(0).uid;
        } else {
            localUid = getIntent().getStringExtra(EXTRA_LOCAL_UID);
            if (localUid == null) localUid = "you";
            long buyIn = 5000;
            roster.add(new Player(localUid, "You", 0, buyIn, true));
            roster.add(new Player("bot1", "Aarav", 1, buyIn, false));
            roster.add(new Player("bot2", "Priya", 2, buyIn, false));
            roster.add(new Player("bot3", "Kabir", 3, buyIn, false));
        }

        GameDefinition def = GamesRegistry.get(GameType.POKER);
        RoomConfig cfg = new RoomConfig(GameType.POKER);
        def.ruleSchema.applyDefaults(cfg);
        if (hotSeat) {
            for (Map.Entry<String, Object> e : HotSeatConfig.get().ruleOverrides.entrySet())
                cfg.put(e.getKey(), e.getValue());
        }
        timerSecs = cfg.intVal("turn_timer", 0);

        buildUi(roomId);

        engine = (PokerEngine) def.engineFactory.create();
        engine.initialize(cfg, roster, System.currentTimeMillis());
        engine.addListener(new GameEngine.Listener() {
            @Override public void onStateChanged() {
                runOnUiThread(PokerActivity.this::render);
            }
            @Override public void onEvent(String kind, Map<String, Object> payload) {
                if (PokerEngine.EVENT_BUY_IN_REQ.equals(kind)) {
                    runOnUiThread(() -> pendingBuyInRequests.add(payload));
                }
            }
        });

        if (hotSeat) {
            launchPassGate(engine.currentTurnUid());
        } else {
            render();
            scheduleBotTick();
        }

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { confirmExit(); }
                });
    }

    // ====================================================================
    // UI BUILD
    // ====================================================================

    private void buildUi(String roomId) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundResource(theme.tableBg());
        setContentView(root);

        // ---- LEFT: slim collapsible opponents dock ----
        leftDock = new CollapsiblePanel(this);
        leftDock.setEdge(CollapsiblePanel.Edge.LEFT);
        leftDock.setHandleLabel("");
        ScrollView oppScroll = new ScrollView(this);
        oppScroll.setVerticalScrollBarEnabled(false);
        opponentColumn = new LinearLayout(this);
        opponentColumn.setOrientation(LinearLayout.VERTICAL);
        opponentColumn.setPadding(dp(6), dp(6), dp(6), dp(6));
        oppScroll.addView(opponentColumn,
                new ViewGroup.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        leftDock.addView(oppScroll,
                new ViewGroup.LayoutParams(dp(120), ViewGroup.LayoutParams.MATCH_PARENT));
        leftDock.setExpanded(true, false); // start expanded; user can collapse
        root.addView(leftDock, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));

        // ---- CENTER COLUMN ----
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        root.addView(center, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        // top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(10), dp(6), dp(10), dp(6));

        roomCodeLabel = topBarText("ROOM " + roomId, true);
        topBar.addView(roomCodeLabel);
        phaseLabel = topBarText("—", false);
        topBar.addView(infoChip("PHASE", phaseLabel));
        currentBetLabel = topBarText("0", false);
        topBar.addView(infoChip("BET", currentBetLabel));
        stackLabel = topBarText("0", false);
        topBar.addView(infoChip("STACK", stackLabel));

        topBar.addView(spacer(), new LinearLayout.LayoutParams(0, 1, 1f));

        timerPill = new TextView(this);
        timerPill.setLetterSpacing(0.18f);
        timerPill.setTextSize(11);
        timerPill.setTypeface(safeFont(theme.fontMono()), Typeface.BOLD);
        timerPill.setTextColor(theme.colorAccentOn());
        timerPill.setPadding(dp(8), dp(2), dp(8), dp(2));
        timerPill.setVisibility(View.GONE);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(dp(9999));
        pillBg.setColor(theme.colorAccent());
        timerPill.setBackground(pillBg);
        topBar.addView(timerPill, marginEnd(dp(8)));

        btnHostMenu = new Button(this);
        btnHostMenu.setText("MENU");
        styleSecondaryBtn(btnHostMenu);
        btnHostMenu.setOnClickListener(v -> showHostMenu());
        topBar.addView(btnHostMenu);

        center.addView(topBar);

        // FELT
        FrameLayout felt = new FrameLayout(this);
        center.addView(felt, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout feltStack = new LinearLayout(this);
        feltStack.setOrientation(LinearLayout.VERTICAL);
        feltStack.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        flp.gravity = Gravity.CENTER;
        felt.addView(feltStack, flp);

        communityRow = new LinearLayout(this);
        communityRow.setOrientation(LinearLayout.HORIZONTAL);
        communityRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < 5; i++) {
            CardView cv = new CardView(this);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(50), dp(72));
            clp.setMargins(dp(3), 0, dp(3), 0);
            communityRow.addView(cv, clp);
        }
        feltStack.addView(communityRow);

        potBlock = new LinearLayout(this);
        potBlock.setOrientation(LinearLayout.VERTICAL);
        potBlock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pbLp.topMargin = dp(8);
        feltStack.addView(potBlock, pbLp);

        TextView potEyebrow = new TextView(this);
        potEyebrow.setText("POT");
        potEyebrow.setLetterSpacing(0.30f);
        potEyebrow.setTextSize(10);
        potEyebrow.setTypeface(safeFont(theme.fontHeading()), Typeface.BOLD);
        potEyebrow.setTextColor(theme.colorFg2());
        potBlock.addView(potEyebrow, lpCenter());

        potAmount = new TextView(this);
        potAmount.setText("0");
        potAmount.setTextSize(26);
        potAmount.setIncludeFontPadding(true);
        potAmount.setTypeface(safeFont(theme.fontMono()), Typeface.BOLD);
        potAmount.setTextColor(theme.colorFg1());
        potAmount.setGravity(Gravity.CENTER);
        potAmount.setPadding(dp(20), dp(6), dp(20), dp(6));
        GradientDrawable potBg = new GradientDrawable();
        potBg.setShape(GradientDrawable.RECTANGLE);
        potBg.setCornerRadius(dp((int) theme.radiusSurfaceDp() + 4));
        potBg.setColor(theme.colorSurface());
        potBg.setStroke(dp(theme.borderWidthDp()), theme.colorAccent());
        potAmount.setBackground(potBg);
        potBlock.addView(potAmount, lpCenter());

        potDetail = new TextView(this);
        potDetail.setTextSize(11);
        potDetail.setLetterSpacing(0.14f);
        potDetail.setTextColor(theme.colorFg2());
        potDetail.setTypeface(safeFont(theme.fontMono()));
        LinearLayout.LayoutParams pdLp = lpCenter();
        pdLp.topMargin = dp(6);
        potBlock.addView(potDetail, pdLp);

        showdownBanner = new LinearLayout(this);
        showdownBanner.setOrientation(LinearLayout.VERTICAL);
        showdownBanner.setGravity(Gravity.CENTER);
        showdownBanner.setVisibility(View.GONE);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sbLp.topMargin = dp(10);
        feltStack.addView(showdownBanner, sbLp);

        // BOTTOM HAND STRIP — hole cards + identity
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(14), dp(4), dp(14), dp(4));
        center.addView(bottom);

        myHoleRow = new LinearLayout(this);
        myHoleRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 2; i++) {
            CardView cv = new CardView(this);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(54), dp(78));
            clp.setMargins(0, 0, dp(6), 0);
            myHoleRow.addView(cv, clp);
        }
        bottom.addView(myHoleRow);

        LinearLayout idCol = new LinearLayout(this);
        idCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        idLp.leftMargin = dp(12);
        bottom.addView(idCol, idLp);

        myName = new TextView(this);
        myName.setLetterSpacing(0.10f);
        myName.setTextSize(13);
        myName.setTypeface(safeFont(theme.fontHeading()), Typeface.BOLD);
        myName.setTextColor(theme.colorFg1());
        idCol.addView(myName);

        myStrength = new TextView(this);
        myStrength.setTextSize(11);
        myStrength.setTextColor(theme.colorFg3());
        myStrength.setTypeface(safeFont(theme.fontBody()));
        idCol.addView(myStrength);

        myChipRack = new ChipRackView(this);
        bottom.addView(myChipRack);

        // ACTION ROW (bottom)
        actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(dp(10), dp(2), dp(10), dp(8));
        center.addView(actionRow);

        btnFold = pillButton("FOLD", false, v -> onFold());
        btnCheckCall = pillButton("CHECK", true, v -> onCheckCall());
        btnRaise = pillButton("RAISE", true, v -> onRaisePressed());
        btnAllIn = pillButton("ALL-IN", false, v -> onAllIn());
        btnPostBlind = pillButton("POST BLIND", true, v -> onPostBlind());
        btnDealHand  = pillButton("DEAL HAND", true, v -> onDealHand());

        actionRow.addView(btnFold, btnRowLp());
        actionRow.addView(btnCheckCall, btnRowLp());
        actionRow.addView(btnRaise, btnRowLp());
        actionRow.addView(btnAllIn, btnRowLp());
        actionRow.addView(btnPostBlind, btnRowLp());
        actionRow.addView(btnDealHand, btnRowLp());

    }

    private LinearLayout infoChip(String eyebrowText, TextView valueView) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), 0, dp(8), 0);

        TextView e = new TextView(this);
        e.setText(eyebrowText);
        e.setLetterSpacing(0.20f);
        e.setTextSize(9);
        e.setTypeface(safeFont(theme.fontHeading()), Typeface.BOLD);
        e.setTextColor(theme.colorFg3());
        col.addView(e);

        col.addView(valueView);
        return col;
    }

    private TextView topBarText(String text, boolean primary) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setLetterSpacing(primary ? 0.18f : 0.08f);
        v.setTextSize(primary ? 13 : 12);
        v.setTypeface(safeFont(primary ? theme.fontHeading() : theme.fontMono()), Typeface.BOLD);
        v.setTextColor(primary ? theme.colorFg2() : theme.colorFg1());
        return v;
    }

    private View spacer() { return new View(this); }

    // ====================================================================
    // ACTION HANDLERS
    // ====================================================================

    private void onFold() {
        submitTurnAction(new Action(localUid, PokerEngine.ACTION_FOLD));
    }

    private void onCheckCall() {
        long owed = engine.currentBet() - engine.committedThisStreet(localUid);
        Action a = owed > 0
                ? new Action(localUid, PokerEngine.ACTION_CALL)
                : new Action(localUid, PokerEngine.ACTION_CHECK);
        submitTurnAction(a);
    }

    private void onAllIn() {
        submitTurnAction(new Action(localUid, PokerEngine.ACTION_ALL_IN));
    }

    private void onPostBlind() {
        submitTurnAction(new Action(localUid, PokerEngine.ACTION_POST_BLIND));
    }

    private void onDealHand() {
        ActionResult r = engine.submit(new Action(localUid, PokerEngine.ACTION_DEAL));
        if (!r.ok) Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show();
        else if (!hotSeat) scheduleBotTick();
    }

    private void onRaisePressed() {
        Player me = engine.player(localUid);
        if (me == null) return;
        long minTarget = engine.minRaiseTarget();
        long curMine = engine.committedThisStreet(localUid);
        long maxTarget = curMine + me.chips;
        long minStaged = Math.max(0, minTarget - curMine);
        long owedToCall = Math.max(0, engine.currentBet() - curMine);

        BetBuilder bb = new BetBuilder(this);
        bb.configure(minStaged, owedToCall, engine.totalPot(), me.chips);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(bb, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(scroller)
                .setPositiveButton("RAISE", (d, w) -> {
                    long staged = bb.getStagedTotal();
                    long amountToAdd = staged < minStaged ? Math.min(minStaged, me.chips) : staged;
                    long target = curMine + Math.min(amountToAdd, me.chips);
                    if (target > maxTarget) target = maxTarget;
                    submitTurnAction(new Action(localUid, PokerEngine.ACTION_RAISE).with("amount", target));
                })
                .setNegativeButton("CANCEL", null)
                .setCancelable(true)
                .create();
        dlg.show();
    }

    private void submitTurnAction(Action a) {
        ActionResult r = engine.submit(a);
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        if (hotSeat) {
            if (engine.isRoundOver()) { showRoundOverHotSeat(); return; }
            String next = engine.currentTurnUid();
            if (next != null && !next.equals(localUid)) launchPassGate(next);
            else render();
        } else {
            scheduleBotTick();
        }
    }

    // ====================================================================
    // BOT TICK
    // ====================================================================

    private final Runnable botRunner = new Runnable() {
        @Override public void run() {
            if (engine.isRoundOver()) {
                bg.postDelayed(() -> {
                    if (!isFinishing()) engine.submit(new Action("system", PokerEngine.ACTION_NEXT_ROUND));
                }, 1800);
                return;
            }
            String turn = engine.currentTurnUid();
            if (turn == null || turn.equals(localUid)) return;
            String phase = engine.currentPhase();

            if (PokerEngine.PHASE_POSTING_BLINDS.equals(phase)) {
                Action a;
                if (engine.isManualBlinds()) {
                    a = new Action(turn, PokerEngine.ACTION_POST_BLIND);
                } else {
                    Player p = engine.player(turn);
                    if (p != null && p.host) a = new Action(turn, PokerEngine.ACTION_DEAL);
                    else { scheduleBotTick(); return; }
                }
                engine.submit(a);
                scheduleBotTick();
                return;
            }

            Action a = PokerBotPolicy.chooseAction(engine, turn, botRng);
            ActionResult r = engine.submit(a);
            if (!r.ok) {
                Action fb = engine.currentBet() > engine.committedThisStreet(turn)
                        ? new Action(turn, PokerEngine.ACTION_CALL)
                        : new Action(turn, PokerEngine.ACTION_CHECK);
                engine.submit(fb);
            }
            scheduleBotTick();
        }
    };

    private void scheduleBotTick() {
        bg.removeCallbacks(botRunner);
        bg.postDelayed(botRunner, 700 + botRng.nextInt(700));
    }

    // ====================================================================
    // RENDER
    // ====================================================================

    private void render() {
        if (pendingPass) { renderHidden(); return; }
        // phase / pot
        potAmount.setText(ChipRackView.formatAmount(engine.totalPot()));
        String phase = engine.currentPhase();
        potDetail.setText("BLINDS " + engine.smallBlind() + "/" + engine.bigBlind()
                + "   ·   " + phase);
        phaseLabel.setText(prettyPhase(phase));
        currentBetLabel.setText(ChipRackView.formatAmount(engine.currentBet()));
        Player me = engine.player(localUid);
        stackLabel.setText(ChipRackView.formatAmount(me == null ? 0 : me.chips));

        renderCommunity();
        renderHoleCards();
        renderOpponents();
        renderActionButtons();
        renderShowdownBanner();
        renderTimer();

        if (me != null) {
            myName.setText(me.displayName.toUpperCase());
            myChipRack.setAmount(me.chips);
            List<Card> hole = engine.holeOf(localUid);
            if (!hole.isEmpty() && !engine.community().isEmpty()) {
                HandResult h = PokerHandRanker.evalHoldem(hole, engine.community());
                myStrength.setText(h.description);
            } else if (!hole.isEmpty()) {
                myStrength.setText(hole.get(0).label() + "  " + hole.get(1).label());
            } else if (engine.isFolded(localUid)) {
                myStrength.setText("FOLDED");
            } else {
                myStrength.setText("");
            }
        }
    }

    private String prettyPhase(String p) {
        if (p == null) return "—";
        switch (p) {
            case PokerEngine.PHASE_POSTING_BLINDS: return "BLINDS";
            case PokerEngine.PHASE_PRE_FLOP: return "PRE-FLOP";
            case PokerEngine.PHASE_FLOP: return "FLOP";
            case PokerEngine.PHASE_TURN: return "TURN";
            case PokerEngine.PHASE_RIVER: return "RIVER";
            case PokerEngine.PHASE_SHOWDOWN: return "SHOWDOWN";
            case PokerEngine.PHASE_ROUND_OVER: return "OVER";
            default: return p;
        }
    }

    private void renderHidden() {
        for (int i = 0; i < myHoleRow.getChildCount(); i++) myHoleRow.getChildAt(i).setVisibility(View.INVISIBLE);
        myName.setText("");
        myStrength.setText("");
        myChipRack.setAmount(0);
        renderActionButtons();
        renderCommunity();
        renderOpponents();
        potAmount.setText(ChipRackView.formatAmount(engine.totalPot()));
    }

    private void renderCommunity() {
        List<Card> comm = engine.community();
        for (int i = 0; i < communityRow.getChildCount(); i++) {
            CardView cv = (CardView) communityRow.getChildAt(i);
            if (i < comm.size()) {
                cv.setVisibility(View.VISIBLE);
                cv.bind(comm.get(i), true);
            } else {
                cv.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void renderHoleCards() {
        List<Card> hole = engine.holeOf(localUid);
        boolean folded = engine.isFolded(localUid);
        for (int i = 0; i < myHoleRow.getChildCount(); i++) {
            CardView cv = (CardView) myHoleRow.getChildAt(i);
            if (i < hole.size() && !folded) {
                cv.setVisibility(View.VISIBLE);
                cv.bind(hole.get(i), true);
            } else {
                cv.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void renderOpponents() {
        opponentColumn.removeAllViews();
        Typeface heading = safeFont(theme.fontHeading());
        Typeface mono = safeFont(theme.fontMono());
        String turnUid = engine.currentTurnUid();
        for (String uid : engine.seatOrder()) {
            if (uid.equals(localUid)) continue;
            Player p = engine.player(uid);
            boolean folded = engine.isFolded(uid);
            boolean allIn = engine.isAllIn(uid);
            boolean isTurn = uid.equals(turnUid);
            int seatIdx = engine.seatOrder().indexOf(uid);
            boolean isDealer = seatIdx == engine.dealerIndex();
            boolean isSb = seatIdx == engine.sbSeat();
            boolean isBb = seatIdx == engine.bbSeat();

            LinearLayout slot = new LinearLayout(this);
            slot.setOrientation(LinearLayout.VERTICAL);
            slot.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp((int) theme.radiusBtnDp() + 2));
            bg.setColor(theme.colorBg());
            bg.setStroke(dp(theme.borderWidthDp()), isTurn ? theme.colorAccent() : theme.colorFg3());
            slot.setBackground(bg);
            slot.setAlpha(folded ? 0.45f : 1f);

            TextView name = new TextView(this);
            String tag = isDealer ? " D" : (isSb ? " sb" : (isBb ? " bb" : ""));
            name.setText(p.displayName.toUpperCase() + tag);
            name.setTextSize(11);
            name.setLetterSpacing(0.08f);
            name.setTypeface(heading, Typeface.BOLD);
            name.setTextColor(theme.colorFg1());
            name.setMaxLines(1);
            name.setEllipsize(TextUtils.TruncateAt.END);
            slot.addView(name);

            TextView meta = new TextView(this);
            long bet = engine.committedThisStreet(uid);
            String state = folded ? "FOLD" : (allIn ? "ALL-IN" : (bet > 0 ? "bet " + ChipRackView.formatAmount(bet) : ""));
            String chips = ChipRackView.formatAmount(p.chips);
            meta.setText(chips + (state.isEmpty() ? "" : "  ·  " + state));
            meta.setTextSize(10);
            meta.setTypeface(mono);
            meta.setTextColor(theme.colorFg3());
            slot.addView(meta);

            // hole cards (only at showdown / round over and not folded)
            if ((PokerEngine.PHASE_SHOWDOWN.equals(engine.currentPhase())
                    || engine.isRoundOver()) && !folded) {
                LinearLayout cards = new LinearLayout(this);
                cards.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                clp.topMargin = dp(4);
                List<Card> hole = engine.holeOf(uid);
                for (int i = 0; i < hole.size(); i++) {
                    CardView cv = new CardView(this);
                    LinearLayout.LayoutParams cclp = new LinearLayout.LayoutParams(dp(28), dp(40));
                    cclp.rightMargin = dp(3);
                    cards.addView(cv, cclp);
                    cv.bind(hole.get(i), true);
                }
                slot.addView(cards, clp);

                if (engine.showdownHands().containsKey(uid)) {
                    TextView strength = new TextView(this);
                    strength.setText(engine.showdownHands().get(uid).description);
                    strength.setTextSize(9);
                    strength.setTextColor(theme.colorFg2());
                    slot.addView(strength);
                }
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(5);
            opponentColumn.addView(slot, lp);
        }
    }

    private void renderActionButtons() {
        Player me = engine.player(localUid);
        boolean myTurn = localUid.equals(engine.currentTurnUid());
        boolean folded = me == null || engine.isFolded(localUid);
        boolean allIn = me != null && engine.isAllIn(localUid);
        String phase = engine.currentPhase();
        boolean isPosting = PokerEngine.PHASE_POSTING_BLINDS.equals(phase);
        boolean isOver = engine.isRoundOver();
        boolean isHost = me != null && me.host;

        // hide everything by default; selectively re-show
        btnFold.setVisibility(View.GONE);
        btnCheckCall.setVisibility(View.GONE);
        btnRaise.setVisibility(View.GONE);
        btnAllIn.setVisibility(View.GONE);
        btnPostBlind.setVisibility(View.GONE);
        btnDealHand.setVisibility(View.GONE);

        if (isOver) {
            // post-round: hot seat shows nothing here (dialog drives next round); demo auto-advances
            btnHostMenu.setVisibility(View.VISIBLE);
            return;
        }

        if (isPosting) {
            if (myTurn) {
                if (engine.isManualBlinds()) {
                    btnPostBlind.setVisibility(View.VISIBLE);
                    btnPostBlind.setEnabled(true);
                    long amount = !engine.sbPosted() ? engine.smallBlind() : engine.bigBlind();
                    btnPostBlind.setText("POST BLIND  " + ChipRackView.formatAmount(amount));
                } else if (isHost) {
                    btnDealHand.setVisibility(View.VISIBLE);
                    btnDealHand.setEnabled(true);
                }
            }
            btnHostMenu.setVisibility(View.VISIBLE);
            return;
        }

        boolean canAct = myTurn && !folded && !allIn;
        long owed = me == null ? 0 : engine.currentBet() - engine.committedThisStreet(localUid);
        btnCheckCall.setVisibility(View.VISIBLE);
        btnCheckCall.setText(owed > 0
                ? "CALL " + ChipRackView.formatAmount(Math.min(owed, me == null ? 0 : me.chips))
                : "CHECK");
        btnCheckCall.setEnabled(canAct);

        btnFold.setVisibility(View.VISIBLE);
        btnFold.setEnabled(canAct && owed > 0);

        btnRaise.setVisibility(View.VISIBLE);
        btnRaise.setEnabled(canAct && me != null && me.chips > 0);

        btnAllIn.setVisibility(View.VISIBLE);
        btnAllIn.setEnabled(canAct && me != null && me.chips > 0);

        for (Button b : new Button[]{btnCheckCall, btnRaise, btnAllIn, btnFold}) {
            b.setAlpha(b.isEnabled() ? 1f : 0.45f);
        }

        btnHostMenu.setVisibility(View.VISIBLE);
    }

    private void renderShowdownBanner() {
        showdownBanner.removeAllViews();
        if (!engine.isRoundOver()) {
            showdownBanner.setVisibility(View.GONE);
            return;
        }
        showdownBanner.setVisibility(View.VISIBLE);

        TextView title = new TextView(this);
        title.setLetterSpacing(0.18f);
        title.setTextSize(13);
        title.setTypeface(safeFont(theme.fontHeading()), Typeface.BOLD);
        title.setTextColor(theme.colorFg1());
        if (engine.soleWinnerUid() != null) {
            Player w = engine.player(engine.soleWinnerUid());
            title.setText((w == null ? "?" : w.displayName.toUpperCase()) + " WINS — EVERYONE FOLDED");
        } else {
            title.setText("SHOWDOWN");
        }
        showdownBanner.addView(title);

        for (Map.Entry<String, Long> e : engine.potWinnings().entrySet()) {
            Player p = engine.player(e.getKey());
            TextView t = new TextView(this);
            t.setText("→ " + (p == null ? e.getKey() : p.displayName.toUpperCase())
                    + "  +" + ChipRackView.formatAmount(e.getValue()));
            t.setTextSize(11);
            t.setTextColor(theme.colorFg2());
            t.setTypeface(safeFont(theme.fontMono()), Typeface.BOLD);
            showdownBanner.addView(t);
        }
    }

    // ====================================================================
    // TURN TIMER
    // ====================================================================

    private void renderTimer() {
        if (timer != null) { timer.cancel(); timer = null; }
        timerPill.setVisibility(View.GONE);
        if (timerSecs <= 0) return;
        if (engine.isRoundOver() || pendingPass) return;
        if (PokerEngine.PHASE_POSTING_BLINDS.equals(engine.currentPhase())) return;
        String turnUid = engine.currentTurnUid();
        if (turnUid == null) return;
        long ms = timerSecs * 1000L;
        timerPill.setVisibility(View.VISIBLE);
        timer = new CountDownTimer(ms, 250) {
            @Override public void onTick(long left) {
                timerPill.setText(((left + 999) / 1000) + "s");
            }
            @Override public void onFinish() {
                timerPill.setVisibility(View.GONE);
                if (turnUid.equals(localUid)) {
                    long owed = engine.currentBet() - engine.committedThisStreet(localUid);
                    Action a = owed > 0
                            ? new Action(localUid, PokerEngine.ACTION_FOLD)
                            : new Action(localUid, PokerEngine.ACTION_CHECK);
                    submitTurnAction(a);
                }
            }
        };
        timer.start();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        bg.removeCallbacksAndMessages(null);
    }

    private void applyImmersive() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat ic =
                new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        ic.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        ic.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Leave game?")
                .setMessage("You'll lose this hand's progress.")
                .setPositiveButton("Leave", (d, w) -> finish())
                .setNegativeButton("Stay", null)
                .show();
    }

    // ====================================================================
    // HOT SEAT
    // ====================================================================

    private void launchPassGate(String nextUid) {
        if (nextUid == null) { render(); return; }
        pendingPass = true;
        pendingNextUid = nextUid;
        if (timer != null) timer.cancel();
        renderHidden();
        Player next = engine.player(nextUid);
        int idx = engine.seatOrder().indexOf(nextUid) + 1;
        startActivityForResult(
                PassTheDeviceActivity.intent(this, next.displayName, idx, engine.seatOrder().size()),
                REQ_PASS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PASS) {
            localUid = pendingNextUid;
            pendingPass = false;
            pendingNextUid = null;
            render();
        }
    }

    private void showRoundOverHotSeat() {
        render();
        new AlertDialog.Builder(this)
                .setTitle("Round over")
                .setMessage(roundResultMessage())
                .setPositiveButton("Next round", (d, w) -> {
                    ActionResult r = engine.submit(new Action(localUid, PokerEngine.ACTION_NEXT_ROUND));
                    if (r.ok) launchPassGate(engine.currentTurnUid());
                    else Toast.makeText(this, r.reason, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private String roundResultMessage() {
        StringBuilder sb = new StringBuilder();
        if (engine.soleWinnerUid() != null) {
            Player w = engine.player(engine.soleWinnerUid());
            sb.append((w == null ? "?" : w.displayName)).append(" wins (everyone else folded).");
        } else {
            sb.append("Showdown.\n");
            for (Map.Entry<String, Long> e : engine.potWinnings().entrySet()) {
                Player p = engine.player(e.getKey());
                sb.append("• ").append(p == null ? e.getKey() : p.displayName)
                        .append(" +").append(e.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    // ====================================================================
    // HOST MENU
    // ====================================================================

    private void showHostMenu() {
        List<String> items = new ArrayList<>();
        items.add("Action history");
        items.add("Hand rankings");
        items.add("How to play");
        items.add("Theme");
        Player me = engine.player(localUid);
        boolean isHost = me != null && me.host;
        if (isHost) {
            if (engine.hostCanEndRound()) items.add("End round now");
            if (engine.hostCanBuyIn()) items.add("Approve buy-ins (" + pendingBuyInRequests.size() + ")");
            items.add("Grant chips to player");
        }
        items.add("Toggle opponents dock");
        items.add("Exit");
        String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(arr, (d, which) -> {
                    String label = arr[which];
                    if (label.equals("Action history")) showActionHistory();
                    else if (label.equals("Hand rankings")) showReference();
                    else if (label.equals("How to play")) showHelp();
                    else if (label.equals("Theme")) showThemePicker();
                    else if (label.equals("End round now")) onHostEndRound();
                    else if (label.startsWith("Approve buy-ins")) onApproveBuyIns();
                    else if (label.equals("Grant chips to player")) onGrantChips();
                    else if (label.equals("Toggle opponents dock")) leftDock.toggle();
                    else if (label.equals("Exit")) confirmExit();
                })
                .show();
    }

    private void showActionHistory() {
        List<PokerEngine.LoggedAction> log = engine.actionLog();
        if (log.isEmpty()) {
            Toast.makeText(this, "No actions yet", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        int curRound = -1;
        for (PokerEngine.LoggedAction la : log) {
            if (la.roundIndex != curRound) {
                if (curRound != -1) sb.append("\n");
                sb.append("ROUND ").append(la.roundIndex + 1).append("\n");
                curRound = la.roundIndex;
            }
            sb.append("  ").append(la.phase).append(" · ")
                    .append(la.actorName).append(" — ").append(la.kind);
            if (la.amount > 0) sb.append(" ").append(la.amount);
            sb.append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Action history (last 3 rounds)")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void onHostEndRound() {
        ActionResult r = engine.submit(new Action(localUid, PokerEngine.ACTION_END_ROUND));
        if (!r.ok) Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show();
        else if (!hotSeat) scheduleBotTick();
    }

    private void onApproveBuyIns() {
        if (pendingBuyInRequests.isEmpty()) {
            Toast.makeText(this, "No pending requests", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[pendingBuyInRequests.size()];
        for (int i = 0; i < pendingBuyInRequests.size(); i++) {
            Map<String, Object> req = pendingBuyInRequests.get(i);
            String uid = (String) req.get("uid");
            long amt = ((Number) req.get("amount")).longValue();
            Player p = engine.player(uid);
            items[i] = (p == null ? uid : p.displayName) + " — " + amt + " chips";
        }
        new AlertDialog.Builder(this)
                .setTitle("Approve buy-ins")
                .setItems(items, (d, which) -> {
                    Map<String, Object> req = pendingBuyInRequests.remove(which);
                    Action a = new Action(localUid, PokerEngine.ACTION_BUY_IN)
                            .with("target_uid", req.get("uid"))
                            .with("amount", req.get("amount"));
                    ActionResult r = engine.submit(a);
                    if (!r.ok) Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void onGrantChips() {
        List<String> uids = new ArrayList<>(engine.seatOrder());
        String[] names = new String[uids.size()];
        for (int i = 0; i < uids.size(); i++) {
            Player p = engine.player(uids.get(i));
            names[i] = p == null ? uids.get(i) : p.displayName;
        }
        new AlertDialog.Builder(this)
                .setTitle("Grant 1000 chips to:")
                .setItems(names, (d, which) -> {
                    Action a = new Action(localUid, PokerEngine.ACTION_BUY_IN)
                            .with("target_uid", uids.get(which))
                            .with("amount", 1000L);
                    ActionResult r = engine.submit(a);
                    if (!r.ok) Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showReference() {
        GameDefinition def = GamesRegistry.get(GameType.POKER);
        StringBuilder sb = new StringBuilder();
        for (String s : def.handRankingsReference) sb.append("• ").append(s).append("\n");
        new AlertDialog.Builder(this)
                .setTitle("Hand rankings")
                .setMessage(sb.toString())
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showHelp() {
        GameDefinition def = GamesRegistry.get(GameType.POKER);
        StringBuilder sb = new StringBuilder();
        for (GameDefinition.HelpSlide s : def.helpSlides) {
            sb.append("● ").append(s.title).append("\n").append(s.body).append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("How to play")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showThemePicker() {
        Theme[] all = com.example.cardclash.core.theme.ThemeRegistry.all().toArray(new Theme[0]);
        String[] names = new String[all.length];
        for (int i = 0; i < all.length; i++) names[i] = all[i].displayName();
        new AlertDialog.Builder(this)
                .setTitle("Theme")
                .setItems(names, (d, which) -> {
                    ThemePrefs.setActive(this, all[which].id());
                    theme = ThemePrefs.activeTheme(this);
                    // Re-skin in place — no recreate, no state loss
                    String roomTxt = roomCodeLabel.getText().toString();
                    setContentView(new View(this)); // detach
                    buildUi(roomTxt.startsWith("ROOM ") ? roomTxt.substring(5) : roomTxt);
                    render();
                })
                .show();
    }

    // ====================================================================
    // STYLE HELPERS
    // ====================================================================

    private Button pillButton(String label, boolean accent, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(true);
        b.setLetterSpacing(0.10f);
        b.setStateListAnimator(null);
        b.setTypeface(safeFont(theme.fontHeading()), Typeface.BOLD);
        b.setTextColor(accent ? theme.colorAccentOn() : theme.colorFg1());
        b.setPadding(dp(8), 0, dp(8), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) theme.radiusBtnDp() + 2));
        bg.setColor(accent ? theme.colorAccent() : theme.colorBg());
        bg.setStroke(dp(theme.borderWidthDp()), accent ? theme.colorAccent() : theme.colorFg3());
        b.setBackground(bg);
        b.setMinHeight(dp(44));
        b.setOnClickListener(listener);
        return b;
    }

    private void styleSecondaryBtn(Button b) {
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setStateListAnimator(null);
        b.setTextColor(theme.colorFg1());
        b.setTypeface(safeFont(theme.fontHeading()), Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) theme.radiusBtnDp()));
        bg.setColor(theme.colorBg());
        bg.setStroke(dp(theme.borderWidthDp()), theme.colorFg3());
        b.setBackground(bg);
        b.setMinHeight(dp(36));
    }

    private LinearLayout.LayoutParams btnRowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        return lp;
    }

    private LinearLayout.LayoutParams marginEnd(int px) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = px;
        return lp;
    }

    private LinearLayout.LayoutParams lpCenter() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        return lp;
    }

    private Typeface safeFont(int fontRes) {
        if (fontRes == 0) return Typeface.DEFAULT;
        try {
            Typeface tf = ResourcesCompat.getFont(this, fontRes);
            return tf != null ? tf : Typeface.DEFAULT;
        } catch (Throwable ignored) {
            return Typeface.DEFAULT;
        }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}

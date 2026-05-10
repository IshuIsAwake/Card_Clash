package com.example.cardclash.games.bluff.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.R;
import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.hotseat.HotSeatConfig;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.Rank;
import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.games.bluff.engine.BluffEngine;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.hotseat.PassTheDeviceActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BluffActivity extends ThemedActivity {

    private static final int REQ_PASS = 4001;

    private BluffEngine engine;
    private boolean hotSeat;
    private String localUid;
    private String pendingNextUid;
    private Rank pendingClaim;
    private final Set<String> selectedCardIds = new LinkedHashSet<>();
    private boolean pendingPass;
    private boolean handVisible = true;
    private boolean inOpenCallWindow = false;

    private TextView modeLabel, ruleLabel, sequenceLabel, pileMeta,
            turnLabel, selectionInfo, handCountLabel, lockedRankLabel, resolution;
    private TextView openCallEyebrow, openCallClaim, openCallSub;
    private Button btnPlay, btnCallBluff, btnSkip, btnClear, btnMenu, btnNoCall;
    private LinearLayout opponentColumn, rankPicker, handRow, openCallOverlay, openCallCallerRow, actionColumn;
    private View rankPickerScroll, handScroll, handArea, centerStack;
    private FrameLayout pileStack;

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_bluff);
        bindViews();
        applyThemeChrome();

        hotSeat = getIntent().getBooleanExtra("hotseat", false) || HotSeatConfig.isActive();
        if (!hotSeat || HotSeatConfig.get() == null) {
            Toast.makeText(this, "Bluff requires Pass and Play in this build.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        HotSeatConfig hs = HotSeatConfig.get();
        List<Player> roster = hs.players;
        localUid = roster.get(0).uid;

        GameDefinition def = GamesRegistry.get(GameType.BLUFF);
        RoomConfig cfg = new RoomConfig(GameType.BLUFF);
        def.ruleSchema.applyDefaults(cfg);
        for (java.util.Map.Entry<String, Object> e : hs.ruleOverrides.entrySet())
            cfg.put(e.getKey(), e.getValue());

        engine = (BluffEngine) def.engineFactory.create();
        engine.initialize(cfg, roster, System.currentTimeMillis());
        engine.addListener(new GameEngine.Listener() {
            @Override public void onStateChanged() { runOnUiThread(BluffActivity.this::render); }
        });

        wireActions();
        modeLabel.setText("BLUFF · PASS AND PLAY");
        renderRankPicker();
        launchPassGate(localUid);
    }

    private void bindViews() {
        modeLabel = findViewById(R.id.modeLabel);
        ruleLabel = findViewById(R.id.ruleLabel);
        sequenceLabel = findViewById(R.id.sequenceLabel);
        pileMeta = findViewById(R.id.pileMeta);
        turnLabel = findViewById(R.id.turnLabel);
        selectionInfo = findViewById(R.id.selectionInfo);
        handCountLabel = findViewById(R.id.handCountLabel);
        lockedRankLabel = findViewById(R.id.lockedRankLabel);
        resolution = findViewById(R.id.resolution);
        openCallEyebrow = findViewById(R.id.openCallEyebrow);
        openCallClaim = findViewById(R.id.openCallClaim);
        openCallSub = findViewById(R.id.openCallSub);
        btnPlay = findViewById(R.id.btnPlay);
        btnCallBluff = findViewById(R.id.btnCallBluff);
        btnSkip = findViewById(R.id.btnSkip);
        btnClear = findViewById(R.id.btnClear);
        btnMenu = findViewById(R.id.btnMenu);
        btnNoCall = findViewById(R.id.btnNoCall);
        opponentColumn = findViewById(R.id.opponentColumn);
        rankPicker = findViewById(R.id.rankPicker);
        handRow = findViewById(R.id.handRow);
        openCallOverlay = findViewById(R.id.openCallOverlay);
        openCallCallerRow = findViewById(R.id.openCallCallerRow);
        actionColumn = findViewById(R.id.actionColumn);
        rankPickerScroll = findViewById(R.id.rankPickerScroll);
        handScroll = findViewById(R.id.handScroll);
        handArea = findViewById(R.id.handArea);
        centerStack = findViewById(R.id.centerStack);
        pileStack = findViewById(R.id.pileStack);
    }

    private void applyThemeChrome() {
        Theme t = ThemePrefs.activeTheme(this);
        findViewById(R.id.tableRoot).setBackgroundResource(t.tableBg());
        openCallOverlay.setBackgroundColor(t.colorBg());

        Typeface display = safeFont(t.fontDisplay());
        Typeface heading = safeFont(t.fontHeading());
        Typeface body = safeFont(t.fontBody());
        Typeface mono = safeFont(t.fontMono());

        modeLabel.setTypeface(heading, Typeface.BOLD);
        modeLabel.setTextColor(t.colorFg1());
        ruleLabel.setTypeface(heading);
        ruleLabel.setTextColor(t.colorFg2());
        sequenceLabel.setTypeface(heading, Typeface.BOLD);
        sequenceLabel.setTextColor(t.colorFg2());
        pileMeta.setTypeface(mono, Typeface.BOLD);
        pileMeta.setTextColor(t.colorFg2());
        turnLabel.setTypeface(heading, Typeface.BOLD);
        turnLabel.setTextColor(t.colorFg1());
        selectionInfo.setTypeface(mono);
        selectionInfo.setTextColor(t.colorFg2());
        handCountLabel.setTypeface(mono);
        handCountLabel.setTextColor(t.colorFg3());
        lockedRankLabel.setTypeface(heading, Typeface.BOLD);
        lockedRankLabel.setTextColor(t.colorAccent());
        resolution.setTypeface(body);
        resolution.setTextColor(t.colorFg2());
        openCallEyebrow.setTypeface(heading, Typeface.BOLD);
        openCallEyebrow.setTextColor(t.colorAccent());
        openCallClaim.setTypeface(display, Typeface.BOLD);
        openCallClaim.setTextColor(t.colorFg1());
        openCallSub.setTypeface(body);
        openCallSub.setTextColor(t.colorFg2());

        styleAccentButton(btnPlay, t);
        styleAccentButton(btnCallBluff, t);
        styleSecondaryButton(btnSkip, t);
        styleSecondaryButton(btnClear, t);
        styleSecondaryButton(btnMenu, t);
        styleAccentButton(btnNoCall, t);
    }

    private void styleAccentButton(Button b, Theme t) {
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        b.setStateListAnimator(null);
        b.setTextColor(t.colorAccentOn());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusBtnDp()));
        bg.setColor(t.colorAccent());
        bg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        b.setBackground(bg);
        b.setMinHeight(dp(44));
    }

    private void styleSecondaryButton(Button b, Theme t) {
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        b.setStateListAnimator(null);
        b.setTextColor(t.colorFg1());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusBtnDp()));
        bg.setColor(t.colorBg());
        bg.setStroke(dp(t.borderWidthDp()), t.colorFg3());
        b.setBackground(bg);
        b.setMinHeight(dp(40));
    }

    private void wireActions() {
        btnPlay.setOnClickListener(v -> doPlay());
        btnCallBluff.setOnClickListener(v -> doCall(localUid));
        btnSkip.setOnClickListener(v -> doSkip());
        btnClear.setOnClickListener(v -> doClear());
        btnMenu.setOnClickListener(v -> showMenu());
        btnNoCall.setOnClickListener(v -> closeOpenCallWindow(true));
    }

    /** Single menu opens a chooser with all chrome actions so the table stays clean. */
    private void showMenu() {
        String hideLabel = handVisible ? "Hide hand" : "Show hand";
        String[] items = { hideLabel, "Rules", "How to play", "Theme", "Exit" };
        new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: handVisible = !handVisible; applyHandVisibility(); break;
                        case 1: showReference(); break;
                        case 2: showHelp(); break;
                        case 3: showThemePicker(); break;
                        case 4: finish(); break;
                    }
                })
                .show();
    }

    private void applyHandVisibility() {
        handScroll.setVisibility(handVisible ? View.VISIBLE : View.GONE);
        int leakViz = handVisible ? View.VISIBLE : View.INVISIBLE;
        if (selectionInfo != null) selectionInfo.setVisibility(leakViz);
        if (lockedRankLabel != null && lockedRankLabel.getVisibility() != View.GONE)
            lockedRankLabel.setVisibility(leakViz);
        if (rankPickerScroll != null && rankPickerScroll.getVisibility() != View.GONE)
            rankPickerScroll.setVisibility(leakViz);
    }

    private void launchPassGate(String nextUid) {
        pendingPass = true;
        pendingNextUid = nextUid;
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
            selectedCardIds.clear();
            pendingClaim = null;
            handVisible = true;
            applyHandVisibility();
            render();
        }
    }

    private void doPlay() {
        Rank rank = engine.sequenceRank() != null ? engine.sequenceRank() : pendingClaim;
        if (rank == null) {
            Toast.makeText(this, "Pick a rank to claim", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCardIds.isEmpty()) {
            Toast.makeText(this, "Pick at least one card", Toast.LENGTH_SHORT).show();
            return;
        }
        Action a = new Action(localUid, BluffEngine.ACTION_PLAY)
                .with("rank", rank.name())
                .with("card_ids", new ArrayList<>(selectedCardIds));
        ActionResult r = engine.submit(a);
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        selectedCardIds.clear();
        pendingClaim = null;
        afterTurnHandoff();
    }

    private void doCall(String callerUid) {
        ActionResult r = engine.submit(new Action(callerUid, BluffEngine.ACTION_CALL_BLUFF));
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        afterTurnHandoff();
    }

    private void doSkip() {
        ActionResult r = engine.submit(new Action(localUid, BluffEngine.ACTION_SKIP));
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        afterTurnHandoff();
    }

    private void doClear() {
        ActionResult r = engine.submit(new Action(localUid, BluffEngine.ACTION_CLEAR));
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        afterTurnHandoff();
    }

    private void afterTurnHandoff() {
        if (engine.isRoundOver()) {
            render();
            String winnerName = engine.player(engine.winnerUid()).displayName;
            new AlertDialog.Builder(this)
                    .setTitle("Round over")
                    .setMessage(winnerName + " wins.\n\n" +
                            (engine.lastResolution() == null ? "" : engine.lastResolution()))
                    .setPositiveButton("Next round", (d, w) -> {
                        engine.nextRound();
                        launchPassGate(engine.currentTurnUid());
                    })
                    .setNegativeButton("Done", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        if (engine.variation().openCall() && engine.callable() && engine.lastClaim() != null) {
            openOpenCallWindow();
            return;
        }
        launchPassGate(engine.currentTurnUid());
    }

    private void openOpenCallWindow() {
        inOpenCallWindow = true;
        Theme t = ThemePrefs.activeTheme(this);
        BluffEngine.Claim c = engine.lastClaim();
        Player claimer = engine.player(c.claimerUid);
        openCallClaim.setText(claimer.displayName.toUpperCase()
                + " CLAIMED " + c.count + " × " + c.rank.label.toUpperCase()
                + (c.count == 1 ? "" : "S"));
        openCallCallerRow.removeAllViews();
        for (String uid : engine.seatOrder()) {
            if (uid.equals(c.claimerUid)) continue;
            Player p = engine.player(uid);
            Button b = new Button(this);
            b.setText("CALL AS " + p.displayName.toUpperCase());
            styleAccentButton(b, t);
            b.setOnClickListener(v -> {
                inOpenCallWindow = false;
                openCallOverlay.setVisibility(View.GONE);
                doCall(uid);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(6), 0, dp(6), 0);
            openCallCallerRow.addView(b, lp);
        }
        openCallOverlay.setVisibility(View.VISIBLE);
        renderHidden();
    }

    private void closeOpenCallWindow(boolean proceedToPass) {
        inOpenCallWindow = false;
        openCallOverlay.setVisibility(View.GONE);
        if (proceedToPass) launchPassGate(engine.currentTurnUid());
    }

    private void renderHidden() {
        handRow.removeAllViews();
        turnLabel.setText("");
        selectionInfo.setText("");
        handCountLabel.setText("");
        btnPlay.setEnabled(false);
        btnCallBluff.setEnabled(false);
        btnSkip.setEnabled(false);
        btnClear.setVisibility(View.GONE);
        for (int i = 0; i < rankPicker.getChildCount(); i++)
            rankPicker.getChildAt(i).setEnabled(false);
        renderOpponents();
        renderCenterPile();
    }

    private void render() {
        if (pendingPass || inOpenCallWindow) { renderHidden(); return; }
        Theme t = ThemePrefs.activeTheme(this);

        ruleLabel.setText(engine.variation().displayName().toUpperCase()
                + (engine.winnerKeepsTurn() ? " · WKT" : ""));

        renderOpponents();
        renderCenterPile();

        boolean myTurn = localUid.equals(engine.currentTurnUid());
        Player me = engine.player(localUid);
        String myName = (me == null ? "You" : me.displayName).toUpperCase();
        turnLabel.setText(myName + (myTurn ? "  —  YOUR TURN" : "  —  WAITING"));

        renderHand(t);
        applyHandVisibility();

        boolean isStarter = localUid.equals(engine.sequenceStarterUid());
        boolean inSequence = engine.sequenceRank() != null;

        if (inSequence) {
            rankPickerScroll.setVisibility(View.GONE);
            lockedRankLabel.setVisibility(View.VISIBLE);
            String starterName = engine.player(engine.sequenceStarterUid()).displayName;
            lockedRankLabel.setText("CLAIMING " + engine.sequenceRank().label.toUpperCase()
                    + "S  ·  STARTED BY " + starterName.toUpperCase());
        } else if (myTurn) {
            rankPickerScroll.setVisibility(View.VISIBLE);
            lockedRankLabel.setVisibility(View.GONE);
        } else {
            rankPickerScroll.setVisibility(View.GONE);
            lockedRankLabel.setVisibility(View.GONE);
        }
        refreshRankPickerSelection();

        Rank effectiveClaim = inSequence ? engine.sequenceRank() : pendingClaim;
        boolean canPlay = myTurn && effectiveClaim != null && !selectedCardIds.isEmpty();
        btnPlay.setEnabled(canPlay);
        btnPlay.setAlpha(canPlay ? 1f : 0.5f);
        selectionInfo.setText(selectedCardIds.size() + " SEL"
                + (effectiveClaim != null ? "  ·  CLAIM " + effectiveClaim.label : ""));
        handCountLabel.setText(engine.handSize(localUid) + " card"
                + (engine.handSize(localUid) == 1 ? "" : "s") + " in hand");

        BluffEngine.Claim c = engine.lastClaim();
        boolean canCallManual = engine.callable() && c != null && !c.claimerUid.equals(localUid)
                && !engine.variation().openCall() && myTurn;
        btnCallBluff.setEnabled(canCallManual);
        btnCallBluff.setAlpha(canCallManual ? 1f : 0.5f);

        boolean canSkip = myTurn && inSequence;
        btnSkip.setEnabled(canSkip);
        btnSkip.setAlpha(canSkip ? 1f : 0.5f);

        boolean canClear = myTurn && inSequence && isStarter;
        btnClear.setVisibility(canClear ? View.VISIBLE : View.GONE);

        boolean pickerEnabled = myTurn && !inSequence;
        for (int i = 0; i < rankPicker.getChildCount(); i++)
            rankPicker.getChildAt(i).setEnabled(pickerEnabled);
    }

    private void renderCenterPile() {
        Theme t = ThemePrefs.activeTheme(this);
        BluffEngine.Claim c = engine.lastClaim();
        if (c != null && engine.callable()) {
            Player claimer = engine.player(c.claimerUid);
            sequenceLabel.setText(claimer.displayName.toUpperCase() + " CLAIMED  ·  "
                    + c.count + " " + c.rank.label.toUpperCase()
                    + (c.count == 1 ? "" : "S"));
            sequenceLabel.setTextColor(t.colorAccent());
        } else if (engine.sequenceRank() != null) {
            sequenceLabel.setText("RANK LOCKED  ·  " + engine.sequenceRank().label.toUpperCase() + "S");
            sequenceLabel.setTextColor(t.colorFg1());
        } else {
            sequenceLabel.setText("NO ACTIVE SEQUENCE");
            sequenceLabel.setTextColor(t.colorFg2());
        }

        pileStack.removeAllViews();
        int shown = Math.min(3, engine.pileSize());
        int cardW = dp(46), cardH = dp(66);
        for (int i = 0; i < shown; i++) {
            View back = makeCardBack(t);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cardW, cardH);
            lp.leftMargin = i * dp(6);
            lp.topMargin = i * dp(3);
            pileStack.addView(back, lp);
        }

        pileMeta.setText(engine.pileSize() + " IN PILE  ·  ROUND " + currentRoundNumber());
        if (engine.lastResolution() != null) {
            resolution.setText(engine.lastResolution());
            resolution.setVisibility(View.VISIBLE);
        } else {
            resolution.setText("");
            resolution.setVisibility(View.GONE);
        }
    }

    private int currentRoundNumber() {
        int total = 0;
        for (String uid : engine.seatOrder()) total += engine.roundsWon(uid);
        return total + 1;
    }

    private View makeCardBack(Theme t) {
        View back = new View(this);
        back.setBackgroundResource(t.cardBackBg());
        return back;
    }

    private void renderOpponents() {
        Theme t = ThemePrefs.activeTheme(this);
        opponentColumn.removeAllViews();
        for (String uid : engine.seatOrder()) {
            if (uid.equals(localUid)) continue;
            Player p = engine.player(uid);
            boolean isTurn = uid.equals(engine.currentTurnUid());
            boolean isStarter = uid.equals(engine.sequenceStarterUid());

            LinearLayout slot = new LinearLayout(this);
            slot.setOrientation(LinearLayout.HORIZONTAL);
            slot.setGravity(Gravity.CENTER_VERTICAL);
            slot.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp((int) t.radiusBtnDp()));
            bg.setColor(t.colorBg());
            bg.setStroke(dp(t.borderWidthDp()), isTurn ? t.colorAccent() : t.colorFg3());
            slot.setBackground(bg);

            // Avatar circle
            TextView avatar = new TextView(this);
            avatar.setText(String.valueOf(p.displayName.charAt(0)).toUpperCase());
            avatar.setTextColor(isTurn ? t.colorAccentOn() : t.colorFg1());
            avatar.setTextSize(13);
            avatar.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
            avatar.setGravity(Gravity.CENTER);
            int sz = dp(28);
            GradientDrawable cb = new GradientDrawable();
            cb.setShape(GradientDrawable.OVAL);
            cb.setColor(isTurn ? t.colorAccent() : t.colorBg());
            cb.setStroke(dp(t.borderWidthDp()), t.colorFg3());
            avatar.setBackground(cb);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(sz, sz);
            alp.setMargins(0, 0, dp(8), 0);
            slot.addView(avatar, alp);

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);

            TextView name = new TextView(this);
            name.setText((p.displayName + (isStarter ? " ★" : "")).toUpperCase());
            name.setTextSize(11);
            name.setLetterSpacing(0.10f);
            name.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
            name.setTextColor(t.colorFg1());
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            text.addView(name);

            TextView meta = new TextView(this);
            meta.setText(engine.handSize(uid) + " · " + engine.roundsWon(uid) + "W");
            meta.setTextSize(10);
            meta.setTypeface(safeFont(t.fontMono()));
            meta.setTextColor(t.colorFg3());
            text.addView(meta);

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            slot.addView(text, tlp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            opponentColumn.addView(slot, lp);
        }
    }

    private void renderRankPicker() {
        rankPicker.removeAllViews();
        for (Rank r : Rank.values()) {
            Button b = new Button(this);
            b.setText(r.label);
            b.setAllCaps(false);
            b.setTextSize(13);
            b.setMinWidth(0);
            b.setMinimumWidth(dp(38));
            b.setMinHeight(dp(38));
            b.setMinimumHeight(dp(38));
            b.setPadding(dp(6), 0, dp(6), 0);
            b.setStateListAnimator(null);
            b.setTag(r);
            b.setOnClickListener(v -> {
                pendingClaim = r;
                refreshRankPickerSelection();
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
            lp.setMargins(0, 0, dp(6), 0);
            rankPicker.addView(b, lp);
        }
        refreshRankPickerSelection();
    }

    private void refreshRankPickerSelection() {
        Theme t = ThemePrefs.activeTheme(this);
        Typeface heading = safeFont(t.fontHeading());
        Rank active = engine.sequenceRank() != null ? engine.sequenceRank() : pendingClaim;
        for (int i = 0; i < rankPicker.getChildCount(); i++) {
            View v = rankPicker.getChildAt(i);
            if (!(v instanceof Button)) continue;
            Button b = (Button) v;
            boolean on = b.getTag() == active;
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp((int) t.radiusBtnDp()));
            bg.setColor(on ? t.colorAccent() : t.colorBg());
            bg.setStroke(dp(t.borderWidthDp()), on ? t.colorAccent() : t.colorFg3());
            b.setBackground(bg);
            b.setTextColor(on ? t.colorAccentOn() : t.colorFg1());
            b.setTypeface(heading, Typeface.BOLD);
        }
    }

    private void renderHand(Theme t) {
        handRow.removeAllViews();
        List<Card> hand = engine.handOf(localUid);
        Typeface heading = safeFont(t.fontHeading());
        Typeface mono = safeFont(t.fontMono());
        for (Card c : hand) {
            View card = makeCardChip(c, t, heading, mono);
            card.setOnClickListener(v -> {
                if (selectedCardIds.contains(c.id)) selectedCardIds.remove(c.id);
                else selectedCardIds.add(c.id);
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(62));
            lp.setMargins(0, 0, dp(6), 0);
            handRow.addView(card, lp);
        }
    }

    private View makeCardChip(Card c, Theme t, Typeface heading, Typeface mono) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        boolean selected = selectedCardIds.contains(c.id);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusCardDp()));
        bg.setColor(t.colorCardFace());
        bg.setStroke(dp(t.borderWidthDp()), selected ? t.colorAccent() : t.colorFg3());
        box.setBackground(bg);
        box.setTranslationY(selected ? -dp(12) : 0f);

        boolean isRed = c.suit == com.example.cardclash.core.models.Suit.HEARTS
                || c.suit == com.example.cardclash.core.models.Suit.DIAMONDS;
        int rankColor = isRed ? t.colorCardRed() : t.colorCardBlack();

        TextView rank = new TextView(this);
        rank.setText(c.rank.label);
        rank.setTextColor(rankColor);
        rank.setTextSize(15);
        rank.setTypeface(heading, Typeface.BOLD);
        box.addView(rank);

        TextView suit = new TextView(this);
        suit.setText(c.suit.glyph);
        suit.setTextColor(rankColor);
        suit.setTextSize(15);
        suit.setTypeface(mono);
        box.addView(suit);

        return box;
    }

    private void showReference() {
        new AlertDialog.Builder(this)
                .setTitle("Bluff rules")
                .setMessage("• Sequence rule: the first play of a sequence locks the rank. "
                        + "Every play after must claim that same rank (bluffs allowed).\n\n"
                        + "• Skip / Pass: pass your turn without playing.\n\n"
                        + "• Clear: only the sequence starter may discard the pile and end the sequence.\n\n"
                        + "• Call Bluff: in OPEN_CALL, anyone may call after each play. In NEXT_ONLY, only the next player.\n\n"
                        + "• Win: empty your hand by the time your turn returns.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showHelp() {
        GameDefinition def = GamesRegistry.get(GameType.BLUFF);
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
                    recreate();
                })
                .show();
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

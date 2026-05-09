package com.example.cardclash.games.teenpatti.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.R;
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
import com.example.cardclash.games.teenpatti.engine.TeenPattiEngine;
import com.example.cardclash.games.teenpatti.engine.TeenPattiHandRanker;
import com.example.cardclash.games.teenpatti.engine.TeenPattiVariation;
import com.example.cardclash.ui.common.CardView;
import com.example.cardclash.ui.common.ChipStackView;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.hotseat.PassTheDeviceActivity;

import java.util.ArrayList;
import java.util.List;

public class TeenPattiActivity extends ThemedActivity {

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_LOCAL_UID = "local_uid";
    public static final String EXTRA_HOTSEAT = "hotseat";
    private static final int REQ_PASS = 4002;

    private TeenPattiEngine engine;
    private String localUid = "you";
    private String pendingNextUid;
    private boolean hotSeat;
    private boolean pendingPass;

    private CardView c1, c2, c3;
    private TextView roomCodeLabel, variationPill, potEyebrow, potAmount, stakeLabel,
            myName, turnIndicator, handLabel;
    private ChipStackView myChips;
    private LinearLayout opponentColumn;
    private Button btnSeen, btnChaal, btnRaise, btnShow, btnFold, btnMenu;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teen_patti);
        bindViews();
        applyThemeChrome();

        hotSeat = getIntent().getBooleanExtra(EXTRA_HOTSEAT, false) && HotSeatConfig.isActive();
        String roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        if (roomId == null) roomId = hotSeat ? "HOTSEAT" : "DEMO00";
        roomCodeLabel.setText("ROOM " + roomId);

        List<Player> roster = new ArrayList<>();
        if (hotSeat) {
            roster.addAll(HotSeatConfig.get().players);
            localUid = roster.get(0).uid;
        } else {
            localUid = getIntent().getStringExtra(EXTRA_LOCAL_UID);
            if (localUid == null) localUid = "you";
            roster.add(new Player(localUid, "You", 0, 1000, true));
            roster.add(new Player("bot1", "Aarav", 1, 1000, false));
            roster.add(new Player("bot2", "Priya", 2, 1000, false));
            roster.add(new Player("bot3", "Kabir", 3, 1000, false));
        }

        GameDefinition def = GamesRegistry.get(GameType.TEEN_PATTI);
        RoomConfig cfg = new RoomConfig(GameType.TEEN_PATTI);
        def.ruleSchema.applyDefaults(cfg);
        if (hotSeat) {
            for (java.util.Map.Entry<String, Object> e : HotSeatConfig.get().ruleOverrides.entrySet())
                cfg.put(e.getKey(), e.getValue());
        }

        engine = (TeenPattiEngine) def.engineFactory.create();
        engine.initialize(cfg, roster, System.currentTimeMillis());
        engine.addListener(new GameEngine.Listener() {
            @Override public void onStateChanged() { runOnUiThread(TeenPattiActivity.this::render); }
        });

        wireActions();

        if (hotSeat) {
            launchPassGate(engine.currentTurnUid());
        } else {
            render();
        }
    }

    private void bindViews() {
        c1 = findViewById(R.id.myCard1);
        c2 = findViewById(R.id.myCard2);
        c3 = findViewById(R.id.myCard3);
        roomCodeLabel = findViewById(R.id.roomCodeLabel);
        variationPill = findViewById(R.id.variationPill);
        potEyebrow = findViewById(R.id.potEyebrow);
        potAmount = findViewById(R.id.potAmount);
        stakeLabel = findViewById(R.id.stakeLabel);
        myName = findViewById(R.id.myName);
        turnIndicator = findViewById(R.id.turnIndicator);
        handLabel = findViewById(R.id.handLabel);
        myChips = findViewById(R.id.myChips);
        opponentColumn = findViewById(R.id.opponentColumn);
        btnSeen  = findViewById(R.id.btnSeen);
        btnChaal = findViewById(R.id.btnChaal);
        btnRaise = findViewById(R.id.btnRaise);
        btnShow  = findViewById(R.id.btnShow);
        btnFold  = findViewById(R.id.btnFold);
        btnMenu  = findViewById(R.id.btnMenu);
    }

    private void applyThemeChrome() {
        Theme t = ThemePrefs.activeTheme(this);
        findViewById(R.id.tableRoot).setBackgroundResource(t.tableBg());
        Typeface display = safeFont(t.fontDisplay());
        Typeface heading = safeFont(t.fontHeading());
        Typeface body = safeFont(t.fontBody());
        Typeface mono = safeFont(t.fontMono());

        roomCodeLabel.setTypeface(heading, Typeface.BOLD);
        roomCodeLabel.setTextColor(t.colorFg1());
        variationPill.setTypeface(heading, Typeface.BOLD);
        variationPill.setTextColor(t.colorAccentOn());
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(dp(9999));
        pillBg.setColor(t.colorAccent());
        variationPill.setBackground(pillBg);

        potEyebrow.setTypeface(heading, Typeface.BOLD);
        potEyebrow.setTextColor(t.colorFg2());
        potAmount.setTypeface(mono, Typeface.BOLD);
        potAmount.setTextColor(t.colorFg1());
        GradientDrawable potBg = new GradientDrawable();
        potBg.setShape(GradientDrawable.RECTANGLE);
        potBg.setCornerRadius(dp((int) t.radiusSurfaceDp()));
        potBg.setColor(t.colorSurface());
        potBg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        potAmount.setBackground(potBg);
        stakeLabel.setTypeface(mono);
        stakeLabel.setTextColor(t.colorFg2());

        myName.setTypeface(heading, Typeface.BOLD);
        myName.setTextColor(t.colorFg1());
        turnIndicator.setTypeface(heading, Typeface.BOLD);
        turnIndicator.setTextColor(t.colorFg1());
        handLabel.setTypeface(body);
        handLabel.setTextColor(t.colorFg2());

        styleAccentButton(btnChaal, t);
        styleAccentButton(btnRaise, t);
        styleAccentButton(btnShow, t);
        styleSecondaryButton(btnSeen, t);
        styleSecondaryButton(btnFold, t);
        styleSecondaryButton(btnMenu, t);
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
        btnSeen.setOnClickListener(v -> submit(TeenPattiEngine.ACTION_TOGGLE_SEEN, false));
        btnChaal.setOnClickListener(v -> submit(TeenPattiEngine.ACTION_CHAAL, true));
        btnRaise.setOnClickListener(v -> {
            Action a = new Action(localUid, TeenPattiEngine.ACTION_RAISE)
                    .with("stake", engine.currentStake() * 2);
            handle(engine.submit(a), true);
        });
        btnShow.setOnClickListener(v -> submit(TeenPattiEngine.ACTION_SHOW, true));
        btnFold.setOnClickListener(v -> submit(TeenPattiEngine.ACTION_FOLD, true));
        btnMenu.setOnClickListener(v -> showMenu());
    }

    private void showMenu() {
        String[] items = { "Hand rankings", "How to play", "Theme", "Exit" };
        new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: showReference(); break;
                        case 1: showHelp(); break;
                        case 2: showThemePicker(); break;
                        case 3: finish(); break;
                    }
                })
                .show();
    }

    private void submit(String kind, boolean turnEnding) {
        handle(engine.submit(new Action(localUid, kind)), turnEnding);
    }

    private void handle(ActionResult r, boolean turnEnding) {
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        if (hotSeat) {
            if (engine.isRoundOver()) { showRoundOverHotSeat(); return; }
            if (turnEnding) afterTurnHandoff();
            else render();
        } else {
            getWindow().getDecorView().postDelayed(this::botTick, 700);
        }
    }

    private void afterTurnHandoff() {
        String next = engine.currentTurnUid();
        if (next == null) { render(); return; }
        launchPassGate(next);
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
            render();
        }
    }

    private void showRoundOverHotSeat() {
        render();
        String winner = engine.winnerUid() == null ? "—" : engine.player(engine.winnerUid()).displayName;
        new AlertDialog.Builder(this)
                .setTitle("Round over")
                .setMessage(winner + " won the round.")
                .setPositiveButton("Next round", (d, w) -> {
                    engine.nextRound();
                    launchPassGate(engine.currentTurnUid());
                })
                .setNegativeButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void botTick() {
        if (engine.isRoundOver()) {
            getWindow().getDecorView().postDelayed(() -> {
                engine.nextRound(); render();
            }, 1500);
            return;
        }
        String turn = engine.currentTurnUid();
        if (turn == null || turn.equals(localUid)) return;
        double r = Math.random();
        Action a;
        if (r < 0.15) a = new Action(turn, TeenPattiEngine.ACTION_FOLD);
        else if (r < 0.40) a = new Action(turn, TeenPattiEngine.ACTION_RAISE)
                .with("stake", engine.currentStake() * 2);
        else a = new Action(turn, TeenPattiEngine.ACTION_CHAAL);
        engine.submit(a);
        getWindow().getDecorView().postDelayed(this::botTick, 700);
    }

    private void renderHidden() {
        c1.setVisibility(View.INVISIBLE);
        c2.setVisibility(View.INVISIBLE);
        c3.setVisibility(View.INVISIBLE);
        myName.setText("");
        myChips.setAmount(0);
        turnIndicator.setText("");
        handLabel.setText("");
        btnSeen.setEnabled(false); btnChaal.setEnabled(false);
        btnRaise.setEnabled(false); btnShow.setEnabled(false); btnFold.setEnabled(false);

        TeenPattiVariation v = engine.variation();
        variationPill.setText(v == null ? "—" : v.displayName().toUpperCase());
        potAmount.setText(String.valueOf(engine.pot()));
        stakeLabel.setText("STAKE  ·  " + engine.currentStake());
        renderOpponents();
    }

    private void render() {
        if (pendingPass) { renderHidden(); return; }
        TeenPattiVariation v = engine.variation();
        variationPill.setText(v == null ? "—" : v.displayName().toUpperCase());
        potAmount.setText(String.valueOf(engine.pot()));
        stakeLabel.setText("STAKE  ·  " + engine.currentStake());

        List<Card> mine = engine.handOf(localUid);
        boolean seen = engine.isSeen(localUid);
        bindCard(c1, mine, 0, seen);
        bindCard(c2, mine, 1, seen);
        bindCard(c3, mine, 2, seen);

        Player me = engine.player(localUid);
        myName.setText(me == null ? "YOU" : me.displayName.toUpperCase());
        myChips.setAmount(me == null ? 0 : me.chips);

        boolean myTurn = localUid.equals(engine.currentTurnUid());
        turnIndicator.setText(engine.isRoundOver() ?
                (engine.winnerUid() == null ? "—" :
                        (engine.winnerUid().equals(localUid) ? "YOU WON THE ROUND" :
                                engine.player(engine.winnerUid()).displayName.toUpperCase() + " WON"))
                : (myTurn ? "YOUR TURN" : "WAITING…"));

        if (seen && !mine.isEmpty()) {
            HandResult h = TeenPattiHandRanker.eval3(mine,
                    v == null ? java.util.Collections.emptySet() : v.wildRanks(System.nanoTime()));
            handLabel.setText(h.description);
        } else {
            handLabel.setText("");
        }

        renderOpponents();

        boolean inRound = !engine.isRoundOver();
        boolean folded = engine.isFolded(localUid);
        btnSeen.setEnabled(inRound && !seen && !folded);
        btnSeen.setAlpha(btnSeen.isEnabled() ? 1f : 0.5f);
        btnChaal.setEnabled(inRound && myTurn && !folded);
        btnChaal.setAlpha(btnChaal.isEnabled() ? 1f : 0.5f);
        btnRaise.setEnabled(inRound && myTurn && !folded);
        btnRaise.setAlpha(btnRaise.isEnabled() ? 1f : 0.5f);
        btnFold.setEnabled(inRound && myTurn && !folded);
        btnFold.setAlpha(btnFold.isEnabled() ? 1f : 0.5f);
        int active = activeCount();
        btnShow.setEnabled(inRound && myTurn && !folded && active == 2);
        btnShow.setAlpha(btnShow.isEnabled() ? 1f : 0.5f);

        if (engine.isRoundOver()) {
            btnSeen.setEnabled(false); btnChaal.setEnabled(false);
            btnRaise.setEnabled(false); btnShow.setEnabled(false); btnFold.setEnabled(false);
        }
    }

    private int activeCount() {
        int n = 0;
        for (String uid : engine.seatOrder()) if (!engine.isFolded(uid)) n++;
        return n;
    }

    private void bindCard(CardView cv, List<Card> hand, int idx, boolean faceUp) {
        if (idx >= hand.size()) { cv.setVisibility(View.INVISIBLE); return; }
        cv.setVisibility(View.VISIBLE);
        cv.bind(hand.get(idx), faceUp || engine.isRoundOver());
    }

    private void renderOpponents() {
        Theme t = ThemePrefs.activeTheme(this);
        opponentColumn.removeAllViews();
        Typeface heading = safeFont(t.fontHeading());
        Typeface mono = safeFont(t.fontMono());
        for (String uid : engine.seatOrder()) {
            if (uid.equals(localUid)) continue;
            Player p = engine.player(uid);
            boolean isTurn = uid.equals(engine.currentTurnUid());
            boolean folded = engine.isFolded(uid);

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
            slot.setAlpha(folded ? 0.45f : 1f);

            TextView avatar = new TextView(this);
            avatar.setText(String.valueOf(p.displayName.charAt(0)).toUpperCase());
            avatar.setTextColor(isTurn ? t.colorAccentOn() : t.colorFg1());
            avatar.setTextSize(13);
            avatar.setTypeface(heading, Typeface.BOLD);
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
            name.setText(p.displayName.toUpperCase() + (folded ? " · FOLD" : ""));
            name.setTextSize(11);
            name.setLetterSpacing(0.10f);
            name.setTypeface(heading, Typeface.BOLD);
            name.setTextColor(t.colorFg1());
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            text.addView(name);

            TextView meta = new TextView(this);
            meta.setText(p.chips + " chips");
            meta.setTextSize(10);
            meta.setTypeface(mono);
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

    private void showReference() {
        GameDefinition def = GamesRegistry.get(GameType.TEEN_PATTI);
        StringBuilder sb = new StringBuilder();
        for (String s : def.handRankingsReference) sb.append("• ").append(s).append("\n");
        new AlertDialog.Builder(this)
                .setTitle("Hand rankings")
                .setMessage(sb.toString())
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showHelp() {
        GameDefinition def = GamesRegistry.get(GameType.TEEN_PATTI);
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

package com.example.cardclash.games.bluff.ui;

import android.app.AlertDialog;
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
import com.example.cardclash.ui.hotseat.PassGate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bluff table — house-rule "sequence" variant. The first play of each sequence
 * locks the rank for everyone; subsequent plays must claim the same rank (bluffs
 * still allowed). The starter is the only one who may clear the pile.
 *
 * <p>Hot-seat (single-device pass-and-play): localUid rotates with the turn
 * pointer. A {@link PassGate} hides the previous player's hand before the next
 * person picks up the device.
 */
public class BluffActivity extends ThemedActivity {

    private BluffEngine engine;
    private boolean hotSeat;
    private String localUid;
    private Rank pendingClaim;          // chosen by sequence-starter for first play
    private final Set<String> selectedCardIds = new LinkedHashSet<>();
    private boolean pendingPass;
    private boolean handVisible = true;

    private TextView modeLabel, ruleLabel, pileCount, lastClaim, resolution,
            sequenceLabel, lockedRankLabel, turnLabel, selectionInfo, handCountLabel;
    private Button btnPlay, btnCallBluff, btnSkip, btnClear, btnToggleHand;
    private LinearLayout opponentColumn, rankPicker, handRow;
    private View rankPickerScroll, handScroll;

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_bluff);

        Theme t = ThemePrefs.activeTheme(this);
        findViewById(R.id.tableRoot).setBackgroundResource(t.drawableTableBackground());

        bindViews();

        hotSeat = getIntent().getBooleanExtra("hotseat", false) || HotSeatConfig.isActive();
        if (!hotSeat || HotSeatConfig.get() == null) {
            Toast.makeText(this, "Bluff requires Hot Seat in this build.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        HotSeatConfig hs = HotSeatConfig.get();
        List<Player> roster = hs.players;
        localUid = roster.get(0).uid;

        GameDefinition def = GamesRegistry.get(GameType.BLUFF);
        RoomConfig cfg = new RoomConfig(GameType.BLUFF);
        def.ruleSchema.applyDefaults(cfg);
        for (java.util.Map.Entry<String, Object> e : hs.ruleOverrides.entrySet()) cfg.put(e.getKey(), e.getValue());

        engine = (BluffEngine) def.engineFactory.create();
        engine.initialize(cfg, roster, System.currentTimeMillis());
        engine.addListener(new GameEngine.Listener() {
            @Override public void onStateChanged() { runOnUiThread(BluffActivity.this::render); }
        });

        wireActions();
        modeLabel.setText("BLUFF · HOT SEAT");
        renderRankPicker();

        showHandoffThen(localUid);
    }

    private void bindViews() {
        modeLabel = findViewById(R.id.modeLabel);
        ruleLabel = findViewById(R.id.ruleLabel);
        pileCount = findViewById(R.id.pileCount);
        lastClaim = findViewById(R.id.lastClaim);
        resolution = findViewById(R.id.resolution);
        sequenceLabel = findViewById(R.id.sequenceLabel);
        lockedRankLabel = findViewById(R.id.lockedRankLabel);
        turnLabel = findViewById(R.id.turnLabel);
        selectionInfo = findViewById(R.id.selectionInfo);
        handCountLabel = findViewById(R.id.handCountLabel);
        btnPlay = findViewById(R.id.btnPlay);
        btnCallBluff = findViewById(R.id.btnCallBluff);
        btnSkip = findViewById(R.id.btnSkip);
        btnClear = findViewById(R.id.btnClear);
        btnToggleHand = findViewById(R.id.btnToggleHand);
        opponentColumn = findViewById(R.id.opponentColumn);
        rankPicker = findViewById(R.id.rankPicker);
        handRow = findViewById(R.id.handRow);
        rankPickerScroll = findViewById(R.id.rankPickerScroll);
        handScroll = findViewById(R.id.handScroll);
    }

    private void wireActions() {
        btnPlay.setOnClickListener(v -> doPlay());
        btnCallBluff.setOnClickListener(v -> doCall());
        btnSkip.setOnClickListener(v -> doSkip());
        btnClear.setOnClickListener(v -> doClear());
        btnToggleHand.setOnClickListener(v -> {
            handVisible = !handVisible;
            applyHandVisibility();
        });

        findViewById(R.id.btnReference).setOnClickListener(v -> showReference());
        findViewById(R.id.btnHelp).setOnClickListener(v -> showHelp());
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());
    }

    private void applyHandVisibility() {
        handScroll.setVisibility(handVisible ? View.VISIBLE : View.GONE);
        btnToggleHand.setText(handVisible ? "▼ Hide hand" : "▲ Show hand");
    }

    private void showHandoffThen(String nextUid) {
        pendingPass = true;
        renderHidden();
        Player next = engine.player(nextUid);
        String sub = engine.lastResolution() != null
                ? engine.lastResolution()
                : "Hand will appear after you continue.";
        PassGate.show(this, next.displayName, sub, () -> {
            localUid = nextUid;
            pendingPass = false;
            selectedCardIds.clear();
            pendingClaim = null;
            handVisible = true;
            applyHandVisibility();
            render();
        });
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

    private void doCall() {
        String caller = engine.variation().openCall() ? localUid : engine.currentTurnUid();
        ActionResult r = engine.submit(new Action(caller, BluffEngine.ACTION_CALL_BLUFF));
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
                    .setMessage(winnerName + " wins the round.\n\n" +
                            (engine.lastResolution() == null ? "" : engine.lastResolution()))
                    .setPositiveButton("Next round", (d, w) -> {
                        engine.nextRound();
                        showHandoffThen(engine.currentTurnUid());
                    })
                    .setNegativeButton("Done", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        showHandoffThen(engine.currentTurnUid());
    }

    private void renderHidden() {
        handRow.removeAllViews();
        turnLabel.setText("Pass to the next player…");
        selectionInfo.setText("");
        handCountLabel.setText("");
        btnPlay.setEnabled(false);
        btnCallBluff.setEnabled(false);
        btnSkip.setEnabled(false);
        btnClear.setVisibility(View.GONE);
        for (int i = 0; i < rankPicker.getChildCount(); i++) {
            rankPicker.getChildAt(i).setEnabled(false);
        }
        renderOpponents();
        renderCenter();
    }

    private void render() {
        if (pendingPass) { renderHidden(); return; }
        Theme t = ThemePrefs.activeTheme(this);
        ruleLabel.setText(engine.variation().displayName()
                + (engine.winnerKeepsTurn() ? " · WKT" : ""));
        renderOpponents();
        renderCenter();

        boolean myTurn = localUid.equals(engine.currentTurnUid());
        Player me = engine.player(localUid);
        turnLabel.setText((me == null ? "You" : me.displayName) +
                (myTurn ? " — your turn" : " — waiting"));

        renderHand(t);
        applyHandVisibility();

        // Sequence/composer state
        boolean isStarter = localUid.equals(engine.sequenceStarterUid());
        boolean inSequence = engine.sequenceRank() != null;

        if (inSequence) {
            // Rank is locked — hide picker, show locked-rank label
            rankPickerScroll.setVisibility(View.GONE);
            lockedRankLabel.setVisibility(View.VISIBLE);
            String starterName = engine.player(engine.sequenceStarterUid()).displayName;
            lockedRankLabel.setText("Sequence: claiming " + engine.sequenceRank().label
                    + "s · started by " + starterName);
        } else if (myTurn) {
            // I'm starting a new sequence — pick the rank
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
        selectionInfo.setText(selectedCardIds.size() + " sel"
                + (effectiveClaim != null ? " · claim " + effectiveClaim.label : ""));
        handCountLabel.setText(engine.handSize(localUid) + " card"
                + (engine.handSize(localUid) == 1 ? "" : "s") + " in hand");

        // Call Bluff: callable + non-claimer + (open or my turn)
        BluffEngine.Claim c = engine.lastClaim();
        boolean canCall = engine.callable() && c != null && !c.claimerUid.equals(localUid)
                && (engine.variation().openCall() || myTurn);
        btnCallBluff.setEnabled(canCall);

        // Skip: my turn AND a sequence is active (can't skip when starting)
        boolean canSkip = myTurn && inSequence;
        btnSkip.setEnabled(canSkip);

        // Clear: my turn AND I'm the sequence starter AND sequence is active
        boolean canClear = myTurn && inSequence && isStarter;
        btnClear.setVisibility(canClear ? View.VISIBLE : View.GONE);

        // Rank picker enabled only when starting a new sequence on my turn
        boolean pickerEnabled = myTurn && !inSequence;
        for (int i = 0; i < rankPicker.getChildCount(); i++) {
            rankPicker.getChildAt(i).setEnabled(pickerEnabled);
        }
    }

    private void renderCenter() {
        Theme t = ThemePrefs.activeTheme(this);
        pileCount.setText(String.valueOf(engine.pileSize()));
        if (engine.sequenceRank() != null) {
            String starter = engine.player(engine.sequenceStarterUid()).displayName;
            sequenceLabel.setText("RANK LOCKED · " + engine.sequenceRank().label
                    + "s · STARTER " + starter.toUpperCase());
            sequenceLabel.setTextColor(t.colorTextPrimary());
        } else {
            sequenceLabel.setText("NO ACTIVE SEQUENCE");
            sequenceLabel.setTextColor(t.colorTextSecondary());
        }
        BluffEngine.Claim c = engine.lastClaim();
        if (c != null && engine.callable()) {
            lastClaim.setText("Last: " + c.count + " × " + c.rank.label
                    + " by " + engine.player(c.claimerUid).displayName);
        } else {
            lastClaim.setText("");
        }
        resolution.setText(engine.lastResolution() == null ? "" : engine.lastResolution());
    }

    private void renderOpponents() {
        Theme t = ThemePrefs.activeTheme(this);
        opponentColumn.removeAllViews();
        for (String uid : engine.seatOrder()) {
            if (uid.equals(localUid)) continue;
            Player p = engine.player(uid);
            boolean isTurn = uid.equals(engine.currentTurnUid());
            boolean isStarter = uid.equals(engine.sequenceStarterUid());

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(isTurn ? t.colorTextPrimary() : t.colorBackground());
            bg.setStroke(dp(2), t.colorTextPrimary());
            row.setBackground(bg);
            int textColor = isTurn ? t.colorBackground() : t.colorTextPrimary();
            int subColor = isTurn ? t.colorBackground() : t.colorTextSecondary();

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);

            TextView name = new TextView(this);
            String label = p.displayName + (isStarter ? " ★" : "");
            name.setText(label);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setTextColor(textColor);
            name.setTextSize(13);
            text.addView(name);

            TextView meta = new TextView(this);
            meta.setText(engine.handSize(uid) + " cards · " + engine.roundsWon(uid) + "W");
            meta.setTextSize(11);
            meta.setTextColor(subColor);
            text.addView(meta);

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(text, tlp);

            if (isTurn) {
                TextView dot = new TextView(this);
                dot.setText("●");
                dot.setTextColor(textColor);
                dot.setTextSize(14);
                row.addView(dot);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            opponentColumn.addView(row, lp);
        }
    }

    private void renderRankPicker() {
        rankPicker.removeAllViews();
        for (Rank r : Rank.values()) {
            Button b = new Button(this);
            b.setText(r.label);
            b.setAllCaps(false);
            b.setTextSize(14);
            b.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            b.setMinWidth(0);
            b.setMinimumWidth(dp(36));
            b.setMinHeight(dp(36));
            b.setMinimumHeight(dp(36));
            b.setPadding(dp(6), 0, dp(6), 0);
            b.setStateListAnimator(null);
            b.setTag(r);
            b.setOnClickListener(v -> {
                pendingClaim = r;
                refreshRankPickerSelection();
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            lp.setMargins(0, 0, dp(4), 0);
            rankPicker.addView(b, lp);
        }
        refreshRankPickerSelection();
    }

    private void refreshRankPickerSelection() {
        Theme t = ThemePrefs.activeTheme(this);
        Rank active = engine.sequenceRank() != null ? engine.sequenceRank() : pendingClaim;
        for (int i = 0; i < rankPicker.getChildCount(); i++) {
            View v = rankPicker.getChildAt(i);
            if (!(v instanceof Button)) continue;
            Button b = (Button) v;
            boolean on = b.getTag() == active;
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(on ? t.colorTextPrimary() : t.colorBackground());
            bg.setStroke(dp(2), t.colorTextPrimary());
            b.setBackground(bg);
            b.setTextColor(on ? t.colorBackground() : t.colorTextPrimary());
        }
    }

    private void renderHand(Theme t) {
        handRow.removeAllViews();
        List<Card> hand = engine.handOf(localUid);
        for (Card c : hand) {
            View card = makeCardChip(c, t);
            card.setOnClickListener(v -> {
                if (selectedCardIds.contains(c.id)) selectedCardIds.remove(c.id);
                else selectedCardIds.add(c.id);
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(58));
            lp.setMargins(0, 0, dp(4), 0);
            handRow.addView(card, lp);
        }
    }

    private View makeCardChip(Card c, Theme t) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        boolean selected = selectedCardIds.contains(c.id);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(selected ? t.colorTextPrimary() : t.colorBackground());
        bg.setStroke(dp(2), t.colorTextPrimary());
        box.setBackground(bg);

        int textColor = selected ? t.colorBackground() : t.colorTextPrimary();

        TextView rank = new TextView(this);
        rank.setText(c.rank.label);
        rank.setTextColor(textColor);
        rank.setTextSize(16);
        rank.setTypeface(rank.getTypeface(), Typeface.BOLD);
        box.addView(rank);

        TextView suit = new TextView(this);
        suit.setText(c.suit.glyph);
        suit.setTextColor(textColor);
        suit.setTextSize(16);
        box.addView(suit);

        return box;
    }

    private void showReference() {
        new AlertDialog.Builder(this)
                .setTitle("Bluff rules")
                .setMessage("• Sequence rule: the first play of a sequence locks the rank. "
                        + "Every play after must claim that same rank (bluffs allowed).\n\n"
                        + "• Skip: pass your turn without playing.\n\n"
                        + "• Clear: only the sequence starter may discard the pile and end the sequence.\n\n"
                        + "• Call Bluff: reveal the most recent claim. Truthful = caller picks up; bluff = claimer picks up.\n\n"
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

    private void showSettings() {
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

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}

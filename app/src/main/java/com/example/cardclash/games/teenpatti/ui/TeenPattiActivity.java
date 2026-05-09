package com.example.cardclash.games.teenpatti.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.cardclash.R;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.hotseat.HotSeatConfig;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.games.teenpatti.engine.TeenPattiEngine;
import com.example.cardclash.games.teenpatti.engine.TeenPattiHandRanker;
import com.example.cardclash.games.teenpatti.engine.TeenPattiVariation;
import com.example.cardclash.ui.common.CardView;
import com.example.cardclash.ui.common.ChipStackView;
import com.example.cardclash.ui.common.PlayerSlotView;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.hotseat.PassGate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Teen Patti table activity.
 *
 * <p>Three modes:
 * <ul>
 *   <li><b>Demo</b> (no extras): you + 3 bots, on-device only.</li>
 *   <li><b>Hot seat</b> (extras "hotseat"=true + {@link HotSeatConfig} populated):
 *       all human players on this device, with a {@link PassGate} between turns.</li>
 *   <li><b>Networked</b> (room id present): not yet wired to engine state sync; lobby
 *       handoff exists but engine is host-local for now.</li>
 * </ul>
 */
public class TeenPattiActivity extends ThemedActivity {

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_LOCAL_UID = "local_uid";
    public static final String EXTRA_HOTSEAT = "hotseat";

    private TeenPattiEngine engine;
    private String localUid = "you";
    private boolean hotSeat;
    private boolean pendingPass;

    private CardView c1, c2, c3;
    private TextView roomCodeLabel, variationPill, potAmount, stakeLabel,
            myName, turnIndicator, handLabel;
    private ChipStackView myChips;
    private PlayerSlotView opp1, opp2, opp3;
    private Button btnSeen, btnChaal, btnRaise, btnShow, btnFold;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teen_patti);

        Theme t = ThemePrefs.activeTheme(this);
        findViewById(R.id.tableRoot).setBackgroundResource(t.drawableTableBackground());

        bindViews();

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

        engine = (TeenPattiEngine) def.engineFactory.create();
        engine.initialize(cfg, roster, System.currentTimeMillis());
        engine.addListener(new GameEngine.Listener() {
            @Override public void onStateChanged() { runOnUiThread(TeenPattiActivity.this::render); }
        });

        wireActions();

        if (hotSeat) {
            // Open the round with a pass-gate so player 1 picks up the device first.
            showHandoffThen(engine.currentTurnUid(), "Tap to see your hand.");
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
        potAmount = findViewById(R.id.potAmount);
        stakeLabel = findViewById(R.id.stakeLabel);
        myName = findViewById(R.id.myName);
        turnIndicator = findViewById(R.id.turnIndicator);
        handLabel = findViewById(R.id.handLabel);
        myChips = findViewById(R.id.myChips);
        opp1 = findViewById(R.id.opponent1);
        opp2 = findViewById(R.id.opponent2);
        opp3 = findViewById(R.id.opponent3);
        btnSeen  = findViewById(R.id.btnSeen);
        btnChaal = findViewById(R.id.btnChaal);
        btnRaise = findViewById(R.id.btnRaise);
        btnShow  = findViewById(R.id.btnShow);
        btnFold  = findViewById(R.id.btnFold);
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

        findViewById(R.id.btnReference).setOnClickListener(v -> showReference());
        findViewById(R.id.btnHelp).setOnClickListener(v -> showHelp());
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());
    }

    private void submit(String kind, boolean turnEnding) {
        handle(engine.submit(new Action(localUid, kind)), turnEnding);
    }

    private void handle(ActionResult r, boolean turnEnding) {
        if (!r.ok) { Toast.makeText(this, r.reason, Toast.LENGTH_SHORT).show(); return; }
        if (hotSeat) {
            if (engine.isRoundOver()) { showRoundOverHotSeat(); return; }
            if (turnEnding) afterTurnHandoff();
        } else {
            getWindow().getDecorView().postDelayed(this::botTick, 700);
        }
    }

    private void afterTurnHandoff() {
        String next = engine.currentTurnUid();
        if (next == null) { render(); return; }
        showHandoffThen(next, null);
    }

    private void showHandoffThen(String nextUid, String subtitle) {
        pendingPass = true;
        renderHidden();
        Player next = engine.player(nextUid);
        PassGate.show(this, next.displayName,
                subtitle != null ? subtitle : "Stake: " + engine.currentStake() + " · Pot: " + engine.pot(),
                () -> {
                    localUid = nextUid;
                    pendingPass = false;
                    render();
                });
    }

    private void showRoundOverHotSeat() {
        render();
        String winner = engine.winnerUid() == null ? "—" : engine.player(engine.winnerUid()).displayName;
        new AlertDialog.Builder(this)
                .setTitle("Round over")
                .setMessage(winner + " won the round.")
                .setPositiveButton("Next round", (d, w) -> {
                    engine.nextRound();
                    showHandoffThen(engine.currentTurnUid(), "Tap to see your hand.");
                })
                .setNegativeButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    /** Naive bot policy: 60% chaal, 25% raise, 15% fold. Demo mode only. */
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
        // Hide the outgoing player's cards/state before they hand off.
        c1.setVisibility(View.INVISIBLE);
        c2.setVisibility(View.INVISIBLE);
        c3.setVisibility(View.INVISIBLE);
        myName.setText("");
        myChips.setAmount(0);
        turnIndicator.setText("Pass to the next player…");
        handLabel.setText("");
        btnSeen.setEnabled(false); btnChaal.setEnabled(false);
        btnRaise.setEnabled(false); btnShow.setEnabled(false); btnFold.setEnabled(false);

        // Variation pill + pot still visible (no privacy concern there)
        TeenPattiVariation v = engine.variation();
        variationPill.setText(v == null ? "—" : v.displayName());
        potAmount.setText(String.valueOf(engine.pot()));
        stakeLabel.setText("Stake: " + engine.currentStake());
        bindOpponentsHidden();
    }

    private void bindOpponentsHidden() {
        // In hot-seat we render every non-local player as an "opponent" (face-down) seat.
        List<String> order = new ArrayList<>(engine.seatOrder());
        order.remove(localUid);
        PlayerSlotView[] slots = {opp1, opp2, opp3};
        for (int i = 0; i < slots.length; i++) {
            if (i >= order.size()) { slots[i].setVisibility(View.INVISIBLE); continue; }
            slots[i].setVisibility(View.VISIBLE);
            String uid = order.get(i);
            Player p = engine.player(uid);
            slots[i].bind(p, !engine.isFolded(uid));
            slots[i].setActive(uid.equals(engine.currentTurnUid()));
        }
    }

    private void render() {
        if (pendingPass) { renderHidden(); return; }
        TeenPattiVariation v = engine.variation();
        variationPill.setText(v == null ? "—" : v.displayName());

        potAmount.setText(String.valueOf(engine.pot()));
        stakeLabel.setText("Stake: " + engine.currentStake());

        List<Card> mine = engine.handOf(localUid);
        boolean seen = engine.isSeen(localUid);
        bindCard(c1, mine, 0, seen);
        bindCard(c2, mine, 1, seen);
        bindCard(c3, mine, 2, seen);

        Player me = engine.player(localUid);
        myName.setText(me == null ? "You" : me.displayName);
        myChips.setAmount(me == null ? 0 : me.chips);

        boolean myTurn = localUid.equals(engine.currentTurnUid());
        turnIndicator.setText(engine.isRoundOver() ?
                (engine.winnerUid() == null ? "—" :
                        (engine.winnerUid().equals(localUid) ? "You won the round!" :
                                engine.player(engine.winnerUid()).displayName + " won."))
                : (myTurn ? "Your turn" : "Waiting…"));

        if (seen && !mine.isEmpty()) {
            HandResult h = TeenPattiHandRanker.eval3(mine,
                    v == null ? java.util.Collections.emptySet() : v.wildRanks(System.nanoTime()));
            handLabel.setText(h.description);
        } else {
            handLabel.setText("");
        }

        bindOpponents();

        boolean inRound = !engine.isRoundOver();
        boolean folded = engine.isFolded(localUid);
        btnSeen.setEnabled(inRound && !seen && !folded);
        btnChaal.setEnabled(inRound && myTurn && !folded);
        btnRaise.setEnabled(inRound && myTurn && !folded);
        btnFold.setEnabled(inRound && myTurn && !folded);
        int active = activeCount();
        btnShow.setEnabled(inRound && myTurn && !folded && active == 2);

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

    private void bindOpponents() {
        List<String> order = new ArrayList<>(engine.seatOrder());
        order.remove(localUid);
        PlayerSlotView[] slots = {opp1, opp2, opp3};
        for (int i = 0; i < slots.length; i++) {
            if (i >= order.size()) { slots[i].setVisibility(View.INVISIBLE); continue; }
            slots[i].setVisibility(View.VISIBLE);
            String uid = order.get(i);
            Player p = engine.player(uid);
            boolean folded = engine.isFolded(uid);
            slots[i].bind(p, !folded);
            slots[i].setActive(uid.equals(engine.currentTurnUid()));
        }
    }

    private void showReference() {
        GameDefinition def = GamesRegistry.get(GameType.TEEN_PATTI);
        new AlertDialog.Builder(this)
                .setTitle("Hand Rankings")
                .setMessage(String.join("\n• ", prepend("• ", def.handRankingsReference)))
                .setPositiveButton("Got it", null)
                .show();
    }

    private static List<String> prepend(String pfx, List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) out.add(s);
        return out;
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
}

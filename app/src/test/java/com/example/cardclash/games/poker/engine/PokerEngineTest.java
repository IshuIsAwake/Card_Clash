package com.example.cardclash.games.poker.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PokerEngineTest {

    private PokerEngine engine;
    private List<Player> roster;
    private RoomConfig cfg;

    @Before public void setUp() {
        roster = new ArrayList<>(Arrays.asList(
                new Player("A", "Alice", 0, 5000, true),
                new Player("B", "Bob",   1, 5000, false),
                new Player("C", "Cara",  2, 5000, false),
                new Player("D", "Dan",   3, 5000, false)
        ));
        cfg = new RoomConfig(GameType.POKER);
        cfg.put("small_blind", 25L);
        cfg.put("big_blind", 50L);
        cfg.put("round_count", 0);
        cfg.put("host_can_end_round", true);
        cfg.put("host_can_buy_in", true);
        cfg.put("blind_mode", "HOST_DEAL");
        cfg.put("min_raise_mode", "LAST");
        engine = new PokerEngine();
        engine.initialize(cfg, roster, 1234L);
        ok(engine.submit(new Action("A", PokerEngine.ACTION_DEAL)));
    }

    @Test public void blindsPosted_andTurnIsUTG() {
        // dealer = A (index 0). SB = B, BB = C. UTG = D.
        assertEquals(PokerEngine.PHASE_PRE_FLOP, engine.currentPhase());
        assertEquals(50L, engine.currentBet());
        assertEquals(25L, engine.committedThisStreet("B"));
        assertEquals(50L, engine.committedThisStreet("C"));
        assertEquals(0L, engine.committedThisStreet("D"));
        assertEquals("D", engine.currentTurnUid());
        assertEquals(4975L, engine.player("B").chips);
        assertEquals(4950L, engine.player("C").chips);
    }

    @Test public void foldAround_givesPotToBigBlind() {
        ok(engine.submit(new Action("D", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_FOLD))); // BB wins
        assertTrue(engine.isRoundOver());
        assertEquals("C", engine.soleWinnerUid());
        // BB had paid 50, SB 25, total pot 75 awarded back to C → C ends with 5000+25
        assertEquals(5000L + 25L, engine.player("C").chips);
        assertEquals(4975L, engine.player("B").chips);
        assertEquals(5000L, engine.player("A").chips);
        assertEquals(5000L, engine.player("D").chips);
    }

    @Test public void checkDown_runsThroughAllStreets_thenShowdown() {
        // 4-way limped pot
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CALL)));    // D calls 50
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CALL)));    // A calls 50
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CALL)));    // SB calls 25 more
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CHECK)));   // BB checks → flop
        assertEquals(PokerEngine.PHASE_FLOP, engine.currentPhase());
        assertEquals(3, engine.community().size());
        assertEquals(1, engine.burnedCount());

        // flop checks around (action begins left of dealer = B)
        assertEquals("B", engine.currentTurnUid());
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CHECK)));
        assertEquals(PokerEngine.PHASE_TURN, engine.currentPhase());
        assertEquals(4, engine.community().size());
        assertEquals(2, engine.burnedCount());

        // turn checks around
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CHECK)));
        assertEquals(PokerEngine.PHASE_RIVER, engine.currentPhase());
        assertEquals(5, engine.community().size());
        assertEquals(3, engine.burnedCount());

        // river checks around → showdown
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CHECK)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CHECK)));
        assertTrue(engine.isRoundOver());
        assertEquals(4, engine.showdownHands().size());
        assertNull(engine.soleWinnerUid()); // showdown, not single fold-around

        // chip conservation: all 200 chips of pot ended up somewhere
        long totalChips = 0;
        for (Player p : new Player[]{engine.player("A"), engine.player("B"), engine.player("C"), engine.player("D")})
            totalChips += p.chips;
        assertEquals(20000L, totalChips);
    }

    @Test public void raiseBelowMinimum_isRejected() {
        // currentBet = 50, minRaise increment = 50, so min raise to = 100
        ActionResult r = engine.submit(new Action("D", PokerEngine.ACTION_RAISE).with("amount", 75L));
        assertFalse(r.ok);
        assertNotNull(r.reason);
    }

    @Test public void raiseReopensActionForOthers_butNotForTheRaiser() {
        // D raises to 200
        ok(engine.submit(new Action("D", PokerEngine.ACTION_RAISE).with("amount", 200L)));
        assertEquals(200L, engine.currentBet());
        assertEquals("A", engine.currentTurnUid());
        ok(engine.submit(new Action("A", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_FOLD)));
        // C re-raises to 600 (min increment was 150 since D raised 50→200; min = 200+150=350)
        ok(engine.submit(new Action("C", PokerEngine.ACTION_RAISE).with("amount", 600L)));
        // action returns to D
        assertEquals("D", engine.currentTurnUid());
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CALL)));
        // street complete → flop
        assertEquals(PokerEngine.PHASE_FLOP, engine.currentPhase());
    }

    @Test public void allInShortStack_buildsSidePotAtShowdown() {
        // Reset with a short-stacked player
        roster = new ArrayList<>(Arrays.asList(
                new Player("A", "Alice", 0, 5000, true),
                new Player("B", "Bob",   1, 5000, false),
                new Player("C", "Cara",  2, 200,  false), // tiny stack
                new Player("D", "Dan",   3, 5000, false)
        ));
        engine = new PokerEngine();
        engine.initialize(cfg, roster, 99L);
        ok(engine.submit(new Action("A", PokerEngine.ACTION_DEAL)));
        // SB=B(25), BB=C(50, has 150 left); UTG=D
        ok(engine.submit(new Action("D", PokerEngine.ACTION_RAISE).with("amount", 1000L)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CALL)));
        // C all-in for remaining 150 (already committed 50, will commit 200 total)
        ok(engine.submit(new Action("C", PokerEngine.ACTION_ALL_IN)));
        // C is short → no reopen for D/A/B who already acted; C is short all-in
        // streets play out
        // Force-end remaining streets via host END_ROUND so we always reach showdown
        ok(engine.submit(new Action("A", PokerEngine.ACTION_END_ROUND)));
        assertTrue(engine.isRoundOver());
        // pot structure: main pot (200 × 4 = 800) eligible for everyone non-folded;
        // side pot (800 × 3 = 2400) eligible for A/B/D
        List<SidePotCalculator.Pot> pots = engine.showdownPots();
        assertEquals(2, pots.size());
        assertEquals(800L, pots.get(0).amount);
        assertTrue(pots.get(0).eligibleUids.contains("C"));
        assertEquals(2400L, pots.get(1).amount);
        assertFalse(pots.get(1).eligibleUids.contains("C"));
    }

    @Test public void hostEndRound_dealsRemainingStreetsAndShowsDown() {
        // 4-way to flop
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CHECK)));
        assertEquals(PokerEngine.PHASE_FLOP, engine.currentPhase());
        // Host A ends the round
        ok(engine.submit(new Action("A", PokerEngine.ACTION_END_ROUND)));
        assertTrue(engine.isRoundOver());
        assertEquals(5, engine.community().size());
        assertEquals(3, engine.burnedCount());
        assertEquals(4, engine.showdownHands().size());
    }

    @Test public void hostBuyIn_addsChipsAtNextRound() {
        // Round 1: everyone folds to BB
        ok(engine.submit(new Action("D", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_FOLD)));
        assertTrue(engine.isRoundOver());
        // Host buys in chips for D between rounds → applied immediately since round is over
        long before = engine.player("D").chips;
        ok(engine.submit(new Action("A", PokerEngine.ACTION_BUY_IN)
                .with("target_uid", "D").with("amount", 1000L)));
        assertEquals(before + 1000L, engine.player("D").chips);
    }

    @Test public void dealerRotates_eachRound() {
        int firstDealer = engine.dealerIndex();
        ok(engine.submit(new Action("D", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_FOLD)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_NEXT_ROUND)));
        assertEquals((firstDealer + 1) % 4, engine.dealerIndex());
    }

    @Test public void matchLast_minRaiseResetsBetweenStreets() {
        // Pre-flop everyone limps to flop
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("B", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CHECK)));
        assertEquals(PokerEngine.PHASE_FLOP, engine.currentPhase());
        // On flop, B raises to 300 (a 300 increment) — under LAST, minRaise becomes 300.
        ok(engine.submit(new Action("B", PokerEngine.ACTION_RAISE).with("amount", 300L)));
        // Increment was 300 → minRaiseSize becomes 300; next raise on flop must be ≥ 600.
        assertEquals(600L, engine.minRaiseTarget());
        ok(engine.submit(new Action("C", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("D", PokerEngine.ACTION_CALL)));
        ok(engine.submit(new Action("A", PokerEngine.ACTION_CALL)));
        // Now on TURN — minRaiseSize MUST reset to BB (50). Min target = 50, not 600.
        assertEquals(PokerEngine.PHASE_TURN, engine.currentPhase());
        assertEquals(50L, engine.minRaiseTarget());
    }

    @Test public void nonHost_cannotEndRound() {
        ActionResult r = engine.submit(new Action("B", PokerEngine.ACTION_END_ROUND));
        assertFalse(r.ok);
    }

    private static void ok(ActionResult r) {
        if (!r.ok) throw new AssertionError("expected ok, got: " + r.reason);
    }
}

package com.example.cardclash.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.cardclash.games.poker.engine.SidePotCalculator;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SidePotCalculatorTest {

    @Test public void noAllIns_singleMainPot() {
        Map<String, Long> commits = new LinkedHashMap<>();
        commits.put("A", 100L);
        commits.put("B", 100L);
        commits.put("C", 100L);
        Set<String> live = new HashSet<>(Arrays.asList("A", "B", "C"));
        List<SidePotCalculator.Pot> pots = SidePotCalculator.compute(commits, live);
        assertEquals(1, pots.size());
        assertEquals(300L, pots.get(0).amount);
    }

    @Test public void shortStackAllIn_buildsSidePot() {
        // A all-in for 50, B and C call to 200
        Map<String, Long> commits = new LinkedHashMap<>();
        commits.put("A", 50L);
        commits.put("B", 200L);
        commits.put("C", 200L);
        Set<String> live = new HashSet<>(Arrays.asList("A", "B", "C"));
        List<SidePotCalculator.Pot> pots = SidePotCalculator.compute(commits, live);
        assertEquals(2, pots.size());
        // Main pot: 50 * 3 = 150, all eligible
        assertEquals(150L, pots.get(0).amount);
        assertTrue(pots.get(0).eligibleUids.contains("A"));
        // Side pot: (200-50) * 2 = 300, only B and C
        assertEquals(300L, pots.get(1).amount);
        assertTrue(!pots.get(1).eligibleUids.contains("A"));
        assertTrue(pots.get(1).eligibleUids.contains("B"));
        assertTrue(pots.get(1).eligibleUids.contains("C"));
    }

    @Test public void foldedPlayer_dropsFromEligibility() {
        Map<String, Long> commits = new LinkedHashMap<>();
        commits.put("A", 100L);
        commits.put("B", 100L);
        commits.put("C", 100L);
        Set<String> live = new HashSet<>(Arrays.asList("A", "B")); // C folded
        List<SidePotCalculator.Pot> pots = SidePotCalculator.compute(commits, live);
        assertEquals(1, pots.size());
        assertEquals(300L, pots.get(0).amount);
        assertTrue(!pots.get(0).eligibleUids.contains("C"));
    }
}

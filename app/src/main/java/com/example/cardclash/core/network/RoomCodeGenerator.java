package com.example.cardclash.core.network;

import java.security.SecureRandom;

public final class RoomCodeGenerator {
    private static final SecureRandom RNG = new SecureRandom();

    private RoomCodeGenerator() {}

    /** 6-digit numeric room code, zero-padded. */
    public static String next() {
        int n = RNG.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}

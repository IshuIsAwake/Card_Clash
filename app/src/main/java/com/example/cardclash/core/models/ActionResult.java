package com.example.cardclash.core.models;

public class ActionResult {
    public final boolean ok;
    public final String reason;

    private ActionResult(boolean ok, String reason) {
        this.ok = ok;
        this.reason = reason;
    }

    public static ActionResult ok() { return new ActionResult(true, null); }
    public static ActionResult fail(String reason) { return new ActionResult(false, reason); }
}

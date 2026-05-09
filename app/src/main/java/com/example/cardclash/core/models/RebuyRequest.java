package com.example.cardclash.core.models;

public class RebuyRequest {
    public String requestId;
    public String playerUid;
    public long amount;
    public long requestedAt;
    public String status; // PENDING / APPROVED / DENIED

    public RebuyRequest() {}

    public RebuyRequest(String requestId, String playerUid, long amount) {
        this.requestId = requestId;
        this.playerUid = playerUid;
        this.amount = amount;
        this.requestedAt = System.currentTimeMillis();
        this.status = "PENDING";
    }
}

package com.example.cardclash.core.models;

public class Player {
    public String uid;
    public String displayName;
    public int seat;
    public long chips;
    public boolean connected;
    public boolean host;

    public Player() {}

    public Player(String uid, String displayName, int seat, long chips, boolean host) {
        this.uid = uid;
        this.displayName = displayName;
        this.seat = seat;
        this.chips = chips;
        this.host = host;
        this.connected = true;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getSeat() { return seat; }
    public void setSeat(int seat) { this.seat = seat; }
    public long getChips() { return chips; }
    public void setChips(long chips) { this.chips = chips; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }
}

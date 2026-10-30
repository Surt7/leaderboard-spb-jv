package com.worldpinball.leaderboard.common;

public record EventId(int id, String name) {

    public String tag() {
        return "[" + id + ":" + name + "]";
    }
}

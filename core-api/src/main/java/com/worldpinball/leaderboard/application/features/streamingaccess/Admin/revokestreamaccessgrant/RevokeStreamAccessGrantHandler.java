package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant;

public interface RevokeStreamAccessGrantHandler {
    RevokeStreamAccessGrantResult handle(RevokeStreamAccessGrantCommand command);
}

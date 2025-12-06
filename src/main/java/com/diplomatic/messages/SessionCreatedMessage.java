package com.diplomatic.messages;

public final class SessionCreatedMessage {
    private final String sessionId;
    private final String userId;

    public SessionCreatedMessage(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
}

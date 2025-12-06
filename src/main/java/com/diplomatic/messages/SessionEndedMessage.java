package com.diplomatic.messages;

public final class SessionEndedMessage {
    private final String sessionId;
    private final boolean success;

    public SessionEndedMessage(String sessionId, boolean success) {
        this.sessionId = sessionId;
        this.success = success;
    }
    public String getSessionId() { return sessionId; }
    public boolean isSuccess() { return success; }
}

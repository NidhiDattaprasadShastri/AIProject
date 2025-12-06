package com.diplomatic.messages;

public class ConversationSavedMessage {
    private final String sessionId;
    private final boolean success;

    public ConversationSavedMessage(String sessionId, boolean success) {
        this.sessionId = sessionId;
        this.success = success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isSuccess() {
        return success;
    }
}

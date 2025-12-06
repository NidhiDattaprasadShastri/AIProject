package com.diplomatic.messages;

public final class SaveConversationMessage {
    private final String sessionId;
    private final String query;
    private final String response;

    public SaveConversationMessage(String sessionId, String query, String response) {
        this.sessionId = sessionId;
        this.query = query;
        this.response = response;
    }
    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public String getResponse() { return response; }
}

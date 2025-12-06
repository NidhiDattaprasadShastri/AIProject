package com.diplomatic.messages;

public class QueryResponseMessage {
    private final String sessionId;
    private final String response;
    private final boolean success;

    public QueryResponseMessage(String sessionId, String response, boolean success) {
        this.sessionId = sessionId;
        this.response = response;
        this.success = success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return success;
    }
}

package com.diplomatic.Models;
import java.time.Instant;

public class ConversationEntry {
    private final String sessionId;
    private final String query;
    private final String response;
    private final Instant timestamp;
    private final String scenario;

    public ConversationEntry(String sessionId, String query, String response, String scenario) {
        this.sessionId = sessionId;
        this.query = query;
        this.response = response;
        this.timestamp = Instant.now();
        this.scenario = scenario;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getQuery() {
        return query;
    }

    public String getResponse() {
        return response;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getScenario() {
        return scenario;
    }

    @Override
    public String toString() {
        return String.format("[%s] Q: %s | R: %s",
                timestamp,
                query.substring(0, Math.min(50, query.length())),
                response.substring(0, Math.min(50, response.length())));
    }
}

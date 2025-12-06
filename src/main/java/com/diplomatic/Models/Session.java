package com.diplomatic.Models;
import java.time.Instant;

public class Session {
    private final String sessionId;
    private final String userId;
    private final Instant createdAt;
    private Instant lastActivityAt;
    private SessionStatus status;

    public enum SessionStatus {
        ACTIVE, INACTIVE, TERMINATED
    }

    public Session(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.lastActivityAt = Instant.now();
        this.status = SessionStatus.ACTIVE;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void updateActivity() {
        this.lastActivityAt = Instant.now();
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Session{id='%s', user='%s', status=%s, created=%s}",
                sessionId, userId, status, createdAt);
    }
}

package com.diplomatic.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CRITICAL FIX: Must implement CborSerializable because it crosses ClusterSingleton boundary
 */
public final class SessionCreatedMessage implements CborSerializable {
    private final String sessionId;
    private final String userId;

    @JsonCreator
    public SessionCreatedMessage(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("userId") String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
}
package com.diplomatic.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * CRITICAL FIX: Must implement CborSerializable because it crosses ClusterSingleton boundary
 */
public final class UserQueryMessage implements CborSerializable {
    private final String sessionId;
    private final String query;
    private final Instant timestamp;
    private final ActorRef<String> replyTo;

    @JsonCreator
    public UserQueryMessage(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("query") String query,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("replyTo") ActorRef<String> replyTo) {
        this.sessionId = sessionId;
        this.query = query;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.replyTo = replyTo;
    }

    // Convenience constructor for when timestamp is not provided
    public UserQueryMessage(String sessionId, String query, ActorRef<String> replyTo) {
        this(sessionId, query, Instant.now(), replyTo);
    }

    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public Instant getTimestamp() { return timestamp; }
    public ActorRef<String> getReplyTo() { return replyTo; }
}
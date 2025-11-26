package com.diplomatic.messages;
import akka.actor.typed.ActorRef;

import java.time.Instant;

public final class UserQueryMessage {
    private final String sessionId;
    private final String query;
    private final Instant timestamp;
    private final ActorRef<String> replyTo; // Will send response back here

    public UserQueryMessage(String sessionId, String query, ActorRef<String> replyTo) {
        this.sessionId = sessionId;
        this.query = query;
        this.timestamp = Instant.now();
        this.replyTo = replyTo;
    }

    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public Instant getTimestamp() { return timestamp; }
    public ActorRef<String> getReplyTo() { return replyTo; }
}

package com.diplomatic.messages;
import akka.actor.typed.ActorRef;

/**
 * Message to route query to classifier - crosses cluster boundaries
 */
public final class RouteToClassifierMessage implements CborSerializable {
    private final String sessionId;
    private final String query;
    private final ActorRef<ClassificationResultMessage> replyTo;

    public RouteToClassifierMessage(String sessionId, String query, ActorRef<ClassificationResultMessage> replyTo) {
        this.sessionId = sessionId;
        this.query = query;
        this.replyTo = replyTo;
    }
    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public ActorRef<ClassificationResultMessage> getReplyTo() { return replyTo; }
}
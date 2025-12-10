package com.diplomatic.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RouteToClassifierMessage implements CborSerializable {
    private final String sessionId;
    private final String query;
    private final ActorRef<ClassificationResultMessage> replyTo;

    @JsonCreator
    public RouteToClassifierMessage(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("query") String query,
            @JsonProperty("replyTo") ActorRef<ClassificationResultMessage> replyTo) {
        this.sessionId = sessionId;
        this.query = query;
        this.replyTo = replyTo;
    }

    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public ActorRef<ClassificationResultMessage> getReplyTo() { return replyTo; }
}
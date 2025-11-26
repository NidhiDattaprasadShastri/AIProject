package com.diplomatic.messages;
import akka.actor.typed.ActorRef;
public final class EndSessionMessage {
    private final String sessionId;
    private final ActorRef<SessionEndedMessage> replyTo;

    public EndSessionMessage(String sessionId, ActorRef<SessionEndedMessage> replyTo) {
        this.sessionId = sessionId;
        this.replyTo = replyTo;
    }

    public String getSessionId() { return sessionId; }
    public ActorRef<SessionEndedMessage> getReplyTo() { return replyTo; }
}

package com.diplomatic.messages;
import akka.actor.typed.ActorRef;
public final class StartSessionMessage {
    private final String userId;
    private final ActorRef<SessionCreatedMessage> replyTo;
    public StartSessionMessage(String userId, ActorRef<SessionCreatedMessage> replyTo) {
        this.userId = userId;
        this.replyTo = replyTo;
    }
    public String getUserId() { return userId; }
    public ActorRef<SessionCreatedMessage> getReplyTo() { return replyTo; }
}

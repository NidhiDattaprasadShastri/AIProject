package com.diplomatic.messages;

import akka.actor.typed.ActorRef;

public final class DiplomaticPrimitiveRequestMessage {
    private final String primitive; // "PROPOSE", "CLARIFY", etc.
    private final String query;
    private final ActorRef<DiplomaticPrimitiveResponseMessage> replyTo;

    public DiplomaticPrimitiveRequestMessage(String primitive, String query,
                                             ActorRef<DiplomaticPrimitiveResponseMessage> replyTo) {
        this.primitive = primitive;
        this.query = query;
        this.replyTo = replyTo;
    }

    public String getPrimitive() { return primitive; }
    public String getQuery() { return query; }
    public ActorRef<DiplomaticPrimitiveResponseMessage> getReplyTo() { return replyTo; }
}

package com.diplomatic.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DiplomaticPrimitiveRequestMessage implements CborSerializable {
    private final String primitive;
    private final String query;
    private final ActorRef<DiplomaticPrimitiveResponseMessage> replyTo;

    @JsonCreator
    public DiplomaticPrimitiveRequestMessage(
            @JsonProperty("primitive") String primitive,
            @JsonProperty("query") String query,
            @JsonProperty("replyTo") ActorRef<DiplomaticPrimitiveResponseMessage> replyTo) {
        this.primitive = primitive;
        this.query = query;
        this.replyTo = replyTo;
    }

    public String getPrimitive() { return primitive; }
    public String getQuery() { return query; }
    public ActorRef<DiplomaticPrimitiveResponseMessage> getReplyTo() { return replyTo; }
}
package com.diplomatic.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CulturalAnalysisRequest implements CulturalAnalysisRequestMessage, CborSerializable {
    private final String query;
    private final String country;
    private final ActorRef<CulturalAnalysisResponseMessage> replyTo;

    @JsonCreator
    public CulturalAnalysisRequest(
            @JsonProperty("query") String query,
            @JsonProperty("country") String country,
            @JsonProperty("replyTo") ActorRef<CulturalAnalysisResponseMessage> replyTo) {
        this.query = query;
        this.country = country;
        this.replyTo = replyTo;
    }

    @Override
    public String getQuery() { return query; }

    @Override
    public String getCountry() { return country; }

    @Override
    public ActorRef<CulturalAnalysisResponseMessage> getReplyTo() { return replyTo; }
}
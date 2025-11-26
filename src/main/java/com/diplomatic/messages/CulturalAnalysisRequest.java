package com.diplomatic.messages;

import akka.actor.typed.ActorRef;

public final class CulturalAnalysisRequest implements CulturalAnalysisRequestMessage {
    private final String query;
    private final String country;
    private final ActorRef<CulturalAnalysisResponseMessage> replyTo;

    public CulturalAnalysisRequest(String query, String country,
                                   ActorRef<CulturalAnalysisResponseMessage> replyTo) {
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

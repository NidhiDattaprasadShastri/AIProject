package com.diplomatic.messages;

import akka.actor.typed.ActorRef;

public interface CulturalAnalysisRequestMessage extends CborSerializable {
    String getQuery();
    String getCountry();
    ActorRef<CulturalAnalysisResponseMessage> getReplyTo();
}
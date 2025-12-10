package com.diplomatic.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SaveConversationMessage implements CborSerializable {
    private final String sessionId;
    private final String query;
    private final String response;

    @JsonCreator
    public SaveConversationMessage(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("query") String query,
            @JsonProperty("response") String response) {
        this.sessionId = sessionId;
        this.query = query;
        this.response = response;
    }

    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public String getResponse() { return response; }
}
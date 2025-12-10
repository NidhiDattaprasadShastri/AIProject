package com.diplomatic.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class LLMResponseMessage implements CborSerializable {
    private final String response;
    private final boolean success;

    @JsonCreator
    public LLMResponseMessage(
            @JsonProperty("response") String response,
            @JsonProperty("success") boolean success) {
        this.response = response;
        this.success = success;
    }

    public String getResponse() { return response; }
    public boolean isSuccess() { return success; }
}
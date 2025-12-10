package com.diplomatic.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DiplomaticPrimitiveResponseMessage implements CborSerializable {
    private final String primitive;
    private final String result;

    @JsonCreator
    public DiplomaticPrimitiveResponseMessage(
            @JsonProperty("primitive") String primitive,
            @JsonProperty("result") String result) {
        this.primitive = primitive;
        this.result = result;
    }

    public String getPrimitive() { return primitive; }
    public String getResult() { return result; }
}
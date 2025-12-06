package com.diplomatic.messages;

public final class DiplomaticPrimitiveResponseMessage {
    private final String primitive;
    private final String result;

    public DiplomaticPrimitiveResponseMessage(String primitive, String result) {
        this.primitive = primitive;
        this.result = result;
    }
    public String getPrimitive() { return primitive; }
    public String getResult() { return result; }
}

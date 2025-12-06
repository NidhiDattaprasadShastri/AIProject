package com.diplomatic.messages;

public final class LLMResponseMessage {
    private final String response;
    private final boolean success;

    public LLMResponseMessage(String response, boolean success) {
        this.response = response;
        this.success = success;
    }
    public String getResponse() { return response; }
    public boolean isSuccess() { return success; }
}

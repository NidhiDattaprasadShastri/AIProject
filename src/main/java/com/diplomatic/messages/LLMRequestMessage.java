package com.diplomatic.messages;

import akka.actor.typed.ActorRef;

import java.util.Map;

public final class LLMRequestMessage {
    private final String prompt;
    private final Map<String, Object> context;
    private final ActorRef<LLMResponseMessage> replyTo;

    public LLMRequestMessage(String prompt, Map<String, Object> context, ActorRef<LLMResponseMessage> replyTo) {
        this.prompt = prompt;
        this.context = context;
        this.replyTo = replyTo;
    }
    public String getPrompt() { return prompt; }
    public Map<String, Object> getContext() { return context; }
    public ActorRef<LLMResponseMessage> getReplyTo() { return replyTo; }
}

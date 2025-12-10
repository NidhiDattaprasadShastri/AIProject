package com.diplomatic.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public final class LLMRequestMessage implements CborSerializable {
    private final String prompt;
    private final Map<String, Object> context;
    private final ActorRef<LLMResponseMessage> replyTo;

    @JsonCreator
    public LLMRequestMessage(
            @JsonProperty("prompt") String prompt,
            @JsonProperty("context") Map<String, Object> context,
            @JsonProperty("replyTo") ActorRef<LLMResponseMessage> replyTo) {
        this.prompt = prompt;
        this.context = context;
        this.replyTo = replyTo;
    }

    public String getPrompt() { return prompt; }
    public Map<String, Object> getContext() { return context; }
    public ActorRef<LLMResponseMessage> getReplyTo() { return replyTo; }
}

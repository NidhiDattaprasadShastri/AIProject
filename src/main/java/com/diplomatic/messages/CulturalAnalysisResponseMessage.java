package com.diplomatic.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public final class CulturalAnalysisResponseMessage implements CborSerializable {
    private final String analysis;
    private final Map<String, Object> context;

    @JsonCreator
    public CulturalAnalysisResponseMessage(
            @JsonProperty("analysis") String analysis,
            @JsonProperty("context") Map<String, Object> context) {
        this.analysis = analysis;
        this.context = context;
    }

    public String getAnalysis() { return analysis; }
    public Map<String, Object> getContext() { return context; }
}

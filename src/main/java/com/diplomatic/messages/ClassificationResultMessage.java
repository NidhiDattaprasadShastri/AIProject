package com.diplomatic.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ClassificationResultMessage implements CborSerializable {
    private final String scenario;
    private final String targetActor;
    private final double confidence;
    private final String detectedCountry;
    private final String detectedPrimitive;

    @JsonCreator
    public ClassificationResultMessage(
            @JsonProperty("scenario") String scenario,
            @JsonProperty("targetActor") String targetActor,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("detectedCountry") String detectedCountry,
            @JsonProperty("detectedPrimitive") String detectedPrimitive) {
        this.scenario = scenario;
        this.targetActor = targetActor;
        this.confidence = confidence;
        this.detectedCountry = detectedCountry;
        this.detectedPrimitive = detectedPrimitive;
    }

    public String getScenario() { return scenario; }
    public String getTargetActor() { return targetActor; }
    public double getConfidence() { return confidence; }
    public String getDetectedCountry() { return detectedCountry; }
    public String getDetectedPrimitive() { return detectedPrimitive; }
}
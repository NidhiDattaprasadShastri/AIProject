package com.diplomatic.messages;

public final class ClassificationResultMessage {
    private final String scenario; // "CULTURAL" or "PRIMITIVE"
    private final String targetActor;
    private final double confidence;
    private final String detectedCountry;
    private final String detectedPrimitive;

    public ClassificationResultMessage(String scenario, String targetActor,
                                       double confidence, String detectedCountry,
                                       String detectedPrimitive) {
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

package com.diplomatic.messages;

import java.util.Map;

public final class CulturalAnalysisResponseMessage {
    private final String analysis;
    private final Map<String, Object> context;

    public CulturalAnalysisResponseMessage(String analysis, Map<String, Object> context) {
        this.analysis = analysis;
        this.context = context;
    }

    public String getAnalysis() { return analysis; }
    public Map<String, Object> getContext() { return context; }
}

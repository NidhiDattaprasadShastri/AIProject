package com.diplomatic.Models;
import java.util.HashMap;
import java.util.Map;

public class UserContext {
    private final String userId;
    private final Map<String, Object> preferences;
    private String primaryCountry;
    private String targetCountry;

    public UserContext(String userId) {
        this.userId = userId;
        this.preferences = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public void setPreference(String key, Object value) {
        preferences.put(key, value);
    }

    public Object getPreference(String key) {
        return preferences.get(key);
    }

    public String getPrimaryCountry() {
        return primaryCountry;
    }

    public void setPrimaryCountry(String primaryCountry) {
        this.primaryCountry = primaryCountry;
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public void setTargetCountry(String targetCountry) {
        this.targetCountry = targetCountry;
    }

    public Map<String, Object> getAllPreferences() {
        return new HashMap<>(preferences);
    }
}

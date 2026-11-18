package com.example.brady_jack_s2327712.model;

/**
 * Categories the nature of situation with clear text identifiers for
 * easy reusability (still to get colour and icon indicators)
 * ----
 * AI proposed an enum instead of alternatives for its
 * protection against invalid inputs only accepting specified values
 * after prompted to check an int constants system for robustness
 */
public enum SituationCategory {
    ROAD_CLOSED("Road closed", false),
    QUEUING_TRAFFIC("Queuing traffic", false),
    LANE_RESTRICTION("Lane restriction", false),
    ROADWORKS("Roadworks", true),
    ROADWORKS_DELAYS("Roadworks - delays", true),
    ROADWORKS_LONG_DELAYS("Roadworks - long delays", true),

    // Other Incident category added to catch unknown incidents without crashing
    OTHER_INCIDENT("Incident", false);

    private final String label;
    private final boolean roadworks;

    SituationCategory(String label, boolean roadworks) {
        this.label = label;
        this.roadworks = roadworks;
    }

    public String getLabel() { return label; }
    public boolean isRoadworks() { return roadworks; }
}
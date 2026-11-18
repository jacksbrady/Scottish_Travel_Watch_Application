package com.example.brady_jack_s2327712.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import com.example.brady_jack_s2327712.R;

/**
 * Categories the nature of situation with clear text identifiers for
 * easy reusability (still to get colour and icon indicators)
 * ----
 * AI proposed an enum instead of alternatives for its
 * protection against invalid inputs only accepting specified values
 * after prompted to check an int constants system for robustness
 */
public enum SituationCategory {
    ROAD_CLOSED("Road closed", false, R.drawable.ic_road_closed, R.color.cat_road_closed),
    QUEUING_TRAFFIC("Queuing traffic", false, R.drawable.ic_traffic, R.color.cat_queuing),
    LANE_RESTRICTION("Lane restriction", false, R.drawable.ic_lane_restriction, R.color.cat_lane_restriction),
    ROADWORKS("Roadworks", true, R.drawable.ic_roadworks, R.color.cat_roadworks),
    ROADWORKS_DELAYS("Roadworks - delays", true, R.drawable.ic_roadworks_delays, R.color.cat_roadworks_delays),
    ROADWORKS_LONG_DELAYS("Roadworks - long delays", true, R.drawable.ic_roadworks_long_delays, R.color.cat_roadworks_long_delays),

    // Other Incident category added to catch unknown incidents without crashing
    OTHER_INCIDENT("Incident", false, R.drawable.ic_incident, R.color.cat_incident);

    private final String label;
    private final boolean roadworks;
    @DrawableRes private final int iconRes;
    @ColorRes private final int colourRes;

    SituationCategory(String label, boolean roadworks, @DrawableRes int iconRes, @ColorRes int colourRes) {
        this.label = label;
        this.roadworks = roadworks;
        this.iconRes = iconRes;
        this.colourRes = colourRes;
    }

    public String getLabel() { return label; }
    public boolean isRoadworks() { return roadworks; }
    public int getIconRes() { return iconRes; }
    public int getColourRes() { return colourRes; }
    }
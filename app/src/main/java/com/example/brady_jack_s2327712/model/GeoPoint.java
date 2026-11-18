package com.example.brady_jack_s2327712.model;

import java.util.Locale;

/**
 * Creates a reusable class representing a single point removing
 * repetition of variables ie: latitude, longitude and place name
 */
public class GeoPoint {

    // NaN = "not found in the XML", ensures coordinates not set
    // to 0 which would be a valid but incorrect coordinate
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String placeName;
    private String junction;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getJunction() { return junction; }
    public void setJunction(String junction) { this.junction = junction; }

    // True only if both coordinates were found
    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    // Best available human-readable name for this point.
    public String getDisplayName() {
        if (placeName != null) return placeName;
        if (junction != null) return junction;
        return String.format(Locale.UK, "%.4f, %.4f", latitude, longitude);
    }

    // Readable text for logcat: Coordinates and Location
    @Override
    public String toString() {
        return getDisplayName() + " (" + latitude + ", " + longitude + ")";
    }
}
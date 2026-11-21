package com.example.brady_jack_s2327712.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TrafficParser creates a Situation for each situationRecord and fills in its fields
 * Also works out missing XML values where possible and provides readable text used by the UI
 */
public class Situation {

    // Turns the XML's dates into a more readable format
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    private final boolean fromRoadworksFeed;

    // Raw values taken from the XML
    private String id;
    private String recordType;
    private String subType;
    private String comment;
    private String roadName;
    private String direction;
    private String probability;
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer operationalLanes;
    private Integer restrictedLanes;
    private String delaysType;
    private boolean urgent;
    private GeoPoint from;
    private GeoPoint to;

    // The situation category is not in the XML so is worked out in decideCategory()
    private SituationCategory category;

    public Situation(boolean fromRoadworksFeed) {
        this.fromRoadworksFeed = fromRoadworksFeed;
    }

    // Called by the parser at </situationRecord>, once all fields are filled in
    public void finishParsing() {

        // Incidents/roadworks without a road in the location seeks
        // for the missing information in comment to more consistently
        // display road names
        if (roadName == null && comment != null) {
            roadName = findRoadInText(comment);
        }
        category = decideCategory();
    }

    /**
     * Decision list selecting the appropriate Situation Category to be added to the
     * situation, with the first match being selected
     * <p>
     * Queueing is checked before lane restrictions because queueing records include
     * lane restrictions
     */

    private SituationCategory decideCategory() {
        if (fromRoadworksFeed || "MaintenanceWorks".equals(recordType)) {
            if (delaysType == null) return SituationCategory.ROADWORKS;
            if (delaysType.equals("delays")) return SituationCategory.ROADWORKS_DELAYS;
            return SituationCategory.ROADWORKS_LONG_DELAYS;
        }
        if ("roadClosed".equals(subType) || (operationalLanes != null && operationalLanes == 0)) {
            return SituationCategory.ROAD_CLOSED;
        }
        if ("AbnormalTraffic".equals(recordType)) return SituationCategory.QUEUING_TRAFFIC;
        if (restrictedLanes != null && restrictedLanes > 0)
            return SituationCategory.LANE_RESTRICTION;
        return SituationCategory.OTHER_INCIDENT;
    }

    // --- Readable Formatting ---
    public String getReadableSubType() {
        return camelCaseToWords(subType);
    }

    public String getReadableDirection() {
        return camelCaseToWords(direction);
    }

    public String getImpactSummary() {
        if (operationalLanes != null) {
            return operationalLanes == 0 ? "All lanes closed" : operationalLanes + " lane(s) open";
        }
        if (restrictedLanes != null) return restrictedLanes + " lane(s) restricted";
        if (delaysType != null) return camelCaseToWords(delaysType);
        return "No delays reported";
    }

    public String getStartText() {
        return formatTime(startTime);
    }

    public String getEndText() {
        return endTime == null ? "Until further notice" : formatTime(endTime);
    }

    public String getDurationText() {
        if (startTime == null) return "Unknown";
        if (endTime == null) {
            long days = ChronoUnit.DAYS.between(startTime, OffsetDateTime.now());
            if (days < 0) return "Not started yet";
            return days == 0 ? "Ongoing - started today" : "Ongoing - started " + days + " day(s) ago";
        }
        long hours = ChronoUnit.HOURS.between(startTime, endTime);
        if (hours < 24) return hours + " hour(s)";
        long days = hours / 24;
        if (days < 7) return days + " day(s)";
        return days + " day(s) (about " + (days / 7) + " week(s))";
    }

    public String getCreationText() {
        return formatTime(creationTime);
    }

    public String getLastUpdatedText() {
        return formatTime(versionTime);
    }


    // Collects the from/to points with valid coordinates into a single list
    public List<GeoPoint> getPoints() {
        List<GeoPoint> points = new ArrayList<>();
        if (from != null && from.hasCoordinates()) points.add(from);
        if (to != null && to.hasCoordinates()) points.add(to);
        return points;
    }

    // Finds first road name in a sentence to be added to the situations details
    public static String findRoadInText(String text) {
        String[] words = text.split(" ");
        for (String word : words) {
            // remove punctuation stuck to the word, e.g. "A78," -> "A78"
            String w = word.replace(",", "").replace(".", "").replace(":", "");
            if (w.length() >= 2
                    && (w.startsWith("M") || w.startsWith("A") || w.startsWith("B"))
                    && Character.isDigit(w.charAt(1))) {
                return w;
            }
        }
        return null;   // no road name found
    }

    // Splits camelCase XML values into words
    public static String camelCaseToWords(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(Character.toLowerCase(c));

            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatTime(OffsetDateTime time) {
        return time == null ? "Unknown" : time.format(DISPLAY_FORMAT);
    }

    // --- Search helpers ---

    // Returns a true value if the specific search contains an incident
    //
    // A longer road name is not matched e.g. M80 is not returned by an M8 search

    public boolean isOnRoad(String query) {
        if (query == null || query.trim().isEmpty()) return true;
        if (roadName == null) return false;
        String q = query.replace(" ", "").toUpperCase(Locale.UK);
        String road = roadName.replace(" ", "").toUpperCase(Locale.UK);
        if (road.equals(q)) return true;
        if (!road.startsWith(q)) return false;
        char lastTyped = q.charAt(q.length() - 1);
        if (!Character.isDigit(lastTyped)) return true;
        return !Character.isDigit(road.charAt(q.length()));
    }

    // Gathers the distance to the nearest end of situation from a selected location
    public double distanceKmFrom(double latitude, double longitude) {
        double nearest = Double.MAX_VALUE;
        for (GeoPoint point : getPoints()) {
            nearest = Math.min(nearest,
                    haversineKm(latitude, longitude, point.getLatitude(), point.getLongitude()));
        }
        return nearest;
    }

    // Finds the distance between 2 individual points factoring the Earth's curvature
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // --- Getters and setters ---

    public boolean isFromRoadworksFeed() {
        return fromRoadworksFeed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public OffsetDateTime getVersionTime() {
        return versionTime;
    }

    public void setVersionTime(OffsetDateTime versionTime) {
        this.versionTime = versionTime;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(Integer operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    public Integer getRestrictedLanes() {
        return restrictedLanes;
    }

    public void setRestrictedLanes(Integer restrictedLanes) {
        this.restrictedLanes = restrictedLanes;
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public GeoPoint getFrom() {
        return from;
    }

    public void setFrom(GeoPoint from) {
        this.from = from;
    }

    public GeoPoint getTo() {
        return to;
    }

    public void setTo(GeoPoint to) {
        this.to = to;
    }

    public SituationCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return category.getLabel() + " | " + roadName + " | " + comment
                + "\n   impact: " + getImpactSummary()
                + " | " + getStartText() + " -> " + getEndText()
                + "\n   from: " + from
                + "\n   to:   " + to;
    }
}
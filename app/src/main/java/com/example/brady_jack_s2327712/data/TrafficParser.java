package com.example.brady_jack_s2327712.data;

import android.util.Log;

import com.example.brady_jack_s2327712.model.GeoPoint;
import com.example.brady_jack_s2327712.model.Situation;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;


/**
 * Takes Traffic Scotland DATEX II XML String and produces
 * a list of Situation objects using PullParser
 */
public class TrafficParser {

    private static final String TAG = "TrafficParser";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * @param xml            the downloaded XML text
     * @param roadworksFeed  true for the roadworks feed, false for unplanned events
     */
    public ArrayList<Situation> parse(String xml, boolean roadworksFeed)
            throws XmlPullParserException, IOException {

        // Skip anything before the first tag
        int start = xml.indexOf('<');
        if (start < 0) throw new XmlPullParserException("No XML found in the data");
        xml = xml.substring(start);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xml));

        ArrayList<Situation> situations = new ArrayList<>();
        Situation current = null;          // situation being filled in
        GeoPoint currentPoint = null;      // not null while inside <from> or <to>
        boolean inComment = false;         // true while inside <generalPublicComment>
        String lastDescriptorValue = null; // last <value> seen inside a location descriptor

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();

                if (tag.equalsIgnoreCase("situationRecord")) {
                    current = new Situation(roadworksFeed);
                    current.setId(xpp.getAttributeValue(null, "id"));
                    current.setRecordType(xpp.getAttributeValue(XSI_NAMESPACE, "type"));
                }
                else if (current == null) {
                }
                else if (tag.equalsIgnoreCase("situationRecordCreationTime")) {
                    current.setCreationTime(parseTime(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("situationRecordVersionTime")) {
                    current.setVersionTime(parseTime(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("probabilityOfOccurrence")) {
                    current.setProbability(xpp.nextText().trim());
                }
                else if (tag.equalsIgnoreCase("overallStartTime")) {
                    current.setStartTime(parseTime(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("overallEndTime")) {
                    current.setEndTime(parseTime(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("numberOfOperationalLanes")) {
                    current.setOperationalLanes(parseInteger(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("numberOfLanesRestricted")) {
                    current.setRestrictedLanes(parseInteger(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("delaysType")) {
                    current.setDelaysType(xpp.nextText().trim());
                }
                else if (tag.equalsIgnoreCase("generalPublicComment")) {
                    inComment = true;
                }
                else if (tag.equalsIgnoreCase("value")) {
                    String text = xpp.nextText().trim();
                    if (inComment) {
                        current.setComment(text);
                    } else if (currentPoint != null) {
                        lastDescriptorValue = text;
                    }
                }
                else if (tag.equalsIgnoreCase("tpegDirection")) {
                    current.setDirection(xpp.nextText().trim());
                }
                else if (tag.equalsIgnoreCase("from") || tag.equalsIgnoreCase("to")) {
                    currentPoint = new GeoPoint();
                }
                else if (tag.equalsIgnoreCase("latitude") && currentPoint != null) {
                    currentPoint.setLatitude(parseDouble(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("longitude") && currentPoint != null) {
                    currentPoint.setLongitude(parseDouble(xpp.nextText()));
                }
                else if (tag.equalsIgnoreCase("tpegIlcPointDescriptorType")) {
                    String type = xpp.nextText().trim();
                    if (type.equals("tpegIlcName1") && current.getRoadName() == null) {
                        current.setRoadName(lastDescriptorValue);
                    }
                }
                else if (tag.equalsIgnoreCase("tpegOtherPointDescriptorType")) {
                    String type = xpp.nextText().trim();
                    if (type.equals("linkName")) {
                        if (current.getRoadName() == null) current.setRoadName(lastDescriptorValue);
                    } else if (currentPoint != null) {
                        currentPoint.setPlaceName(lastDescriptorValue);
                    }
                }
                else if (tag.equalsIgnoreCase("tpegJunctionPointDescriptorType")) {
                    String type = xpp.nextText().trim();
                    if (type.equals("junctionName") && currentPoint != null) {
                        currentPoint.setJunction(lastDescriptorValue);
                    }
                }
                else if (tag.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                        || tag.equalsIgnoreCase("abnormalTrafficType")
                        || tag.equalsIgnoreCase("roadMaintenanceType")) {
                    current.setSubType(xpp.nextText().trim());
                }
                else if (tag.equalsIgnoreCase("urgentRoadworks")) {
                    current.setUrgent(xpp.nextText().trim().equalsIgnoreCase("true"));
                }
            }
            else if (eventType == XmlPullParser.END_TAG && current != null) {
                String tag = xpp.getName();

                if (tag.equalsIgnoreCase("generalPublicComment")) {
                    inComment = false;
                }
                else if (tag.equalsIgnoreCase("from")) {
                    current.setFrom(currentPoint);
                    currentPoint = null;
                }
                else if (tag.equalsIgnoreCase("to")) {
                    current.setTo(currentPoint);
                    currentPoint = null;
                }
                else if (tag.equalsIgnoreCase("situationRecord")) {
                    current.finishParsing();
                    situations.add(current);
                    current = null;
                }
            }

            eventType = xpp.next();   // get the next event and repeat
        }

        Log.d(TAG, "Parsed " + situations.size() + (roadworksFeed ? " roadworks" : " incidents"));
        return situations;
    }

    private static OffsetDateTime parseTime(String text) {
        try {
            return OffsetDateTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            Log.w(TAG, "Unreadable date: " + text);
            return null;
        }
    }

    private static Integer parseInteger(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Unreadable number: " + text);
            return null;
        }
    }

    private static double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Unreadable coordinate: " + text);
            return Double.NaN;
        }
    }
}
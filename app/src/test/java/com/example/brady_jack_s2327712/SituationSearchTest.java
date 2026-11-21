package com.example.brady_jack_s2327712;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.brady_jack_s2327712.model.GeoPoint;
import com.example.brady_jack_s2327712.model.Situation;

import org.junit.Test;

public class SituationSearchTest {

    private static Situation situationOn(String road, double lat, double lon) {
        Situation s = new Situation(true);
        s.setRoadName(road);
        GeoPoint point = new GeoPoint();
        point.setLatitude(lat);
        point.setLongitude(lon);
        s.setFrom(point);
        s.finishParsing();
        return s;
    }

    @Test
    public void roadSearch_matchesExactRoad_ignoringCase() {
        Situation m8 = situationOn("M8", 55.87, -4.27);
        assertTrue(m8.isOnRoad("M8"));
        assertTrue(m8.isOnRoad("m8"));
        assertTrue(m8.isOnRoad(" m 8 "));
    }

    @Test
    public void roadSearch_doesNotMatchLongerRoadNumbers() {
        assertFalse(situationOn("M80", 56.0, -4.0).isOnRoad("M8"));
        assertFalse(situationOn("A740", 55.8, -4.4).isOnRoad("A74"));
    }

    @Test
    public void roadSearch_matchesRoadWithSuffix() {
        assertTrue(situationOn("A74(M)", 55.1, -3.3).isOnRoad("A74"));
    }

    @Test
    public void roadSearch_partialLetterShowsAllRoadsOfThatType() {
        assertTrue(situationOn("M8", 55.87, -4.27).isOnRoad("M"));
        assertFalse(situationOn("A82", 56.0, -4.6).isOnRoad("M"));
    }

    @Test
    public void roadSearch_emptyQueryMatchesEverything() {
        assertTrue(situationOn("A9", 56.5, -3.6).isOnRoad(""));
        assertTrue(situationOn("A9", 56.5, -3.6).isOnRoad(null));
    }

    @Test
    public void distance_glasgowToEdinburgh_isAbout67km() {
        Situation edinburgh = situationOn("A8", 55.9533, -3.1883);
        assertEquals(67.0, edinburgh.distanceKmFrom(55.8666, -4.2504), 2.0);
    }

    @Test
    public void distance_noCoordinates_isNeverWithinRange() {
        Situation noPoints = new Situation(true);
        noPoints.finishParsing();
        assertTrue(noPoints.distanceKmFrom(55.8666, -4.2504) > 100000);
    }

    @Test
    public void findRoadInText_findsFirstRoad() {
        assertEquals("A78", Situation.findRoadInText("The A78 is closed, diversions in place."));
        assertNull(Situation.findRoadInText("Essential works at Brechin"));
    }

    @Test
    public void findRoadInText_keepsOnlyTheRoadNumber() {
        assertEquals("M8", Situation.findRoadInText("M8MONKLANDS"));
        assertEquals("A74(M)", Situation.findRoadInText("A74(M) J19 Eaglesfield"));
        assertNull(Situation.findRoadInText("Off Network"));
        assertNull(Situation.findRoadInText(null));
    }
}
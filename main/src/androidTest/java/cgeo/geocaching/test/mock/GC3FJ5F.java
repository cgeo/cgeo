package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GC3FJ5F extends MockedCache {

    public GC3FJ5F() {
        super(new Geopoint(51.3398833, 7.0304833));
    }

    @Override
    public String getName() {
        return "Herminghauspark";
    }

    @Override
    public float getDifficulty() {
        return 1.0f;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return "GC3FJ5F";
    }

    @Override
    public String getOwnerDisplayName() {
        return "Lineflyer";
    }

    @NonNull
    @Override
    public String getOwnerUserId() {
        return getOwnerDisplayName();
    }

    @NonNull
    @Override
    public CacheSize getSize() {
        return CacheSize.LARGE;
    }

    @Override
    public float getTerrain() {
        return 2.0f;
    }

    @Override
    public CacheType getType() {
        return CacheType.MULTI;
    }

    @Override
    public String getHint() {
        return "[FINAL] Geteilter Baumstumpf, dann ganz unten (Spoiler-Bild)";
    }

    @Override
    public String getDescription() {
        return "<b>Update 31.12.2014:</b><br> Koordinaten für Station 2 neu eingemessen.<br>";
    }

    @Override
    public String getShortDescription() {
        return "<b>Spaziergang durch den Velberter Herminghauspark.</b><br> <br> Ich möchte euch einen Besuch im Velberter Herminghauspark nahelegen. Hier wurde in den letzten Jahren viel getan um den Park attraktiv für Besucher und insbesondere für Familien zu machen. Aber seht selbst...<br>";
    }

    @Override
    public String getCacheId() {
        return "2823776";
    }

    @Override
    public String getGuid() {
        return "ba954eb7-29c5-4b4a-a6d8-8546b05b5bae";
    }

    @Override
    public String getLocation() {
        return "Nordrhein-Westfalen, Germany";
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.test.mock.MockedCache#isOwn()
     */
    @Override
    public boolean isOwner() {
        if ("Lineflyer".equals(Settings.getUserName())) {
            return true;
        }
        return super.isOwner();
    }

    @Override
    public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2012-03-29", getDateFormat());
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @NonNull
    @Override
    public List<String> getAttributes() {
        final String[] attributes = {
                "night_no",
                "picnic_yes",
                "restrooms_yes",
                "touristok_yes",
                "parking_yes",
                "wheelchair_yes",
                "food_yes",
                "hike_med_yes",
                "winter_no",
                "onehour_yes",
                "kids_yes",
                "stroller_yes",
                "dogs_yes"
        };
        return new MockedLazyInitializedList<>(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
        logCounts.put(LogType.PUBLISH_LISTING, 1);
        logCounts.put(LogType.FOUND_IT, 411);
        logCounts.put(LogType.DIDNT_FIND_IT, 2);
        logCounts.put(LogType.NOTE, 19);
        logCounts.put(LogType.ENABLE_LISTING, 8);
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 8);
        logCounts.put(LogType.OWNER_MAINTENANCE, 14);
        logCounts.put(LogType.NEEDS_MAINTENANCE, 4);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 53;
    }

    @Override
    @NonNull
    public List<Image> getSpoilers() {
        return Collections.singletonList(Image.NONE);
    }

    @NonNull
    @Override
    public List<Waypoint> getWaypoints() {
        final List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(new Waypoint("PARKNG", new Geopoint("N 51° 20.448 E 007° 01.828"), "Parkplatz", "PK", "Parkplatz für PKW, Motorrad (kostenfrei) und Wohnmobile (kostenpflichtig)", WaypointType.PARKING));
        waypoints.add(new Waypoint("STAGE1", new Geopoint("N 51° 20.393 E 007° 01.829"), "Wasserstrudel", "S1", "Quersumme der vierstelligen Jahreszahl der Stiftung an den Park = A", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("STAGE2", new Geopoint("N 51° 20.316 E 007° 01.825"), "Balancieren", "S2", "Anzahl der Metallstreben von der Mitte nach außen = B", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("STAGE3", new Geopoint("N 51° 20.271 E 007° 01.993"), "Klangstäbe", "S3", "Anzahl der Klangstäbe = C", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("STAGE4", new Geopoint("N 51° 20.402 E 007° 02.042"), "Farbscheiben", "S4", "Wie lange soll man laut der Anleitung auf die Scheiben schauen, bis der gewünschte Effekt eintritt. \nAnzahl Sekunden ist = D", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("STAGE5", new Geopoint("N 51° 20.352 E 007° 01.891"), "Kugelspiel", "S5", "Monat der Stiftung an den Park = E", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("STAGE6", new Geopoint("N 51° 20.392 E 007° 01.866"), "Bank", "S6", "Welche Nummer hat die Bank = F", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("TOILE1", new Geopoint("N 51° 20.362 E 007° 01.796"), "Toilette 1", "T1", "Öffentliche Toilette im Park", WaypointType.WAYPOINT));
        waypoints.add(new Waypoint("TOILE2", new Geopoint("N 51° 20.387 E 007° 02.020"), "Toilette 2", "T2", "Öffentliche Toilette im Park", WaypointType.WAYPOINT));
        waypoints.add(new Waypoint("TABLE", new Geopoint("N 51° 20.355 E 007° 01.979"), "Picknickplatz", "T3", "Picknicktische am Erlebnisspielplatz", WaypointType.WAYPOINT));
        waypoints.add(new Waypoint("START2", new Geopoint("N 51° 20.375 E 007° 02.113"), "Alternativer Start", "Z1", "Zweiter Eingang zum Park", WaypointType.TRAILHEAD));
        waypoints.add(new Waypoint("ZUGANG", new Geopoint("N 51° 20.370 E 007° 01.578"), "Abfahrt vom Panoramaradweg Niederbergbahn", "Z2", "Hier müsst ihr den Panoramaradweg verlassen um zum Herminghauspark zu gelangen.", WaypointType.TRAILHEAD));
        return waypoints;
    }

}

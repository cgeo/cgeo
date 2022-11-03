package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.xml.sax.InputSource;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeokretyParserTest extends AbstractResourceInstrumentationTestCase {

    public void testParse() throws Exception {
        final Application app = CgeoApplication.getInstance();

        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables).hasSize(2);

        // Check first GK in list
        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("c:geo One");
        assertThat(trackable1.getGeocode()).isEqualTo("GKB580");
        assertThat(trackable1.getDistance()).isEqualTo(0);
        assertThat(trackable1.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_traditional));

        // Check second GK in list
        final Trackable trackable2 = trackables.get(1);
        assertThat(trackable2).isNotNull();
        assertThat(trackable2.getName()).isEqualTo("c:geo Two");
        assertThat(trackable2.getGeocode()).isEqualTo("GKB581");
        assertThat(trackable2.getDistance()).isEqualTo(0);
        assertThat(trackable2.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_post));
    }

    public void testParseResponse() throws Exception {
        final ImmutablePair<Integer, List<String>> response1 = GeokretyParser.parseResponse(getFileContent(R.raw.geokret142_xml));
        assertThat(response1).isNotNull();
        assertThat(response1.getLeft()).isNotNull();
        assertThat(response1.getLeft()).isEqualTo(0);
        assertThat(response1.getRight()).isNotNull();
        assertThat(response1.getRight()).hasSize(2);
        assertThat(response1.getRight().get(0)).isEqualTo("Identical log has been submited.");
        assertThat(response1.getRight().get(1)).isEqualTo("There is an entry with this date. Correct the date or the hour.");

        final ImmutablePair<Integer, List<String>> response2 = GeokretyParser.parseResponse(getFileContent(R.raw.geokret143_xml));
        assertThat(response2).isNotNull();
        assertThat(response2.getLeft()).isNotNull();
        assertThat(response2.getLeft()).isEqualTo(27334);
        assertThat(response2.getRight()).isNotNull();
        assertThat(response2.getRight()).hasSize(0);

        final ImmutablePair<Integer, List<String>> response3 = GeokretyParser.parseResponse(getFileContent(R.raw.geokret144_xml));
        assertThat(response3).isNotNull();
        assertThat(response3.getLeft()).isNotNull().isEqualTo(0);
        assertThat(response3.getRight()).isNotNull().hasSize(2);
        assertThat(response3.getRight().get(0)).isEqualTo("Wrong secid");
        assertThat(response3.getRight().get(1)).isEqualTo("Wrond date or time"); // sic
    }

    public static void testGetType() throws Exception {
        final Application app = CgeoApplication.getInstance();
        assertThat(GeokretyParser.getType(0)).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_traditional));
        assertThat(GeokretyParser.getType(1)).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_book_or_media));
        assertThat(GeokretyParser.getType(2)).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_human));
        assertThat(GeokretyParser.getType(3)).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_coin));
        assertThat(GeokretyParser.getType(4)).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_post));
        assertThat(GeokretyParser.getType(5)).isNull();
        assertThat(GeokretyParser.getType(42)).isNull();
    }

    public void testParseNoValueFields() throws Exception {
        final Application app = CgeoApplication.getInstance();

        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret146_xml)));
        assertThat(trackables).hasSize(1);

        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("Wojna");
        assertThat(trackable1.getGeocode()).isEqualTo("GKC241");
        assertThat(trackable1.getReleased()).isNull();
        assertThat(trackable1.getDistance()).isEqualTo(-1.0f);
        assertThat(trackable1.getImage()).isNull();
        assertThat(trackable1.getSpottedType()).isEqualTo(Trackable.SPOTTED_OWNER);
        assertThat(trackable1.getSpottedName()).isNull();
        assertThat(trackable1.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_traditional));
    }

    public void testParseDescription() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret145_xml)));
        assertThat(trackables).hasSize(1);

        // Check first GK in list
        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("c:geo Test");
        assertThat(trackable1.getGeocode()).isEqualTo("GKC240");
        assertThat(trackable1.getDistance()).isEqualTo(2254);
        assertThat(trackable1.getDetails()).isEqualTo("Dieser Geokret dient zum Testen von c:geo.<br />" +
                "Er befindet sich nicht wirklich im gelisteten Cache.<br />" +
                "<br />" +
                "Bitte ignorieren.");
    }

    public void testMissing() throws Exception {
        final List<Trackable> trackables1 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret145_xml)));
        assertThat(trackables1).hasSize(1);
        final Trackable trackable1 = trackables1.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.isMissing()).isTrue();

        final List<Trackable> trackables2 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret146_xml)));
        assertThat(trackables2).hasSize(1);
        final Trackable trackable2 = trackables2.get(0);
        assertThat(trackable2).isNotNull();
        assertThat(trackable2.isMissing()).isFalse();

        final List<Trackable> trackables3 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables3).hasSize(2);
        final Trackable trackable3 = trackables3.get(0);
        assertThat(trackable3).isNotNull();
        assertThat(trackable3.isMissing()).isFalse();
        final Trackable trackable4 = trackables3.get(1);
        assertThat(trackable4).isNotNull();
        assertThat(trackable4.isMissing()).isTrue();
    }

    public void testFullDetails() throws Exception {
        final Application app = CgeoApplication.getInstance();

        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret47496_details_xml)));
        assertThat(trackables).hasSize(1);

        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("c:geo tests");
        assertThat(trackable1.getGeocode()).isEqualTo("GKB988");
        assertThat(trackable1.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_traditional));
        assertThat(trackable1.getOwner()).isEqualTo("kumy");
        assertThat(trackable1.getDistance()).isEqualTo(143);
        assertThat(trackable1.isMissing()).isFalse();
        assertThat(trackable1.getDetails()).isEqualTo("virtual, just for testing c:geo app<br />" +
                "<br />" +
                "<br />" +
                "<br />" +
                "<br />" +
                "Test to break c:geo");
        assertThat(trackable1.getSpottedType()).isEqualTo(Trackable.SPOTTED_USER);
        assertThat(trackable1.getSpottedName()).isEqualTo("gueta");
    }

    public void testLogs() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret47496_details_xml)));
        assertThat(trackables).hasSize(1);
        final Trackable trackable1 = trackables.get(0);

        // Check the logs
        final SynchronizedDateFormat dateFormat = new SynchronizedDateFormat("yyyy-MM-dd kk:mm", TimeZone.getTimeZone("UTC"), Locale.US);
        final List<LogEntry> logs = trackable1.getLogs();
        assertThat(logs).hasSize(6);

        final LogEntry log6 = logs.get(5);
        assertThat(log6.author).isEqualTo("kumy");
        assertThat(log6.id).isEqualTo(673734);
        assertThat(log6.date).isEqualTo(dateFormat.parse("2015-03-29 14:10").getTime());
        assertThat(log6.getDisplayText()).isEqualTo("Test");
        assertThat(log6.logType).isEqualTo(LogType.NOTE);
        assertThat(log6.cacheName).isNullOrEmpty();
        assertThat(log6.cacheGeocode).isNullOrEmpty();

        final LogEntry log5 = logs.get(4);
        assertThat(log5.author).isEqualTo("kumy");
        assertThat(log5.id).isEqualTo(722027);
        assertThat(log5.date).isEqualTo(dateFormat.parse("2015-07-04 00:00").getTime());
        assertThat(log5.getDisplayText()).isEqualTo("Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of \"de Finibus Bonorum et Malorum\" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, \"Lorem ipsum dolor sit amet..\", comes from a line in section 1.10.32.");
        assertThat(log5.logType).isEqualTo(LogType.VISIT);
        assertThat(log5.cacheName).isEqualTo("GC2E895");
        assertThat(log5.cacheGeocode).isEqualTo("GC2E895");

        final LogEntry log4 = logs.get(3);
        assertThat(log4.author).isEqualTo("kumy");
        assertThat(log4.id).isEqualTo(912063);
        assertThat(log4.date).isEqualTo(dateFormat.parse("2016-01-03 12:00").getTime());
        assertThat(log4.getDisplayText()).isEqualTo("test drop");
        assertThat(log4.logType).isEqualTo(LogType.PLACED_IT);
        assertThat(log4.cacheName).isEqualTo("GC5BRQK");
        assertThat(log4.cacheGeocode).isEqualTo("GC5BRQK");

        final LogEntry log3 = logs.get(2);
        assertThat(log3.author).isEqualTo("kumy");
        assertThat(log3.id).isEqualTo(967654);
        assertThat(log3.date).isEqualTo(dateFormat.parse("2016-02-12 12:00").getTime());
        assertThat(log3.getDisplayText()).isEqualTo("Multiline 1<br />" +
                "<br />" +
                "Multiline 2<br />" +
                "Multiline 3<br />" +
                "<br />" +
                "<br />" +
                "<br />" +
                "Multiline 4");
        assertThat(log3.logType).isEqualTo(LogType.DISCOVERED_IT);
        assertThat(log3.cacheName).isEqualTo("GC5BRQK");
        assertThat(log3.cacheGeocode).isEqualTo("GC5BRQK");

        final LogEntry log2 = logs.get(1);
        assertThat(log2.author).isEqualTo("gueta");
        assertThat(log2.id).isEqualTo(967656);
        assertThat(log2.date).isEqualTo(dateFormat.parse("2016-03-21 18:31").getTime());
        assertThat(log2.getDisplayText()).isEqualTo("Grabbed");
        assertThat(log2.logType).isEqualTo(LogType.GRABBED_IT);
        assertThat(log2.cacheName).isNullOrEmpty();
        assertThat(log2.cacheGeocode).isNullOrEmpty();

        final LogEntry log1 = logs.get(0);
        assertThat(log1.author).isEqualTo("kumy");
        assertThat(log1.id).isEqualTo(911689);
        assertThat(log1.date).isEqualTo(dateFormat.parse("2016-05-02 12:00").getTime());
        assertThat(log1.getDisplayText()).isEqualTo("test images in log");
        assertThat(log1.logType).isEqualTo(LogType.NOTE);
        assertThat(log1.cacheName).isNullOrEmpty();
        assertThat(log1.cacheGeocode).isNullOrEmpty();

        final List<Image> logImages1 = log1.logImages;
        assertThat(logImages1).hasSize(2);
        final Image image1 = logImages1.get(0);
        assertThat(image1.getTitle()).isEqualTo("test logo 2");
        assertThat(image1.getUrl()).isEqualTo("https://geokrety.org/obrazki/14622133503lxv2.png");
        final Image image2 = logImages1.get(1);
        assertThat(image2.getTitle()).isEqualTo("test 1");
        assertThat(image2.getUrl()).isEqualTo("https://geokrety.org/obrazki/1462213331lhaiu.png");
    }

    public static void testGetLastSpottedUsername() throws Exception {

        final LogEntry note = new LogEntry.Builder()
                .setLogType(LogType.NOTE)
                .setAuthor("authorNote")
                .build();

        final LogEntry placed = new LogEntry.Builder()
                .setLogType(LogType.PLACED_IT)
                .setAuthor("authorPlaced")
                .build();

        final LogEntry grabbed = new LogEntry.Builder()
                .setLogType(LogType.GRABBED_IT)
                .setAuthor("authorGrabbed")
                .build();

        final LogEntry visit = new LogEntry.Builder()
                .setLogType(LogType.VISIT)
                .setAuthor("authorVisit")
                .build();

        final LogEntry discovered = new LogEntry.Builder()
                .setLogType(LogType.DISCOVERED_IT)
                .setAuthor("authorDiscovered")
                .build();

        final String userUnknown = CgeoApplication.getInstance().getString(cgeo.geocaching.R.string.user_unknown);
        assertThat(GeokretyParser.getLastSpottedUsername(new ArrayList<>())).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries1 = new ArrayList<>();
        logsEntries1.add(note);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries1)).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries2 = new ArrayList<>();
        logsEntries2.add(note);
        logsEntries2.add(placed);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries2)).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries3 = new ArrayList<>();
        logsEntries3.add(visit);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries3)).isEqualTo("authorVisit");

        final List<LogEntry> logsEntries4 = new ArrayList<>();
        logsEntries4.add(note);
        logsEntries4.add(visit);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries4)).isEqualTo("authorVisit");

        final List<LogEntry> logsEntries5 = new ArrayList<>();
        logsEntries5.add(placed);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries5)).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries6 = new ArrayList<>();
        logsEntries6.add(placed);
        logsEntries6.add(visit);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries6)).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries7 = new ArrayList<>();
        logsEntries7.add(visit);
        logsEntries7.add(placed);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries7)).isEqualTo("authorVisit");

        final List<LogEntry> logsEntries8 = new ArrayList<>();
        logsEntries8.add(placed);
        logsEntries8.add(visit);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries8)).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries9 = new ArrayList<>();
        logsEntries9.add(grabbed);
        logsEntries9.add(visit);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries9)).isEqualTo("authorGrabbed");

        final List<LogEntry> logsEntries10 = new ArrayList<>();
        logsEntries10.add(discovered);
        logsEntries10.add(visit);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries10)).isEqualTo(userUnknown);

        final List<LogEntry> logsEntries11 = new ArrayList<>();
        logsEntries11.add(note);
        logsEntries11.add(discovered);
        logsEntries11.add(note);
        logsEntries11.add(visit);
        logsEntries11.add(note);
        assertThat(GeokretyParser.getLastSpottedUsername(logsEntries11)).isEqualTo(userUnknown);
    }

    public void testLogsWithComments() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret46072_details_xml)));
        assertThat(trackables).hasSize(1);
        final Trackable trackable1 = trackables.get(0);

        // Check the logs
        final List<LogEntry> logs = trackable1.getLogs();
        assertThat(logs).hasSize(7);

        final LogEntry logNocybe = logs.get(2);
        assertThat(logNocybe.id).isEqualTo(855750);
        assertThat(logNocybe.author).isEqualTo("nocybe1810");
        assertThat(logNocybe.logType).isEqualTo(LogType.GRABBED_IT);
        assertThat(logNocybe.cacheName).isNullOrEmpty();
        assertThat(logNocybe.cacheGeocode).isNullOrEmpty();
    }
}

package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Java6Assertions.assertThat;
import org.junit.Test;

public class EventTableTest {

    @Test
    public void testToString() {

        final EventTable et = new EventTable();
        et.rawset("Name", "myname");
        et.rawset("text", "sometext");
        et.rawset("bool", true);
        et.rawset("int", 5);

        final Zone zone = new Zone();
        zone.rawset("Name", "myZone");
        et.rawset("zone", zone);

        //test that circulars are avoided
        et.rawset("same", et);

        final String etString = et.toString();
        assertThat(etString).startsWith("[EventTable]myname");
        assertThat(etString).contains("text=sometext");
        assertThat(etString).contains("bool=true");
        assertThat(etString).contains("int=5");
        assertThat(etString).contains("[Zone]myZone");
        assertThat(etString).contains("same=[EventTable]myname");
    }

}

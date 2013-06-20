package cgeo.geocaching.speech;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.AndroidTestCase;

import java.util.Locale;

public class TextFactoryTest extends AndroidTestCase {

    private static final Geopoint MY_POSITION = new Geopoint(15, -86);
    private static final Geopoint NORTH_6100M = new Geopoint(15.054859, -86);
    private static final Geopoint WEST_1MILE = new Geopoint(15, -86.014984);
    private static final Geopoint SOUTH_1020M = new Geopoint(14.990827, -86);
    private static final Geopoint EAST_123M = new Geopoint(15, -85.998855);
    private static final Geopoint WEST_34M = new Geopoint(15, -86.000317);
    private static final Geopoint EAST_1M = new Geopoint(15, -85.999990);
    private static final Geopoint EAST_1FT = new Geopoint(15, -85.999996);

    private Locale defaultLocale1;
    private Locale defaultLocale2;
    private boolean defaultMetric;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Resources resources = cgeoapplication.getInstance().getResources();
        final Configuration config = resources.getConfiguration();
        defaultLocale1 = config.locale;
        defaultLocale2 = Locale.getDefault();
        defaultMetric = Settings.isUseMetricUnits();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        setLocale(defaultLocale1, defaultLocale2, defaultMetric);
    }

    public void testGetDistanceEn() {
        setLocale(Locale.UK, true);
        assertEquals("12 o'clock. 6 kilometers", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 o'clock. 1.6 kilometers", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 o'clock. 1 kilometer", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 o'clock. 120 meters", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 o'clock. 34 meters", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 o'clock. 1 meter", TextFactory.getText(MY_POSITION, EAST_1M, 180));
        assertEquals("12 o'clock. 0 meters", TextFactory.getText(MY_POSITION, MY_POSITION, 0));

        setLocale(Locale.UK, false);
        assertEquals("12 o'clock. 4 miles", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 o'clock. 1 mile", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 o'clock. 0.6 miles", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 o'clock. 400 feet", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 o'clock. 111 feet", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 o'clock. 1 foot", TextFactory.getText(MY_POSITION, EAST_1FT, 180));
        assertEquals("12 o'clock. 0 feet", TextFactory.getText(MY_POSITION, MY_POSITION, 0));
    }

    public void testGetDistanceDe() {
        setLocale(Locale.GERMANY, true);
        assertEquals("12 Uhr. 6 Kilometer", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 Uhr. 1,6 Kilometer", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 Uhr. 1 Kilometer", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 Uhr. 120 Meter", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 Uhr. 34 Meter", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 Uhr. 1 Meter", TextFactory.getText(MY_POSITION, EAST_1M, 180));
        assertEquals("12 Uhr. 0 Meter", TextFactory.getText(MY_POSITION, MY_POSITION, 0));

        setLocale(Locale.GERMANY, false);
        assertEquals("12 Uhr. 4 Meilen", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 Uhr. 1 Meile", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 Uhr. 0,6 Meilen", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 Uhr. 400 Fuß", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 Uhr. 111 Fuß", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 Uhr. 1 Fuß", TextFactory.getText(MY_POSITION, EAST_1FT, 180));
        assertEquals("12 Uhr. 0 Fuß", TextFactory.getText(MY_POSITION, MY_POSITION, 0));
    }

    public void testGetDistanceFr() {
        setLocale(Locale.FRANCE, true);
        assertEquals("12 heures. 6 kilomètres", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 heures. 1,6 kilomètres", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 heures. 1 kilomètre", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 heures. 120 mètres", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 heures. 34 mètres", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 heures. 1 mètre", TextFactory.getText(MY_POSITION, EAST_1M, 180));
        assertEquals("12 heures. 0 mètre", TextFactory.getText(MY_POSITION, MY_POSITION, 0));

        setLocale(Locale.FRANCE, false);
        assertEquals("12 heures. 4 milles", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 heures. 1 mille", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 heures. 0,6 milles", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 heures. 400 pieds", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 heures. 111 pieds", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 heures. 1 pied", TextFactory.getText(MY_POSITION, EAST_1FT, 180));
        assertEquals("12 heures. 0 pied", TextFactory.getText(MY_POSITION, MY_POSITION, 0));
    }

    public void testGetDistanceIt() {
        setLocale(Locale.ITALY, true);
        assertEquals("12 precisi. 6 chilometri", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 precisi. 1,6 chilometri", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 precisi. 1 chilometro", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 precisi. 120 metri", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 precisi. 34 metri", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 precisi. 1 metro", TextFactory.getText(MY_POSITION, EAST_1M, 180));
        assertEquals("12 precisi. 0 metri", TextFactory.getText(MY_POSITION, MY_POSITION, 0));

        setLocale(Locale.ITALY, false);
        assertEquals("12 precisi. 4 miglia", TextFactory.getText(MY_POSITION, NORTH_6100M, 0));
        assertEquals("9 precisi. 1 miglio", TextFactory.getText(MY_POSITION, WEST_1MILE, 0));
        assertEquals("6 precisi. 0,6 miglia", TextFactory.getText(MY_POSITION, SOUTH_1020M, 0));
        assertEquals("3 precisi. 400 piedi", TextFactory.getText(MY_POSITION, EAST_123M, 0));
        assertEquals("11 precisi. 111 piedi", TextFactory.getText(MY_POSITION, WEST_34M, 290));
        assertEquals("9 precisi. 1 piede", TextFactory.getText(MY_POSITION, EAST_1FT, 180));
        assertEquals("12 precisi. 0 piedi", TextFactory.getText(MY_POSITION, MY_POSITION, 0));
    }

    private static void setLocale(Locale locale, boolean metric) {
        setLocale(locale, locale, metric);
    }

    private static void setLocale(Locale locale1, Locale locale2, boolean metric) {
        final Configuration config = new Configuration();
        config.locale = locale1;
        final Resources resources = cgeoapplication.getInstance().getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Locale.setDefault(locale2);
        Settings.setUseMetricUnits(metric);
    }
}

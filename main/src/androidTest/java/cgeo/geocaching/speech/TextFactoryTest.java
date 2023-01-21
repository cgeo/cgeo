package cgeo.geocaching.speech;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TextFactoryTest {

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

    @Before
    public void setUp() throws Exception {
        final Resources resources = CgeoApplication.getInstance().getResources();
        final Configuration config = resources.getConfiguration();
        defaultLocale1 = config.locale;
        defaultLocale2 = Locale.getDefault();
        defaultMetric = !Settings.useImperialUnits();
    }

    @After
    public void tearDown() {
        setLocale(defaultLocale1, defaultLocale2, defaultMetric);
    }

    @Test
    public void testGetTextEn() {
        setLocale(Locale.UK, true);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("one o'clock. 6 kilometers");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("9 o'clock. 1.6 kilometers");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("6 o'clock. one kilometer");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("3 o'clock. 120 meters");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("11 o'clock. 34 meters");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1M, 180)).isEqualTo("9 o'clock. one meter");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("12 o'clock. 0 meters");

        setLocale(Locale.UK, false);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("one o'clock. 4 miles");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("9 o'clock. one mile");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("6 o'clock. 0.6 miles");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("3 o'clock. 400 feet");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("11 o'clock. 111 feet");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1FT, 180)).isEqualTo("9 o'clock. one foot");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("12 o'clock. 0 feet");
    }

    @Test
    public void testGetTextDe() {
        setLocale(Locale.GERMANY, true);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("ein Uhr. 6 Kilometer");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("9 Uhr. 1,6 Kilometer");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("6 Uhr. ein Kilometer");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("3 Uhr. 120 Meter");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("11 Uhr. 34 Meter");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1M, 180)).isEqualTo("9 Uhr. ein Meter");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("12 Uhr. 0 Meter");

        setLocale(Locale.GERMANY, false);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("ein Uhr. 4 Meilen");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("9 Uhr. eine Meile");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("6 Uhr. 0,6 Meilen");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("3 Uhr. 400 Fuß");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("11 Uhr. 111 Fuß");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1FT, 180)).isEqualTo("9 Uhr. ein Fuß");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("12 Uhr. 0 Fuß");
    }

    @Test
    public void testGetTextFr() {
        setLocale(Locale.FRANCE, true);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("une heure. 6 kilomètres");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("9 heures. 1,6 kilomètres");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("6 heures. un kilomètre");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("3 heures. 120 mètres");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("11 heures. 34 mètres");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1M, 180)).isEqualTo("9 heures. un mètre");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("12 heures. 0 mètre");

        setLocale(Locale.FRANCE, false);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("une heure. 4 milles");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("9 heures. un mille");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("6 heures. 0,6 milles");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("3 heures. 400 pieds");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("11 heures. 111 pieds");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1FT, 180)).isEqualTo("9 heures. un pied");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("12 heures. 0 pied");
    }

    @Test
    public void testGetTextIt() {
        setLocale(Locale.ITALY, true);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("a ore una. 6 chilometri");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("a ore 9. 1,6 chilometri");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("a ore 6. un chilometro");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("a ore 3. 120 metri");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("a ore 11. 34 metri");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1M, 180)).isEqualTo("a ore 9. un metro");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("a ore 12. 0 metri");

        setLocale(Locale.ITALY, false);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("a ore una. 4 miglia");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("a ore 9. un miglio");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("a ore 6. 0,6 miglia");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("a ore 3. 400 piedi");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("a ore 11. 111 piedi");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1FT, 180)).isEqualTo("a ore 9. un piede");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("a ore 12. 0 piedi");
    }

    @Test
    public void testGetTextSv() {
        setLocale(new Locale("sv", "SE"), true);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("Klockan ett. 6 kilometer");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("Klockan 9. 1,6 kilometer");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("Klockan 6. en kilometer");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("Klockan 3. 120 meter");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("Klockan 11. 34 meter");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1M, 180)).isEqualTo("Klockan 9. en meter");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("Klockan 12. 0 meter");

        setLocale(new Locale("sv", "SE"), false);
        assertThat(TextFactory.getText(MY_POSITION, NORTH_6100M, 330)).isEqualTo("Klockan ett. 4 engelsk mil");
        assertThat(TextFactory.getText(MY_POSITION, WEST_1MILE, 0)).isEqualTo("Klockan 9. en engelsk mil");
        assertThat(TextFactory.getText(MY_POSITION, SOUTH_1020M, 0)).isEqualTo("Klockan 6. 0,6 engelsk mil");
        assertThat(TextFactory.getText(MY_POSITION, EAST_123M, 0)).isEqualTo("Klockan 3. 400 fot");
        assertThat(TextFactory.getText(MY_POSITION, WEST_34M, 290)).isEqualTo("Klockan 11. 111 fot");
        assertThat(TextFactory.getText(MY_POSITION, EAST_1FT, 180)).isEqualTo("Klockan 9. en fot");
        assertThat(TextFactory.getText(MY_POSITION, MY_POSITION, 0)).isEqualTo("Klockan 12. 0 fot");
    }

    private static void setLocale(final Locale locale, final boolean metric) {
        setLocale(locale, locale, metric);
    }

    private static void setLocale(final Locale locale1, final Locale locale2, final boolean metric) {
        final Configuration config = new Configuration();
        config.locale = locale1;
        final Resources resources = CgeoApplication.getInstance().getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Locale.setDefault(locale2);
        TestSettings.setUseImperialUnits(!metric);
    }
}

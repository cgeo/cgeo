package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

@TargetApi(8)
public class GpxFileListActivityTest extends ActivityInstrumentationTestCase2<GpxFileListActivity> {
    private final GpxFileListActivity importGpxActivity = new GpxFileListActivity();

    public GpxFileListActivityTest() {
        super(GpxFileListActivity.class);
    }

    public void testPocketQueryCreator() {
        assertImport("pocketquery_name.zip");
        assertImport("pocketquery_ä.1. .zip");
        assertImport("pocketquery_name.ZIP");
        assertImport("pocketquery_name.gpx");
    }

    public void testDifferentFileTypes() {
        assertImport("1234567.gpx");
        assertImport("1234567.GPX");
        assertImport(".gpx");
        assertImport("1234567.loc");
        assertImport("1234567.LOC");
        assertImport("1234567.zip");
        assertImport("1234567.ZIP");
    }

    public void testPocketQueries() {
        assertImport("12345678.zip");
        assertImport("1234567_query.zip");
        assertImport("12345678_query.zip");
        assertImport("12345678_my_query_1.zip");
        assertImport("12345678_my query.zip");

        denyImport("1234567.gpy");
        denyImport("1234567.agpx");
        denyImport("1234567");
        denyImport("");
        denyImport("gpx");
        denyImport("test.zip");
        denyImport("zip");
        denyImport(".zip");
        denyImport("123456.zip");
        denyImport("1234567query.zip");
        denyImport("1234567_.zip");

        denyImport("1234567-wpts.gpx");
        denyImport("1234567-wpts-1.gpx");
        denyImport("1234567-wpts(1).gpx");
    }

    public void testOpenCachingExports() {
        assertImport("ocde12345.zip");
        assertImport("ocde12345678.zip");

        denyImport("ocde_12345678.zip");
        denyImport("acde12345678.zip");
    }

    private void assertImport(String fileName) {
        assertThat(importGpxActivity.filenameBelongsToList(fileName)).isTrue();
    }

    private void denyImport(String fileName) {
        assertThat(importGpxActivity.filenameBelongsToList(fileName)).isFalse();
    }
}

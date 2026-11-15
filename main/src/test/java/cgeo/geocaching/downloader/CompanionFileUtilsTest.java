package cgeo.geocaching.downloader;

import cgeo.geocaching.utils.CalendarUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CompanionFileUtilsTest {

    @Test
    public void testReadDataFromStream() {
        final String content = "#comment\n"
                + "remote.parsetype=42\n"
                + "remote.page=https\\://example.org/maps/europe\n"
                + "remote.file=hessen.map\n"
                + "remote.date=2020-12-10\n"
                + "local.file=map43.map\n"
                + "displayname=Hessen\n";

        final CompanionFileUtils.DownloadedFileData data = CompanionFileUtils.readData(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)), "map43.map" + CompanionFileUtils.INFOFILE_SUFFIX);

        assertThat(data).isNotNull();
        assertThat(data.remoteParsetype).isEqualTo(42);
        assertThat(data.remotePage).isEqualTo("https://example.org/maps/europe");
        assertThat(data.remoteFile).isEqualTo("hessen.map");
        assertThat(data.remoteDate).isEqualTo(CalendarUtils.parseYearMonthDay("2020-12-10"));
        assertThat(data.localFile).isEqualTo("map43.map");
        assertThat(data.displayName).isEqualTo("Hessen");
    }

}

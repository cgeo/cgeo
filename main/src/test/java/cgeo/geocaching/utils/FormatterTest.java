package cgeo.geocaching.utils;

import java.util.Locale;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class FormatterTest {

    @Test
    public void testFormatGCEventTime() {
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  Start time:  12:00 PM  </div>   <div id=\"mcd4\">  End time:  1:00 PM  </div>  </div>")).isEqualTo("12:00 - 13:00");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  Hora de Inicio  12:00  </div>   <div id=\"mcd4\">  Hora de finalización  13:00  </div>  </div>")).isEqualTo("12:00 - 13:00");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  시작 시간:  PM 12:00  </div>   <div id=\"mcd4\">  종료 시간:  PM 1:00  </div>  </div>")).isEqualTo("12:00 - 13:00");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  Start time:  12:30 PM  </div>   <div id=\"mcd4\">  End time:  2:45 PM  </div>  </div>")).isEqualTo("12:30 - 14:45");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  Hora de Inicio  12:30  </div>   <div id=\"mcd4\">  Hora de finalización  14:45  </div>  </div>")).isEqualTo("12:30 - 14:45");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  시작 시간:  PM 12:30  </div>   <div id=\"mcd4\">  종료 시간:  PM 2:45  </div>  </div>")).isEqualTo("12:30 - 14:45");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  시작 시간:  AM 12:00  </div>   <div id=\"mcd4\">  종료 시간:  PM 12:00  </div>  </div>")).isEqualTo("00:00 - 12:00");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  Hora de Inicio  0:00  </div>   <div id=\"mcd4\">  Hora de finalización  12:00  </div>  </div>")).isEqualTo("00:00 - 12:00");
        assertThat(Formatter.formatGCEventTime("<div class=\"minorCacheDetails&#32;Clear\">  <div id=\"mcd3\">  Start time:  12:00 AM  </div>   <div id=\"mcd4\">  End time:  12:00 PM  </div>  </div>")).isEqualTo("00:00 - 12:00");
    }

    @Test
    public void testFormatBytes() {
        final Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertThat(Formatter.formatBytes(0)).isEqualTo("0 B");
            assertThat(Formatter.formatBytes(1023)).isEqualTo("1023 B");
            assertThat(Formatter.formatBytes(1024)).isEqualTo("1.0 KiB");
            assertThat(Formatter.formatBytes(1536)).isEqualTo("1.5 KiB");
            assertThat(Formatter.formatBytes(1024L * 1024)).isEqualTo("1.0 MiB");
            assertThat(Formatter.formatBytes(5L * 1024 * 1024 * 1024)).isEqualTo("5.0 GiB");
            assertThat(Formatter.formatBytes(1024L * 1024 * 1024 * 1024)).isEqualTo("1.0 TiB");
            assertThat(Formatter.formatBytes(1024L * 1024 * 1024 * 1024 * 1024)).isEqualTo("1.0 PiB");
            assertThat(Formatter.formatBytes(Long.MAX_VALUE)).isEqualTo("8.0 EiB");
            // SI values are not rounded up to the next binary unit
            assertThat(Formatter.formatBytes(1_000_000)).isEqualTo("976.6 KiB");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}

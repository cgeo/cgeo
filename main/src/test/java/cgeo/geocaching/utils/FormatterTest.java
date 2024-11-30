package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
}

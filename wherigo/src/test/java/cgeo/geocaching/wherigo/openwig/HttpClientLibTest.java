package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.platform.HttpResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link HttpClientLib}'s Lua-facing glue: converting a Lua headers table to/from a
 * Java Map, building the "StatusCode"/"Body"/"Headers" result table from an HttpResult, and
 * swallowing a failed call into a nil result rather than throwing into the calling script. The
 * JavaFunction wrappers themselves (Get/Post) aren't exercised here - that would need a full
 * LuaCallFrame/LuaThread fixture for what is otherwise a thin, mechanical argument-marshalling
 * layer; execute()/toHeaderMap() carry the actual logic and are tested directly instead.
 */
public class HttpClientLibTest {

    @Test
    public void toHeaderMapConvertsLuaTableToStringMap() {
        final LuaTable headers = new LuaTableImpl();
        headers.rawset("Accept", "application/json");
        headers.rawset("X-Custom", "value");

        final Map<String, String> map = HttpClientLib.toHeaderMap(headers);

        assertThat(map).containsEntry("Accept", "application/json").containsEntry("X-Custom", "value");
    }

    @Test
    public void toHeaderMapReturnsNullForNullTable() {
        assertThat(HttpClientLib.toHeaderMap(null)).isNull();
    }

    @Test
    public void executeBuildsResultTableFromSuccessfulCall() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "text/plain");

        final LuaTable result = HttpClientLib.execute(() -> new HttpResult(200, "hello", headers));

        assertThat(result.rawget("StatusCode")).isEqualTo(200.0);
        assertThat(result.rawget("Body")).isEqualTo("hello");
        final LuaTable headerTable = (LuaTable) result.rawget("Headers");
        assertThat(headerTable.rawget("Content-Type")).isEqualTo("text/plain");
    }

    @Test
    public void executeProducesEmptyHeadersTableWhenResultHeadersAreNull() {
        final LuaTable result = HttpClientLib.execute(() -> new HttpResult(204, null, null));

        assertThat(result.rawget("StatusCode")).isEqualTo(204.0);
        assertThat(result.rawget("Body")).isNull();
        final LuaTable headerTable = (LuaTable) result.rawget("Headers");
        assertThat(headerTable.len()).isEqualTo(0);
    }

    @Test
    public void executeReturnsNullWhenTheCallThrows() {
        final LuaTable result = HttpClientLib.execute(() -> {
            throw new RuntimeException("network is down");
        });

        assertThat(result).isNull();
    }
}

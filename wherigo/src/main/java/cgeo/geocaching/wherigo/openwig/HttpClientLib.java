package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.platform.HttpResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lua binding for {@code JakeDot.HttpClient}: synchronous GET/POST requests, delegating the
 * actual network call to {@code Engine.http} (see {@link cgeo.geocaching.wherigo.openwig.platform.HttpClient}
 * for why that indirection exists). This is a custom, non-standard extension - see
 * WherigoLib#register for why it lives under JakeDot.* rather than Wherigo.*.
 * <p>
 * Get(url) / Get(url, headers) and Post(url, body) / Post(url, body, headers) return a table with
 * "StatusCode", "Body" and "Headers" fields, or nil if the request failed (network error,
 * timeout, etc.) - failures are logged rather than thrown into the calling Lua script.
 */
final class HttpClientLib {

    private static final JavaFunction get = new JavaFunction() {
        public int call(final LuaCallFrame callFrame, final int nArguments) {
            BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for HttpClient.Get");
            final String url = (String) callFrame.get(0);
            final Map<String, String> headers = nArguments >= 2 ? toHeaderMap((LuaTable) callFrame.get(1)) : null;
            callFrame.push(execute(() -> Engine.http.get(url, headers)));
            return 1;
        }
    };

    private static final JavaFunction post = new JavaFunction() {
        public int call(final LuaCallFrame callFrame, final int nArguments) {
            BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for HttpClient.Post");
            final String url = (String) callFrame.get(0);
            final String body = nArguments >= 2 ? (String) callFrame.get(1) : null;
            final Map<String, String> headers = nArguments >= 3 ? toHeaderMap((LuaTable) callFrame.get(2)) : null;
            callFrame.push(execute(() -> Engine.http.post(url, body, headers)));
            return 1;
        }
    };

    private HttpClientLib() {
    }

    static void register() {
        Engine.instance.savegame.addJavafunc(get);
        Engine.instance.savegame.addJavafunc(post);
    }

    static LuaTable createTable() {
        final LuaTable table = new LuaTableImpl();
        table.rawset("Get", get);
        table.rawset("Post", post);
        return table;
    }

    static Map<String, String> toHeaderMap(final LuaTable headers) {
        if (headers == null) {
            return null;
        }
        final Map<String, String> map = new LinkedHashMap<>();
        Object key = null;
        while ((key = headers.next(key)) != null) {
            final Object value = headers.rawget(key);
            map.put(key.toString(), value == null ? null : value.toString());
        }
        return map;
    }

    static LuaTable execute(final HttpCall call) {
        try {
            final HttpResult result = call.run();
            final LuaTable resultTable = new LuaTableImpl();
            resultTable.rawset("StatusCode", LuaState.toDouble(result.statusCode));
            resultTable.rawset("Body", result.body);
            final LuaTable headerTable = new LuaTableImpl();
            if (result.headers != null) {
                for (final Map.Entry<String, String> entry : result.headers.entrySet()) {
                    headerTable.rawset(entry.getKey(), entry.getValue());
                }
            }
            resultTable.rawset("Headers", headerTable);
            return resultTable;
        } catch (RuntimeException re) {
            Engine.log("HTTP: request failed: " + re, Engine.LOG_WARN);
            return null;
        }
    }

    interface HttpCall {
        HttpResult run();
    }
}

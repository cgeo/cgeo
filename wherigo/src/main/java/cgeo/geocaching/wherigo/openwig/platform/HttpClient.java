package cgeo.geocaching.wherigo.openwig.platform;

import java.util.Map;

/**
 * Platform-independent interface for synchronous HTTP requests, so Lua-facing bindings in the
 * openwig engine can make network calls without the engine itself depending on the app's network
 * stack. Wired up the same way as {@link UI} / {@link LocationService}: implemented outside
 * openwig (using whatever HTTP client the app already uses) and injected via
 * {@code Engine.http} at engine startup.
 * <p>
 * Calls are synchronous/blocking, matching how every other Lua-facing function in this VM works.
 * Implementations should throw an unchecked exception on failure (network error, timeout, etc.)
 * rather than returning a sentinel value - the Lua binding is responsible for turning that into a
 * script-friendly result (see HttpClientLib).
 */
public interface HttpClient {

    HttpResult get(String url, Map<String, String> headers);

    HttpResult post(String url, String body, Map<String, String> headers);
}

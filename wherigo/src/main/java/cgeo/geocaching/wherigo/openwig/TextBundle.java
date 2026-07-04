package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A cartridge-defined bundle of translated text, modelled on Java's own {@code ResourceBundle}
 * file convention: one Media resource per {@code .properties} file, keyed in the "Resources"
 * table by language tag ("" for the language-neutral default, "de", "de_AT", etc.). GetText(key,
 * language) walks the same language_COUNTRY_variant -> language_COUNTRY -> language -> default
 * fallback chain ResourceBundle uses, falling back one level at a time whenever a key is missing
 * from a more specific file - not just when the whole file is missing.
 * <p>
 * The language tag is a caller-supplied parameter, not the device/JVM default locale: the
 * cartridge (or whatever tracks the player's chosen language) decides what "current language"
 * means and passes it in explicitly, so this class has no ambient locale dependency at all.
 */
public class TextBundle extends EventTable {

    private LuaTable resources;
    private final Map<Integer, Properties> parsedCache = new HashMap<>();

    private static JavaFunction getText = new JavaFunction() {
        public int call(final LuaCallFrame callFrame, final int nArguments) {
            final TextBundle bundle = (TextBundle) callFrame.get(0);
            final String key = (String) callFrame.get(1);
            final String language = (String) callFrame.get(2);
            callFrame.push(bundle.getText(key, language));
            return 1;
        }
    };

    public static void register() {
        Engine.instance.savegame.addJavafunc(getText);
    }

    protected String luaTostring() { return "a ZTextBundle instance"; }

    public TextBundle() {
        table.rawset("GetText", getText);
    }

    protected void setItem(final String key, final Object value) {
        if ("Resources".equals(key) && value instanceof LuaTable) {
            resources = (LuaTable) value;
        } else {
            super.setItem(key, value);
        }
    }

    /** Resolves key against language's fallback chain, or null if not found in any of the
     * bundle's files. language is a caller-supplied tag such as "de_AT_tirol", "de_AT", "de", or
     * null/"" for the language-neutral default - this class never consults the device locale. */
    public String getText(final String key, final String language) {
        if (resources == null || key == null) {
            return null;
        }
        for (final String tag : candidateTags(language)) {
            final Object entry = resources.rawget(tag);
            if (entry instanceof Media) {
                final String value = propertiesFor((Media) entry).getProperty(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /** Peels underscore-delimited segments off the end of language one at a time - e.g.
     * "de_AT_tirol" -> "de_AT_tirol", "de_AT", "de", "" - always ending in "" (default). */
    static List<String> candidateTags(final String language) {
        final List<String> tags = new ArrayList<>(4);
        String tag = language == null ? "" : language;
        while (!tag.isEmpty()) {
            tags.add(tag);
            final int lastUnderscore = tag.lastIndexOf('_');
            if (lastUnderscore < 0) {
                break;
            }
            tag = tag.substring(0, lastUnderscore);
        }
        tags.add("");
        return tags;
    }

    private Properties propertiesFor(final Media media) {
        final Integer cacheKey = media.id;
        final Properties cached = parsedCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        final Properties props = new Properties();
        try {
            final byte[] data = readMediaBytes(media);
            if (data != null) {
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
        } catch (IOException e) {
            Engine.log("TEXT: failed to load properties bundle media " + media.id + ": " + e, Engine.LOG_WARN);
        }
        parsedCache.put(cacheKey, props);
        return props;
    }

    /** Seam for tests: production code reads the resource from the cartridge via Engine. */
    protected byte[] readMediaBytes(final Media media) throws IOException {
        return Engine.mediaFile(media);
    }
}

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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * A cartridge-defined bundle of translated text, modelled on Java's own {@code ResourceBundle}
 * file convention: one Media resource per {@code .properties} file, keyed in the "Resources"
 * table by locale tag ("" for the language-neutral default, "de", "de_AT", etc.). GetText(key)
 * walks the same language_COUNTRY_variant -> language_COUNTRY -> language -> default fallback
 * chain ResourceBundle uses, falling back one level at a time whenever a key is missing from a
 * more specific file - not just when the whole file is missing.
 */
public class TextBundle extends EventTable {

    private LuaTable resources;
    private final Map<Integer, Properties> parsedCache = new HashMap<>();

    private static JavaFunction getText = new JavaFunction() {
        public int call(final LuaCallFrame callFrame, final int nArguments) {
            final TextBundle bundle = (TextBundle) callFrame.get(0);
            final String key = (String) callFrame.get(1);
            callFrame.push(bundle.getText(key));
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

    /** Resolves key against the current default locale's fallback chain, or null if not found
     * in any of the bundle's files. */
    public String getText(final String key) {
        if (resources == null || key == null) {
            return null;
        }
        for (final String tag : candidateTags(Locale.getDefault())) {
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

    /** language_COUNTRY_variant -> language_COUNTRY -> language -> "" (default), omitting any
     * level whose more-specific parts are empty - the common-case ResourceBundle cascade. */
    static List<String> candidateTags(final Locale locale) {
        final String language = locale.getLanguage();
        final String country = locale.getCountry();
        final String variant = locale.getVariant();

        final List<String> tags = new ArrayList<>(4);
        if (!language.isEmpty() && !country.isEmpty() && !variant.isEmpty()) {
            tags.add(language + "_" + country + "_" + variant);
        }
        if (!language.isEmpty() && !country.isEmpty()) {
            tags.add(language + "_" + country);
        }
        if (!language.isEmpty()) {
            tags.add(language);
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

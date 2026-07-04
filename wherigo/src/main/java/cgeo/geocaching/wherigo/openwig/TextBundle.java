package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A single cartridge-authored text resource holding translations for several languages, modelled
 * on Java's own {@code ResourceBundle} file convention but consolidated into one physical file
 * instead of one-file-per-language: sections are delimited by a "[tag]" header line (empty tag,
 * i.e. plain "[]", for the language-neutral default), each followed by ordinary
 * {@code key=value} properties lines up to the next header or end of file. A file with no
 * headers at all is treated entirely as the default section.
 * <p>
 * Extends {@link Media} to reuse its existing single-resource machinery (id/type, and the
 * "Resources" setItem that assigns this object's one physical file) - authoring this is exactly
 * like authoring any other ZMedia, just with GetText(key, language) added on top. This is a
 * custom, non-standard extension, so it's constructed as JakeDot.ZTextBundle() rather than under
 * the real Wherigo.* namespace (see WherigoLib#register).
 * <p>
 * GetText(key, language) walks the same language_COUNTRY_variant -> language_COUNTRY ->
 * language -> default fallback chain ResourceBundle uses, falling back one level at a time
 * whenever a key is missing from a more specific section - not just when the whole section is
 * missing. language is a caller-supplied tag, not the device/JVM default locale: the cartridge
 * (or whatever tracks the player's chosen language) decides what "current language" means and
 * passes it in explicitly, so this class has no ambient locale dependency at all.
 */
public class TextBundle extends Media {

    private Map<String, Properties> sections;

    private static JavaFunction getText = new JavaFunction() {
        public int call(final LuaCallFrame callFrame, final int nArguments) {
            BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for GetText");
            final TextBundle bundle = (TextBundle) callFrame.get(0);
            final String key = (String) callFrame.get(1);
            final String language = nArguments >= 3 ? (String) callFrame.get(2) : null;
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

    /** Resolves key against language's fallback chain, or null if not found in any section of
     * this bundle's file. */
    public String getText(final String key, final String language) {
        if (key == null) {
            return null;
        }
        ensureParsed();
        for (final String tag : candidateTags(language)) {
            final Properties section = sections.get(tag);
            if (section != null) {
                final String value = section.getProperty(key);
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

    private void ensureParsed() {
        if (sections != null) {
            return;
        }
        sections = new HashMap<>();
        try {
            final byte[] data = readOwnBytes();
            if (data != null) {
                parseSections(new String(data, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            Engine.log("TEXT: failed to load text bundle media " + id + ": " + e, Engine.LOG_WARN);
        }
    }

    private void parseSections(final String text) {
        // a UTF-8 BOM (common in Windows-authored files) would otherwise stay attached to the
        // first line, making "[]"/"[de]" fail the startsWith("[") check
        final String cleaned = text.startsWith("\uFEFF") ? text.substring(1) : text;
        String currentTag = "";
        final StringBuilder buffer = new StringBuilder();
        for (final String line : cleaned.split("\n", -1)) {
            final String trimmed = line.trim();
            if (trimmed.length() >= 2 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
                flushSection(currentTag, buffer);
                currentTag = trimmed.substring(1, trimmed.length() - 1).trim();
                buffer.setLength(0);
            } else {
                buffer.append(line).append('\n');
            }
        }
        flushSection(currentTag, buffer);
    }

    private void flushSection(final String tag, final StringBuilder buffer) {
        Properties props = sections.get(tag);
        if (props == null) {
            props = new Properties();
            sections.put(tag, props);
        }
        try {
            props.load(new StringReader(buffer.toString()));
        } catch (IOException e) {
            Engine.log("TEXT: failed to parse section [" + tag + "] of text bundle media " + id + ": " + e, Engine.LOG_WARN);
        }
    }

    /** Seam for tests: production code reads this bundle's own resource bytes via Engine. */
    protected byte[] readOwnBytes() throws IOException {
        return Engine.mediaFile(this);
    }
}

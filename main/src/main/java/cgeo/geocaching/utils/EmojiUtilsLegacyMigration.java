package cgeo.geocaching.utils;

import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversion helpers needed only to read legacy, pre-migration emoji values (stored as int codepoints) and turn them
 * into the String representation the app uses today. Kept separate from {@link EmojiUtils} because none of this is
 * needed for current emoji handling - it exists purely to keep old databases and GPX files readable.
 */
public final class EmojiUtilsLegacyMigration {
    public static final int NO_EMOJI_LEGACY = 0;

    // Map legacy colored circle markers to the standard circle emoji of choice
    private static final String CIRCLE_BLACK = emoji(0x26AB);   // ⚫
    private static final String CIRCLE_WHITE = emoji(0x26AA);   // ⚪
    private static final String CIRCLE_RED = emoji(0x1F534);    // 🔴
    private static final String CIRCLE_ORANGE = emoji(0x1F7E0); // 🟠
    private static final String CIRCLE_YELLOW = emoji(0x1F7E1); // 🟡
    private static final String CIRCLE_GREEN = emoji(0x1F7E2);  // 🟢
    private static final String CIRCLE_BLUE = emoji(0x1F535);   // 🔵
    private static final String CIRCLE_PURPLE = emoji(0x1F7E3); // 🟣
    private static final String CIRCLE_BROWN = emoji(0x1F7E4);  // 🟤

    private static final Map<Integer, String> LEGACY_CIRCLE_MIGRATION = buildLegacyCircleMigration();

    private static Map<Integer, String> buildLegacyCircleMigration() {
        final Map<Integer, String> m = new LinkedHashMap<>();
        m.put(0xe000, CIRCLE_BLACK); // original RGB(0,0,0)
        m.put(0xe001, CIRCLE_BLUE); // original RGB(0,0,127)
        m.put(0xe002, CIRCLE_BLUE); // original RGB(0,0,254)
        m.put(0xe003, CIRCLE_GREEN); // original RGB(0,127,0)
        m.put(0xe004, CIRCLE_GREEN); // original RGB(0,127,127)
        m.put(0xe005, CIRCLE_BLUE); // original RGB(0,127,254)
        m.put(0xe006, CIRCLE_GREEN); // original RGB(0,254,0)
        m.put(0xe007, CIRCLE_GREEN); // original RGB(0,254,127)
        m.put(0xe008, CIRCLE_BLUE); // original RGB(0,254,254)
        m.put(0xe009, CIRCLE_BROWN); // original RGB(127,0,0)
        m.put(0xe00a, CIRCLE_PURPLE); // original RGB(127,0,127)
        m.put(0xe00b, CIRCLE_PURPLE); // original RGB(127,0,254)
        m.put(0xe00c, CIRCLE_BROWN); // original RGB(127,127,0)
        m.put(0xe00d, CIRCLE_WHITE); // original RGB(127,127,127)
        m.put(0xe00e, CIRCLE_PURPLE); // original RGB(127,127,254)
        m.put(0xe00f, CIRCLE_GREEN); // original RGB(127,254,0)
        m.put(0xe010, CIRCLE_GREEN); // original RGB(127,254,127)
        m.put(0xe011, CIRCLE_WHITE); // original RGB(127,254,254)
        m.put(0xe012, CIRCLE_RED); // original RGB(254,0,0)
        m.put(0xe013, CIRCLE_RED); // original RGB(254,0,127)
        m.put(0xe014, CIRCLE_PURPLE); // original RGB(254,0,254)
        m.put(0xe015, CIRCLE_ORANGE); // original RGB(254,127,0)
        m.put(0xe016, CIRCLE_ORANGE); // original RGB(254,127,127)
        m.put(0xe017, CIRCLE_PURPLE); // original RGB(254,127,254)
        m.put(0xe018, CIRCLE_YELLOW); // original RGB(254,254,0)
        m.put(0xe019, CIRCLE_YELLOW); // original RGB(254,254,127)
        m.put(0xe01a, CIRCLE_WHITE); // original RGB(254,254,254)
        return m;
    }

    private EmojiUtilsLegacyMigration() {
        // utility class
    }

    private static String emoji(final int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    @Nullable
    public static String legacyIntToEmojiString(final int cp) {
        if (cp == NO_EMOJI_LEGACY) {
            return null;
        }
        final String migratedCircle = LEGACY_CIRCLE_MIGRATION.get(cp);
        if (migratedCircle != null) {
            return migratedCircle;
        }
        return new String(Character.toChars(cp));
    }

    /**
     * Parses the value of a GPX {@code <cgeo:assignedEmoji>} element. Pre-migration files store the int codepoint as
     * decimal text; post-migration files store the emoji String directly.
     */
    @Nullable
    public static String parseGpxAssignedEmoji(@Nullable final String raw) {
        if (raw == null) {
            return null;
        }
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // legacy GPX stored the int codepoint as decimal text
        boolean allDigits = true;
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                allDigits = false;
                break;
            }
        }
        if (allDigits) {
            try {
                return legacyIntToEmojiString(Integer.parseInt(trimmed));
            } catch (final NumberFormatException ignore) {
                return trimmed;
            }
        }
        return trimmed;
    }
}

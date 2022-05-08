package cgeo.geocaching.connector.gc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility functions related to GC
 */
public final class GCUtils {

    private static final String SEQUENCE_GCID = "0123456789ABCDEFGHJKMNPQRTVWXYZ";
    private static final Map<Character, Integer> MAP_GCID = new HashMap<>();
    private static final long GC_BASE31 = 31;
    private static final long GC_BASE16 = 16;

    static {
        int cnt = 0;
        for (char c : SEQUENCE_GCID.toCharArray()) {
            MAP_GCID.put(c, cnt++);
        }
    }

    /**
     * Convert GCCode (geocode) to (old) GCIds
     *
     * For invalid gccodes (including null), 0 will be returned
     *
     * Based on http://www.geoclub.de/viewtopic.php?f=111&t=54859&start=40
     * see http://support.groundspeak.com/index.php?pg=kb.printer.friendly&id=1#p221 (checked on 13.9.20; seems to be outdated)
     * see for algorithm e.g. http://kryptografie.de/kryptografie/chiffre/gc-code.htm (german)
     */
    public static long gcCodeToGcId(@Nullable final String gccode) {
        return codeToId("GC", gccode);
    }

    /**
     * Takes given code, removes first two chars and converts remainder to a gc-like number.
     * GC-invalid chars (like O or I) are treated with value -1
     * Method will return same value as {@link #gcCodeToGcId(String)} for legal GC Codes and "something" for invalid ones
     * Function is needed in legacy code e.g. to sort GPX parser codes. You should probably not use it for new code...
     */
    @Deprecated
    public static long gcLikeCodeToGcLikeId(final String code) {
        if (code == null || code.length() < 2) {
            return 0;
        }
        return codeToId(code.substring(0, 2), code, true);
    }

    /**
     * Converts (old) GCIds to GCCode. For invalid id's, empty string will be returned (never null)
     */
    @NonNull
    public static String gcIdToGcCode(final long gcId) {
        return idToCode("GC", gcId);
    }

    /**
     * Converts LogCode to LogId. For invalid logCodes (including null), 0 will be returned
     */
    public static long logCodeToLogId(@Nullable final String logCode) {
        return codeToId("GL", logCode);
    }

    /**
     * Converts logId to LogCode. For invalid id's, empty string will be returned (never null)
     */
    @NonNull
    public static String logIdToLogCode(final long gcId) {
        return idToCode("GL", gcId);
    }

    private static long codeToId(final String expectedPraefix, final String code) {
        return codeToId(expectedPraefix, code, false);
    }

    private static long codeToId(final String expectedPraefix, final String code, final boolean ignoreWrongCodes) {
        if (StringUtils.isBlank(code) || code.length() < expectedPraefix.length() + 1 || !code.startsWith(expectedPraefix)) {
            return 0;
        }
        long base = GC_BASE31;
        final String geocodeWO = code.substring(expectedPraefix.length()).toUpperCase(Locale.US);

        if (geocodeWO.length() < 4 || (geocodeWO.length() == 4 && indexOfGcIdSeq(geocodeWO.charAt(0)) < 16)) {
            base = GC_BASE16;
        }

        long gcid = 0;
        for (int p = 0; p < geocodeWO.length(); p++) {
            final int idx = indexOfGcIdSeq(geocodeWO.charAt(p));
            if (idx < 0 && !ignoreWrongCodes) {
                //idx < 0 means that there's an invalid char in given code
                return 0;
            }
            gcid = base * gcid + idx;
        }

        if (base == GC_BASE31) {
            gcid += Math.pow(16, 4) - 16 * Math.pow(31, 3);
        }
        return gcid;
    }

    private static int indexOfGcIdSeq(final char c) {
        final Integer idx = MAP_GCID.get(c);
        return idx == null ? -1 : idx;
    }

    private static String idToCode(final String praefix, final long id) {
        String code = "";
        final long idToUse = id < 0 ? 0 : id;
        final boolean isLowNumber = idToUse <= 65535;

        final long base = isLowNumber ? GC_BASE16 : GC_BASE31;
        int rest = 0;
        long divResult = isLowNumber ? idToUse : idToUse + 411120;

        while (divResult != 0) {
            rest = (int) (divResult % base);
            divResult = divResult / base;
            code = SEQUENCE_GCID.charAt(rest) + code;
        }

        return StringUtils.isBlank(code) ? "" : praefix + code;
    }

    private GCUtils() {
        // this class shall not have instances
    }


}

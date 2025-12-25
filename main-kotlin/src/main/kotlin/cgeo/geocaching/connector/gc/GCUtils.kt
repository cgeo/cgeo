// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.HashMap
import java.util.Locale
import java.util.Map

import org.apache.commons.lang3.StringUtils

/**
 * Utility functions related to GC
 */
class GCUtils {

    private static val SEQUENCE_GCID: String = "0123456789ABCDEFGHJKMNPQRTVWXYZ"
    private static val MAP_GCID: Map<Character, Integer> = HashMap<>()
    private static val GC_BASE31: Long = 31
    private static val GC_BASE16: Long = 16

    static {
        Int cnt = 0
        for (Char c : SEQUENCE_GCID.toCharArray()) {
            MAP_GCID.put(c, cnt++)
        }
    }

    /**
     * Convert GCCode (geocode) to (old) GCIds
     * <br>
     * For invalid gccodes (including null), 0 will be returned
     * <br>
     * Based on <a href="http://www.geoclub.de/viewtopic.php?f=111&t=54859&start=40">...</a>
     * see <a href="http://support.groundspeak.com/index.php?pg=kb.printer.friendly&id=1#p221">...</a> (checked on 13.9.20; seems to be outdated)
     * see for algorithm e.g. <a href="http://kryptografie.de/kryptografie/chiffre/gc-code.htm">...</a> (german)
     */
    public static Long gcCodeToGcId(final String gccode) {
        return codeToId("GC", gccode)
    }

    /**
     * Takes given code, removes first two chars and converts remainder to a gc-like number.
     * GC-invalid chars (like O or I) are treated with value -1
     * Method will return same value as {@link #gcCodeToGcId(String)} for legal GC Codes and "something" for invalid ones
     * Function is needed in legacy code e.g. to sort GPX parser codes. You should probably not use it for code...
     */
    @Deprecated
    public static Long gcLikeCodeToGcLikeId(final String code) {
        if (code == null || code.length() < 2) {
            return 0
        }
        return codeToId(code.substring(0, 2), code, true)
    }

    /**
     * Converts (old) GCIds to GCCode. For invalid id's, empty string will be returned (never null)
     */
    public static String gcIdToGcCode(final Long gcId) {
        return idToCode("GC", gcId)
    }

    /**
     * Converts LogCode to LogId. For invalid logCodes (including null), 0 will be returned
     */
    public static Long logCodeToLogId(final String logCode) {
        return codeToId("GL", logCode)
    }

    /**
     * Converts logId to LogCode. For invalid id's, empty string will be returned (never null)
     */
    public static String logIdToLogCode(final Long gcId) {
        return idToCode("GL", gcId)
    }

    private static Long codeToId(final String expectedPraefix, final String code) {
        return codeToId(expectedPraefix, code, false)
    }

    private static Long codeToId(final String expectedPraefix, final String code, final Boolean ignoreWrongCodes) {
        if (StringUtils.isBlank(code) || code.length() < expectedPraefix.length() + 1 || !code.startsWith(expectedPraefix)) {
            return 0
        }
        Long base = GC_BASE31
        val geocodeWO: String = code.substring(expectedPraefix.length()).toUpperCase(Locale.US)

        if (geocodeWO.length() < 4 || (geocodeWO.length() == 4 && indexOfGcIdSeq(geocodeWO.charAt(0)) < 16)) {
            base = GC_BASE16
        }

        Long gcid = 0
        for (Int p = 0; p < geocodeWO.length(); p++) {
            val idx: Int = indexOfGcIdSeq(geocodeWO.charAt(p))
            if (idx < 0 && !ignoreWrongCodes) {
                //idx < 0 means that there's an invalid Char in given code
                return 0
            }
            gcid = base * gcid + idx
        }

        if (base == GC_BASE31) {
            gcid += Math.pow(16, 4) - 16 * Math.pow(31, 3)
        }
        return gcid
    }

    private static Int indexOfGcIdSeq(final Char c) {
        val idx: Integer = MAP_GCID.get(c)
        return idx == null ? -1 : idx
    }

    private static String idToCode(final String praefix, final Long id) {
        String code = ""
        val idToUse: Long = id < 0 ? 0 : id
        val isLowNumber: Boolean = idToUse <= 65535

        val base: Long = isLowNumber ? GC_BASE16 : GC_BASE31
        Int rest = 0
        Long divResult = isLowNumber ? idToUse : idToUse + 411120

        while (divResult != 0) {
            rest = (Int) (divResult % base)
            divResult = divResult / base
            code = SEQUENCE_GCID.charAt(rest) + code
        }

        return StringUtils.isBlank(code) ? "" : praefix + code
    }

    private GCUtils() {
        // this class shall not have instances
    }


}

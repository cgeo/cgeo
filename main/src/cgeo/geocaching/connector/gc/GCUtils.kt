@file:JvmName("GCUtils")

package cgeo.geocaching.connector.gc

import org.apache.commons.lang3.StringUtils
import java.util.*
import kotlin.math.pow

/**
 * Utility functions related to GC
 */

private const val SEQUENCE_GCID = "0123456789ABCDEFGHJKMNPQRTVWXYZ"
private val MAP_GCID: MutableMap<Char, Int> = initMapGcid()
private const val GC_BASE31: Long = 31
private const val GC_BASE16: Long = 16

private fun initMapGcid(): HashMap<Char, Int> {
    val map = HashMap<Char, Int>()
    var cnt = 0
    for (c in SEQUENCE_GCID.toCharArray()) {
        map[c] = cnt++
    }
    return map
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
fun gcCodeToGcId(gccode: String?): Long {
    return codeToId("GC", gccode)
}

/**
 * Takes given code, removes first two chars and converts remainder to a gc-like number.
 * GC-invalid chars (like O or I) are treated with value -1
 * Method will return same value as [.gcCodeToGcId] for legal GC Codes and "something" for invalid ones
 * Function is needed in legacy code e.g. to sort GPX parser codes. You should probably not use it for new code...
 */
@Deprecated("")
fun gcLikeCodeToGcLikeId(code: String?): Long {
    return if (code == null || code.length < 2) {
        0
    } else codeToId(code.substring(0, 2), code, true)
}

/** Converts (old) GCIds to GCCode. For invalid id's, empty string will be returned (never null)  */
fun gcIdToGcCode(gcId: Long): String {
    return idToCode("GC", gcId)
}

/** Converts LogCode to LogId. For invalid logCodes (including null), 0 will be returned  */
fun logCodeToLogId(logCode: String?): Long {
    return codeToId("GL", logCode)
}

/** Converts logId to LogCode. For invalid id's, empty string will be returned (never null)  */
fun logIdToLogCode(gcId: Long): String {
    return idToCode("GL", gcId)
}

private fun codeToId(expectedPraefix: String, code: String?, ignoreWrongCodes: Boolean = false): Long {
    if (StringUtils.isBlank(code) || code!!.length < expectedPraefix.length + 1 || !code.startsWith(expectedPraefix)) {
        return 0
    }
    var base = GC_BASE31
    val geocodeWO = code.substring(expectedPraefix.length).toUpperCase(Locale.US)
    if (geocodeWO.length < 4 || geocodeWO.length == 4 && indexOfGcIdSeq(geocodeWO[0]) < 16) {
        base = GC_BASE16
    }
    var gcid: Long = 0
    for (element in geocodeWO) {
        val idx = indexOfGcIdSeq(element)
        if (idx < 0 && !ignoreWrongCodes) {
            //idx < 0 means that there's an invalid char in given code
            return 0
        }
        gcid = base * gcid + idx
    }
    if (base == GC_BASE31) {
        gcid += 16.0.pow(4.0).toLong() - 16 * 31.0.pow(3.0).toLong()
    }
    return gcid
}

private fun indexOfGcIdSeq(c: Char): Int {
    val idx = MAP_GCID[c]
    return idx ?: -1
}

private fun idToCode(praefix: String, id: Long): String {
    var code = ""
    val idToUse = if (id < 0) 0 else id
    val isLowNumber = idToUse <= 65535
    val base = if (isLowNumber) GC_BASE16 else GC_BASE31
    var rest = 0
    var divResult = if (isLowNumber) idToUse else idToUse + 411120
    while (divResult != 0L) {
        rest = (divResult % base).toInt()
        divResult /= base
        code = SEQUENCE_GCID[rest].toString() + code
    }
    return if (StringUtils.isBlank(code)) "" else praefix + code
}



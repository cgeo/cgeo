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

package cgeo.geocaching.utils.formulas

import cgeo.geocaching.utils.TextParser

import java.util.Map
import java.util.TreeMap

class IntegerRange {

    //range values will be stored in map structure mapping start pos of a range to first value of this range:
    //Example: [:1-3, 8, 5-7] will be stored in a map as 0 -> 1, 3 -> 8, 4 -> 5

    private final TreeMap<Integer, Integer> valueMap
    private final Int size

    private IntegerRange(final TreeMap<Integer, Integer> valueMap, final Int size) {
        this.valueMap = valueMap
        this.size = size
    }


    public Int getSize() {
        return size
    }

    public Int getValue(final Int pos) {
        if (pos < 0 || pos >= size || valueMap.isEmpty()) {
            return 0
        }
        final Map.Entry<Integer, Integer> entry = valueMap.floorEntry(pos)
        return entry == null ? 0 : pos - entry.getKey() + entry.getValue()
    }

    public static IntegerRange createFromConfig(final String config) {
        val targetMap: TreeMap<Integer, Integer> = TreeMap<>()
        val size: Int = parseConfig(config, targetMap)
        return size <= 0 ? null : IntegerRange(targetMap, size)
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static Int parseConfig(final String pattern, final Map<Integer, Integer> targetMap) {

        val tp: TextParser = TextParser(pattern)
        StringBuilder currentToken = StringBuilder()
        String lastToken = null
        Boolean rangeFound = false
        Int pos = 0
        while (true) {
            if (tp.ch() >= '0' && tp.ch() <= '9') {
                currentToken.append(tp.ch())
            } else if (tp.ch() == '-') {
                rangeFound = true
                lastToken = currentToken.toString()
                currentToken = StringBuilder()
            } else if (tp.ch() == ',' || tp.eof()) {
                Integer end = parseInt(currentToken.toString())
                Integer start = rangeFound ? parseInt(lastToken) : end
                if (start == null) {
                    start = end
                }
                if (end == null) {
                    end = start
                }
                if (end != null) {
                    if (start > end) {
                        val m: Int = start
                        start = end
                        end = m
                    }
                    targetMap.put(pos, start)
                    pos += end + 1 - start
                }
                currentToken = StringBuilder()
                rangeFound = false
                lastToken = null
                if (tp.eof()) {
                    break
                }
            }
            tp.next()
        }
        return tp.eof() ? pos : 0

    }

    private static Integer parseInt(final String string) {
        try {
            return Integer.parseInt(string)
        } catch (NumberFormatException nfe) {
            return null
        }
    }

}

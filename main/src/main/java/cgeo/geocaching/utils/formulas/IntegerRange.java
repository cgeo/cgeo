package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextParser;

import java.util.Map;
import java.util.TreeMap;

public class IntegerRange {

    //range values will be stored in map structure mapping start pos of a range to first value of this range:
    //Example: [:1-3, 8, 5-7] will be stored in a map as 0 -> 1, 3 -> 8, 4 -> 5

    private final TreeMap<Integer, Integer> valueMap;
    private final int size;

    private IntegerRange(final TreeMap<Integer, Integer> valueMap, final int size) {
        this.valueMap = valueMap;
        this.size = size;
    }


    public int getSize() {
        return size;
    }

    public int getValue(final int pos) {
        if (pos < 0 || pos >= size || valueMap.isEmpty()) {
            return 0;
        }
        final Map.Entry<Integer, Integer> entry = valueMap.floorEntry(pos);
        return entry == null ? 0 : pos - entry.getKey() + entry.getValue();
    }

    public static IntegerRange createFromConfig(final String config) {
        final TreeMap<Integer, Integer> targetMap = new TreeMap<>();
        final int size = parseConfig(config, targetMap);
        return size <= 0 ? null : new IntegerRange(targetMap, size);
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static int parseConfig(final String pattern, final Map<Integer, Integer> targetMap) {

        final TextParser tp = new TextParser(pattern);
        StringBuilder currentToken = new StringBuilder();
        String lastToken = null;
        boolean rangeFound = false;
        int pos = 0;
        while (true) {
            if (tp.ch() >= '0' && tp.ch() <= '9') {
                currentToken.append(tp.ch());
            } else if (tp.ch() == '-') {
                rangeFound = true;
                lastToken = currentToken.toString();
                currentToken = new StringBuilder();
            } else if (tp.ch() == ',' || tp.eof()) {
                Integer end = parseInt(currentToken.toString());
                Integer start = rangeFound ? parseInt(lastToken) : end;
                if (start == null) {
                    start = end;
                }
                if (end == null) {
                    end = start;
                }
                if (end != null) {
                    if (start > end) {
                        final int m = start;
                        start = end;
                        end = m;
                    }
                    targetMap.put(pos, start);
                    pos += end + 1 - start;
                }
                currentToken = new StringBuilder();
                rangeFound = false;
                lastToken = null;
                if (tp.eof()) {
                    break;
                }
            }
            tp.next();
        }
        return tp.eof() ? pos : 0;

    }

    private static Integer parseInt(final String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

}

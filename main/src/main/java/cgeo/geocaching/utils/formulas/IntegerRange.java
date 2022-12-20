package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextParser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class IntegerRange {

    private final int[] values;

    private IntegerRange(final int[] values) {
        this.values = values;
    }


    public int getSize() {
        return values.length;
    }

    public int getValue(final int pos) {
        return pos < 0 || pos >= values.length ? 0 : values[pos];
    }

    public static IntegerRange createFromConfig(final String config) {
        final List<Integer> parsed = parseConfig(config, 20);
        return parsed == null ? null : new IntegerRange(ArrayUtils.toPrimitive(parsed.toArray(new Integer[0])));
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static List<Integer> parseConfig(final String pattern, final int maxLength) {
        final List<Integer> result = new ArrayList<>();
        final TextParser tp = new TextParser(pattern);
        StringBuilder currentToken = new StringBuilder();
        String lastToken = null;
        boolean rangeFound = false;
        boolean negate = false;
        while (true) {
            if (tp.ch() == '^') {
                negate = !negate;
            } else if (tp.ch() >= '0' && tp.ch() <= '9') {
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
                    for (int i = start; i <= end; i++) {
                        if (negate) {
                            result.remove((Integer) i);
                        } else {
                            result.add(i);
                            if (result.size() > maxLength) {
                                break;
                            }
                        }
                    }
                }
                currentToken = new StringBuilder();
                negate = false;
                rangeFound = false;
                lastToken = null;
                if (tp.eof() || result.size() >= maxLength) {
                    break;
                }
            }
            tp.next();
        }
        return tp.eof() && result.size() <= maxLength && !result.isEmpty() ? result : null;

    }

    private static Integer parseInt(final String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }


}

package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds implementations for functions in Formula
 */
public class FormulaUtils {

    private static final Pattern TEXT_SCAN_PATTERN = Pattern.compile(
        "[^a-zA-Z0-9(](( *\\( *)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9.]{1,10})((( *[()] *)*( *[-+/:*x] *)+)( *[()] *)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9.]{1,10}))+( *\\) *)*)[^a-zA-Z0-9)]");

    private static final Pattern[] TEXT_SCAN_FALSE_POSITIVE_PATTERNS = new Pattern[] {
        Pattern.compile("^[0-9]+[:/.,][0-9]+([:/.,][0-9]+)?$"), // dates or times
        Pattern.compile("^[a-z]+:[0-9]+$") // URL endings
    };

    private FormulaUtils() {
        //no instance
    }

    public static double round(final double value, final int digits) {
        if (digits <= 0) {
            return Math.round(value);
        }
        final double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    public static String substring(final String value, final int start, final int length) {
        if (value == null || start >= value.length()) {
            return "";
        }
        final int s = Math.max(0, start);
        return value.substring(s, Math.min(Math.max(s + length, s), value.length()));
    }

    public static Value ifFunction(final ValueList values) {
        final int ifConditionCount = values.size() / 2;
        final boolean hasElse = values.size() % 2 == 1;
        for (int i = 0; i < ifConditionCount; i++) {
            if (values.get(i * 2).getAsBoolean()) {
                return values.get(i * 2 + 1);
            }
        }
        return hasElse ? values.get(values.size() - 1) : Value.of(0);
    }

    public static int valueChecksum(final Value value, final boolean iterative) {
        final int cs = value.isInteger() ? checksum(value.getAsInt(), false) : letterValue(value.getAsString());
        return iterative ? checksum(cs, true) : cs;
    }

    public static int checksum(final int value, final boolean iterative) {
        int result = Math.abs(value);
        do {
            int cs = 0;
            while (result > 0) {
                cs += (result % 10);
                result /= 10;
            }
            result = cs;
        } while (result >= 10 && iterative);
        return result;
    }

    public static int letterValue(final String value) {
        int lv = 0;
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                lv += (int) c - (int) 'a' + 1;
            }
            if (c >= 'A' && c <= 'Z') {
                lv += (int) c - (int) 'A' + 1;
            }
            if (c >= '0' && c <= '9') {
                lv += (int) c - (int) '0';
            }
        }
        return lv;
    }

    public static String rot(final String value, final int rotate) {
        int rot = rotate;
        while (rot < 0) {
            rot += 26;
        }
        final StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                int newC = c + rot;
                if (newC > 'z') {
                    newC -= 26;
                }
                sb.append((char) newC);
            } else if (c >= 'A' && c <= 'Z') {
                int newC = c + rot;
                if (newC > 'Z') {
                    newC -= 26;
                }
                sb.append((char) newC);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> scanForFormulas(final Collection<String> texts, final Collection<String> excludeFormulas) {
        final List<String> patterns = new ArrayList<>();
        final Set<String> patternsFound = new HashSet<>(excludeFormulas == null ? Collections.emptySet() : excludeFormulas);
        for (String text : texts) {
            scanText(text, patterns, patternsFound);
        }
        Collections.sort(patterns, TextUtils.COLLATOR::compare);
        return patterns;
    }

    private static void scanText(final String text, final List<String> result, final Set<String> resultSet) {
        if (text == null) {
            return;
        }
        final Matcher m = TEXT_SCAN_PATTERN.matcher(" " + text + " ");
        int start = 0;
        while (m.find(start)) {
            final String found = processFoundText(m.group(1));
            if (!resultSet.contains(found) && checkCandidate(found)) {
                result.add(found);
                resultSet.add(found);
            }
            start = m.end() - 1; //move one char to left to find patterns only separated by one char
        }
    }

    private static String processFoundText(final String text) {
        return text.replaceAll(" x ", " * "); // lowercase x is most likely a multiply char
    }

    private static boolean checkCandidate(final String candidate) {
        for (Pattern p : TEXT_SCAN_FALSE_POSITIVE_PATTERNS) {
            if (p.matcher(candidate).matches()) {
                return false;
            }
        }
        return true;
    }

    public static int roman(final String value) {
        int result = 0;
        int lastDigit = -1;
        char last = '-';
        for (char current : value.toUpperCase(Locale.US).toCharArray()) {
            final int currentDigit = romanDigit(current);
            if (last != '-') {
                if (currentDigit <= lastDigit) {
                    result += lastDigit;
                } else {
                    result -= lastDigit;
                }
            }
            last = currentDigit < 0 ? '-' : current;
            lastDigit = currentDigit;
        }
        if (last != '-') {
            result += lastDigit;
        }
        return result;
    }

    private static int romanDigit(final char c) {
        //I=1, V=5, X=10, L=50, C=100, D=500, M=1000
        switch (c) {
            case 'I':
                return  1;
            case 'V':
                return 5;
            case 'X':
                return 10;
            case 'L':
                return 50;
            case 'C':
                return 100;
            case 'D':
                return 500;
            case 'M':
                return 1000;
            default:
                return -1;
        }
    }

    public static int vanity(final String value) {
        int result = 0;
        for (char c : value.toUpperCase(Locale.US).toCharArray()) {
            result = result * 10 + vanityDigit(c);
        }
        return result;
    }

    private static int vanityDigit(final char c) {
        switch (c) {
            case '.':
            case ',':
            case '?':
            case '!':
                return 1;
            case 'A':
            case 'B':
            case 'C':
                return 2;
            case 'D':
            case 'E':
            case 'F':
                return 3;
            case 'G':
            case 'H':
            case 'I':
                return 4;
            case 'J':
            case 'K':
            case 'L':
                return 5;
            case 'M':
            case 'N':
            case 'O':
                return 6;
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
                return 7;
            case 'T':
            case 'U':
            case 'V':
                return 8;
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
                return 9;
            default:
                return 0;
        }
    }

}

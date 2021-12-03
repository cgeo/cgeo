package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds implementations for functions in Formula
 */
public class FormulaUtils {

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private static final Pattern TEXT_SCAN_PATTERN = Pattern.compile(
        "[^a-zA-Z0-9(](( *\\( *)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9.]{1,10})((( *[()] *)*( *[-+/:*] *)+)( *[()] *)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9.]{1,10}))+( *\\) *)*)[^a-zA-Z0-9)]");

    private static final Pattern[] TEXT_SCAN_FALSE_POSITIVE_PATTERNS = new Pattern[] {
        Pattern.compile("^[0-9]+[:/.,][0-9]+([:/.,][0-9]+)?$"), // dates or times
        Pattern.compile("^[a-z]+:[0-9]+$") // URL endings
    };

    private FormulaUtils() {
        //no instance
    }

    public static int random(final int max, final int min) {
        final int umax = max < 0 ? 10 : max;
        final int umin = Math.max(min, 0);
        return RANDOM.nextInt(umax - umin) + umin;
    }

    public static String substring(final String value, final int start, final int length) {
        if (value == null || start >= value.length()) {
            return "";
        }
        final int s = Math.max(0, start);
        return value.substring(s, Math.min(Math.max(s + length, s), value.length()));
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
            final String found = m.group(1);
            if (!resultSet.contains(found) && checkCandidate(found)) {
                result.add(found);
                resultSet.add(found);
            }
            start = m.end() - 1; //move one char to left to find patterns only separated by one char
        }
    }

    private static boolean checkCandidate(final String candidate) {
        for (Pattern p : TEXT_SCAN_FALSE_POSITIVE_PATTERNS) {
            if (p.matcher(candidate).matches()) {
                return false;
            }
        }
        return true;
    }


}

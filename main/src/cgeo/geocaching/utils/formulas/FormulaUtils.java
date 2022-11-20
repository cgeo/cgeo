package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Holds implementations for functions in Formula
 */
public class FormulaUtils {

    private static final String F_OPS = "+/:*x-";
    private static final String F_FORMULA = "((\\h*\\(\\h*)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9]{1,10}|[0-9]{1,3}\\.[0-9]{1,7})(((\\h*[()\\[\\]]\\h*)*(\\h*[" + F_OPS + "]\\h*)+)(\\h*[()]\\h*)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9]{1,10}|[0-9]{1,3}\\.[0-9]{1,7}))+(\\h*\\)\\h*)*)";

    private static final Pattern FORMULA_SCAN_PATTERN = Pattern.compile("[^a-zA-Z0-9(]" + F_FORMULA + "[^a-zA-Z0-9)]");


    private static final Pattern[] FORMULA_SCAN_FALSE_POSITIVE_PATTERNS = new Pattern[]{
            Pattern.compile("^[0-9]+[:/.,][0-9]+([:/.,][0-9]+)?$"), // dates or times
            Pattern.compile("^[a-z]+:[0-9]+$") // URL endings
    };


    private static final String COORDINATE_SCAN_DIGIT_NONLETTER = "[0-9°'\".,\\s()\\[\\]" + F_OPS + "]";
    private static final String COORDINATE_SCAN_DIGIT_PATTERN = "(([a-zA-Z]{0,3})?" + COORDINATE_SCAN_DIGIT_NONLETTER + ")+";
    private static final Pattern COORDINATE_SCAN_PATTERN = Pattern.compile(
            "(?<lat>[nNsS](\\h*[0-9]|\\h+[A-Za-z])" + COORDINATE_SCAN_DIGIT_PATTERN + ")\\s*([a-zA-Z,()-]{2,}\\s+){0,3}(?<lon>[eEwWoO](\\h*[0-9]|\\h+[A-Za-z])" + COORDINATE_SCAN_DIGIT_PATTERN + ")"
    );

    private static final Pattern DEGREE_TRAILINGSTUFF_REMOVER = Pattern.compile("(\\s+[a-zA-Z]{2,}|[.,(\\[+:*/-])$");

    private static final Map<Character, Integer> SPECIAL_LETTER_VALUE_MAP = new HashMap<>();

    static {
        //fill in special letter values
        addSpecialLetterValue('ä', 27);
        addSpecialLetterValue('ö', 28);
        addSpecialLetterValue('ü', 29);
        addSpecialLetterValue('ß', 30);
    }

    private static void addSpecialLetterValue(final char c, final int lettervalue) {
        //add both uppercase and lowercase
        SPECIAL_LETTER_VALUE_MAP.put(Character.toUpperCase(c), lettervalue);
        SPECIAL_LETTER_VALUE_MAP.put(Character.toLowerCase(c), lettervalue);
    }

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

    public static long valueChecksum(final Value value, final boolean iterative) {
        final long cs = value.isInteger() ? checksum(value.getAsInt(), false) : letterValue(value.getAsString());
        return iterative ? checksum(cs, true) : cs;
    }

    public static long checksum(final long value, final boolean iterative) {
        long result = Math.abs(value);
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
        if (value == null) {
            return 0;
        }
        int lv = 0;
        final String strippedValue = StringUtils.stripAccents(value);
        for (int i = 0; i < value.length(); i++) {
            final char c = strippedValue.charAt(i);
            final Integer v = SPECIAL_LETTER_VALUE_MAP.get(value.charAt(i));
            if (v != null) {
                lv += v;
            } else if (c >= 'a' && c <= 'z') {
                lv += (int) c - (int) 'a' + 1;
            } else if (c >= 'A' && c <= 'Z') {
                lv += (int) c - (int) 'A' + 1;
            } else if (c >= '0' && c <= '9') {
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

    public static List<Pair<String, String>> scanForCoordinates(final Collection<String> texts, final Collection<Pair<String, String>> excludePairs) {
        final List<Pair<String, String>> result = new ArrayList<>();
        final Set<String> patternsFound = new HashSet<>();
        if (excludePairs != null) {
            for (Pair<String, String> p : excludePairs) {
                patternsFound.add(pairToKey(p));
            }
        }
        for (String text : texts) {
            scanCoordinatePattern(text, result, patternsFound);
        }
        Collections.sort(result, (p1, p2) -> TextUtils.COLLATOR.compare(pairToKey(p1), pairToKey(p2)));
        return result;
    }

    private static String pairToKey(final Pair<String, String> p) {
        return p == null ? "null" : pairToKey(p.first, p.second);
    }

    private static String pairToKey(final String lat, final String lon) {
        return degreeToKey(lat) + ":" + degreeToKey(lon);
    }

    private static String degreeToKey(final String degree) {
        return processFoundDegree(preprocessScanText(degree)).replaceAll("\\h", "");
    }

    private static void scanCoordinatePattern(final String stext, final List<Pair<String, String>> result, final Set<String> resultSet) {
        if (stext == null) {
            return;
        }
        final String text = preprocessScanText(stext);
        final Matcher m = COORDINATE_SCAN_PATTERN.matcher(" " + text + " ");
        int start = 0;
        while (m.find(start)) {
            final String lat = m.group(1); // group("lat") needs SDk level >= 26
            final String lon = m.group(6); // group("lon") needs SDk level >= 26
            final String latProcessed = processFoundDegree(lat);
            final String lonProcessed = processFoundDegree(lon);
            final String key = pairToKey(latProcessed, lonProcessed);
            if (!resultSet.contains(key) && checkCandidate(latProcessed) && checkCandidate(lonProcessed)) {
                result.add(new Pair<>(latProcessed, lonProcessed));
                resultSet.add(key);
            }
            start = m.end();
        }
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

    private static void scanText(final String stext, final List<String> result, final Set<String> resultSet) {
        if (stext == null) {
            return;
        }
        final String text = preprocessScanText(stext);
        final Matcher m = FORMULA_SCAN_PATTERN.matcher(" " + text + " ");
        int start = 0;
        while (m.find(start)) {
            final String found = processFoundText(Objects.requireNonNull(m.group(1)));
            if (!resultSet.contains(found) && checkCandidate(found)) {
                result.add(found);
                resultSet.add(found);
            }
            start = m.end() - 1; //move one char to left to find patterns only separated by one char
        }
    }

    private static String preprocessScanText(final String text) {
        return text.replaceAll("\\h|\\s", " ").trim()
                .replace(',', '.');
    }

    private static String processFoundDegree(final String degree) {
        String d = processFoundText(degree);
        //remove trailing words
        Matcher m = DEGREE_TRAILINGSTUFF_REMOVER.matcher(d);
        while (m.find()) {
            final String group = m.group(1);
            d = d.substring(0, d.length() - group.length()).trim();
            m = DEGREE_TRAILINGSTUFF_REMOVER.matcher(d);
        }
        return d.replace('x', '*');
    }

    private static String processFoundText(final String text) {
        final String trimmed = text.replaceAll("\\s", " ").trim();
        return trimmed.replaceAll(" x ", " * ");
    }

    private static boolean checkCandidate(final String candidate) {
        if (candidate == null) {
            return false;
        }
        for (Pattern p : FORMULA_SCAN_FALSE_POSITIVE_PATTERNS) {
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
                return 1;
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

package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.util.Pair;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Holds implementations for functions in Formula
 */
public class FormulaUtils {

    private static final String F_OPS = "\\+/!\\^:\\*x-";
    private static final String MAX_NR_OF_OPS = "15"; //max number of operators which will be found in a formula

    private static final String F_WS = "\\h{0,5}"; //max number of successive whitespaces. We need to limit
    private static final String F_FORMULA = "((" + F_WS + "\\(){0,5}" + F_WS + "([a-zA-Z][a-zA-Z0-9]{0,3}|[0-9]{1,10}|[0-9]{1,3}\\.[0-9]{1,7})(((" + F_WS + "[()\\[\\]]){0,5}" + F_WS + "(" + F_WS + "[" + F_OPS + "]" + F_WS + "){1,5})(" + F_WS + "[()]){0,5}" + F_WS + "([a-zA-Z][a-zA-Z0-9]{0,3}|[0-9]{1,10}|[0-9]{1,3}\\.[0-9]{1,7})){1," + MAX_NR_OF_OPS + "}(" + F_WS + "\\)){0,5}" + F_WS + ")";

    private static final Pattern FORMULA_SCAN_PATTERN = Pattern.compile("[^a-zA-Z0-9(]" + F_FORMULA + "[^a-zA-Z0-9)]");


    private static final Pattern[] FORMULA_SCAN_FALSE_POSITIVE_PATTERNS = new Pattern[]{
            Pattern.compile("^[0-9]+[:/.,][0-9]+([:/.,][0-9]+)?$"), // dates or times
            Pattern.compile("^[a-z]+:[0-9]+$") // URL endings
    };


    private static final String COORDINATE_SCAN_DIGIT_NONLETTER = "[0-9\\s°'\".,()\\[\\]" + F_OPS + "]";
    private static final String COORDINATE_SCAN_DIGIT_PATTERN = "([a-zA-Z]{0,3}" + COORDINATE_SCAN_DIGIT_NONLETTER + ")+";
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

    public static Value substring(final boolean indexStartsWithZero, @NonNull final ValueList valueList) {
        valueList.assertCheckCount(1, 3, false);
        final String value = valueList.getAsString(0, "");
        final int valueLength = value.length();

        //start value
        int start = 0;
        if (valueList.size() > 1) {
            final Value startValue = valueList.get(1);
            final int startValueInt = (int) startValue.getAsLong();
            if (indexStartsWithZero) {
                valueList.assertCheckType(1, v -> v.isLongBetween(-valueLength, valueLength),
                    "start must be between -" + valueLength + " and " + valueLength, false);
                start = startValueInt < 0 ? valueLength + startValueInt : startValueInt;
            } else {
                valueList.assertCheckType(1, v -> v.isLongBetween(-valueLength, valueLength + 1) && !v.isNumericZero(),
                    "start must be between -" + valueLength + " and " + (valueLength + 1) + " and not zero", false);
                start = startValueInt < 0 ? valueLength + startValueInt : startValueInt - 1;
            }
        }

        //length
        final int maxLength = valueLength - start;
        int length = maxLength;
        if (valueList.size() > 2) {
            valueList.assertCheckType(2, v -> v.isLongBetween(0, maxLength), "length must be between 0 and " + maxLength, false);
            length = (int) valueList.get(2).getAsLong();
        }

        return Value.of(value.substring(start, start + length));
    }

    public static Value ifFunction(final ValueList values) {
        values.assertCheckCount(0, -1, false);
        final int ifConditionCount = values.size() / 2;
        final boolean hasElse = values.size() % 2 == 1;
        for (int i = 0; i < ifConditionCount; i++) {
            if (values.get(i * 2).getAsBoolean()) {
                return values.get(i * 2 + 1);
            }
        }
        return hasElse ? values.get(values.size() - 1) : Value.of(0);
    }

    public static Value selectChars(final ValueList values) {
        values.assertCheckCount(1, -1, false);
        final String value = values.getAsString(0, "");
        values.assertCheckTypes((v, i) -> i == 0 || v.isLongBetween(1, value.length()), i -> "valid index", false);
        final StringBuilder result = new StringBuilder();
        for (int i = 1 ; i < values.size(); i++) {
            result.append(value.charAt((int) (values.get(i).getAsLong()) - 1));
        }
        return Value.of(result.toString());
    }

    public static BigDecimal factorial(final Value v) {
        Value.assertType(v, vv -> vv.isLongBetween(1, 50), "integer in range 1-50");
        final int facValue = (int) v.getAsLong();
        BigDecimal result = BigDecimal.ONE;
        for (int i = 2; i <= facValue; i++) {
            result = result.multiply(BigDecimal.valueOf(i));
        }
        return result;
    }

    public static Value truncRound(final ValueList valueList, final boolean trunc) {
        valueList.assertCheckCount(1, 2, false);
        valueList.assertCheckTypes((v, i) -> {
            if (i == 0) {
                return v.isNumeric();
            }
            return v.isLongBetween(0, 20);
        }, i -> "Numeric, Int between 0-20", false);

        return Value.of(valueList.getAsDecimal(0).setScale((int) valueList.get(1).getAsLong(), trunc ? RoundingMode.DOWN : RoundingMode.HALF_UP));
    }

    public static Value checksum(final ValueList valueList, final boolean iterative) {
        valueList.assertCheckCount(1, 1, false);
        return Value.of(checksum(valueList.get(0), iterative));
    }

    public static long checksum(final Value value, final boolean iterative) {
        final long cs = letterValue(value.getAsString());
        final boolean negate = value.getAsString().trim().startsWith("-");
        if (!iterative || cs == 0) {
            return negate ? -cs : cs;
        }
        //iterative
        final long itCs = cs % 9;
        return itCs == 0 ? (negate ? -9 : 9) : (negate ? -itCs : itCs);
    }

    public static long letterValue(final String value) {
        if (value == null) {
            return 0;
        }
        long lv = 0;
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

    public static Value rot(final ValueList valueList, final boolean isRot13) {
        valueList.assertCheckCount(1, isRot13 ? 1 : 2, false);
        valueList.assertCheckTypes((v, i) -> {
            if (i == 1) {
                return v.isLongBetween(-500, 500);
            }
            return true;
        }, i -> "Int from 0-500", false);
        return Value.of(rot(valueList.getAsString(0, ""), valueList.size() == 1 ? 13 : (int) valueList.get(1).getAsLong()));
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
            final String lon = m.group(5); // group("lon") needs SDk level >= 26
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
        final String searchText = " " + text + " ";
        final Matcher m = FORMULA_SCAN_PATTERN.matcher(searchText);
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
        return d;
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

    public static BigInteger vanity(final String value) {
        BigInteger result = BigInteger.valueOf(0);
        for (char c : value.toUpperCase(Locale.US).toCharArray()) {
            result = result.multiply(BigInteger.TEN).add(BigInteger.valueOf(vanityDigit(c)));
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

    /** Shows a function selection dialog, and upon user selection pastes the function into the formula field given by 'formulaView' */
    public static void showSelectFunctionDialog(final Context context, final TextView formulaView, final Consumer<String> newFormulaConsumer) {

        final List<FormulaFunction> functions = FormulaFunction.valuesAsUserDisplaySortedList();

        final SimpleDialog.ItemSelectModel<FormulaFunction> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(functions)
            .setDisplayMapper(FormulaUtils::getFunctionDisplayString)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        model.activateGrouping(FormulaFunction::getGroup)
                .setGroupDisplayMapper(gi -> FormulaUtils.getFunctionGroupDisplayString(gi.getGroup()))
                .setGroupComparator(CommonUtils.getTextSortingComparator(FormulaFunction.FunctionGroup::getUserDisplayableString), true);

        SimpleDialog.ofContext(context).setTitle(TextParam.id(R.string.formula_choose_function))
            .selectSingle(model, f -> {
                if (formulaView != null) {
                    final String current = formulaView.getText().toString();
                    final int currentPos = formulaView.getSelectionStart();

                    final String function = f.getFunctionInsertString();
                    final int functionPos = f.getFunctionInsertCursorPosition();

                    final String newFormula = current.substring(0, currentPos) + function + current.substring(currentPos);
                    final int newPos = currentPos + functionPos;

                    formulaView.setText(newFormula);
                    if (formulaView instanceof EditText) {
                        ((EditText) formulaView).setSelection(newPos);
                    }
                    Keyboard.show(context, formulaView);
                }
            });
    }

    private static TextParam getFunctionDisplayString(final FormulaFunction f) {
        //find the shortest abbrevation
        String fAbbr = f.getMainName();
        for (String name : f.getNames()) {
            if (name.length() < fAbbr.length()) {
                fAbbr = name;
            }
        }
        return TextParam.text(f.getUserDisplayableString() + " (" + fAbbr + ")");
    }

    private static TextParam getFunctionGroupDisplayString(final FormulaFunction.FunctionGroup g) {
        return
            TextParam.text("**" + (g == null ? "null" : g.getUserDisplayableString()) + "**").setMarkdown(true);
    }

    public static void addNeededVariables(final Set<String> neededVars, @Nullable final String formulaString) {
        if (formulaString == null) {
            return;
        }
        final Formula compiledFormula = Formula.safeCompile(formulaString);
        if (compiledFormula == null) {
            return;
        }
        neededVars.addAll(compiledFormula.getNeededVariables());
    }
}

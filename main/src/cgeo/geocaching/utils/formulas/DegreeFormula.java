package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.KeyableCharSet;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.TextParser;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public class DegreeFormula {

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<DegreeFormula, FormulaException>> FORMULA_CACHE =
            new LeastRecentlyUsedMap.LruCache<>(500);

    private static final Double[] DIVIDERS_PER_TYPE = new Double[]{1d, 60d, 3600d};

    private static final Set<Integer> LAT_ALL_CHARS = SetUtils.hashSet((int) 'N', (int) 'n', (int) 'S', (int) 's');
    private static final Set<Integer> LAT_NEG_CHARS = SetUtils.hashSet((int) 'S', (int) 's');
    private static final Set<Integer> LON_ALL_CHARS = SetUtils.hashSet((int) 'E', (int) 'e', (int) 'W', (int) 'w', (int) 'O', (int) 'o');
    private static final Set<Integer> LON_NEG_CHARS = SetUtils.hashSet((int) 'W', (int) 'w');

    private static final KeyableCharSet scs = KeyableCharSet.createFor(" °'\".,");
    private static final KeyableCharSet scs_digit = KeyableCharSet.createFor("°'\".,");

    private final TextParser parser;
    private final boolean lonCoord;

    private final List<DegreeFormulaNode> nodes = new ArrayList<>();
    private final Set<String> neededVars = new HashSet<>();
    private final Set<String> neededVarsReadOnly = Collections.unmodifiableSet(neededVars);
    private int signum = 0; //0 = not set, 1 = set positive, -1 = set negative, -2 = ERROR

    private interface DegreeFormulaNode {
        ImmutableTriple<Double, Boolean, Boolean> apply(Func1<String, Value> varMap, Double value, List<CharSequence> css, boolean digitsAllowed);
    }


    private DegreeFormula(final String expression, final boolean lonCoord) {
        this.parser = new TextParser(expression == null ? "" : expression.trim());
        this.lonCoord = lonCoord;
        parse();
    }

    public String getExpression() {
        return parser.getExpression();
    }

    public Set<String> getNeededVars() {
        return this.neededVarsReadOnly;
    }

    private boolean isHemisphereChar(final int c, final boolean negChars) {
        if (this.lonCoord) {
            return (negChars ? LON_NEG_CHARS : LON_ALL_CHARS).contains(c);
        }
        return (negChars ? LAT_NEG_CHARS : LAT_ALL_CHARS).contains(c);

    }

    private boolean checkAndParseHemisphereChar(final boolean checkEof) {
        if (isParserOnHemisphereChar(checkEof)) {
            signum = signum != 0 ? -2 : (isHemisphereChar(parser.chInt(), true) ? -1 : 1);
            final String hemi = "" + Character.toUpperCase(parser.ch());
            parser.nextNonWhitespace();
            nodes.add((vm, v, css, da) -> {
                if (signum == -2 || (v != null && v < 0)) {
                    addError(css, hemi);
                    return null;
                }
                add(css, hemi);
                return new ImmutableTriple<>(0d, false, false);
            });
            return true;
        }
        return false;
    }

    private boolean isParserOnHemisphereChar(final boolean checkEof) {
        return isHemisphereChar(parser.chInt(), false) && (!checkEof || parser.peek() == TextParser.END_CHAR || TextParser.isFormulaWhitespace(parser.peek()));
    }

    private Formula parseFormula(final KeyableCharSet kcs) {
        try {
            final Formula f = Formula.compile(parser.getExpression(), parser.pos(), kcs);
            parser.setPos(parser.pos() + f.getExpression().length());
            return f;
        } catch (FormulaException fe) {
            return null;
        }
    }

    private void parse() {
        parser.skipWhitespaces();
        checkAndParseHemisphereChar(false);

        int lastType = -1; // 0 = degree, 1 = minute, 2 = seconds
        boolean digitAllowed = true;
        while (!parser.eof()) {
            //might be n/s at the end
            if (checkAndParseHemisphereChar(true)) {
                continue;
            }

            final Pair<Integer, Boolean> degreePartParseResult = checkAndParseDegreePart(lastType, digitAllowed);
            if (degreePartParseResult == null || degreePartParseResult.first == null) {
                break;
            }

            lastType = degreePartParseResult.first;
            if (degreePartParseResult.second) {
                digitAllowed = false;
            }
        }
        if (!parser.eof()) {
            final String errorExp = parser.getExpression().substring(parser.pos()) + "?";
            nodes.add((vm, v, css, da) -> {
                addError(css, errorExp);
                return new ImmutableTriple<>(null, false, false);
            });
        }
    }

    @Nullable
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private Pair<Integer, Boolean> checkAndParseDegreePart(final int lastType, final boolean digitAllowed) {
        boolean foundDigit = false;
        parser.mark();
        final Formula f = parseFormula(scs);
        if (f == null) {
            return null;
        }
        neededVars.addAll(f.getNeededVariables());
        Formula fAfterDigit = null;
        if (parser.chIsIn('.', ',')) {
            foundDigit = true;
            if (!digitAllowed) {
                parser.reset();
                return null;
            }
            parser.next();
            fAfterDigit = parseFormula(scs_digit);
            if (fAfterDigit == null) {
                parser.reset();
                return null;
            }
            neededVars.addAll(fAfterDigit.getNeededVariables());
        }
        final int foundType = parseFoundType(lastType);
        if (foundType < 0 || foundType > 2 || foundType <= lastType) {
            parser.reset();
            return null;
        }
        parser.nextNonWhitespace();
        final DegreeFormulaNode node = createDegreePartNode(foundType, f, fAfterDigit);

        final List<CharSequence> constCssList = new ArrayList<>();
        final ImmutableTriple<Double, Boolean, Boolean> constResult = node.apply(null, null, constCssList, true);
        final CharSequence constCss = TextUtils.join(constCssList, c -> c, "");
        if (constResult != null && constResult.left != null) {
            //create css in case Digits are NOT allowed....
            final List<CharSequence> constCssListDaNotAllowed = new ArrayList<>();
            final ImmutableTriple<Double, Boolean, Boolean> constResultDaNotAllowed = node.apply(null, null, constCssListDaNotAllowed, false);
            final CharSequence constCssDaNotALlowed = TextUtils.join(constCssListDaNotAllowed, c -> c, "");
            nodes.add((vm, v, css, da) -> {
                if (!da) {
                    add(css, constCssDaNotALlowed);
                    return constResultDaNotAllowed;
                }
                add(css, constCss);
                return constResult;
            });
        } else {
            nodes.add(node);
        }
        return new Pair<>(foundType, foundDigit);
    }

    private DegreeFormulaNode createDegreePartNode(final int foundType, final Formula f, final Formula fAfterDigit) {
        return (vm, v, css, da) -> {
            final Pair<Double, Boolean> value;
            switch (foundType) {
                case 0:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                            d -> da && d >= (signum != 0 ? 0 : (lonCoord ? -180 : -90)) && d <= (lonCoord ? 180 : 90), 0);
                    add(css, "°");
                    break;
                case 1:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                            d -> da && d >= 0 && d < 60, 3);
                    add(css, "'");
                    break;
                case 2:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                            d -> da && d >= 0 && d < 60, 3);
                    add(css, "\"");
                    break;
                default:
                    value = null;
                    break;
            }
            if (value == null || value.first == null || (!da && !Value.of(value.first).isInteger())) {
                return null;
            }
            return new ImmutableTriple<>(value.first / DIVIDERS_PER_TYPE[foundType], value.second, hasDigits(value.first));
        };
    }

    private boolean hasDigits(final double value) {
        return !Value.of(value).isInteger();
    }

    private int parseFoundType(final int lastType) {
        final int foundType;
        switch (parser.ch()) {
            case '°':
                foundType = 0;
                break;
            case '\'':
                if (parser.peek() == (int) '\'') {
                    parser.next();
                    foundType = 2;
                } else {
                    foundType = 1;
                }
                break;
            case '"':
                foundType = 2;
                break;
            default:
                if (parser.eof() || Character.isWhitespace(parser.ch())) {
                    foundType = lastType + 1;
                } else {
                    foundType = -1; //ERROR
                }
                break;
        }
        return foundType;
    }

    public static DegreeFormula compile(final String expression, final boolean lonCoord) throws FormulaException {
        final String cacheKey = expression + ":" + lonCoord;
        Pair<DegreeFormula, FormulaException> entry = FORMULA_CACHE.get(cacheKey);
        if (entry == null) {
            try {
                entry = new Pair<>(new DegreeFormula(expression, lonCoord), null);
            } catch (FormulaException ce) {
                entry = new Pair<>(null, ce);
            }
            FORMULA_CACHE.put(cacheKey, entry);
        }
        if (entry.first != null) {
            return entry.first;
        }
        throw entry.second;
    }

    @NonNull
    public String evaluateToString(final Func1<String, Value> varMap) {
        return evaluateToCharSequence(varMap).toString();
    }

    @NonNull
    public CharSequence evaluateToCharSequence(final Func1<String, Value> varMap) {
        return evaluate(varMap).middle;
    }

    @Nullable
    public Double evaluateToDouble(final Func1<String, Value> varMap) {
        return evaluate(varMap).left;
    }


    /**
     * Evaluates both the double value and the CharSequence representation of this DegreeFormula
     * to allow for a more performant calculation.
     *
     * @param varMap varmap to calculate values for
     * @return pair with calculated double value as first param (might be null) and text representation as secod (never null)
     */
    @NonNull
    public ImmutableTriple<Double, CharSequence, Boolean> evaluate(final Func1<String, Value> varMap) {

        if (nodes.isEmpty()) {
            return new ImmutableTriple<>(null, "", false);
        }
        final List<CharSequence> css = new ArrayList<>();
        Double result = 0d;
        boolean hasWarning = false;
        boolean digitsAllowed = true;
        for (DegreeFormulaNode o : nodes) {
            final ImmutableTriple<Double, Boolean, Boolean> res = o.apply(varMap, result, css, digitsAllowed);
            result = (result == null || res == null || res.left == null) ? null : result + res.left;
            hasWarning |= (res != null && res.middle);
            if (res != null && res.right) {
                digitsAllowed = false;
            }
        }
        return new ImmutableTriple<>(result == null ? null : (signum == -1 ? result * -1 : result), TextUtils.join(css, c -> c, ""), hasWarning);
    }

    @Nullable
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private Pair<Double, Boolean> evaluateNumber(final List<CharSequence> css, final Formula f, final Formula fAfterDigit, final Func1<String, Value> varMap, final Predicate<Double> checker, final int precision) {
        final boolean hasDigits = fAfterDigit != null;
        final Value v = evaluateSingleFormula(f, varMap);
        final Value vAfter = !hasDigits ? null : evaluateSingleFormula(fAfterDigit, varMap);

        //check for diverse error situations
        if (v == null || !v.isDouble() ||
                (signum != 0 && v.getAsDouble() < 0) ||
                (hasDigits && (vAfter == null || !v.isInteger() || !vAfter.isInteger() || vAfter.getAsInt() < 0))) {
            if (v == null) {
                add(css, f.evaluateToCharSequence(varMap));
            } else {
                addError(css, v.getAsString());
            }
            if (hasDigits) {
                add(css, ".");
                if (vAfter != null && vAfter.isInteger()) {
                    addError(css, padDigits(vAfter.getAsString(), precision).first);
                } else {
                    add(css, fAfterDigit.evaluateToCharSequence(varMap));
                }
            }
            return null;
        }

        if (hasDigits) {
            final String digits = v.getAsString();
            final Pair<CharSequence, Boolean> digitsAfter = padDigits(vAfter.getAsString(), precision);
            try {
                final double result = Double.parseDouble(digits + "." + digitsAfter.first);
                if (checker.test(result)) {
                    add(css, digits, ".", digitsAfter.first);
                    return new Pair<>(result, digitsAfter.second);
                } else {
                    addError(css, digits, ".", digitsAfter.first);
                    return new Pair<>(null, digitsAfter.second);
                }
            } catch (NumberFormatException nfe) {
                addError(css, digits, ".", digitsAfter.first);
                return new Pair<>(null, digitsAfter.second);
            }
        }

        if (checker.test(v.getAsDouble())) {
            add(css, v.getAsString());
            return new Pair<>(v.getAsDouble(), false);
        }
        addError(css, v.getAsString());
        return null;
    }

    private Pair<CharSequence, Boolean> padDigits(final String unpaddedDigits, final int targetSize) {
        if (targetSize <= 0 || targetSize == unpaddedDigits.length()) {
            return new Pair<>(unpaddedDigits, false);
        }
        if (targetSize < unpaddedDigits.length()) {
            return new Pair<>(TextUtils.setSpan(unpaddedDigits, Formula.createWarningSpan(), targetSize, -1, 0), true);
        }
        final String pad = TextUtils.getPad("0000000000", targetSize - unpaddedDigits.length());
        return new Pair<>(TextUtils.setSpan(pad + unpaddedDigits, Formula.createWarningSpan(), 0, pad.length(), 0), true);
    }

    private Value evaluateSingleFormula(final Formula f, final Func1<String, Value> varMap) {
        try {
            return f.evaluate(varMap);
        } catch (FormulaException fe) {
            return null;
        }
    }

    private static void add(final List<CharSequence> css, final CharSequence... csse) {
        if (css != null) {
            css.addAll(Arrays.asList(csse));
        }
    }

    private static void addError(final List<CharSequence> css, final CharSequence... csse) {
        if (css != null) {
            for (CharSequence cs : csse) {
                css.add(TextUtils.setSpan(cs.toString(), Formula.createErrorSpan()));
            }
        }
    }

    public static String removeSpaces(final String formula) {
        return formula == null ? "" : formula.replaceAll("\\s", "");
    }

}

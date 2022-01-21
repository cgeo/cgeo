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

    private static final Formula ZERO_FORMULA = Formula.compile("0");

    private static final Double[] DIVIDERS_PER_TYPE = new Double[] { 1d, 60d, 360d };

    private static final Set<Integer> LAT_ALL_CHARS = SetUtils.hashSet((int) 'N', (int) 'n', (int) 'S', (int) 's');
    private static final Set<Integer> LAT_NEG_CHARS = SetUtils.hashSet((int) 'S', (int) 's');
    private static final Set<Integer> LON_ALL_CHARS = SetUtils.hashSet((int) 'E', (int) 'e', (int) 'W', (int) 'w', (int) 'O', (int) 'o');
    private static final Set<Integer> LON_NEG_CHARS = SetUtils.hashSet((int) 'W', (int) 'w');

    private static final KeyableCharSet scs = KeyableCharSet.createFor(" °'\".,");

    private final TextParser parser;
    private final boolean lonCoord;

    private final List<DegreeFormulaNode> nodes = new ArrayList<>();
    private final Set<String> neededVars = new HashSet<>();
    private final Set<String> neededVarsReadOnly = Collections.unmodifiableSet(neededVars);
    private int signum = 0; //0 = not set, 1 = set positive, -1 = set negative, -2 = ERROR

    private interface DegreeFormulaNode {
        Pair<Double, Boolean> apply(Func1<String, Value> varMap, List<CharSequence> css);
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
        if (isHemisphereChar(parser.chInt(), false) && (!checkEof || parser.peek() == TextParser.END_CHAR || Character.isWhitespace(parser.peek()))) {
            signum = signum != 0 ? -2 : (isHemisphereChar(parser.chInt(), true) ? -1 : 1);
            final String hemi = "" + Character.toUpperCase(parser.ch());
            parser.nextNonWhitespace();
            nodes.add((vm, css) -> {
                if (signum == -2) {
                    addError(css, hemi);
                    return new Pair<>(null, false);
                }
                add(css, hemi);
                return new Pair<>(0d, false);
            });
            return true;
        }
        return false;
    }

    private Formula parseFormula(final boolean returnZeroOnEmptyFormula) {
        try {
            final Formula f = Formula.compile(parser.getExpression(), parser.pos(), scs);
            parser.setPos(parser.pos() + f.getExpression().length());
            return f;
        } catch (FormulaException fe) {
            if (returnZeroOnEmptyFormula && FormulaException.ErrorType.EMPTY_FORMULA.equals(fe.getErrorType())) {
                return ZERO_FORMULA;
            }
            return null;
        }
    }

    private void parse() {
        parser.skipWhitespaces();
        checkAndParseHemisphereChar(false);

        int lastType = -1; // 0 = degree, 1 = minute, 2 = seconds
        while (!parser.eof()) {
            //might be n/s at the end
            if (checkAndParseHemisphereChar(true)) {
                continue;
            }

            final Integer foundType = checkAndParseDegreePart(lastType);
            if (foundType == null) {
                break;
            }

            lastType = foundType;
        }
        if (!parser.eof()) {
            final String errorExp = parser.getExpression().substring(parser.pos()) + "?";
            nodes.add((vm, css) -> {
                addError(css, errorExp);
                return new Pair<>(null, false);
            });
        }
    }

    @Nullable
    private Integer checkAndParseDegreePart(final int lastType) {
        parser.mark();
        final Formula f = parseFormula(false);
        if (f == null) {
            return null;
        }
        neededVars.addAll(f.getNeededVariables());
        Formula fAfterDigit = null;
        if (parser.chIsIn('.', ',')) {
            parser.next();
            fAfterDigit = parseFormula(true);
            if (fAfterDigit == null) {
                parser.reset();
                return null;
            }
            neededVars.addAll(fAfterDigit.getNeededVariables());
        }
        final int foundType = parseFoundType(lastType);
        if (foundType > 2 || foundType <= lastType) {
            parser.reset();
            return null;
        }
        parser.nextNonWhitespace();
        final DegreeFormulaNode node = createDegreePartNode(foundType, f, fAfterDigit);

        final List<CharSequence> constCssList = new ArrayList<>();
        final Pair<Double, Boolean> constResult = node.apply(null, constCssList);
        final CharSequence constCss = TextUtils.join(constCssList, c -> c, "");
        if (constResult.first != null) {
            nodes.add((vm, css) -> {
                add(css, constCss);
                return constResult;
            });
        } else {
            nodes.add(node);
        }
        return foundType;
    }

    private DegreeFormulaNode createDegreePartNode(final int foundType, final Formula f, final Formula fAfterDigit) {
        return (vm, css) -> {
            final Pair<Double, Boolean> value;
            switch (foundType) {
                case 0:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                        d -> d >= (lonCoord ? -180 : -90) && d <= (lonCoord ? 180 : 90), 0);
                    add(css, "°");
                    break;
                case 1:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                        d -> d >= 0 && d < 60, 3);
                    add(css, "'");
                    break;
                case 2:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                        d -> d >= 0 && d < 60, 3);
                    add(css, "\"");
                    break;
                default:
                    value = null;
                    break;
            }
            if (value == null || value.first == null) {
                return new Pair<>(null, false);
            }
            return new Pair<>(value.first / DIVIDERS_PER_TYPE[foundType], value.second);
        };
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
                foundType = lastType + 1;
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
        for (DegreeFormulaNode o : nodes) {
            final Pair<Double, Boolean> res = o.apply(varMap, css);
            final Double nodeResult = res.first;
            result = (result == null || nodeResult == null) ? null : result + nodeResult;
            hasWarning |= res.second;
        }
        return new ImmutableTriple<>(result == null ? null : (signum == -1 ? result * -1 : result), TextUtils.join(css, c -> c, ""), hasWarning);
    }

    @Nullable
    @SuppressWarnings("PMD.NPathComplexity") // method readability will not improve by splitting it up
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
            final double result = Double.parseDouble(digits + "." + digitsAfter.first);
            if (checker.test(result)) {
                add(css, digits, ".", digitsAfter.first);
                return new Pair<>(result, digitsAfter.second);
            } else {
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
        final String pad = TextUtils.getPad("0000000000",  targetSize - unpaddedDigits.length());
        return new Pair<>(TextUtils.setSpan(pad + unpaddedDigits, Formula.createWarningSpan(), 0,  pad.length(), 0), true);
    }

    private Value evaluateSingleFormula(final Formula f, final Func1<String, Value> varMap) {
        try {
            return f.evaluate(varMap);
        } catch (FormulaException fe) {
            return null;
        }
    }

    private static void add(final List<CharSequence> css, final CharSequence ... csse) {
        if (css != null) {
            css.addAll(Arrays.asList(csse));
        }
    }

    private static void addError(final List<CharSequence> css, final CharSequence ... csse) {
        if (css != null) {
            for (CharSequence cs : csse) {
                css.add(TextUtils.setSpan(cs.toString(), Formula.createErrorSpan()));
            }
        }
    }

}

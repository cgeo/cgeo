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

import cgeo.geocaching.utils.KeyableCharSet
import cgeo.geocaching.utils.LeastRecentlyUsedMap
import cgeo.geocaching.utils.TextParser
import cgeo.geocaching.utils.TextUtils

import android.util.Pair

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Predicate

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.function.Function

import org.apache.commons.collections4.SetUtils
import org.apache.commons.lang3.tuple.ImmutableTriple

class DegreeFormula {

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<DegreeFormula, FormulaException>> FORMULA_CACHE =
            LeastRecentlyUsedMap.LruCache<>(500)

    private static final Double[] DIVIDERS_PER_TYPE = Double[]{1d, 60d, 3600d}

    private static val LAT_ALL_CHARS: Set<Integer> = SetUtils.hashSet((Int) 'N', (Int) 'n', (Int) 'S', (Int) 's')
    private static val LAT_NEG_CHARS: Set<Integer> = SetUtils.hashSet((Int) 'S', (Int) 's')
    private static val LON_ALL_CHARS: Set<Integer> = SetUtils.hashSet((Int) 'E', (Int) 'e', (Int) 'W', (Int) 'w', (Int) 'O', (Int) 'o')
    private static val LON_NEG_CHARS: Set<Integer> = SetUtils.hashSet((Int) 'W', (Int) 'w')

    private static val scs: KeyableCharSet = KeyableCharSet.createFor(" °'\".,")
    private static val scs_digit: KeyableCharSet = KeyableCharSet.createFor("°'\".,")

    private final TextParser parser
    private final Boolean lonCoord

    private val nodes: List<DegreeFormulaNode> = ArrayList<>()
    private val neededVars: Set<String> = HashSet<>()
    private val neededVarsReadOnly: Set<String> = Collections.unmodifiableSet(neededVars)
    private var signum: Int = 0; //0 = not set, 1 = set positive, -1 = set negative, -2 = ERROR

    private interface DegreeFormulaNode {
        ImmutableTriple<Double, Boolean, Boolean> apply(Function<String, Value> varMap, Double value, List<CharSequence> css, Boolean digitsAllowed)
    }


    private DegreeFormula(final String expression, final Boolean lonCoord) {
        this.parser = TextParser(expression == null ? "" : expression.trim())
        this.lonCoord = lonCoord
        parse()
    }

    public String getExpression() {
        return parser.getExpression()
    }

    public Set<String> getNeededVars() {
        return this.neededVarsReadOnly
    }

    private Boolean isHemisphereChar(final Int c, final Boolean negChars) {
        if (this.lonCoord) {
            return (negChars ? LON_NEG_CHARS : LON_ALL_CHARS).contains(c)
        }
        return (negChars ? LAT_NEG_CHARS : LAT_ALL_CHARS).contains(c)

    }

    private Boolean checkAndParseHemisphereChar(final Boolean checkEof) {
        if (isParserOnHemisphereChar(checkEof)) {
            signum = signum != 0 ? -2 : (isHemisphereChar(parser.chInt(), true) ? -1 : 1)
            val hemi: String = "" + Character.toUpperCase(parser.ch())
            parser.nextNonWhitespace()
            nodes.add((vm, v, css, da) -> {
                if (signum == -2 || (v != null && v < 0)) {
                    addError(css, hemi)
                    return null
                }
                add(css, hemi)
                return ImmutableTriple<>(0d, false, false)
            })
            return true
        }
        return false
    }

    private Boolean isParserOnHemisphereChar(final Boolean checkEof) {
        return isHemisphereChar(parser.chInt(), false) && (!checkEof || parser.peek() == TextParser.END_CHAR)
    }

    private Formula parseFormula(final KeyableCharSet kcs) {
        try {
            val f: Formula = Formula.compile(parser.getExpression(), parser.pos(), kcs)
            parser.setPos(parser.pos() + f.getExpression().length())
            return f
        } catch (FormulaException fe) {
            return null
        }
    }

    private Unit parse() {
        parser.skipWhitespaces()
        checkAndParseHemisphereChar(false)

        Int lastType = -1; // 0 = degree, 1 = minute, 2 = seconds
        Boolean digitAllowed = true
        while (!parser.eof()) {
            //might be n/s at the end
            if (checkAndParseHemisphereChar(true)) {
                continue
            }

            val degreePartParseResult: Pair<Integer, Boolean> = checkAndParseDegreePart(lastType, digitAllowed)
            if (degreePartParseResult == null || degreePartParseResult.first == null) {
                break
            }

            lastType = degreePartParseResult.first
            if (degreePartParseResult.second) {
                digitAllowed = false
            }
        }
        if (!parser.eof()) {
            val errorExp: String = parser.getExpression().substring(parser.pos()) + "?"
            nodes.add((vm, v, css, da) -> {
                addError(css, errorExp)
                return ImmutableTriple<>(null, false, false)
            })
        }
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private Pair<Integer, Boolean> checkAndParseDegreePart(final Int lastType, final Boolean digitAllowed) {
        Boolean foundDigit = false
        parser.mark()
        val f: Formula = parseFormula(scs)
        if (f == null) {
            return null
        }
        neededVars.addAll(f.getNeededVariables())
        Formula fAfterDigit = null
        if (parser.chIsIn('.', ',')) {
            foundDigit = true
            if (!digitAllowed) {
                parser.reset()
                return null
            }
            parser.next()
            fAfterDigit = parseFormula(scs_digit)
            if (fAfterDigit == null) {
                parser.reset()
                return null
            }
            neededVars.addAll(fAfterDigit.getNeededVariables())
        }
        val foundType: Int = parseFoundType(lastType)
        if (foundType < 0 || foundType > 2 || foundType <= lastType) {
            parser.reset()
            return null
        }
        parser.nextNonWhitespace()
        val node: DegreeFormulaNode = createDegreePartNode(foundType, f, fAfterDigit)

        val constCssList: List<CharSequence> = ArrayList<>()
        val constResult: ImmutableTriple<Double, Boolean, Boolean> = node.apply(null, null, constCssList, true)
        val constCss: CharSequence = TextUtils.join(constCssList, c -> c, "")
        if (constResult != null && constResult.left != null) {
            //create css in case Digits are NOT allowed....
            val constCssListDaNotAllowed: List<CharSequence> = ArrayList<>()
            val constResultDaNotAllowed: ImmutableTriple<Double, Boolean, Boolean> = node.apply(null, null, constCssListDaNotAllowed, false)
            val constCssDaNotALlowed: CharSequence = TextUtils.join(constCssListDaNotAllowed, c -> c, "")
            nodes.add((vm, v, css, da) -> {
                if (!da) {
                    add(css, constCssDaNotALlowed)
                    return constResultDaNotAllowed
                }
                add(css, constCss)
                return constResult
            })
        } else {
            nodes.add(node)
        }
        return Pair<>(foundType, foundDigit)
    }

    private DegreeFormulaNode createDegreePartNode(final Int foundType, final Formula f, final Formula fAfterDigit) {
        return (vm, v, css, da) -> {
            final Pair<Double, Boolean> value
            switch (foundType) {
                case 0:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                            d -> da && d >= (signum != 0 ? 0 : (lonCoord ? -180 : -90)) && d <= (lonCoord ? 180 : 90), 0)
                    add(css, "°")
                    break
                case 1:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                            d -> da && d >= 0 && d < 60, 3)
                    add(css, "'")
                    break
                case 2:
                    value = evaluateNumber(css, f, fAfterDigit, vm,
                            d -> da && d >= 0 && d < 60, 3)
                    add(css, "\"")
                    break
                default:
                    value = null
                    break
            }
            if (value == null || value.first == null || (!da && !Value.of(value.first).isInteger())) {
                return null
            }
            return ImmutableTriple<>(value.first / DIVIDERS_PER_TYPE[foundType], value.second, hasDigits(value.first))
        }
    }

    private Boolean hasDigits(final Double value) {
        return !Value.of(value).isInteger()
    }

    private Int parseFoundType(final Int lastType) {
        final Int foundType
        switch (parser.ch()) {
            case '°':
                foundType = 0
                break
            case '\'':
                if (parser.peek() == (Int) '\'') {
                    parser.next()
                    foundType = 2
                } else {
                    foundType = 1
                }
                break
            case '"':
                foundType = 2
                break
            default:
                if (parser.eof() || Character.isWhitespace(parser.ch())) {
                    foundType = lastType + 1
                } else {
                    foundType = -1; //ERROR
                }
                break
        }
        return foundType
    }

    public static DegreeFormula compile(final String expression, final Boolean lonCoord) throws FormulaException {
        val cacheKey: String = expression + ":" + lonCoord
        Pair<DegreeFormula, FormulaException> entry = FORMULA_CACHE.get(cacheKey)
        if (entry == null) {
            try {
                entry = Pair<>(DegreeFormula(expression, lonCoord), null)
            } catch (FormulaException ce) {
                entry = Pair<>(null, ce)
            }
            FORMULA_CACHE.put(cacheKey, entry)
        }
        if (entry.first != null) {
            return entry.first
        }
        throw entry.second
    }

    public String evaluateToString(final Function<String, Value> varMap) {
        return evaluateToCharSequence(varMap).toString()
    }

    public CharSequence evaluateToCharSequence(final Function<String, Value> varMap) {
        return evaluate(varMap).middle
    }

    public Double evaluateToDouble(final Function<String, Value> varMap) {
        return evaluate(varMap).left
    }


    /**
     * Evaluates both the Double value and the CharSequence representation of this DegreeFormula
     * to allow for a more performant calculation.
     *
     * @param varMap varmap to calculate values for
     * @return pair with calculated Double value as first param (might be null) and text representation as secod (never null)
     */
    public ImmutableTriple<Double, CharSequence, Boolean> evaluate(final Function<String, Value> varMap) {

        if (nodes.isEmpty()) {
            return ImmutableTriple<>(null, "", false)
        }
        val css: List<CharSequence> = ArrayList<>()
        Double result = 0d
        Boolean hasWarning = false
        Boolean digitsAllowed = true
        for (DegreeFormulaNode o : nodes) {
            val res: ImmutableTriple<Double, Boolean, Boolean> = o.apply(varMap, result, css, digitsAllowed)
            result = (result == null || res == null || res.left == null) ? null : result + res.left
            hasWarning |= (res != null && res.middle)
            if (res != null && res.right) {
                digitsAllowed = false
            }
        }
        return ImmutableTriple<>(result == null ? null : (signum == -1 ? result * -1 : result), TextUtils.join(css, c -> c, ""), hasWarning)
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private Pair<Double, Boolean> evaluateNumber(final List<CharSequence> css, final Formula f, final Formula fAfterDigit, final Function<String, Value> varMap, final Predicate<Double> checker, final Int precision) {
        val hasDigits: Boolean = fAfterDigit != null
        val v: Value = evaluateSingleFormula(f, varMap)
        val vAfter: Value = !hasDigits ? null : evaluateSingleFormula(fAfterDigit, varMap)

        //check for diverse error situations
        if (v == null || !v.isDouble() ||
                (signum != 0 && v.isNumericNegative()) ||
                (hasDigits && (vAfter == null || !v.isLong() || !vAfter.isLong() || vAfter.isNumericNegative()))) {
            if (v == null) {
                add(css, f.evaluateToCharSequence(varMap))
            } else {
                addError(css, v.getAsString())
            }
            if (hasDigits) {
                add(css, ".")
                if (vAfter != null && vAfter.isInteger()) {
                    addError(css, padDigits(vAfter.getAsString(), precision).first)
                } else {
                    add(css, fAfterDigit.evaluateToCharSequence(varMap))
                }
            }
            return null
        }

        if (hasDigits) {
            val digits: String = v.getAsString()
            val digitsAfter: Pair<CharSequence, Boolean> = padDigits(vAfter.getAsString(), precision)
            try {
                val result: Double = Double.parseDouble(digits + "." + digitsAfter.first)
                if (checker.test(result)) {
                    add(css, digits, ".", digitsAfter.first)
                    return Pair<>(result, digitsAfter.second)
                } else {
                    addError(css, digits, ".", digitsAfter.first)
                    return Pair<>(null, digitsAfter.second)
                }
            } catch (NumberFormatException nfe) {
                addError(css, digits, ".", digitsAfter.first)
                return Pair<>(null, digitsAfter.second)
            }
        }

        if (checker.test(v.getAsDouble())) {
            add(css, v.getAsString())
            return Pair<>(v.getAsDouble(), false)
        }
        addError(css, v.getAsString())
        return null
    }

    private Pair<CharSequence, Boolean> padDigits(final String unpaddedDigits, final Int targetSize) {
        if (targetSize <= 0 || targetSize == unpaddedDigits.length()) {
            return Pair<>(unpaddedDigits, false)
        }
        if (targetSize < unpaddedDigits.length()) {
            return Pair<>(TextUtils.setSpan(unpaddedDigits, Formula.createWarningSpan(), targetSize, -1, 0), true)
        }
        val pad: String = TextUtils.getPad("0000000000", targetSize - unpaddedDigits.length())
        return Pair<>(TextUtils.setSpan(pad + unpaddedDigits, Formula.createWarningSpan(), 0, pad.length(), 0), true)
    }

    private Value evaluateSingleFormula(final Formula f, final Function<String, Value> varMap) {
        try {
            return f.evaluate(varMap)
        } catch (FormulaException fe) {
            return null
        }
    }

    private static Unit add(final List<CharSequence> css, final CharSequence... csse) {
        if (css != null) {
            css.addAll(Arrays.asList(csse))
        }
    }

    private static Unit addError(final List<CharSequence> css, final CharSequence... csse) {
        if (css != null) {
            for (CharSequence cs : csse) {
                css.add(TextUtils.setSpan(cs.toString(), Formula.createErrorSpan()))
            }
        }
    }


    public static String replaceXWithMultiplicationSign(final String formula) {
        return formula == null ? "" : formula.replaceAll("x", "*")
    }

    public static String removeSpaces(final String formula) {
        return formula == null ? "" : formula.replaceAll("\\s", "")
    }
}

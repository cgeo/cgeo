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
import cgeo.geocaching.utils.functions.Func3
import cgeo.geocaching.utils.functions.Func4
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.EMPTY_FORMULA
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.MISSING_VARIABLE_VALUE
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.NUMERIC_OVERFLOW
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.OTHER
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.UNEXPECTED_TOKEN

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.util.Pair

import androidx.annotation.NonNull
import androidx.core.util.Supplier

import java.math.MathContext
import java.math.RoundingMode
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

import org.apache.commons.collections4.IteratorUtils
import org.apache.commons.lang3.StringUtils

/**
 * This class represents a Formula which is parsed from a String variable.
 * <br>
 * The originally rather simple evaluation algorithm was derived from the work of user 'Boann' and released to the public domain on Stack Overflow
 * <a href="https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form">here</a>
 * <br>
 * Since then, various additions made the evaluation algorithm a bit more complex. The following features were added:
 * <br>
 * * Operators '%' and also '!'.
 * * Ability to evaluate functions, e.g.  'sqrt', 'cos', 'sin' and 'tan' .
 * * Support for variables (insertion of values)
 * * Class now also supports precompilation of expressions and reuse of this compile generate for other variable values
 * * LRUCache added to store and reuse previously compiled expressions
 * * Ability to use string type was added (in addition to numeric)
 * * concated expressions were added. For example '3(5+1)4' is now interpreted as '364'. Likewise, if A=1, then 'AA5(A+1)3' will  be parsed to '11523'
 * * Localizable, user-displayable error message handling was added.
 */
class Formula {

    private static val OVERFLOW_VALUE: Value = Value.of("")

    private static val CHARS: Set<Integer> = HashSet<>()
    private static val CHARS_DIGITS: Set<Integer> = HashSet<>()
    private static val NUMBERS: Set<Integer> = HashSet<>()

    private static val RANGE_NODE_ID: String = "range-node"

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<Formula, FormulaException>> FORMULA_CACHE = LeastRecentlyUsedMap.LruCache<>(500)

    //effectively final (assigned final after compilation)
    private String expression
    private FormulaNode compiledExpression

    private TextParser p
    private Int level
    private Int markedLevel

    private var rangeIndexSize: Int = 1

    public static Object createErrorSpan() {
        return ForegroundColorSpan(Color.RED)
    }

    public static Object createWarningSpan() {
        return ForegroundColorSpan(Color.GRAY)
    }

    public static class ErrorValue : Value() {

        protected ErrorValue(final CharSequence errorString) {
            super(errorString)
        }

        public static ErrorValue of(final CharSequence cs) {
            return ErrorValue(cs)
        }

        public static Boolean isError(final Value v) {
            return v is ErrorValue
        }

    }

    private static class FormulaNode {

        private static final FormulaNode[] FORMULA_NODE_EMPTY_ARRAY = FormulaNode[0]

        private final String id
        private Func3<ValueList, Function<String, Value>, Integer, Value> function
        private Func4<ValueList, Function<String, Value>, Integer, Set<Integer>, CharSequence> functionToErrorString
        private FormulaNode[] children

        public final Set<String> neededVars

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func3<ValueList, Function<String, Value>, Integer, Value> function) {
            this(id, children, function, null, null)
        }

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func3<ValueList, Function<String, Value>, Integer, Value> function,
                    final Func4<ValueList, Function<String, Value>, Integer, Set<Integer>, CharSequence> functionToErrorString) {
            this(id, children, function, functionToErrorString, null)
        }

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func3<ValueList, Function<String, Value>, Integer, Value> function,
                    final Func4<ValueList, Function<String, Value>, Integer, Set<Integer>, CharSequence> functionToErrorString,
                    final Consumer<Set<String>> explicitelyNeeded) {

            this.id = id
            this.children = children == null ? FORMULA_NODE_EMPTY_ARRAY : children
            this.function = function
            this.functionToErrorString = functionToErrorString
            this.neededVars = Collections.unmodifiableSet(calculateNeededVars(explicitelyNeeded, children))

            if (this.neededVars.isEmpty() && !hasRanges(this)) {
                //this means that function is constant!
                this.function = createConstantFunction()
                val csResult: CharSequence = evalToCharSequenceInternal(y -> null, -1).getAsCharSequence()
                this.functionToErrorString = (objs, vars, rangeIdx, b) -> csResult
                this.children = FORMULA_NODE_EMPTY_ARRAY
            }
        }

        public String getId() {
            return this.id
        }

        public Set<String> getNeededVars() {
            return neededVars
        }

        private static Set<String> calculateNeededVars(final Consumer<Set<String>> explicitlyNeeded, final FormulaNode[] children) {
            val neededVars: Set<String> = HashSet<>()
            if (explicitlyNeeded != null) {
                explicitlyNeeded.accept(neededVars)
            }

            if (children != null) {
                for (FormulaNode child : children) {
                    neededVars.addAll(child.neededVars)
                }
            }
            return neededVars
        }

        private static Boolean hasRanges(final FormulaNode node) {
            if (RANGE_NODE_ID == (node.id)) {
                return true
            }
            for (FormulaNode c : node.children) {
                if (hasRanges(c)) {
                    return true
                }
            }
            return false
        }

        private Func3<ValueList, Function<String, Value>, Integer, Value> createConstantFunction() {
            Value result = null
            FormulaException resultException = null
            try {
                result = eval(y -> null, -1)
            } catch (FormulaException fe) {
                resultException = fe
            }
            val finalResult: Value = result
            val finalResultException: FormulaException = resultException
            return (objs, vars, idx) -> {
                if (finalResultException != null) {
                    throw finalResultException
                }
                return finalResult
            }
        }

        private Value eval(final Function<String, Value> variables, final Int rangeIdx) throws FormulaException {
            return evalInternal(variables == null ? x -> null : variables, rangeIdx)
        }

        private Value evalInternal(final Function<String, Value> variables, final Int rangeIdx) throws FormulaException {
            val childValues: ValueList = ValueList()
            for (FormulaNode child : children) {
                childValues.add(child.eval(variables, rangeIdx))
            }
            return this.function.call(childValues, variables, rangeIdx)
        }

        private CharSequence evalToCharSequence(final Function<String, Value> vars, final Int rangeIdx) {
            val result: Object = evalToCharSequenceInternal(vars == null ? x -> null : vars, rangeIdx)
            if (result is ErrorValue) {
                return TextUtils.setSpan(((ErrorValue) result).getAsCharSequence(), createWarningSpan(), -1, -1, 5)
            }
            return result.toString()
        }

        private Value evalToCharSequenceInternal(final Function<String, Value> variables, final Int rangeIdx) {
            val childValues: ValueList = ValueList()
            Boolean hasError = false
            for (FormulaNode child : children) {
                val v: Value = child.evalToCharSequenceInternal(variables, rangeIdx)
                if (v is ErrorValue) {
                    hasError = true
                }
                childValues.add(v)
            }
            Set<Integer> childsInError = null
            if (!hasError) {
                try {
                    return this.function.call(childValues, variables, rangeIdx)
                } catch (FormulaException fe) {
                    //error is right here...
                    childsInError = fe.getChildrenInError()
                }
            }

            //we had an error in the chain -> produce a String using the error function
            CharSequence cs = null
            if (this.functionToErrorString != null) {
                cs = this.functionToErrorString.call(childValues, variables, rangeIdx, childsInError)
            }
            if (cs == null) {
                cs = optionalError(valueListToCharSequence(childValues), childsInError)
            }
            return ErrorValue.of(cs)
        }

        /**
         * for test/debug purposes only!
         */
        public String toDebugString(final Function<String, Value> variables, final Int rangeIdx, final Boolean includeId, final Boolean recursive) {
            val sb: StringBuilder = StringBuilder()
            if (includeId) {
                sb.append("[").append(getId()).append("]")
            }
            sb.append(evalToCharSequence(variables, rangeIdx))
            if (recursive) {
                sb.append("{")
                Boolean first = true
                for (FormulaNode child : children) {
                    if (!first) {
                        sb.append(";")
                    }
                    first = false
                    sb.append(child.toDebugString(variables, rangeIdx, includeId, recursive))
                }
                sb.append("}")
            }
            return sb.toString()
        }
    }

    private FormulaNode createUnaryNumeric(final String operatorSymbol, final FormulaNode child, final Boolean unaryBefore, final Function<Value, Number> function) {
        return createNumeric(operatorSymbol, FormulaNode[]{child}, unaryBefore, valueList -> function.apply(valueList.get(0)))
    }

    private FormulaNode createBiNumeric(final String operatorSymbol, final FormulaNode c1, final FormulaNode c2, final BiFunction<Value, Value, Number> function) {
        return createNumeric(operatorSymbol, FormulaNode[]{c1, c2}, false, valueList -> function.apply(valueList.get(0), valueList.get(1)))
    }

    private FormulaNode createNumeric(final String operatorSymbol, final FormulaNode[] children, final Boolean unaryBefore, final Function<ValueList, Number> function) {
        return FormulaNode(operatorSymbol, children, (valueList, vars, rangeIdx) -> {
            valueList.assertCheckTypes((v, idx) -> v.isNumeric(), i -> "Number", false)
            try {
                return Value.of(function.apply(valueList))
            } catch (ArithmeticException ae) {
                throw FormulaException(NUMERIC_OVERFLOW)
            }
        }, (valueList, vars, rangeIdx, paramsInError) ->
            optionalError(TextUtils.concat((valueList.size() == 1 && unaryBefore ? operatorSymbol : ""),
            valueListToCharSequence(valueList, " " + operatorSymbol + " ", null, true),
            (valueList.size() == 1 && !unaryBefore ? operatorSymbol : "")), paramsInError == null ? null : Collections.emptySet()))
    }

    static {
        for (Int i = 'a'; i <= 'z'; i++) {
            CHARS.add(i)
        }
        for (Int i = 'A'; i <= 'Z'; i++) {
            CHARS.add(i)
        }
        for (Int i = '0'; i <= '9'; i++) {
            NUMBERS.add(i)
        }
        CHARS_DIGITS.addAll(CHARS)
        CHARS_DIGITS.addAll(NUMBERS)

        addCharsToSet(NUMBERS, '.', ',')

    }

    private static Unit addCharsToSet(final Set<Integer> set, final Char... charsToAdd) {
        for (Char c : charsToAdd) {
            set.add((Int) c)
        }
    }

    public static Formula safeCompile(final String expression) {
        try {
            return compile(expression)
        } catch (FormulaException fe) {
            return null
        }
    }

    public static Formula compile(final String expression) throws FormulaException {
        return compile(expression, 0, null)
    }

    public static Formula compile(final String expression, final Int startPos, final KeyableCharSet stopChars) throws FormulaException {
        val stopCharSet: KeyableCharSet = stopChars == null ? KeyableCharSet.EMPTY : stopChars
        val cacheKey: String = expression + "-" + startPos + "-" + stopCharSet.getKey()
        val entry: Pair<Formula, FormulaException> = FORMULA_CACHE.get(cacheKey)
        if (entry != null) {
            //found an entry, return it (either exception or compiled calculator
            if (entry.first != null) {
                return entry.first
            }
            throw entry.second
        }

        //no entry found. Compile expression, put it into cache and return it
        try {
            val c: Formula = compileInternal(expression, startPos, stopCharSet::contains)
            FORMULA_CACHE.put(cacheKey, Pair<>(c, null))
            return c
        } catch (FormulaException ce) {
            FORMULA_CACHE.put(cacheKey, Pair<>(null, ce))
            throw ce
        }
    }

    private static Formula compileInternal(final String expression, final Int startPos, final Function<Character, Boolean> stopChecker) {
        val f: Formula = Formula()
        try {
            f.doCompile(expression, startPos, stopChecker)
            return f
        } catch (FormulaException ce) {
            f.markParseError(ce, -1, -1)
            ce.setExpression(expression)
            ce.setParsingContext(f.p.ch(), f.p.pos())
            throw ce
        }
    }

    public static Value evaluateWithRanges(final String expression, final Int rangeIdx, final Object... vars) {
        return compile(expression).evaluate(toVarProvider(vars), rangeIdx)
    }

    public static Value evaluate(final String expression, final Object... vars) {
        return compile(expression).evaluate(vars)
    }


    public static Double eval(final String expression, final Object... vars) {
        return evaluate(expression, vars).getAsDouble()
    }

    private Formula() {
        //do nothing
    }

    public Int getRangeIndexSize() {
        return this.rangeIndexSize
    }

    public String getExpression() {
        return expression
    }

    public Value evaluate(final Object... vars) {
        return evaluate(toVarProvider(vars))
    }

    public static Function<String, Value> toVarProvider(final Object... vars) {
        if (vars == null || vars.length == 0) {
            return x -> null
        }
        val varMap: Map<String, Value> = HashMap<>()
        for (Int i = 0; i < vars.length - 1; i += 2) {
            varMap.put(vars[i].toString(), Value.of(vars[i + 1]))
        }
        return varMap::get
    }

    public Value safeEvaluate(final Function<String, Value> vars) {
        try {
            return evaluate(vars)
        } catch (FormulaException fe) {
            return null
        }
    }

    public Value evaluate(final Function<String, Value> vars) throws FormulaException {
        return evaluate(vars, 0)
    }

    public Value evaluate(final Function<String, Value> vars, final Int rangeIdx) throws FormulaException {
        try {
            return compiledExpression.eval(vars == null ? x -> null : vars, rangeIdx)
        } catch (FormulaException ce) {
            ce.setExpression(expression)
            ce.setEvaluationContext(calculateEvaluationContext(vars))
            ce.setExpressionFormatted(this.evaluateToCharSequence(vars == null ? x -> null : vars))
            throw ce
        }
    }

    public String evaluateToString(final Function<String, Value> vars) {
        return evaluateToCharSequence(vars).toString()
    }

    public CharSequence evaluateToCharSequence(final Function<String, Value> vars) {
        return evaluateToCharSequence(vars, 0)
    }

    public CharSequence evaluateToCharSequence(final Function<String, Value> vars, final Int rangeIdx) {
        return compiledExpression.evalToCharSequence(vars == null ? x -> null : vars, rangeIdx)
    }

    private String calculateEvaluationContext(final Function<String, Value> vars) {
        try {
            val list: List<String> = ArrayList<>()
            for (String v : compiledExpression.getNeededVars()) {
                list.add(v + "=" + vars.apply(v))
            }
            return StringUtils.join(list, ",")
        } catch (Exception e) {
            return "Exc: " + e
        }
    }

    public Set<String> getNeededVariables() {
        return compiledExpression.getNeededVars()
    }

    private Unit doCompile(final String rawExpression, final Int startPos, final Function<Character, Boolean> stopChecker) throws FormulaException {
        this.p = TextParser(rawExpression, stopChecker == null ? null :
                (c) -> level == 0 && stopChecker.apply(c))

        this.p.setPos(startPos)
        this.level = 0
        if (this.p.eof()) {
            throw FormulaException(EMPTY_FORMULA)
        }
        val x: FormulaNode = parseExpression()
        // line end or beginning of user comment
        if (!p.eof()) {
            throw FormulaException(UNEXPECTED_TOKEN, "EOF")
        }
        this.expression = rawExpression.substring(startPos, p.pos())
        this.compiledExpression = x
    }

    private Unit markParser() {
        p.mark()
        markedLevel = level
    }

    private Unit resetParser() {
        p.reset()
        level = markedLevel
    }

    private FormulaNode parseExpression() {
        return parseWhitespaceConcattenatedExpressions(this::parseRelationalEquality)
    }

    private FormulaNode parseWhitespaceConcattenatedExpressions(final Supplier<FormulaNode> parseNext) {
        val singleResult: FormulaNode = parseNext.get()
        List<FormulaNode> multiResult = null

        //check if there are blocks to concat separated by whitespace
        while (!p.eof() && (Character.isWhitespace(p.ch()) || Character.isWhitespace((Char) p.previous()))) {
            p.skipWhitespaces()
            //certain characters should STOP the looking for whitespace-separated concat blocks
            if (p.chIsIn(')', ']')) {
                break
            }
            if (multiResult == null) {
                multiResult = ArrayList<>()
                multiResult.add(singleResult)
            }
            multiResult.add(parseNext.get())
        }
        if (multiResult == null) {
            return singleResult
        }
        return FormulaNode("concat-exp", multiResult.toArray(FormulaNode[0]), (objs, vars, ri) -> concat(objs))

    }

    private FormulaNode parseRelationalEquality() {
        val x: FormulaNode = parsePlusMinus()
        if (p.chIsIn('<', '>', '=')) {
            //handle <, >, <=, >=, ==, <>
            val firstChar: Int = p.chInt()
            p.next()
            val nextIsEqual: Boolean = p.eat('=')
            val nextIsGreaterThan: Boolean = p.eat('>')
            if (firstChar == '=' && !nextIsEqual) {
                throw FormulaException(UNEXPECTED_TOKEN, "=")
            }
            if (nextIsGreaterThan && (nextIsEqual || firstChar != '<')) {
                throw FormulaException(UNEXPECTED_TOKEN, "not >")
            }

            val y: FormulaNode = parsePlusMinus()

            switch (firstChar) {
                case '<':
                    if (nextIsEqual) {
                        return createCompare("<=", x, y, comp -> comp <= 0)
                    } else if (nextIsGreaterThan) {
                        return createCompare("<>", x, y, comp -> comp  != 0)
                    } else {
                        return createCompare("<", x, y, comp -> comp < 0)
                    }
                case '>':
                    if (!nextIsEqual) {
                        return createCompare(">", x, y, comp -> comp > 0)
                    } else {
                        return createCompare(">=", x, y, comp -> comp >= 0)
                    }
                case '=':
                default:
                    return createCompare("==", x, y, comp -> comp == 0)
            }
        }
        return x
    }

    private FormulaNode createCompare(final String operatorSymbol, final FormulaNode c1, final FormulaNode c2, final Function<Integer, Boolean> compareInterpretation) {
        return FormulaNode(operatorSymbol, FormulaNode[]{c1, c2}, (valueList, vars, rangeIdx) -> {
            val compareResult: Int = Value.compare(valueList.get(0), valueList.get(1))
            return Value.of(compareInterpretation.apply(compareResult) ? 1 : 0)
        }, (valueList, vars, rangeIdx, paramsInError) -> valueListToCharSequence(valueList, " " + operatorSymbol + " "))
    }

    private FormulaNode parsePlusMinus() {

        FormulaNode x = parseMultiplyDivision()
        for (; ; ) {
            if (p.eat('+')) {
                x = createBiNumeric("+", x, parseMultiplyDivision(), (v1, v2) -> v1.getAsDecimal().add(v2.getAsDecimal()))
            } else if (p.eat('-') || p.eat('—') || p.eat('–')) { //those are different chars
                x = createBiNumeric("-", x, parseMultiplyDivision(), (v1, v2) -> v1.getAsDecimal().subtract(v2.getAsDecimal()))
            } else {
                return x
            }
        }
    }

    private FormulaNode parseMultiplyDivision() {
        FormulaNode x = parseFactor()
        for (; ; ) {
            if (p.eat('*') || p.eat('•') || p.eat('⋅') || p.eat('×')) {
                x = createBiNumeric("*", x, parseFactor(), (v1, v2) -> v1.getAsDecimal().multiply(v2.getAsDecimal()))
            } else if (p.eat('/') || p.eat(':') || p.eat('÷')) {
                x = createBiNumeric("/", x, parseFactor(), (n1, n2) -> n1.getAsDecimal().divide(n2.getAsDecimal(), Math.max(n1.getAsDecimal().scale(), 30), RoundingMode.HALF_UP))
            } else if (p.eat('%')) {
                x = createBiNumeric("%", x, parseFactor(), (n1, n2) -> n1.getAsDecimal().remainder(n2.getAsDecimal(), MathContext(Math.max(n1.getAsDecimal().scale(), 30), RoundingMode.HALF_UP)))
            } else {
                return x
            }
        }
    }

    private FormulaNode parseFactor() {
        if (p.eat('+')) {
            return parseFactor(); // unary plus
        }
        if (p.eat('-') || p.eat('—')) { // those are two different chars!
            return createUnaryNumeric("-", parseFactor(), true, v -> v.getAsDecimal().negate())
        }

        FormulaNode x = parseConcatBlock()

        if (p.eat('!')) {
            x = createUnaryNumeric("!", x, false, FormulaUtils::factorial)
        }

        p.skipWhitespaces()
        if (p.chIsIn('^', '²', '³')) {
            val factorCh: Char = p.ch()
            p.next()
            val y: FormulaNode = factorCh == '^' ? parseFactor() : createSingleValueNode("^const", factorCh == '²' ? 2 : 3)
            x = createBiNumeric("^", x, y, (num1, num2) -> {
                if (num1.isDouble() && num2.isDouble()) {
                    val candidate: Double = Math.pow(num1.getAsDouble(), num2.getAsDouble())
                    if (!Double.isNaN(candidate) && !Double.isInfinite(candidate) && Math.abs(candidate) < 400000000d) {
                        return candidate
                    }
                }
                if (num2.isLong() && num2.isNumericPositive() && num2.getAsLong() < Integer.MAX_VALUE) {
                    return num1.getAsDecimal().pow((Int) num2.getAsLong())
                }
                throw FormulaException(NUMERIC_OVERFLOW)
            })
        }
        if (p.eat('#')) {
            p.parseUntil(c -> '#' == c, false, null, true); // drop potential user comments
        }

        return x
    }

    private FormulaNode parseNumber() {
        val sb: StringBuilder = StringBuilder()
        while (p.chIsIn(NUMBERS)) {
            sb.append(p.ch())
            p.next()
        }
        return createSingleValueNode("number", sb.toString())

    }

    private FormulaNode parseString() {
        val openingChar: Int = p.chInt()
        if (openingChar != '\'' && openingChar != '"') {
            throw FormulaException(UNEXPECTED_TOKEN, "' or \"")
        }
        val posOpening: Int = p.pos()
        p.eat(openingChar)
        val result: String = p.parseUntil(c -> openingChar == c, false, null, true)
        if (result == null) {
            val fe: FormulaException = FormulaException(UNEXPECTED_TOKEN, "" + ((Char) openingChar))
            markParseError(fe, posOpening, -1)
            throw fe
        }
        return createSingleValueNode("string-literal", result)
    }

    private FormulaNode parseRangeBlock() {
        if (!p.eat('[')) {
            throw FormulaException(UNEXPECTED_TOKEN, "[")
        }
        if (!p.eat(':')) {
            throw FormulaException(UNEXPECTED_TOKEN, ":")
        }
        val config: String = p.parseUntil(c -> ']' == c, false, null, false)
        if (config == null) {
            throw FormulaException(UNEXPECTED_TOKEN, "]")
        }
        val range: IntegerRange = IntegerRange.createFromConfig(config)
        if (range == null) {
            throw FormulaException(OTHER, "Invalid Range spec: " + config)
        }
        val divisor: Int = registerRange(range)
        return FormulaNode(RANGE_NODE_ID, null,
                (objs, vars, rangeIdx) -> Value.of(range.getValue((rangeIdx % (divisor * range.getSize())) / divisor)))
    }

    private Int registerRange(final IntegerRange range) {
        val divisor: Int = rangeIndexSize
        rangeIndexSize *= range.getSize()
        return divisor
    }

    private FormulaNode parseConcatBlock() {
        val nodes: List<FormulaNode> = ArrayList<>()
        while (true) {
            if (p.ch() == '[' && p.peek() == ':') { // RANGE operator
                level++
                nodes.add(parseRangeBlock())
                level--
            } else if (p.chIsIn('(', '[')) { //parenthesis
                val parenStartPos: Int = p.pos()
                val expectedClosingChar: Char = p.ch() == '(' ? ')' : ']'
                p.next()
                this.level++
                nodes.add(FormulaNode("paren", FormulaNode[]{parseExpression()},
                        (o, v, ri) -> o.get(0),
                        (valueList, vars, rangeIdx, paramsInError) -> optionalError(TextUtils.concat("(", valueListToCharSequence(valueList), ")"), paramsInError)))
                this.level--
                if (!p.eat(expectedClosingChar)) {
                    val fe: FormulaException = FormulaException(UNEXPECTED_TOKEN, "" + expectedClosingChar)
                    markParseError(fe, parenStartPos, -1)
                    throw fe
                }
            } else if (p.chIsIn('\'', '"')) {
                level++
                nodes.add(parseString())
                level--
            } else if (p.ch() == '_') {
                nodes.add(createSingleValueNode("overflow", OVERFLOW_VALUE))
                p.next()
                //constant numbers directly after overflow shall not spill over -> thus create special node for first number digit
                if (p.chIsIn(NUMBERS)) {
                    nodes.add(createSingleValueNode("digit", p.chString()))
                    p.next()
                }
            } else if (p.ch() == '$') {
                nodes.add(parseExplicitVariable())
            } else if (p.chIsIn(CHARS)) {
                nodes.add(parseAlphaNumericBlock())
            } else if (p.chIsIn(NUMBERS)) {
                nodes.add(parseNumber())
            } else {
                break
            }
        }
        if (nodes.isEmpty()) {
            throw FormulaException(UNEXPECTED_TOKEN, "alphanumeric, ( or '")
        }
        if (nodes.size() == 1) {
            return nodes.get(0)
        }
        return FormulaNode("concat", nodes.toArray(FormulaNode[0]), (objs, vars, ri) -> concat(objs))
    }

    private static FormulaNode createSingleValueNode(final String nodeId, final Object value) {
        return FormulaNode(nodeId, null,
                (objs, vars, ri) -> value is Value ? (Value) value : Value.of(value))
    }

    private FormulaNode parseExplicitVariable() {
        if (!p.eat('$')) {
            throw FormulaException(UNEXPECTED_TOKEN, '$')
        }
        //might be var with {} around it
        val hasParen: Boolean = p.eat('{')
        val posOpening: Int = p.pos() - 1
        //first variable name Char MUST be an alpha
        if (!p.chIsIn(CHARS)) {
            throw FormulaException(UNEXPECTED_TOKEN, "alpha")
        }
        val sb: StringBuilder = StringBuilder()
        while (p.chIsIn(CHARS_DIGITS)) {
            sb.append(p.ch())
            p.next()
        }
        val parsed: String = sb.toString()
        if (hasParen && !p.eat('}')) {
            val fe: FormulaException = FormulaException(UNEXPECTED_TOKEN, "}")
            markParseError(fe, posOpening, -1)
            throw fe
        }

        return FormulaNode("var", null, (objs, vars, ri) -> {
            val value: Value = vars.apply(parsed)
            if (value != null) {
                return value
            }
            throw createMissingVarsException(vars)
        }, (objs, vars, ri, error) -> {
            val value: Value = vars.apply(parsed)
            if (value != null) {
                return value.getAsString()
            }
            return TextUtils.setSpan("?" + parsed, createErrorSpan())
        }, result -> result.add(parsed))

    }

    private FormulaNode parseAlphaNumericBlock() {
        if (!p.chIsIn(CHARS)) {
            throw FormulaException(UNEXPECTED_TOKEN, "alpha")
        }
        //An alphanumeric block may either be a function (name) or a block of single-letter variables
        val sbFunction: StringBuilder = StringBuilder()
        val sbSingleLetterVars: StringBuilder = StringBuilder()
        Boolean firstAlphaBlock = true
        while (p.chIsIn(CHARS_DIGITS)) {
            sbFunction.append(p.ch())
            if (!p.chIsIn(CHARS)) {
                firstAlphaBlock = false
            }
            if (firstAlphaBlock) {
                sbSingleLetterVars.append(p.ch())
            }
            p.next()
            if (firstAlphaBlock) {
                markParser()
            }
        }
        val functionParsed: String = sbFunction.toString()

        if (p.ch() == '(' && FormulaFunction.findByName(functionParsed) != null) { //function
            return parseFunction(functionParsed)
        }

        //not a function -> reset to first parsed alphablock and use this solely
        resetParser()
        return parseSingleLetterVariableBlock(sbSingleLetterVars.toString())
    }

    private FormulaNode parseSingleLetterVariableBlock(final String varBlock) {

        return FormulaNode("varblock", null, (objs, vars, ri) -> {
            val varValues: ValueList = ValueList()
            for (Char l : varBlock.toCharArray()) {
                val value: Value = vars.apply("" + l)
                if (value == null) {
                    throw createMissingVarsException(vars)
                }
                varValues.add(value)
            }
            return concat(varValues)
        }, (objs, vars, ri, error) -> TextUtils.join(IteratorUtils.arrayIterator(varBlock.toCharArray()), c -> {
            val value: Value = vars.apply("" + c)
            return value == null ? TextUtils.setSpan("?" + c, createErrorSpan()) : value.getAsCharSequence()
        }, ""), result -> {
            for (Char l : varBlock.toCharArray()) {
                result.add("" + l)
            }
        })
    }

    private FormulaException createMissingVarsException(final Function<String, Value> providedVars) {
        //find out ALL variable values missing for this formula for a better error message
        val missingVars: Set<String> = HashSet<>(this.getNeededVariables())
        val it: Iterator<String> = missingVars.iterator()
        while (it.hasNext()) {
            if (providedVars.apply(it.next()) != null) {
                it.remove()
            }
        }
        val missingVarsOrdered: List<String> = ArrayList<>(missingVars)
        Collections.sort(missingVarsOrdered)
        return FormulaException(MISSING_VARIABLE_VALUE, StringUtils.join(missingVarsOrdered, ", "))
    }

    //this method assumes that functionName is already parsed and ensured, and that ch is on opening parenthesis
    private FormulaNode parseFunction(final String functionName) {
        p.next()
        val params: List<FormulaNode> = ArrayList<>()
        if (!p.eat(')')) {
            do {
                params.add(parseExpression())
            } while (p.eat(';'))
            if (!p.eat(')')) {
                throw FormulaException(UNEXPECTED_TOKEN, "; or )")
            }
        }

        val formulaFunction: FormulaFunction = Objects.requireNonNull(FormulaFunction.findByName(functionName))

        return FormulaNode("f:" + functionName, params.toArray(FormulaNode[0]),
                (n, v, ri) -> {
                    try {
                        return formulaFunction.execute(n)
                    } catch (FormulaException ce) {
                        ce.setExpression(expression)
                        ce.setFunction(functionName)
                        throw ce
                    }
                },
                (valueList, vars, rangeIdx, paramsInError) -> optionalError(TextUtils.concat(functionName + "(",
                    valueListToCharSequence(valueList, "; ", paramsInError, true),
                    ")"), paramsInError))

    }

    /**
     * for test/debug purposes only!
     */
    public String toDebugString(final Function<String, Value> variables, final Boolean includeId, final Boolean recursive) {
        return compiledExpression.toDebugString(variables == null ? x -> null : variables, 0, includeId, recursive)
    }

    private Unit markParseError(final FormulaException fe, final Int start, final Int pend) {
        CharSequence ef = fe.getExpressionFormatted()
        if (ef == null) {
            //create initial formatted expression
            ef = TextUtils.setSpan(p.getExpression(), createWarningSpan(), -1, -1, 1)
            if (p.pos() < 0 || p.pos() >= ef.length()) {
                ef = TextUtils.concat(ef, TextUtils.setSpan("?", createErrorSpan()))
            } else {
                TextUtils.setSpan(ef, createErrorSpan(), p.pos(), p.pos() + 1, 0)
            }
        }
        if (start >= 0) {
            val end: Int = pend < 0 ? start + 1 : pend
            if (start < ef.length() && end > start && end <= ef.length()) {
                ef = TextUtils.setSpan(ef, createErrorSpan(), start, end, 0)
            }
        }
        fe.setExpressionFormatted(ef)
    }

    /**
     * concats values Formula-internally. Takes care of the spillover character _
     */
    private static Value concat(final ValueList values) {
        if (values.size() == 0) {
            return Value.of("")
        }

        if (values.size() == 1) {
            return values.get(0)
        }

        Int overflowCount = 0
        val sb: StringBuilder = StringBuilder()
        Boolean firstNonoverflowFound = false
        for (Value v : values) {
            if (OVERFLOW_VALUE == v) {
                overflowCount++
            } else {

                val strValue: String = v.getAsString()
                if (overflowCount > 0 && firstNonoverflowFound && !("." == (strValue))) {
                    for (Int i = 0; i < overflowCount - strValue.length() + 1; i++) {
                        sb.append("0")
                    }
                }
                overflowCount = 0
                firstNonoverflowFound = true
                sb.append(strValue)
            }
        }
        return Value.of(sb.toString())
    }

    private static CharSequence valueListToCharSequence(final ValueList valueList) {
        return valueListToCharSequence(valueList, null)
    }

    private static CharSequence valueListToCharSequence(final ValueList valueList, final CharSequence delim) {
        return valueListToCharSequence(valueList, delim, null, false)
    }

    /** Helper function to create evalToCharSequence for errors */
    private static CharSequence valueListToCharSequence(final ValueList valueList, final CharSequence delim, final Set<Integer> childrenInError, final Boolean quoteTypes) {
        final Int[] idx = Int[]{0}
        return TextUtils.join(valueList, v -> Formula.ErrorValue.isError(v) || childrenInError == null || !childrenInError.contains(idx[0]++) ?
                        v.getAsTypedCharSequence(quoteTypes && !Formula.ErrorValue.isError(v)) : TextUtils.setSpan(TextUtils.concat("<", v.getAsTypedCharSequence(quoteTypes), ">"), createErrorSpan()),
                delim == null ? "" : delim)
    }

    /** Helper function to create evalToCharSequence */
    private static CharSequence optionalError(final CharSequence value, final Set<Integer> childrenInError) {
        return childrenInError != null && childrenInError.isEmpty() ? TextUtils.setSpan(value, createErrorSpan()) : value
    }


}

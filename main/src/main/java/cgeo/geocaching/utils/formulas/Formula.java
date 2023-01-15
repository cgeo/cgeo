package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.KeyableCharSet;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.TextParser;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;
import cgeo.geocaching.utils.functions.Func3;
import cgeo.geocaching.utils.functions.Func4;
import static cgeo.geocaching.utils.TextParser.isFormulaWhitespace;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.EMPTY_FORMULA;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.MISSING_VARIABLE_VALUE;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.OTHER;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.UNEXPECTED_TOKEN;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a Formula which is parsed from a String variable.
 *
 * The originally rather simple evaluation algorithm was derived from the work of user 'Boann' and released to the public domain on Stack Overflow:
 * https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
 *
 * Since then, various additions made the evaluation algorithm a bit more complex. The following features were added:
 *
 * * Operators '%' and also '!'.
 * * Ability to evaluate functions, e.g.  'sqrt', 'cos', 'sin' and 'tan' .
 * * Support for variables (insertion of values)
 * * Class now also supports precompilation of expressions and reuse of this compile generate for other variable values
 * * LRUCache added to store and reuse previously compiled expressions
 * * Ability to use string type was added (in addition to numeric)
 * * concated expressions were added. For example '3(5+1)4' is now interpreted as '364'. Likewise, if A=1, then 'AA5(A+1)3' will  be parsed to '11523'
 * * Localizable, user-displayable error message handling was added.
 */
public final class Formula {

    public static final String VALID_OPERATOR_PATTERN = "+\\-*/%^*:!";

    private static final Value OVERFLOW_VALUE = Value.of("");

    private static final Set<Integer> CHARS = new HashSet<>();
    private static final Set<Integer> CHARS_DIGITS = new HashSet<>();
    private static final Set<Integer> NUMBERS = new HashSet<>();

    private static final String RANGE_NODE_ID = "range-node";

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<Formula, FormulaException>> FORMULA_CACHE = new LeastRecentlyUsedMap.LruCache<>(500);

    //effectively final (assigned final after compilation)
    private String expression;
    private FormulaNode compiledExpression;

    private TextParser p;
    private int level;
    private int markedLevel;

    private int rangeIndexSize = 1;

    public static Object createErrorSpan() {
        return new ForegroundColorSpan(Color.RED);
    }

    public static Object createWarningSpan() {
        return new ForegroundColorSpan(Color.GRAY);
    }

    private static class ErrorValue extends Value {

        protected ErrorValue(final CharSequence errorString) {
            super(errorString);
        }

        public CharSequence getErrorString() {
            return (CharSequence) getRaw();
        }

        public static ErrorValue of(final CharSequence cs) {
            return new ErrorValue(cs);
        }

        public static boolean isError(final Value v) {
            return v instanceof ErrorValue;
        }

        public static CharSequence getErrorString(final Value v) {
            return v.getRaw() instanceof CharSequence ? (CharSequence) v.getRaw() : v.getAsString();
        }
    }

    private static class FormulaNode {

        private static final FormulaNode[] FORMULA_NODE_EMPTY_ARRAY = new FormulaNode[0];

        private final String id;
        private Func3<ValueList, Func1<String, Value>, Integer, Value> function;
        private Func4<ValueList, Func1<String, Value>, Integer, Boolean, CharSequence> functionToErrorString;
        private FormulaNode[] children;

        public final Set<String> neededVars;

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func3<ValueList, Func1<String, Value>, Integer, Value> function,
                    final Func4<ValueList, Func1<String, Value>, Integer, Boolean, CharSequence> functionToErrorString) {
            this(id, children, function, functionToErrorString, null);
        }

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func3<ValueList, Func1<String, Value>, Integer, Value> function,
                    final Func4<ValueList, Func1<String, Value>, Integer, Boolean, CharSequence> functionToErrorString,
                    final Action1<Set<String>> explicitelyNeeded) {

            this.id = id;
            this.children = children == null ? FORMULA_NODE_EMPTY_ARRAY : children;
            this.function = function;
            this.functionToErrorString = functionToErrorString;
            this.neededVars = Collections.unmodifiableSet(calculateNeededVars(explicitelyNeeded, children));

            if (this.neededVars.isEmpty() && !hasRanges(this)) {
                //this means that function is constant!
                this.function = createConstantFunction();
                final CharSequence csResult = evalToCharSequenceInternal(y -> null, -1).getAsCharSequence();
                this.functionToErrorString = (objs, vars, rangeIdx, b) -> csResult;
                this.children = FORMULA_NODE_EMPTY_ARRAY;
            }
        }

        public String getId() {
            return this.id;
        }

        public Set<String> getNeededVars() {
            return neededVars;
        }

        private static Set<String> calculateNeededVars(final Action1<Set<String>> explicitlyNeeded, final FormulaNode[] children) {
            final Set<String> neededVars = new HashSet<>();
            if (explicitlyNeeded != null) {
                explicitlyNeeded.call(neededVars);
            }

            if (children != null) {
                for (FormulaNode child : children) {
                    neededVars.addAll(child.neededVars);
                }
            }
            return neededVars;
        }

        private static boolean hasRanges(final FormulaNode node) {
            if (RANGE_NODE_ID.equals(node.id)) {
                return true;
            }
            for (FormulaNode c : node.children) {
                if (hasRanges(c)) {
                    return true;
                }
            }
            return false;
        }

        private Func3<ValueList, Func1<String, Value>, Integer, Value> createConstantFunction() {
            Value result = null;
            FormulaException resultException = null;
            try {
                result = eval(y -> null, -1);
            } catch (FormulaException fe) {
                resultException = fe;
            }
            final Value finalResult = result;
            final FormulaException finalResultException = resultException;
            return (objs, vars, idx) -> {
                if (finalResultException != null) {
                    throw finalResultException;
                }
                return finalResult;
            };
        }

        private Value eval(final Func1<String, Value> variables, final int rangeIdx) throws FormulaException {
            return evalInternal(variables == null ? x -> null : variables, rangeIdx);
        }

        private Value evalInternal(final Func1<String, Value> variables, final int rangeIdx) throws FormulaException {
            final ValueList childValues = new ValueList();
            for (FormulaNode child : children) {
                childValues.add(child.eval(variables, rangeIdx));
            }
            return this.function.call(childValues, variables, rangeIdx);
        }

        private CharSequence evalToCharSequence(final Func1<String, Value> vars, final int rangeIdx) {
            final Object result = evalToCharSequenceInternal(vars == null ? x -> null : vars, rangeIdx);
            if (result instanceof ErrorValue) {
                return TextUtils.setSpan(((ErrorValue) result).getAsCharSequence(), createWarningSpan(), -1, -1, 5);
            }
            return result.toString();
        }

        private Value evalToCharSequenceInternal(final Func1<String, Value> variables, final int rangeIdx) {
            final ValueList childValues = new ValueList();
            boolean hasError = false;
            for (FormulaNode child : children) {
                final Value v = child.evalToCharSequenceInternal(variables, rangeIdx);
                if (v instanceof ErrorValue) {
                    hasError = true;
                }
                childValues.add(v);
            }
            boolean isDirectError = false;
            if (!hasError) {
                try {
                    return this.function.call(childValues, variables, rangeIdx);
                } catch (FormulaException fe) {
                    //error is right here...
                    isDirectError = true;
                }
            }

            //we had an error in the chain -> produce a String using the error function
            CharSequence cs = null;
            if (this.functionToErrorString != null) {
                cs = this.functionToErrorString.call(childValues, variables, rangeIdx, isDirectError);
            }
            if (cs == null) {
                //default error string function
                if (childValues.size() > 0) {
                    cs = TextUtils.join(childValues, Value::getAsCharSequence, "");
                } else {
                    cs = "?";
                }
                if (isDirectError) {
                    cs = TextUtils.setSpan(cs, createErrorSpan());
                }
            }

            return ErrorValue.of(cs);

        }

        public static ValueList toValueList(final Collection<FormulaNode> nodes, final Func1<String, Value> variables, final int rangeIdx) {
            final ValueList childValues = new ValueList();
            for (FormulaNode child : nodes) {
                childValues.add(child.eval(variables, rangeIdx));
            }
            return childValues;
        }

        /**
         * for test/debug purposes only!
         */
        public String toDebugString(final Func1<String, Value> variables, final int rangeIdx, final boolean includeId, final boolean recursive) {
            final StringBuilder sb = new StringBuilder();
            if (includeId) {
                sb.append("[").append(getId()).append("]");
            }
            sb.append(evalToCharSequence(variables, rangeIdx));
            if (recursive) {
                sb.append("{");
                for (FormulaNode child : children) {
                    sb.append(child.toDebugString(variables, rangeIdx, includeId, recursive));
                }
                sb.append("}");
            }
            return sb.toString();
        }
    }

    private FormulaNode createNumeric(final String id, final FormulaNode[] children, final Func2<ValueList, Func1<String, Value>, Value> function) {
        return new FormulaNode(id, children, (objs, vars, rangeIdx) -> {
            objs.checkAllDouble();
            return function.call(objs, vars);
        }, (objs, vars, rangeIdx, error) ->
                TextUtils.join(objs, v -> v.isDouble() || ErrorValue.isError(v) ? v.getAsCharSequence() : TextUtils.setSpan(
                        TextUtils.concat("'", v.getAsCharSequence(), "'"), createErrorSpan()), " " + id + " "));
    }

    static {
        for (int i = 'a'; i <= 'z'; i++) {
            CHARS.add(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            CHARS.add(i);
        }
        for (int i = '0'; i <= '9'; i++) {
            NUMBERS.add(i);
        }
        CHARS_DIGITS.addAll(CHARS);
        CHARS_DIGITS.addAll(NUMBERS);

        addCharsToSet(NUMBERS, '.', ',');

    }

    private static void addCharsToSet(final Set<Integer> set, final char... charsToAdd) {
        for (char c : charsToAdd) {
            set.add((int) c);
        }
    }

    public static Formula compile(final String expression) throws FormulaException {
        return compile(expression, 0, null);
    }

    public static Formula compile(final String expression, final int startPos, final KeyableCharSet stopChars) throws FormulaException {
        final KeyableCharSet stopCharSet = stopChars == null ? KeyableCharSet.EMPTY : stopChars;
        final String cacheKey = expression + "-" + startPos + "-" + stopCharSet.getKey();
        final Pair<Formula, FormulaException> entry = FORMULA_CACHE.get(cacheKey);
        if (entry != null) {
            //found an entry, return it (either exception or compiled calculator
            if (entry.first != null) {
                return entry.first;
            }
            throw entry.second;
        }

        //no entry found. Compile expression, put it into cache and return it
        try {
            final Formula c = compileInternal(expression, startPos, stopCharSet::contains);
            FORMULA_CACHE.put(cacheKey, new Pair<>(c, null));
            return c;
        } catch (FormulaException ce) {
            FORMULA_CACHE.put(cacheKey, new Pair<>(null, ce));
            throw ce;
        }
    }

    private static Formula compileInternal(final String expression, final int startPos, final Func1<Character, Boolean> stopChecker) {
        final Formula f = new Formula();
        try {
            f.doCompile(expression, startPos, stopChecker);
            return f;
        } catch (FormulaException ce) {
            f.markParseError(ce, -1, -1);
            ce.setExpression(expression);
            ce.setParsingContext(f.p.ch(), f.p.pos());
            throw ce;
        }
    }

    public static Value evaluateWithRanges(final String expression, final int rangeIdx, final Object... vars) {
        return compile(expression).evaluate(toVarProvider(vars), rangeIdx);
    }

    public static Value evaluate(final String expression, final Object... vars) {
        return compile(expression).evaluate(vars);
    }


    public static double eval(final String expression, final Object... vars) {
        return evaluate(expression, vars).getAsDouble();
    }

    private Formula() {
        //do nothing
    }

    public int getRangeIndexSize() {
        return this.rangeIndexSize;
    }

    public String getExpression() {
        return expression;
    }

    public Value evaluate(final Object... vars) {
        return evaluate(toVarProvider(vars));
    }

    public static Func1<String, Value> toVarProvider(final Object... vars) {
        if (vars == null || vars.length == 0) {
            return x -> null;
        }
        final Map<String, Value> varMap = new HashMap<>();
        for (int i = 0; i < vars.length - 1; i += 2) {
            varMap.put(vars[i].toString(), Value.of(vars[i + 1]));
        }
        return varMap::get;
    }

    public Value evaluate(final Func1<String, Value> vars) throws FormulaException {
        return evaluate(vars, 0);
    }

    public Value evaluate(final Func1<String, Value> vars, final int rangeIdx) throws FormulaException {
        try {
            return compiledExpression.eval(vars == null ? x -> null : vars, rangeIdx);
        } catch (FormulaException ce) {
            ce.setExpression(expression);
            ce.setEvaluationContext(calculateEvaluationContext(vars));
            ce.setExpressionFormatted(this.evaluateToCharSequence(vars == null ? x -> null : vars));
            throw ce;
        }
    }

    public String evaluateToString(final Func1<String, Value> vars) {
        return evaluateToCharSequence(vars).toString();
    }

    public CharSequence evaluateToCharSequence(final Func1<String, Value> vars) {
        return evaluateToCharSequence(vars, 0);
    }

    public CharSequence evaluateToCharSequence(final Func1<String, Value> vars, final int rangeIdx) {
        return compiledExpression.evalToCharSequence(vars == null ? x -> null : vars, rangeIdx);
    }

    private String calculateEvaluationContext(final Func1<String, Value> vars) {
        try {
            final List<String> list = new ArrayList<>();
            for (String v : compiledExpression.getNeededVars()) {
                list.add(v + "=" + vars.call(v));
            }
            return StringUtils.join(list, ",");
        } catch (Exception e) {
            return "Exc: " + e;
        }
    }

    public Set<String> getNeededVariables() {
        return compiledExpression.getNeededVars();
    }

    private void doCompile(final String rawExpression, final int startPos, final Func1<Character, Boolean> stopChecker) throws FormulaException {
        this.p = new TextParser(rawExpression, stopChecker == null ? null :
                (c) -> level == 0 && stopChecker.call(c));

        this.p.setPos(startPos);
        this.level = 0;
        if (this.p.eof()) {
            throw new FormulaException(EMPTY_FORMULA);
        }
        final FormulaNode x = parseExpression();
        // line end or beginning of user comment
        if (!p.eof()) {
            throw new FormulaException(UNEXPECTED_TOKEN, "EOF");
        }
        this.expression = rawExpression.substring(startPos, p.pos());
        this.compiledExpression = x;
    }

    private void markParser() {
        p.mark();
        markedLevel = level;
    }

    private void resetParser() {
        p.reset();
        level = markedLevel;
    }

    private FormulaNode parseExpression() {
        return parseWhitespaceConcattenatedExpressions(this::parseRelationalEquality);
    }

    private FormulaNode parseWhitespaceConcattenatedExpressions(final Supplier<FormulaNode> parseNext) {
        final FormulaNode singleResult = parseNext.get();
        List<FormulaNode> multiResult = null;

        //check if there are blocks to concat separated by whitespace
        while (!p.eof() && (Character.isWhitespace(p.ch()) || Character.isWhitespace((char) p.previous()))) {
            p.skipWhitespaces();
            //certain characters should STOP the looking for whitespace-separated concat blocks
            if (p.chIsIn(')', ']')) {
                break;
            }
            if (multiResult == null) {
                multiResult = new ArrayList<>();
                multiResult.add(singleResult);
            }
            multiResult.add(parseNext.get());
        }
        if (multiResult == null) {
            return singleResult;
        }
        return new FormulaNode("concat-exp", multiResult.toArray(new FormulaNode[0]), (objs, vars, ri) -> concat(objs), null);

    }

    private FormulaNode parseRelationalEquality() {
        final FormulaNode x = parsePlusMinus();
        if (p.chIsIn('<', '>', '=')) {
            //handle <, >, <=, >=, ==, <>
            final int firstChar = p.chInt();
            p.next();
            final boolean nextIsEqual = p.eat('=');
            final boolean nextIsGreaterThan = p.eat('>');
            if (firstChar == '=' && !nextIsEqual) {
                throw new FormulaException(UNEXPECTED_TOKEN, "=");
            }
            if (nextIsGreaterThan && (nextIsEqual || firstChar != '<')) {
                throw new FormulaException(UNEXPECTED_TOKEN, "not >");
            }

            final FormulaNode y = parsePlusMinus();

            switch (firstChar) {
                case '<':
                    if (nextIsEqual) {
                        return createNumeric("<=", new FormulaNode[]{x, y}, createCompareFunction((o1, o2) -> o1.compareTo(o2) <= 0));
                    } else if (nextIsGreaterThan) {
                        return createNumeric("<>", new FormulaNode[]{x, y}, createCompareFunction((o1, o2) -> o1.compareTo(o2) != 0));
                    } else {
                        return createNumeric("<", new FormulaNode[]{x, y}, createCompareFunction((o1, o2) -> o1.compareTo(o2) < 0));
                    }
                case '>':
                    if (!nextIsEqual) {
                        return createNumeric(">", new FormulaNode[]{x, y}, createCompareFunction((o1, o2) -> o1.compareTo(o2) > 0));
                    } else {
                        return createNumeric(">=", new FormulaNode[]{x, y}, createCompareFunction((o1, o2) -> o1.compareTo(o2) >= 0));
                    }
                case '=':
                default:
                    return createNumeric("==", new FormulaNode[]{x, y}, createCompareFunction((o1, o2) -> o1.compareTo(o2) == 0));
            }
        }
        return x;
    }

    private <T> Func2<ValueList, Func1<String, Value>, Value> createCompareFunction(final Func2<Comparable<T>, Comparable<T>, Boolean> compare) {
        return (nums, vars) -> {
            final boolean useString = !nums.get(0).isDouble() || !nums.get(1).isDouble();
            final Comparable<T> o1 = (Comparable<T>) (useString ? nums.getAsString(0, "") : nums.getAsDouble(0));
            final Comparable<T> o2 = (Comparable<T>) (useString ? nums.getAsString(1, "") : nums.getAsDouble(1));
            return Value.of(compare.call(o1, o2) ? 1 : 0);
        };

    }


    private FormulaNode parsePlusMinus() {

        FormulaNode x = parseMultiplyDivision();
        for (; ; ) {
            if (p.eat('+')) {
                x = createNumeric("+", new FormulaNode[]{x, parseMultiplyDivision()}, (nums, vars) -> Value.of(nums.getAsDouble(0) + nums.getAsDouble(1)));
            } else if (p.eat('-') || p.eat('—')) { //those are two different chars
                x = createNumeric("-", new FormulaNode[]{x, parseMultiplyDivision()}, (nums, vars) -> Value.of(nums.getAsDouble(0) - nums.getAsDouble(1)));
            } else {
                return x;
            }
        }
    }

    private FormulaNode parseMultiplyDivision() {
        FormulaNode x = parseFactor();
        for (; ; ) {
            if (p.eat('*') || p.eat('•') || eatXMultiplier()) {
                x = createNumeric("*", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) * nums.getAsDouble(1)));
            } else if (p.eat('/') || p.eat(':') || p.eat('÷')) {
                x = createNumeric("/", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) / nums.getAsDouble(1)));
            } else if (p.eat('%')) {
                x = createNumeric("%", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) % nums.getAsDouble(1)));
            } else {
                return x;
            }
        }
    }

    private boolean eatXMultiplier() {
        p.skipWhitespaces();
        //'x' (lowercase) is considered a multiplier symbol (and not a variable) if it is surrounded by whitespace
        if (p.ch() == 'x' && isFormulaWhitespace(p.peek()) && isFormulaWhitespace(p.previous())) {
            p.next();
            return true;
        }
        return false;
    }

    private FormulaNode parseFactor() {
        if (p.eat('+')) {
            return parseFactor(); // unary plus
        }
        if (p.eat('-') || p.eat('—')) { // those are two different chars!
            return createNumeric("-", new FormulaNode[]{parseFactor()}, (nums, vars) -> Value.of(-nums.getAsDouble(0)));
        }

        FormulaNode x = parseConcatBlock();

        if (p.eat('!')) {
            x = createNumeric("!", new FormulaNode[]{x}, (nums, vars) -> {
                final int facValue = (int) nums.getAsInt(0, 0);
                if (!nums.get(0).isInteger() || facValue < 0) {
                    throw new FormulaException(WRONG_TYPE, "positive Integer", nums.get(0), nums.get(0).getType());
                }
                int result = 1;
                for (int i = 2; i <= facValue; i++) {
                    result *= i;
                }
                return Value.of(result);
            });
        }

        p.skipWhitespaces();
        if (p.chIsIn('²', '³')) {
            final int factor = p.ch() == '³' ? 3 : 2;
            p.next();
            x = createNumeric("^" + factor, new FormulaNode[]{x}, (nums, vars) -> {
                if (!nums.get(0).isDouble()) {
                    throw new FormulaException(WRONG_TYPE, "numeric", nums.get(0), nums.get(0).getType());
                }
                return Value.of(Math.pow(nums.get(0).getAsDouble(), factor));
            });
        }

        if (p.eat('^')) {
            x = createNumeric("^", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(Math.pow(nums.getAsDouble(0), nums.getAsDouble(1))));
        }
        if (p.eat('#')) {
            p.parseUntil(c -> '#' == c, false, null, true); // drop potential user comments
        }

        return x;
    }

    private FormulaNode parseNumber() {
        final StringBuilder sb = new StringBuilder();
        while (p.chIsIn(NUMBERS)) {
            sb.append(p.ch());
            p.next();
        }
        return createSingleValueNode("number", sb.toString());

    }

    private FormulaNode parseString() {
        final int openingChar = p.chInt();
        if (openingChar != '\'' && openingChar != '"') {
            throw new FormulaException(UNEXPECTED_TOKEN, "' or \"");
        }
        final int posOpening = p.pos();
        p.eat(openingChar);
        final String result = p.parseUntil(c -> openingChar == c, false, null, true);
        if (result == null) {
            final FormulaException fe = new FormulaException(UNEXPECTED_TOKEN, "" + ((char) openingChar));
            markParseError(fe, posOpening, -1);
            throw fe;
        }
        return createSingleValueNode("string-literal", result);
    }

    private FormulaNode parseRangeBlock() {
        if (!p.eat('[')) {
            throw new FormulaException(UNEXPECTED_TOKEN, "[");
        }
        if (!p.eat(':')) {
            throw new FormulaException(UNEXPECTED_TOKEN, ":");
        }
        final String config = p.parseUntil(c -> ']' == c, false, null, false);
        if (config == null) {
            throw new FormulaException(UNEXPECTED_TOKEN, "]");
        }
        final IntegerRange range = IntegerRange.createFromConfig(config);
        if (range == null) {
            throw new FormulaException(OTHER, "Invalid Range spec: " + config);
        }
        final int divisor = registerRange(range);
        return new FormulaNode(RANGE_NODE_ID, null,
                (objs, vars, rangeIdx) -> Value.of(range.getValue((rangeIdx % (divisor * range.getSize())) / divisor)), null);
    }

    private int registerRange(final IntegerRange range) {
        final int divisor = rangeIndexSize;
        rangeIndexSize *= range.getSize();
        return divisor;
    }

    private FormulaNode parseConcatBlock() {
        final List<FormulaNode> nodes = new ArrayList<>();
        while (true) {
            if (p.ch() == '[' && p.peek() == ':') { // RANGE operator
                level++;
                nodes.add(parseRangeBlock());
                level--;
            } else if (p.chIsIn('(', '[')) { //parenthesis
                final int parenStartPos = p.pos();
                final char expectedClosingChar = p.ch() == '(' ? ')' : ']';
                p.next();
                this.level++;
                nodes.add(new FormulaNode("paren", new FormulaNode[]{parseExpression()}, (o, v, ri) -> o.get(0),
                        (o, v, ri, error) -> TextUtils.concat("(", o.get(0).getAsCharSequence(), ")")));
                this.level--;
                if (!p.eat(expectedClosingChar)) {
                    final FormulaException fe = new FormulaException(UNEXPECTED_TOKEN, "" + expectedClosingChar);
                    markParseError(fe, parenStartPos, -1);
                    throw fe;
                }
            } else if (p.chIsIn('\'', '"')) {
                level++;
                nodes.add(parseString());
                level--;
            } else if (p.ch() == '_') {
                nodes.add(createSingleValueNode("overflow", OVERFLOW_VALUE));
                p.next();
                //constant numbers directly after overflow shall not spill over -> thus create special node for first number digit
                if (p.chIsIn(NUMBERS)) {
                    nodes.add(createSingleValueNode("digit", p.chString()));
                    p.next();
                }
            } else if (p.ch() == '$') {
                nodes.add(parseExplicitVariable());
            } else if (p.chIsIn(CHARS)) {
                nodes.add(parseAlphaNumericBlock());
            } else if (p.chIsIn(NUMBERS)) {
                nodes.add(parseNumber());
            } else {
                break;
            }
        }
        if (nodes.isEmpty()) {
            throw new FormulaException(UNEXPECTED_TOKEN, "alphanumeric, ( or '");
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new FormulaNode("concat", nodes.toArray(new FormulaNode[0]), (objs, vars, ri) -> concat(objs), null);
    }

    private static FormulaNode createSingleValueNode(final String nodeId, final Object value) {
        return new FormulaNode(nodeId, null,
                (objs, vars, ri) -> value instanceof Value ? (Value) value : Value.of(value), null);
    }

    private FormulaNode parseExplicitVariable() {
        if (!p.eat('$')) {
            throw new FormulaException(UNEXPECTED_TOKEN, '$');
        }
        //might be var with {} around it
        final boolean hasParen = p.eat('{');
        final int posOpening = p.pos() - 1;
        //first variable name char MUST be an alpha
        if (!p.chIsIn(CHARS)) {
            throw new FormulaException(UNEXPECTED_TOKEN, "alpha");
        }
        final StringBuilder sb = new StringBuilder();
        while (p.chIsIn(CHARS_DIGITS)) {
            sb.append(p.ch());
            p.next();
        }
        final String parsed = sb.toString();
        if (hasParen && !p.eat('}')) {
            final FormulaException fe = new FormulaException(UNEXPECTED_TOKEN, "}");
            markParseError(fe, posOpening, -1);
            throw fe;
        }

        return new FormulaNode("var", null, (objs, vars, ri) -> {
            final Value value = vars.call(parsed);
            if (value != null) {
                return value;
            }
            throw createMissingVarsException(vars);
        }, (objs, vars, ri, error) -> {
            final Value value = vars.call(parsed);
            if (value != null) {
                return value.getAsString();
            }
            return TextUtils.setSpan("?" + parsed, createErrorSpan());
        }, result -> result.add(parsed));

    }

    private FormulaNode parseAlphaNumericBlock() {
        if (!p.chIsIn(CHARS)) {
            throw new FormulaException(UNEXPECTED_TOKEN, "alpha");
        }
        //An alphanumeric block may either be a function (name) or a block of single-letter variables
        final StringBuilder sbFunction = new StringBuilder();
        final StringBuilder sbSingleLetterVars = new StringBuilder();
        boolean firstAlphaBlock = true;
        while (p.chIsIn(CHARS_DIGITS)) {
            sbFunction.append(p.ch());
            if (!p.chIsIn(CHARS)) {
                firstAlphaBlock = false;
            }
            if (firstAlphaBlock) {
                sbSingleLetterVars.append(p.ch());
            }
            p.next();
            if (firstAlphaBlock) {
                markParser();
            }
        }
        final String functionParsed = sbFunction.toString();

        if (p.ch() == '(' && FormulaFunction.findByName(functionParsed) != null) { //function
            return parseFunction(functionParsed);
        }

        //not a function -> reset to first parsed alphablock and use this solely
        resetParser();
        return parseSingleLetterVariableBlock(sbSingleLetterVars.toString());
    }

    @NonNull
    private FormulaNode parseSingleLetterVariableBlock(final String varBlock) {

        return new FormulaNode("varblock", null, (objs, vars, ri) -> {
            final ValueList varValues = new ValueList();
            for (char l : varBlock.toCharArray()) {
                final Value value = vars.call("" + l);
                if (value == null) {
                    throw createMissingVarsException(vars);
                }
                varValues.add(value);
            }
            return concat(varValues);
        }, (objs, vars, ri, error) -> TextUtils.join(IteratorUtils.arrayIterator(varBlock.toCharArray()), c -> {
            final Value value = vars.call("" + c);
            return value == null ? TextUtils.setSpan("?" + c, createErrorSpan()) : value.getAsCharSequence();
        }, ""), result -> {
            for (char l : varBlock.toCharArray()) {
                result.add("" + l);
            }
        });
    }

    private FormulaException createMissingVarsException(final Func1<String, Value> providedVars) {
        //find out ALL variable values missing for this formula for a better error message
        final Set<String> missingVars = new HashSet<>(this.getNeededVariables());
        final Iterator<String> it = missingVars.iterator();
        while (it.hasNext()) {
            if (providedVars.call(it.next()) != null) {
                it.remove();
            }
        }
        final List<String> missingVarsOrdered = new ArrayList<>(missingVars);
        Collections.sort(missingVarsOrdered);
        return new FormulaException(MISSING_VARIABLE_VALUE, StringUtils.join(missingVarsOrdered, ", "));
    }

    //this method assumes that functionName is already parsed and ensured, and that ch is on opening parenthesis
    @NonNull
    private FormulaNode parseFunction(final String functionName) {
        p.next();
        final List<FormulaNode> params = new ArrayList<>();
        if (!p.eat(')')) {
            do {
                params.add(parseExpression());
            } while (p.eat(';'));
            if (!p.eat(')')) {
                throw new FormulaException(UNEXPECTED_TOKEN, "; or )");
            }
        }

        return new FormulaNode("f:" + functionName, params.toArray(new FormulaNode[0]),
                (n, v, ri) -> {
                    try {
                        return Objects.requireNonNull(FormulaFunction.findByName(functionName)).execute(n);
                    } catch (FormulaException ce) {
                        ce.setExpression(expression);
                        ce.setFunction(functionName);
                        throw ce;
                    } catch (RuntimeException re) {
                        final FormulaException ce = new FormulaException(re, OTHER, re.getMessage());
                        ce.setExpression(expression);
                        ce.setFunction(functionName);
                        throw ce;

                    }
                },
                (n, v, ri, error) -> functionName + "(" + StringUtils.join(n, ","));

    }

    /**
     * for test/debug purposes only!
     */
    public String toDebugString(final Func1<String, Value> variables, final boolean includeId, final boolean recursive) {
        return compiledExpression.toDebugString(variables == null ? x -> null : variables, 0, includeId, recursive);
    }

    private void markParseError(final FormulaException fe, final int start, final int pend) {
        CharSequence ef = fe.getExpressionFormatted();
        if (ef == null) {
            //create initial formatted expression
            ef = TextUtils.setSpan(p.getExpression(), createWarningSpan(), -1, -1, 1);
            if (p.pos() < 0 || p.pos() >= ef.length()) {
                ef = TextUtils.concat(ef, TextUtils.setSpan("?", createErrorSpan()));
            } else {
                TextUtils.setSpan(ef, createErrorSpan(), p.pos(), p.pos() + 1, 0);
            }
        }
        if (start >= 0) {
            final int end = pend < 0 ? start + 1 : pend;
            if (start < ef.length() && end > start && end <= ef.length()) {
                ef = TextUtils.setSpan(ef, createErrorSpan(), start, end, 0);
            }
        }
        fe.setExpressionFormatted(ef);
    }

    /**
     * concats values Formula-internally. Takes care of the spillover character _
     */
    private static Value concat(final ValueList values) {
        if (values.size() == 0) {
            return Value.of("");
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        int overflowCount = 0;
        final StringBuilder sb = new StringBuilder();
        boolean firstNonoverflowFound = false;
        for (Value v : values) {
            if (OVERFLOW_VALUE == v) {
                overflowCount++;
            } else {

                final String strValue = v.getAsString();
                if (overflowCount > 0 && firstNonoverflowFound && !(".".equals(strValue))) {
                    for (int i = 0; i < overflowCount - strValue.length() + 1; i++) {
                        sb.append("0");
                    }
                }
                overflowCount = 0;
                firstNonoverflowFound = true;
                sb.append(strValue);
            }
        }
        return Value.of(sb.toString());
    }


}

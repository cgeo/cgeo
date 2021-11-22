package cgeo.geocaching.utils.calc;

import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;
import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.EMPTY_FORMULA;
import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.MISSING_VARIABLE_VALUE;
import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.OTHER;
import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.UNEXPECTED_TOKEN;
import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.WRONG_TYPE;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Calculator util class to help parse and evaluate calculation formulas.
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
 *
 */
public final class Calculator {

    public static final String VALID_OPERATOR_PATTERN = "+\\-*/%^*:!";

    private static final Set<Integer> CHARS = new HashSet<>();
    private static final Set<Integer> CHARS_DIGITS = new HashSet<>();
    private static final Set<Integer> NUMBERS = new HashSet<>();

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<Calculator, CalculatorException>> CALCULATION_CACHE = new LeastRecentlyUsedMap.LruCache<>(500);

    private final String expression;

    //needed only during parsing
    private int pos = -1;
    private int ch;
    private int markedPos = -1;
    private int markedCh = ch;

    //stores compiled expression
    private CalcNode compiledExpression;


    private static class CalcNode {

        public final String id;
        public final Func2<ValueList, Func1<String, Value>, Value> function;
        public final CalcNode[] children;

        public final Action1<Set<String>> neededVars;

        CalcNode(final String id, final CalcNode[] children, final Func2<ValueList, Func1<String, Value>, Value> function) {
            this(id, children, function, null);
        }

        CalcNode(final String id, final CalcNode[] children, final Func2<ValueList, Func1<String, Value>, Value> function, final Action1<Set<String>> neededVars) {

            this.id = id;
            this.children = children == null ? new CalcNode[0] : children;
            this.function = function;
            this.neededVars = neededVars;
        }

        public Value eval(final Func1<String, Value> variables) {
            final ValueList childValues = new ValueList();
            for (CalcNode child : children) {
                childValues.add(child.eval(variables));
            }
            return this.function.call(childValues, variables);
        }

        //calculates the variable names needed by this function
        public void calculateNeededVariables(final Set<String> result) {
            if (this.neededVars != null) {
                this.neededVars.call(result);
            }
            for (CalcNode child : children) {
                child.calculateNeededVariables(result);
            }
        }
    }

    private CalcNode createNumeric(final String id, final CalcNode[] children, final Func2<ValueList, Func1<String, Value>, Value> function) {
        return new CalcNode(id, children, (objs, vars) -> {
            objs.checkAllDouble();
            return function.call(objs, vars);
        });
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
        NUMBERS.add((int) '.');
    }

    public static Calculator compile(final String expression) {
        final Pair<Calculator, CalculatorException> entry = CALCULATION_CACHE.get(expression);
        if (entry != null) {
            //found an entry, return it (either exception or compiled calculator
            if (entry.first != null) {
                return entry.first;
            }
            throw entry.second;
        }

        //no entry found. Compile expression, put it into cache and return it
        try {
            final Calculator c = compileInternal(expression);
            CALCULATION_CACHE.put(expression, new Pair<>(c, null));
            return c;
        } catch (CalculatorException ce) {
            CALCULATION_CACHE.put(expression, new Pair<>(null, ce));
            throw ce;
        }
    }

    private static Calculator compileInternal(final String expression) {
        final Calculator calc = new Calculator(expression);
        try {
            calc.compile();
            return calc;
        } catch (CalculatorException ce) {
            ce.setExpression(expression);
            ce.setParsingContext(calc.ch, calc.pos);
            throw ce;
        }
    }

    public static Value evaluate(final String expression, final Object ... vars) {
        return compile(expression).evaluate(vars);
    }


    public static double eval(final String expression, final Object ... vars) {
        return evaluate(expression, vars).getAsDouble();
    }

    private Calculator(final String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    protected char currChar() {
        return (char) ch;
    }

    protected int currPos() {
        return pos;
    }

    public Value evaluate(final Object ... vars) {
        if (vars == null || vars.length == 0) {
            return evaluate(x -> null);
        }
        final Map<String, Value> varMap = new HashMap<>();
        for (int i = 0; i < vars.length - 1; i += 2) {
            varMap.put(vars[i].toString(), Value.of(vars[i + 1]));
        }
        return evaluate(varMap::get);
    }

    public Value evaluate(final Func1<String, Value> vars) {
        try {
            return compiledExpression.eval(vars == null ? x -> null : vars);
        } catch (CalculatorException ce) {
            ce.setExpression(expression);
            ce.setEvaluationContext(calculateEvaluationContext(vars));
            throw ce;
        }
    }

    private String calculateEvaluationContext(final Func1<String, Value> vars) {
        try {
            final Set<String> neededVars = new HashSet<>();
            compiledExpression.calculateNeededVariables(neededVars);
            final List<String> list = new ArrayList<>();
            for (String v : neededVars) {
                list.add(v + "=" + vars.call(v));
            }
            return StringUtils.join(list, ",");
        } catch (Exception e) {
            return "Exc: " + e.toString();
        }
    }

    public Set<String> getNeededVariables() {
        final Set<String> result = new HashSet<>();
        compiledExpression.calculateNeededVariables(result);
        return result;
    }

    private void compile() {
        if (StringUtils.isBlank(expression)) {
            throw new CalculatorException(EMPTY_FORMULA);
        }
        nextChar();
        final CalcNode x = parseExpression();
        if (pos < expression.length()) {
            throw new CalculatorException(UNEXPECTED_TOKEN, "EOF");
        }
        this.compiledExpression = x;
    }

    private void nextChar() {
        ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
    }

    private void mark() {
        markedCh = ch;
        markedPos = pos;
    }

    private void reset() {
        ch = markedCh;
        pos = markedPos;
    }

    private boolean eat(final int charToEat) {
        while (Character.isWhitespace(ch)) {
            nextChar();
        }
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private CalcNode parseExpression() {
        CalcNode x = parseTerm();
        for (;;) {
            if (eat('+')) {
                x = createNumeric("+", new CalcNode[]{x, parseTerm()}, (nums, vars) -> Value.of(nums.getAsDouble(0) + nums.getAsDouble(1)));
            } else if (eat('-')) {
                x = createNumeric("-", new CalcNode[]{x, parseTerm()}, (nums, vars) -> Value.of(nums.getAsDouble(0) - nums.getAsDouble(1)));
            } else {
                return x;
            }
        }
    }

    private CalcNode parseTerm() {
        CalcNode x = parseFactor();
        for (;;) {
            if (eat('*')) {
                x = createNumeric("*", new CalcNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) * nums.getAsDouble(1)));
            } else if (eat('/') || eat(':')) {
                x = createNumeric("/", new CalcNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) / nums.getAsDouble(1)));
            } else if (eat('%')) {
                x = createNumeric("%", new CalcNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) % nums.getAsDouble(1)));
            } else {
                return x;
            }
        }
    }

    private CalcNode parseFactor() {
        if (eat('+')) {
            return parseFactor(); // unary plus
        }
        if (eat('-')) {
            return createNumeric("-", new CalcNode[]{parseFactor()}, (nums, vars) -> Value.of(-nums.getAsDouble(0)));
        }

        CalcNode x = parseConcatBlock();

        if (eat('!')) {
            x = createNumeric("!", new CalcNode[]{x}, (nums, vars) -> {
                final int facValue = nums.getAsInt(0, 0);
                if (!nums.get(0).isInteger() || facValue < 0) {
                    throw new CalculatorException(WRONG_TYPE, "positive Integer", nums.get(0), nums.get(0).getType());
                }
                int result = 1;
                for (int i = 2; i <= facValue; i++) {
                    result *= i;
                }
                return Value.of(result);
            });
        }

        if (eat('^')) {
            x = createNumeric("^", new CalcNode[]{x, parseFactor()}, (nums, vars) -> Value.of(Math.pow(nums.getAsDouble(0), nums.getAsDouble(1))));
        }

        return x;
    }

    private CalcNode parseString() {
        final StringBuilder sb = new StringBuilder();
        boolean foundEnd = false;
        eat('\'');
        while (ch != -1) {
            if (ch == '\'') {
                nextChar();
                if (ch != '\'') {
                    foundEnd = true;
                    break;
                }
            }
            sb.append((char) ch);
            nextChar();
        }
        if (!foundEnd) {
            throw new CalculatorException(UNEXPECTED_TOKEN, "'");
        }
        final String literal = sb.toString();
        return new CalcNode("string-literal", null, (objs, vars) -> Value.of(literal));
    }


    private CalcNode parseConcatBlock() {
        final List<CalcNode> nodes = new ArrayList<>();
        while (true) {
            if (ch == '(') {
                nextChar();
                nodes.add(parseExpression());
                if (!eat(')')) {
                    throw new CalculatorException(UNEXPECTED_TOKEN, ")");
                }
            } else if (ch == '\'') {
                nodes.add(parseString());
            } else if (ch == '.') {
                nodes.add(new CalcNode("decimal-point", null, (objs, vars) -> Value.of(".")));
                nextChar();
            } else if (ch == '$') {
                nodes.add(parseExplicitVariable());
            } else if (CHARS.contains(ch)) {
                nodes.add(parseAlphaNumericBlock());
            } else if (NUMBERS.contains(ch)) {
                nodes.add(parseNumberBlock());
            } else {
                break;
            }
        }
        if (nodes.isEmpty()) {
            throw new CalculatorException(UNEXPECTED_TOKEN, "alphanumeric, ( or '");
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new CalcNode("concat", nodes.toArray(new CalcNode[0]), (objs, vars) -> CalculatorUtils.concat(objs));
    }

    private CalcNode parseNumberBlock() {
        final StringBuilder sb = new StringBuilder();
        boolean foundDecPoint = false;
        boolean lastFoundIsDec = false;
        while (NUMBERS.contains(ch)) {
            if (ch == '.') {
                if (foundDecPoint) {
                    throw new CalculatorException(UNEXPECTED_TOKEN, "digit");
                }
                foundDecPoint = true;
                lastFoundIsDec = true;
                mark();
            } else {
                lastFoundIsDec = false;
            }
            sb.append((char) ch);
            nextChar();
        }
        //if the last found char was a ., then don't eat it
        if (lastFoundIsDec) {
            reset();
        }
        final double value = Double.parseDouble(sb.toString());
        return createNumeric("literal-num", null, (o, v) -> Value.of(value));
    }

    private CalcNode parseExplicitVariable() {
        if (!eat('$')) {
            throw new CalculatorException(UNEXPECTED_TOKEN, '$');
        }
        //first char MUST be an alpha
        if (!CHARS.contains(ch)) {
            throw new CalculatorException(UNEXPECTED_TOKEN, "alpha");
        }
        final StringBuilder sb = new StringBuilder();
        while (CHARS_DIGITS.contains(ch)) {
            sb.append((char) ch);
            nextChar();
        }
        final String parsed = sb.toString();

        return new CalcNode("var", null, (objs, vars) -> {
            final Value value = vars.call(parsed);
            if (value != null) {
                return value;
            }
            throw createMissingVarsException(vars);
        }, result -> result.add(parsed));

    }

    private CalcNode parseAlphaNumericBlock() {
        if (!CHARS.contains(ch)) {
            throw new CalculatorException(UNEXPECTED_TOKEN, "alpha");
        }
        //An alphanumeric block may either be a function (name) or a block of single-letter variables
        final StringBuilder sbFunction = new StringBuilder();
        final StringBuilder sbSingleLetterVars = new StringBuilder();
        boolean firstAlphaBlock = true;
        while (CHARS_DIGITS.contains(ch)) {
            sbFunction.append((char) ch);
            if (!CHARS.contains(ch)) {
                firstAlphaBlock = false;
            }
            if (firstAlphaBlock) {
                sbSingleLetterVars.append((char) ch);
            }
            nextChar();
            if (firstAlphaBlock) {
                mark();
            }
        }
        final String functionParsed = sbFunction.toString();

        if (ch == '(' && CalculatorFunction.findByName(functionParsed) != null) { //function
            return parseFunction(functionParsed);
        }

        //not a function -> reset to first parsed alphablock and use this solely
        reset();
        return parseSingleLetterVariableBlock(sbSingleLetterVars.toString());
    }

    @NonNull
    private CalcNode parseSingleLetterVariableBlock(final String varBlock) {

        return new CalcNode("varblock", null, (objs, vars) -> {
            final ValueList varValues = new ValueList();
            for (char l : varBlock.toCharArray()) {
                final Value value = vars.call("" + l);
                if (value == null) {
                    throw createMissingVarsException(vars);
                }
                varValues.add(value);
            }
            return CalculatorUtils.concat(varValues);
        }, result -> {
            for (char l : varBlock.toCharArray()) {
                result.add("" + l);
            }
        });
    }

    private CalculatorException createMissingVarsException(final Func1<String, Value> providedVars) {
        //find out ALL variable values missing for this calculation for a better error message
        final Set<String> missingVars = this.getNeededVariables();
        final Iterator<String> it = missingVars.iterator();
        while (it.hasNext()) {
            if (providedVars.call(it.next()) != null) {
                it.remove();
            }
        }
        final List<String> missingVarsOrdered = new ArrayList<>(missingVars);
        Collections.sort(missingVarsOrdered);
        return new CalculatorException(MISSING_VARIABLE_VALUE, StringUtils.join(missingVarsOrdered, ", "));
    }

    //this method assumes that functionName is already parsed and ensured, and that ch is on opening parenthesis
    @NonNull
    private CalcNode parseFunction(final String functionName) {
        nextChar();
        final List<CalcNode> params = new ArrayList<>();
        if (!eat(')')) {
            do {
                params.add(parseExpression());
            } while (eat(';'));
            if (!eat(')')) {
                throw new CalculatorException(UNEXPECTED_TOKEN, "; or )");
            }
        }

        return new CalcNode("f:" + functionName, params.toArray(new CalcNode[0]),
            (n, v) -> {
                try {
                    return Objects.requireNonNull(CalculatorFunction.findByName(functionName)).execute(n);
                } catch (CalculatorException ce) {
                    ce.setExpression(expression);
                    ce.setFunction(functionName);
                    throw ce;
                } catch (RuntimeException re) {
                    final CalculatorException ce = new CalculatorException(re, OTHER, re.getMessage());
                    ce.setExpression(expression);
                    ce.setFunction(functionName);
                    throw ce;

                }
            });

    }

}

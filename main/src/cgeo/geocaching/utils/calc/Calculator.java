package cgeo.geocaching.utils.calc;

import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;
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
import java.util.List;
import java.util.Locale;
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
 * * Operators '%' and '!'.
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

    public static final String VALID_OPERATOR_PATTERN = "+\\-*/%^*!";

    private static final Set<Integer> CHARS = new HashSet<>();
    private static final Set<Integer> NUMBERS = new HashSet<>();

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<Calculator, CalculatorException>> CALCULATION_CACHE = new LeastRecentlyUsedMap.LruCache<>(500);

    private static final Map<String, Func1<Object[], Object>> FUNCTIONS = new HashMap<>();

    private final String expression;

    //needed only during parsing
    private int pos = -1;
    private int ch;

    //stores compiled expression
    private CalcNode compiledExpression;


    private static class CalcNode {

        public final String id;
        public final Func2<Object[], Map<String, Object>, Object> function;
        public final CalcNode[] children;

        public final Action2<Set<String>, Set<String>> neededVars;

        CalcNode(final String id, final CalcNode[] children, final Func2<Object[], Map<String, Object>, Object> function) {
            this(id, children, function, null);
        }

        CalcNode(final String id, final CalcNode[] children, final Func2<Object[], Map<String, Object>, Object> function, final Action2<Set<String>, Set<String>> neededVars) {

            this.id = id;
            this.children = children == null ? new CalcNode[0] : children;
            this.function = function;
            this.neededVars = neededVars;
        }

        public Object eval(final Map<String, Object> variables) {
            final Object[] childValues = new Object[children.length];
            for (int i = 0; i < children.length; i++) {
                childValues[i] = children[i].eval(variables);
            }
            return this.function.call(childValues, variables);
        }

        //calculates the variable names needed by this function
        public void calculateNeededVariables(final Set<String> providedVars, final Set<String> result) {
            if (this.neededVars != null) {
                this.neededVars.call(providedVars, result);
            }
            for (CalcNode child : children) {
                child.calculateNeededVariables(providedVars, result);
            }
        }
    }

    private CalcNode createNumeric(final String id, final CalcNode[] children, final Func2<Number[], Map<String, Object>, Object> function) {
        return new CalcNode(id, children, (objs, vars) -> function.call(CalculatorUtils.toNumericArray(objs), vars));
    }

    private static void addFunction(final String name, final Func1<Object[], Object> function) {
        FUNCTIONS.put(name.toLowerCase(Locale.US), function);
    }

    private static void addNumericFunction(final String name, final Func1<Number[], Number> numericFunction) {
        addFunction(name, params -> numericFunction.call(CalculatorUtils.toNumericArray(params)));
    }

    private static void addNumericSingleValueFunction(final String name, final Func1<Number[], Number> numericFunction) {
        addFunction(name, params -> {
            CalculatorUtils.checkParameters(params, 1, 1);
            return numericFunction.call(CalculatorUtils.toNumericArray(params));
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
        NUMBERS.add((int) '.');

        addNumericSingleValueFunction("sqrt", p -> Math.sqrt(p[0].doubleValue()));
        addNumericSingleValueFunction("sin", p -> Math.sin(Math.toRadians(p[0].doubleValue())));
        addNumericSingleValueFunction("cos", p -> Math.cos(Math.toRadians(p[0].doubleValue())));
        addNumericSingleValueFunction("tan", p -> Math.tan(Math.toRadians(p[0].doubleValue())));
        addNumericSingleValueFunction("abs", p -> Math.abs(p[0].doubleValue()));
        addNumericSingleValueFunction("round", p -> Math.round(p[0].doubleValue()));

        addNumericFunction("random", CalculatorUtils::random);
        addFunction("length", p -> p[0].toString().length());

        addNumericFunction("checksum", p -> CalculatorUtils.checksum(p, false));
        addNumericFunction("ichecksum", p -> CalculatorUtils.checksum(p, true));
        addFunction("lettervalue", CalculatorUtils::letterValue);
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
            ce.setParsingContext((char) calc.ch, calc.pos);
            throw ce;
        }
    }

    public static double eval(final String expression, final Object ... vars) {
        return compile(expression).eval(vars);
    }

    private Calculator(final String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    protected char getCurrentChar() {
        return (char) ch;
    }

    protected int getCurrentPos() {
        return pos;
    }

    public double eval(final Object ... vars) {
        if (vars == null || vars.length == 0) {
            return eval(Collections.emptyMap());
        }
        final Map<String, Object> varMap = new HashMap<>();
        for (int i = 0; i < vars.length - 1; i += 2) {
            varMap.put(vars[i].toString(), vars[i + 1]);
        }
        return eval(varMap);
    }

    public double eval(final Map<String, Object> vars) {
        final Object result = evaluate(vars);
        return result instanceof Number ? ((Number) result).doubleValue() : 0d;
    }

    public Object evaluate(final Map<String, Object> vars) {
        try {
            return compiledExpression.eval(vars == null ? Collections.emptyMap() : vars);
        } catch (CalculatorException ce) {
            ce.setExpression(expression);
            ce.setEvaluationContext(vars);
            throw ce;
        }
    }

    public Set<String> getNeededVariables(final Set<String> providedVariables) {
        final Set<String> result = new HashSet<>();
        compiledExpression.calculateNeededVariables(providedVariables, result);
        return result;
    }

    private void compile() {
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

    /**
     * Grammar:
     *
     * expression = term | expression `+` term | expression `-` term
     * term = factor | term `*` factor | term `/` factor
     * factor = `+` factor | `-` factor | `(` expression `)`
     *        | number | functionName factor | factor `^` factor
     */
    private CalcNode parseExpression() {
        CalcNode x = parseTerm();
        for (;;) {
            if (eat('+')) {
                x = createNumeric("+", new CalcNode[]{x, parseTerm()}, (nums, vars) -> nums[0].doubleValue() + nums[1].doubleValue());
            } else if (eat('-')) {
                x = createNumeric("-", new CalcNode[]{x, parseTerm()}, (nums, vars) -> nums[0].doubleValue() - nums[1].doubleValue());
            } else {
                return x;
            }
        }
    }

    private CalcNode parseTerm() {
        CalcNode x = parseFactor();
        for (;;) {
            if (eat('*')) {
                x = createNumeric("*", new CalcNode[]{x, parseFactor()}, (nums, vars) -> nums[0].doubleValue() * nums[1].doubleValue());
            } else if (eat('/')) {
                x = createNumeric("/", new CalcNode[]{x, parseFactor()}, (nums, vars) -> nums[0].doubleValue() / nums[1].doubleValue());
            } else if (eat('%')) {
                x = createNumeric("%", new CalcNode[]{x, parseFactor()}, (nums, vars) -> nums[0].doubleValue() % nums[1].doubleValue());
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
            return createNumeric("-", new CalcNode[]{parseFactor()}, (nums, vars) -> -nums[0].doubleValue());
        }

        CalcNode x = parseConcatBlock();

        if (eat('!')) {
            x = createNumeric("!", new CalcNode[]{x}, (nums, vars) -> {
                if (!CalculatorUtils.isInteger(nums[0]) || nums[0].intValue() < 0) {
                    throw new CalculatorException(WRONG_TYPE, "positive Integer", nums[0].doubleValue(), CalculatorUtils.getValueType(nums[0]));
                }
                int result = 1;
                for (int i = 2; i <= nums[0].intValue(); i++) {
                    result *= i;
                }
                return result;
            });
        }

        if (eat('^')) {
            x = createNumeric("^", new CalcNode[]{x, parseFactor()}, (nums, vars) -> Math.pow(nums[0].doubleValue(), nums[1].doubleValue()));
        }

        return x;
    }

    private CalcNode parseString() {
        final StringBuilder sb = new StringBuilder();
        boolean foundEnd = false;
        eat('\'');
        while (ch != -1) {
            sb.append((char) ch);
            nextChar();
            if (ch == '\'') {
                nextChar();
                if (ch != '\'') {
                    foundEnd = true;
                    break;
                }
            }
        }
        if (!foundEnd) {
            throw new CalculatorException(UNEXPECTED_TOKEN, "'");
        }
        final String literal = sb.toString();
        return new CalcNode("string-literal", null, (objs, vars) -> literal);
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
            } else if (CHARS.contains(ch)) {
                nodes.add(parseAlphaBlock());
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
        while (NUMBERS.contains(ch)) {
            sb.append((char) ch);
            nextChar();
        }
        final double value = Double.parseDouble(sb.toString());
        return createNumeric("literal-num", null, (o, v) -> value);
    }

    private CalcNode parseAlphaBlock() {
        final StringBuilder sb = new StringBuilder();
        while (CHARS.contains(ch)) {
            sb.append((char) ch);
            nextChar();
        }
        final String parsed = sb.toString();

        if (ch == '(' && FUNCTIONS.containsKey(parsed)) { //function
            return parseFunction(parsed);
        }


        return new CalcNode("varblock", null, (objs, vars) -> {
            if (vars.containsKey(parsed)) {
                return vars.get(parsed);
            }
            final Object[] varValues = new Object[parsed.length()];
            int i = 0;
            for (char l : parsed.toCharArray()) {
                if (!vars.containsKey("" + l)) {
                    //find out ALL variable values missing for this calculation for a better error message
                    final Set<String> missingVars = this.getNeededVariables(vars.keySet());
                    for (Map.Entry<String, Object> var : vars.entrySet()) {
                        if (var.getValue() != null) {
                            missingVars.remove(var.getKey());
                        }
                    }
                    final List<String> missingVarsOrdered = new ArrayList<>(missingVars);
                    Collections.sort(missingVarsOrdered);
                    throw new CalculatorException(MISSING_VARIABLE_VALUE, StringUtils.join(missingVarsOrdered, ", "));
                }
                varValues[i++] = vars.get("" + l);
            }
            return CalculatorUtils.concat(varValues);
        }, (providedVars, result) -> {
            if (providedVars.contains(parsed)) {
                result.add(parsed);
            } else {
                for (char l : parsed.toCharArray()) {
                    result.add("" + l);
                }
            }

        });
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
                    return Objects.requireNonNull(FUNCTIONS.get(functionName)).call(n);
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

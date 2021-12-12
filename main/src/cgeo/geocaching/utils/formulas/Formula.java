package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.EMPTY_FORMULA;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.MISSING_VARIABLE_VALUE;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.OTHER;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.UNEXPECTED_TOKEN;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import android.util.Pair;

import androidx.annotation.NonNull;

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
 *
 */
public final class Formula {

    public static final String VALID_OPERATOR_PATTERN = "+\\-*/%^*:!";

    private static final Value OVERFLOW_VALUE = Value.of("");

    private static final Set<Integer> CHARS = new HashSet<>();
    private static final Set<Integer> CHARS_DIGITS = new HashSet<>();
    private static final Set<Integer> NUMBERS = new HashSet<>();

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<Formula, FormulaException>> FORMULA_CACHE = new LeastRecentlyUsedMap.LruCache<>(500);

    //effectively final (assigned final after compilation)
    private String expression;
    private FormulaNode compiledExpression;


    //needed only during parsing/compilation
    private int pos = -1;
    private int ch;
    private int level;
    private String rawExpression; //the expression as given to compiler (includes e.g. chars before startPos and after stopChars)
    private Func1<Character, Boolean> stopChecker;

    private int markedPos = -1;
    private int markedCh = ch;
    private int markedLevel;

    public static class StopCharSet {

        public static final StopCharSet EMPTY = createFor(null);

        private final Set<Character> stopCharSet;
        private final String stopCharString;

        private StopCharSet(final String stopCharString) {
            this.stopCharString = stopCharString == null ? "" : stopCharString;
            this.stopCharSet = new HashSet<>();
            if (stopCharString != null) {
                for (char stopChar : stopCharString.toCharArray()) {
                    this.stopCharSet.add(stopChar);
                }
            }
        }

        public static StopCharSet createFor(final String stopChars) {
            return new StopCharSet(stopChars);
        }

        public String getKey() {
            return stopCharString;
        }

        public boolean contains(final char c) {
            return stopCharSet.contains(c);
        }

    }

    private static class FormulaNode {

        private static final FormulaNode[] FORMULA_NODE_EMPTY_ARRAY = new FormulaNode[0];

        private final String id;
        private Func2<ValueList, Func1<String, Value>, Value> function;
        private final Func2<ValueList, Func1<String, Value>, String> functionToString;
        private FormulaNode[] children;

        public final Set<String> neededVars;

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func2<ValueList, Func1<String, Value>, Value> function,
                    final Func2<ValueList, Func1<String, Value>, String> functionToString) {
            this(id, children, function, functionToString, null);
        }

        FormulaNode(final String id, final FormulaNode[] children,
                    final Func2<ValueList, Func1<String, Value>, Value> function,
                    final Func2<ValueList, Func1<String, Value>, String> functionToString,
                    final Action1<Set<String>> explicitelyNeeded) {

            this.id = id;
            this.children = children == null ? FORMULA_NODE_EMPTY_ARRAY : children;
            this.function = function;
            this.functionToString = functionToString;
            this.neededVars = Collections.unmodifiableSet(calculateNeededVars(explicitelyNeeded, children));

            if (this.neededVars.isEmpty()) {
                //this means that function is constant!
                this.function = createConstantFunction();
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

        private Func2<ValueList, Func1<String, Value>, Value> createConstantFunction() {
            Value result = null;
            FormulaException resultException = null;
            try {
                result = eval(y -> null);
            } catch (FormulaException fe) {
                resultException = fe;
            }
            final Value finalResult = result;
            final FormulaException finalResultException = resultException;
            return (objs, vars) -> {
                if (finalResultException != null) {
                    throw finalResultException;
                }
                return finalResult;
            };
        }

        public Value eval(final Func1<String, Value> variables) throws FormulaException {
            final ValueList childValues = new ValueList();
            for (FormulaNode child : children) {
                childValues.add(child.eval(variables));
            }
            return this.function.call(childValues, variables);
        }

        public Object evalToString(final Func1<String, Value> variables) {
            final ValueList childValues = new ValueList();
            boolean hasError = false;
            for (FormulaNode child : children) {
                final Object v = child.evalToString(variables);
                if (v instanceof Value) {
                    childValues.add((Value) v);
                } else {
                    hasError = true;
                    childValues.add(Value.of(v));
                }
            }
            if (!hasError) {
                try {
                    return this.function.call(childValues, variables);
                } catch (FormulaException fe) {
                    //do nothing
                }
            }

            //we had an error in the chain -> produce a String using the error function
            if (this.functionToString != null) {
                return this.functionToString.call(childValues, variables);
            }

            //default "functionToString"
            if (childValues.size() > 0) {
                return StringUtils.join(childValues, "");
            }
            return "_";
         }

         public static ValueList toValueList(final Collection<FormulaNode> nodes, final Func1<String, Value> variables) {
            final ValueList childValues = new ValueList();
            for (FormulaNode child : nodes) {
                childValues.add(child.eval(variables));
            }
            return childValues;
        }

        /** for test/debug purposes only! */
        public String toDebugString(final Func1<String, Value> variables, final boolean includeId, final boolean recursive) {
            final StringBuilder sb = new StringBuilder();
            if (includeId) {
                sb.append("[").append(getId()).append("]");
            }
            sb.append(evalToString(variables));
            if (recursive) {
                sb.append("{");
                for (FormulaNode child : children) {
                    sb.append(child.toDebugString(variables, includeId, recursive));
                }
                sb.append("}");
            }
            return sb.toString();
        }
    }

    private FormulaNode createNumeric(final String id, final FormulaNode[] children, final Func2<ValueList, Func1<String, Value>, Value> function) {
        return new FormulaNode(id, children, (objs, vars) -> {
            objs.checkAllDouble();
            return function.call(objs, vars);
        }, (objs, vars) -> StringUtils.join(objs, " " + id + " "));
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

    public static Formula compile(final String expression) {
        return compile(expression, 0, null);
    }

    public static Formula compile(final String expression, final int startPos, final StopCharSet stopChars) {
        final StopCharSet stopCharSet = stopChars == null ? StopCharSet.EMPTY : stopChars;
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
            final Formula c =  compileInternal(expression, startPos, stopCharSet::contains);
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
            ce.setExpression(expression);
            ce.setParsingContext(f.ch, f.pos);
            throw ce;
        }
    }

    public static Value evaluate(final String expression, final Object ... vars) {
        return compile(expression).evaluate(vars);
    }


    public static double eval(final String expression, final Object ... vars) {
        return evaluate(expression, vars).getAsDouble();
    }

    private Formula()  {
        //do nothing
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
        return evaluate(toVarProvider(vars));
    }

    public static Func1<String, Value> toVarProvider(final Object ... vars) {
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
        try {
            return compiledExpression.eval(vars == null ? x -> null : vars);
        } catch (FormulaException ce) {
            ce.setExpression(expression);
            ce.setEvaluationContext(calculateEvaluationContext(vars));
            throw ce;
        }
    }

    public String evaluateToString(final Func1<String, Value> vars) {
        return compiledExpression.evalToString(vars == null ? x -> null : vars).toString();
    }

    private String calculateEvaluationContext(final Func1<String, Value> vars) {
        try {
            final List<String> list = new ArrayList<>();
            for (String v : compiledExpression.getNeededVars()) {
                list.add(v + "=" + vars.call(v));
            }
            return StringUtils.join(list, ",");
        } catch (Exception e) {
            return "Exc: " + e.toString();
        }
    }

    public Set<String> getNeededVariables() {
        return compiledExpression.getNeededVars();
    }

    protected void doCompile(final String rawExpression, final int startPos, final Func1<Character, Boolean> stopChecker) throws FormulaException {
        this.rawExpression = rawExpression;
        this.stopChecker = stopChecker;
        this.level = 0;
        this.pos = startPos - 1; //important to set before the EMPTY_FORMULA check: generates better error message for MultiFormulas
        if (StringUtils.isBlank(rawExpression) || startPos >= rawExpression.length()) {
            throw new FormulaException(EMPTY_FORMULA);
        }
        nextChar();
        final FormulaNode x = parseExpression();
        if (ch > -1) {
            throw new FormulaException(UNEXPECTED_TOKEN, "EOF");
        }
        this.expression = rawExpression.substring(startPos, pos);
        this.compiledExpression = x;
    }

    private void nextChar() {
        ch = (++pos < rawExpression.length()) ? rawExpression.charAt(pos) : -1;
        if (level == 0 && stopChecker != null && stopChecker.call((char) ch)) {
            //stop parsing
            ch = -1;
        }
    }

    private void mark() {
        markedCh = ch;
        markedPos = pos;
        markedLevel = level;
    }

    private void reset() {
        ch = markedCh;
        pos = markedPos;
        level = markedLevel;
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

    private FormulaNode parseExpression() {
        FormulaNode x = parseTerm();
        for (;;) {
            if (eat('+')) {
                x = createNumeric("+", new FormulaNode[]{x, parseTerm()}, (nums, vars) -> Value.of(nums.getAsDouble(0) + nums.getAsDouble(1)));
            } else if (eat('-') || eat('—')) { //those are two different chars
                x = createNumeric("-", new FormulaNode[]{x, parseTerm()}, (nums, vars) -> Value.of(nums.getAsDouble(0) - nums.getAsDouble(1)));
            } else {
                return x;
            }
        }
    }

    private FormulaNode parseTerm() {
        FormulaNode x = parseFactor();
        for (;;) {
            if (eat('*') || eat('•')) {
                x = createNumeric("*", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) * nums.getAsDouble(1)));
            } else if (eat('/') || eat(':') || eat('÷')) {
                x = createNumeric("/", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) / nums.getAsDouble(1)));
            } else if (eat('%')) {
                x = createNumeric("%", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(nums.getAsDouble(0) % nums.getAsDouble(1)));
            } else {
                return x;
            }
        }
    }

    private FormulaNode parseFactor() {
        if (eat('+')) {
            return parseFactor(); // unary plus
        }
        if (eat('-') || eat('—')) { // those are two different chars!
            return createNumeric("-", new FormulaNode[]{parseFactor()}, (nums, vars) -> Value.of(-nums.getAsDouble(0)));
        }

        FormulaNode x = parseConcatBlock();

        if (eat('!')) {
            x = createNumeric("!", new FormulaNode[]{x}, (nums, vars) -> {
                final int facValue = nums.getAsInt(0, 0);
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

        if (eat('^')) {
            x = createNumeric("^", new FormulaNode[]{x, parseFactor()}, (nums, vars) -> Value.of(Math.pow(nums.getAsDouble(0), nums.getAsDouble(1))));
        }

        return x;
    }

    private FormulaNode parseNumber() {
        final StringBuilder sb = new StringBuilder();
        while (NUMBERS.contains(ch)) {
            sb.append((char) ch);
            nextChar();
        }
        return createSingleValueNode("number", sb.toString());

    }

    private FormulaNode parseString() {
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
            throw new FormulaException(UNEXPECTED_TOKEN, "'");
        }
        return createSingleValueNode("string-literal", sb.toString());
    }


    private FormulaNode parseConcatBlock() {
        final List<FormulaNode> nodes = new ArrayList<>();
        while (true) {
            if (ch == '(' || ch == '[') {
                final char expectedClosingChar = (ch == '(' ? ')' : ']');
                nextChar();
                this.level++;
                nodes.add(new FormulaNode("paren", new FormulaNode[]{parseExpression()}, (o, v) -> o.get(0), (o, v) -> "(" + o.get(0) + ")"));
                this.level--;
                if (!eat(expectedClosingChar)) {
                    throw new FormulaException(UNEXPECTED_TOKEN, "" + expectedClosingChar);
                }
            } else if (ch == '\'') {
                level++;
                nodes.add(parseString());
                level--;
            } else if (ch == '_') {
                nodes.add(createSingleValueNode("overflow", OVERFLOW_VALUE));
                nextChar();
                //constant numbers directly after overflow shall not spill over -> thus create special node for first number digit
                if (NUMBERS.contains(ch)) {
                    nodes.add(createSingleValueNode("digit", "" + (char) ch));
                    nextChar();
                }
            } else if (ch == '$') {
                nodes.add(parseExplicitVariable());
            } else if (CHARS.contains(ch)) {
                nodes.add(parseAlphaNumericBlock());
            } else if (NUMBERS.contains(ch)) {
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
        return new FormulaNode("concat", nodes.toArray(new FormulaNode[0]), (objs, vars) -> concat(objs), null);
    }

    private static FormulaNode createSingleValueNode(final String nodeId, final Object value) {
        return new FormulaNode(nodeId, null, (objs, vars) ->
            value instanceof Value ? (Value) value : Value.of(value), null);
    }

    private FormulaNode parseExplicitVariable() {
        if (!eat('$')) {
            throw new FormulaException(UNEXPECTED_TOKEN, '$');
        }
        //first char MUST be an alpha
        if (!CHARS.contains(ch)) {
            throw new FormulaException(UNEXPECTED_TOKEN, "alpha");
        }
        final StringBuilder sb = new StringBuilder();
        while (CHARS_DIGITS.contains(ch)) {
            sb.append((char) ch);
            nextChar();
        }
        final String parsed = sb.toString();

        return new FormulaNode("var", null, (objs, vars) -> {
            final Value value = vars.call(parsed);
            if (value != null) {
                return value;
            }
            throw createMissingVarsException(vars);
        }, (objs, vars) -> {
            final Value value = vars.call(parsed);
            if (value != null) {
                return value.getAsString();
            }
            return "_";
        }, result -> result.add(parsed));

    }

    private FormulaNode parseAlphaNumericBlock() {
        if (!CHARS.contains(ch)) {
            throw new FormulaException(UNEXPECTED_TOKEN, "alpha");
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

        if (ch == '(' && FormulaFunction.findByName(functionParsed) != null) { //function
            return parseFunction(functionParsed);
        }

        //not a function -> reset to first parsed alphablock and use this solely
        reset();
        return parseSingleLetterVariableBlock(sbSingleLetterVars.toString());
    }

    @NonNull
    private FormulaNode parseSingleLetterVariableBlock(final String varBlock) {

        return new FormulaNode("varblock", null, (objs, vars) -> {
            final ValueList varValues = new ValueList();
            for (char l : varBlock.toCharArray()) {
                final Value value = vars.call("" + l);
                if (value == null) {
                    throw createMissingVarsException(vars);
                }
                varValues.add(value);
            }
            return concat(varValues);
        }, (objs, vars) -> {
            final ValueList varValues = new ValueList();
            for (char l : varBlock.toCharArray()) {
                final Value value = vars.call("" + l);
                varValues.add(value == null ? Value.of("_") : value);
            }
            return concat(varValues).getAsString();
        }, result -> {
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
        nextChar();
        final List<FormulaNode> params = new ArrayList<>();
        if (!eat(')')) {
            do {
                params.add(parseExpression());
            } while (eat(';'));
            if (!eat(')')) {
                throw new FormulaException(UNEXPECTED_TOKEN, "; or )");
            }
        }

        return new FormulaNode("f:" + functionName, params.toArray(new FormulaNode[0]),
            (n, v) -> {
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
            (n, v) -> functionName + "(" + StringUtils.join(n, ","));

    }

    /** for test/debug purposes only! */
    public String toDebugString(final Func1<String, Value> variables, final boolean includeId, final boolean recursive) {
        return compiledExpression.toDebugString(variables == null ? x -> null : variables, includeId, recursive);
    }

    /** concats values Formula-internally. Takes care of the spillover character _ */
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

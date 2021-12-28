package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DegreeFormula {

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<DegreeFormula, FormulaException>> FORMULA_CACHE =
        new LeastRecentlyUsedMap.LruCache<>(500);


    private static final Set<Integer> ALL_CHARS = new HashSet<>();
    private static final Set<Integer> NEG_CHARS = new HashSet<>();

    private static final Formula.StopCharSet scs = Formula.StopCharSet.createFor(" °'\"");

    static {
        for (char c : new char[]{'N', 'n', 'E', 'e', 'O', 'o'}) {
            ALL_CHARS.add((int) c);
        }
        for (char c : new char[]{'S', 's', 'W', 'w'}) {
            ALL_CHARS.add((int) c);
            NEG_CHARS.add((int) c);
        }
    }

    private final List<Object> nodes = new ArrayList<>();
    private final Set<String> neededVars;

    private final String expression;
    private int pos = -1;
    private int ch = -1;

    private DegreeFormula(final String expression) {
        this.expression = expression;
        parse();
        this.neededVars = Collections.unmodifiableSet(calculatedNeededVars());
    }

    public String getExpression() {
        return expression;
    }

    public Set<String> getNeededVars() {
        return this.neededVars;
    }

    private void parse() {
        final int c = nextNonWhitespaceChar();
        if (ALL_CHARS.contains(c)) {
            nodes.add((char) c);
            nextNonWhitespaceChar();
        }
        int pValue = 1;
        boolean stop = false;
        while (!stop && ch != -1) {
            if (ALL_CHARS.contains(ch) && (peek() == -1 || Character.isWhitespace(peek()))) {
                nodes.add((char) ch);
            } else {
                try {
                    final Formula f = Formula.compile(expression, pos, scs);
                    pos += f.getExpression().length();
                    switch (ch) {
                        case (int) '°':
                            nodes.add(new Pair<>(f, 1));
                            pValue = 60;
                            break;
                        case (int) '\'':
                            if (peek() == '\'') {
                                nextChar();
                                nodes.add(new Pair<>(f, 60));
                            } else {
                                nodes.add(new Pair<>(f, 360));
                            }
                            pValue = 360;
                            break;
                        case (int) '"':
                            nodes.add(new Pair<>(f, 360));
                            pValue = 360;
                            break;
                        default:
                            nodes.add(new Pair<>(f, pValue));
                            if (pValue < 360) {
                                pValue *= 60;
                            }
                            break;
                    }


                } catch (FormulaException fe) {
                    nodes.add(expression.substring(pos));
                    stop = true;
                }
            }
            nextNonWhitespaceChar();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> calculatedNeededVars() {
        final Set<String> result = new HashSet<>();
        for (Object node : nodes) {
            if (node instanceof Pair<?, ?>) {
                result.addAll(((Pair<Formula, Integer>) node).first.getNeededVariables());
            }
        }
        return result;
    }

    public static DegreeFormula compile(final String expression) throws FormulaException {
        final String cacheKey = expression;
        Pair<DegreeFormula, FormulaException> entry = FORMULA_CACHE.get(cacheKey);
        if (entry == null) {
            try {
                entry = new Pair<>(new DegreeFormula(expression), null);
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


    public String evaluateToString(final Func1<String, Value> varMap) {
        return evaluateToCharSequence(varMap).toString();
    }

    @SuppressWarnings("unchecked")
    public CharSequence evaluateToCharSequence(final Func1<String, Value> varMap) {

        final List<CharSequence> css = new ArrayList<>();
        for (Object o : nodes) {
            if (o instanceof Character) {
                css.add("" + o);
            } else if (o instanceof String) {
                css.add(TextUtils.setSpan((String) o, Formula.createWarningSpan(), -1, -1, 1));
            } else {
                final Pair<Formula, Integer> p = (Pair<Formula, Integer>) o;
                css.add(p.first.evaluateToCharSequence(varMap));
                switch (p.second) {
                    case 1:
                        css.add("°");
                        break;
                    case 60:
                        css.add("'");
                        break;
                    case 360:
                        css.add("\"");
                        break;
                    default:
                        break;
                }
            }
        }
        return TextUtils.join(css, c -> c, "");
    }

    @SuppressWarnings("unchecked")
    public Double evaluate(final Func1<String, Value> varMap) {
        int sign = 1;
        double result = 0d;
        boolean foundAny = false;
        for (Object o : nodes) {
            if (o instanceof Character) {
                if (NEG_CHARS.contains((int) ((Character) o))) {
                    sign = -sign;
                }
            } else if (o instanceof String) {
                return null;
            } else {
                foundAny = true;
                final Pair<Formula, Integer> p = (Pair<Formula, Integer>) o;
                try {
                    result += p.first.evaluate(varMap).getAsDouble() / p.second;
                } catch (FormulaException fe) {
                    return null;
                }
            }
        }
        return foundAny ? result * sign : null;
    }

    private int nextChar() {
        pos++;
        ch = pos >= expression.length() ? -1 : expression.charAt(pos);
        return (char) ch;
    }

    private int peek() {
        return (pos + 1 >= expression.length() ? -1 : (int) expression.charAt(pos + 1));
    }

    private int nextNonWhitespaceChar() {
        do {
            nextChar();
        } while (Character.isWhitespace((char) ch));
        return ch;
    }
}

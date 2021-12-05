package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.functions.Func1;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents and maintains an expression containing multiple {@link Formula}s at once
 *
 * For example, the expression "N 4(A+1)° 23.B(B+1)4'" (a calculatable lat coordinate in DD° MM.MMM' format)
 * could be maintained by a MultiFormula containing two {@link Formula}s "4(A+1)" and "23.B(B+1)4"
 */
public class MultiFormula {

    //Caches last used compiled expressions for performance reasons
    private static final LeastRecentlyUsedMap<String, Pair<MultiFormula, FormulaException>> FORMULA_CACHE =
        new LeastRecentlyUsedMap.LruCache<>(500);

    private final MultiFormulaConfig config;
    private final String expression;
    private final Set<String> neededVars = new HashSet<>();

    private Object[] compiledParts;

    public static class MultiFormulaConfig {

        private final Object[] parts;
        private final String cacheKey;

        public MultiFormulaConfig(final String ... partConfigs) {
            this.parts = new Object[partConfigs.length];
            int idx = 0;
            for (String partConfig : partConfigs) {
                final char type = partConfig.charAt(0);
                final String config = partConfig.substring(1);
                switch (type) {
                    case 'f':
                        this.parts[idx] = Formula.StopCharSet.createFor(config);
                        break;
                    case 'p':
                        this.parts[idx] = StringUtils.isBlank(config) ? Pattern.compile("\\s*") : Pattern.compile("\\s*" + config + "\\s*");
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown type " + type);
                }
                idx++;
            }
            this.cacheKey = StringUtils.joinWith("-", (Object[]) partConfigs);
        }

    }

    public static boolean isValidForConfig(final MultiFormulaConfig config, final String expression) {
        try {
            compile(config, expression);
            return true;
        } catch (FormulaException fe) {
            return false;
        }
    }

    public static MultiFormula compile(final MultiFormulaConfig config, final String expression) throws FormulaException {
        final String cacheKey = config.cacheKey + "-" + expression;
        Pair<MultiFormula, FormulaException> entry = FORMULA_CACHE.get(cacheKey);
        if (entry == null) {
            try {
                entry = new Pair<>(compileInternal(config, expression), null);
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

    private static MultiFormula compileInternal(final MultiFormulaConfig config, final String expression) {
        final String usedExpression = expression == null ? "" : expression;
        final MultiFormula mc = new MultiFormula(config, usedExpression);
        final List<Object> compiledParts = new ArrayList<>();
        int start = 0;

        mc.neededVars.clear();
        for (Object part : config.parts) {
            if (part instanceof Formula.StopCharSet) {
                final Formula c = Formula.compile(usedExpression, start, (Formula.StopCharSet) part);
                start += c.getExpression().length();
                compiledParts.add(c);
                mc.neededVars.addAll(c.getNeededVariables());
            } else if (part instanceof Pattern) {
                final Matcher m = ((Pattern) part).matcher(usedExpression);
                if (!m.find(start) || m.start() != start) {
                    throw new FormulaException(FormulaException.ErrorType.OTHER, "Not found: " + m.pattern() + " at pos: " + start);
                }
                compiledParts.add(m.group());
                start = m.end();
            }
        }
        mc.compiledParts = compiledParts.toArray(new Object[0]);
        return mc;
    }

    protected MultiFormula(final MultiFormulaConfig config, final String expression) {
        this.expression = expression;
        this.config = config;
    }

    public String getExpression() {
        return expression;
    }

    public MultiFormulaConfig getConfig() {
        return config;
    }

    public ValueList evaluate(final Func1<String, Value> vars) {
        final ValueList result = new ValueList();
        for (Object compiledPart : compiledParts) {
            if (compiledPart instanceof Formula) {
                result.add(((Formula) compiledPart).evaluate(vars));
            } else {
                result.add(Value.of(compiledPart));
            }
        }
        return result;
    }

    public String evaluateToString(final Func1<String, Value> vars) {
        final StringBuilder sb = new StringBuilder();
        for (Value v : evaluate(vars)) {
            sb.append(v.getAsString());
        }
        return sb.toString();
    }

    public Set<String> getNeededVars() {
        return neededVars;
    }


}

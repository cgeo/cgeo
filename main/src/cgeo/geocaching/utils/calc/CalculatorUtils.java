package cgeo.geocaching.utils.calc;

import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.WRONG_PARAMETER_COUNT;
import static cgeo.geocaching.utils.calc.CalculatorException.ErrorType.WRONG_TYPE;

import java.util.Random;

/**
 * Holds implementations for functions in calculator
 */
public class CalculatorUtils {

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private CalculatorUtils() {
        //no instance
    }

    public static Object concat(final Object[] values) {
        checkParameters(values, 1, -1);

        if (values.length == 1) {
            return values[0];
        }

        boolean containsString = false;
        final StringBuilder sb = new StringBuilder();
        for (Object v : values) {
            if (v instanceof Number) {
                if (isInteger((Number) v)) {
                    sb.append(((Number) v).intValue());
                } else {
                    throw new CalculatorException(WRONG_TYPE, "integer", ((Number) v).doubleValue(), CalculatorUtils.getValueType(v));
                }
            } else {
                containsString = true;
                sb.append(v.toString());
            }
        }
        return containsString ? sb.toString() : Double.parseDouble(sb.toString());
    }


    public static Number random(final Number[] params) {
        checkParameters(params, -1, 2);
        final int max = params.length >= 1 ? params[0].intValue() : 10;
        final int min = params.length >= 2 ? params[1].intValue() : 0;
        return RANDOM.nextInt(max - min) + min;
    }

    public static Number checksum(final Number param, final boolean iterative) {
        int result = Math.abs(param.intValue());
        do {
            int cs = 0;
            while (result > 0) {
                cs += (result % 10);
                result /= 10;
            }
            result = cs;
        } while (result >= 10 && iterative);
        return result;
    }

    public static int letterValue(final String value) {
        int lv = 0;
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                lv += (int) c - (int) 'a' + 1;
            }
            if (c >= 'A' && c <= 'Z') {
                lv += (int) c - (int) 'A' + 1;
            }
            if (c >= '0' && c <= '9') {
                lv += (int) c - (int) '0';
            }
        }
        return lv;
    }

    public static String rot(final String value, final int rotParam) {
        int rot = rotParam;
        while (rot < 0) {
            rot += 26;
        }
        final StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                int newC = c + rot;
                if (newC > 'z') {
                    newC -= 26;
                }
                sb.append((char) newC);
            } else if (c >= 'A' && c <= 'Z') {
                int newC = c + rot;
                if (newC > 'Z') {
                    newC -= 26;
                }
                sb.append((char) newC);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isInteger(final Number n) {
        return Math.abs(Math.round(n.doubleValue()) - n.doubleValue()) < 0.000000001d;
    }

    public static void checkParameters(final Object[] params, final int minCount, final int maxCount) {
        if ((minCount > 0 && (params == null || params.length < minCount)) ||
            (maxCount > 0 && (params != null && params.length > maxCount))) {
            throw new CalculatorException(WRONG_PARAMETER_COUNT, minCount < 0 ? "-" : "" + minCount, maxCount < 0 ? "-" : "" + maxCount, params == null ? 0 : params.length);
        }

    }

    public static Number[] toNumericArray(final Object[] objs) {
        final Number[] numVals = new Number[objs == null ? 0 : objs.length];
        for (int i = 0; i < numVals.length; i++) {
            if (!(objs[i] instanceof Number)) {
                throw new CalculatorException(WRONG_TYPE, "Number", objs[i], CalculatorUtils.getValueType(objs[i]));
            }
            numVals[i] = (Number) objs[i];
        }
        return numVals;
    }

    public static String getValueType(final Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

}

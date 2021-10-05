package cgeo.geocaching.utils.calc;

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

    public static Value concat(final ValueList values) {
        values.checkCount(1, -1);

        if (values.size() == 1) {
            return values.get(0);
        }

        boolean containsString = false;
        final StringBuilder sb = new StringBuilder();
        for (Value v : values) {
            if (v.isDouble()) {
                if (v.isInteger()) {
                    sb.append(v.getAsInt());
                } else {
                    throw new CalculatorException(WRONG_TYPE, "integer", v, v.getType());
                }
            } else {
                containsString = true;
                sb.append(v.toString());
            }
        }
        return Value.of(containsString ? sb.toString() : Double.parseDouble(sb.toString()));
    }


    public static int random(final int max, final int min) {
        final int umax = max < 0 ? 10 : max;
        final int umin = Math.max(min, 0);
        return RANDOM.nextInt(umax - umin) + umin;
    }

    public static int checksum(final int value, final boolean iterative) {
        int result = Math.abs(value);
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

    public static String rot(final String value, final int rotate) {
        int rot = rotate == 0 ? 13 : rotate;
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

}

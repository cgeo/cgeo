/*
Copyright (c) 2007-2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
--
--
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.stdlib
*/
package cgeo.geocaching.wherigo.kahlua.stdlib;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.util.Locale;
import java.util.Random;

public enum MathLib implements JavaFunction {

    ABS,
    ACOS,
    ASIN,
    ATAN,
    ATAN2,
    CEIL,
    COS,
    COSH,
    DEG,
    EXP,
    FLOOR,
    FMOD,
    FREXP,
    LDEXP,
    LOG,
    LOG10,
    MODF,
    POW,
    RAD,
    RANDOM,
    RANDOMSEED,
    SIN,
    SINH,
    SQRT,
    TAN,
    TANH;

    public static final double EPS = 1e-15;

    private static final double LN10_INV = 1.0d / ln(10.0d);
    private static final double LN2_INV  = 1.0d / ln(2.0d);

    // atan polynomial constants
    private static final double SQ2P1 = 2.414213562373095048802e0;
    private static final double SQ2M1 = .414213562373095048802e0;
    private static final double P4    = .161536412982230228262e2;
    private static final double P3    = .26842548195503973794141e3;
    private static final double P2    = .11530293515404850115428136e4;
    private static final double P1    = .178040631643319697105464587e4;
    private static final double P0    = .89678597403663861959987488e3;
    private static final double Q4    = .5895697050844462222791e2;
    private static final double Q3    = .536265374031215315104235e3;
    private static final double Q2    = .16667838148816337184521798e4;
    private static final double Q1    = .207933497444540981287275926e4;
    private static final double Q0    = .89678597403663861962481162e3;
    private static final double PIO2  = 1.5707963267948966135E0;

    public static void register(final LuaState state) {
        final LuaTable math = new LuaTableImpl();
        state.getEnvironment().rawset("math", math);
        math.rawset("pi", LuaState.toDouble(Math.PI));
        math.rawset("huge", LuaState.toDouble(Double.POSITIVE_INFINITY));
        for (final MathLib f : values()) {
            math.rawset(f.name().toLowerCase(Locale.ROOT), f);
        }
    }

    @Override
    public String toString() {
        return "math." + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public int call(final LuaCallFrame callFrame, final int nArguments) {
        switch (this) {
            case ABS:        return abs(callFrame, nArguments);
            case ACOS:       return acos(callFrame, nArguments);
            case ASIN:       return asin(callFrame, nArguments);
            case ATAN:       return atan(callFrame, nArguments);
            case ATAN2:      return atan2(callFrame, nArguments);
            case CEIL:       return ceil(callFrame, nArguments);
            case COS:        return cos(callFrame, nArguments);
            case COSH:       return cosh(callFrame, nArguments);
            case DEG:        return deg(callFrame, nArguments);
            case EXP:        return expLua(callFrame, nArguments);
            case FLOOR:      return floor(callFrame, nArguments);
            case FMOD:       return fmod(callFrame, nArguments);
            case FREXP:      return frexp(callFrame, nArguments);
            case LDEXP:      return ldexp(callFrame, nArguments);
            case LOG:        return log(callFrame, nArguments);
            case LOG10:      return log10(callFrame, nArguments);
            case MODF:       return modf(callFrame, nArguments);
            case POW:        return powLua(callFrame, nArguments);
            case RAD:        return rad(callFrame, nArguments);
            case RANDOM:     return random(callFrame, nArguments);
            case RANDOMSEED: return randomseed(callFrame, nArguments);
            case SIN:        return sin(callFrame, nArguments);
            case SINH:       return sinh(callFrame, nArguments);
            case SQRT:       return sqrt(callFrame, nArguments);
            case TAN:        return tan(callFrame, nArguments);
            case TANH:       return tanh(callFrame, nArguments);
            default: return 0;
        }
    }

    private double getDoubleArg(final LuaCallFrame callFrame, final int argc) {
        final Double d = BaseLib.getArg(callFrame, argc, LuaType.NUMBER, name().toLowerCase(Locale.ROOT));
        return d.doubleValue();
    }

    // ---- Lua dispatch methods (instance, non-static) ----

    private int abs(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.abs(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int ceil(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.ceil(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int floor(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.floor(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int modf(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        double x = getDoubleArg(callFrame, 1);
        boolean negate = false;
        if (isNegative(x)) {
            negate = true;
            x = -x;
        }
        double intPart = Math.floor(x);
        double fracPart;
        if (Double.isInfinite(intPart)) {
            fracPart = 0.0d;
        } else {
            fracPart = x - intPart;
        }
        if (negate) {
            intPart = -intPart;
            fracPart = -fracPart;
        }
        callFrame.push(LuaState.toDouble(intPart), LuaState.toDouble(fracPart));
        return 2;
    }

    private int fmod(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "Not enough arguments");
        double v1 = getDoubleArg(callFrame, 1);
        double v2 = getDoubleArg(callFrame, 2);
        final double res;
        if (Double.isInfinite(v1) || Double.isNaN(v1)) {
            res = Double.NaN;
        } else if (Double.isInfinite(v2)) {
            res = v1;
        } else {
            v2 = Math.abs(v2);
            boolean negate = false;
            if (isNegative(v1)) {
                negate = true;
                v1 = -v1;
            }
            double r = v1 - Math.floor(v1 / v2) * v2;
            res = negate ? -r : r;
        }
        callFrame.push(LuaState.toDouble(res));
        return 1;
    }

    private int random(final LuaCallFrame callFrame, final int nArguments) {
        final Random rng = callFrame.thread.state.random;
        if (nArguments == 0) {
            callFrame.push(LuaState.toDouble(rng.nextDouble()));
            return 1;
        }
        double tmp = getDoubleArg(callFrame, 1);
        int m = (int) tmp;
        int n;
        if (nArguments == 1) {
            n = m;
            m = 1;
        } else {
            tmp = getDoubleArg(callFrame, 2);
            n = (int) tmp;
        }
        callFrame.push(LuaState.toDouble(m + rng.nextInt(n - m + 1)));
        return 1;
    }

    private int randomseed(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        final Object o = callFrame.get(0);
        if (o != null) {
            callFrame.thread.state.random.setSeed(o.hashCode());
        }
        return 0;
    }

    private int cosh(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        final double x = getDoubleArg(callFrame, 1);
        final double ex = exp(x);
        callFrame.push(LuaState.toDouble((ex + 1.0d / ex) * 0.5d));
        return 1;
    }

    private int sinh(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        final double x = getDoubleArg(callFrame, 1);
        final double ex = exp(x);
        callFrame.push(LuaState.toDouble((ex - 1.0d / ex) * 0.5d));
        return 1;
    }

    private int tanh(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        final double x = getDoubleArg(callFrame, 1);
        final double ex = exp(2.0d * x);
        callFrame.push(LuaState.toDouble((ex - 1.0d) / (ex + 1.0d)));
        return 1;
    }

    private int deg(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.toDegrees(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int rad(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.toRadians(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int acos(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(acos(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int asin(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(asin(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int atan(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(atan(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int atan2(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "Not enough arguments");
        callFrame.push(LuaState.toDouble(atan2(getDoubleArg(callFrame, 1), getDoubleArg(callFrame, 2))));
        return 1;
    }

    private int cos(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.cos(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int sin(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.sin(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int tan(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.tan(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int sqrt(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(Math.sqrt(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int expLua(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(exp(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int powLua(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "Not enough arguments");
        callFrame.push(LuaState.toDouble(pow(getDoubleArg(callFrame, 1), getDoubleArg(callFrame, 2))));
        return 1;
    }

    private int log(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(ln(getDoubleArg(callFrame, 1))));
        return 1;
    }

    private int log10(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        callFrame.push(LuaState.toDouble(ln(getDoubleArg(callFrame, 1)) * LN10_INV));
        return 1;
    }

    private int frexp(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "Not enough arguments");
        final double x = getDoubleArg(callFrame, 1);
        final double e;
        final double m;
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            e = 0.0d;
            m = x;
        } else {
            e = Math.ceil(ln(x) * LN2_INV);
            int div = 1 << ((int) e);
            m = x / div;
        }
        callFrame.push(LuaState.toDouble(m), LuaState.toDouble(e));
        return 2;
    }

    private int ldexp(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "Not enough arguments");
        final double mVal = getDoubleArg(callFrame, 1);
        final double dE  = getDoubleArg(callFrame, 2);
        final double ret;
        final double tmp = mVal + dE;
        if (Double.isInfinite(tmp) || Double.isNaN(tmp)) {
            ret = mVal;
        } else {
            ret = mVal * (1 << (int) dE);
        }
        callFrame.push(LuaState.toDouble(ret));
        return 1;
    }

    // ---- Public math utility methods (called from outside) ----

    public static boolean isNegative(final double vDouble) {
        return Double.doubleToLongBits(vDouble) < 0;
    }

    /**
     * Rounds towards even numbers.
     */
    public static double round(final double x) {
        if (x < 0) {
            return -round(-x);
        }
        final double x1 = x + 0.5d;
        final double x2 = Math.floor(x1);
        if (x2 == x1) {
            return x2 - ((long) x2 & 1);
        }
        return x2;
    }

    /**
     * Rounds to keep {@code precision} decimals. Rounds towards even numbers.
     */
    public static double roundToPrecision(final double x, final int precision) {
        final double roundingOffset = ipow(10, precision);
        return round(x * roundingOffset) / roundingOffset;
    }

    public static double roundToSignificantNumbers(final double x, final int precision) {
        if (x == 0) {
            return 0;
        }
        if (x < 0) {
            return -roundToSignificantNumbers(-x, precision);
        }
        final double lowerLimit = ipow(10.0d, precision - 1);
        final double upperLimit = lowerLimit * 10.0d;
        double multiplier = 1.0d;
        while (multiplier * x < lowerLimit) {
            multiplier *= 10.0d;
        }
        while (multiplier * x >= upperLimit) {
            multiplier /= 10.0d;
        }
        return round(x * multiplier) / multiplier;
    }

    public static double pow(final double base, final double exponent) {
        if ((int) exponent == exponent) {
            return ipow(base, (int) exponent);
        } else {
            return fpow(base, exponent);
        }
    }

    public static double ipow(final double base, final int exponent) {
        int e = exponent;
        boolean inverse = false;
        if (isNegative(e)) {
            e = -e;
            inverse = true;
        }
        double b = (e & 1) != 0 ? base : 1;
        double bb = base;
        for (e >>= 1; e != 0; e >>= 1) {
            bb *= bb;
            if ((e & 1) != 0) {
                b *= bb;
            }
        }
        return inverse ? 1.0d / b : b;
    }

    public static double atan2(final double arg1, final double arg2) {
        double a1 = arg1;
        double a2 = arg2;
        // both are 0 or arg1 is +/- inf
        if (a1 + a2 == a1) {
            if (a1 > 0) return PIO2;
            if (a1 < 0) return -PIO2;
            return 0.0d;
        }
        a1 = atan(a1 / a2);
        if (a2 < 0) {
            return a1 <= 0 ? a1 + Math.PI : a1 - Math.PI;
        }
        return a1;
    }

    // ---- Package-private math utility methods (internal use only) ----

    /** Taylor expansion of exp(x). */
    static double exp(final double x) {
        double x_acc = 1.0d;
        double div = 1.0d;
        double res = 0.0d;
        while (Math.abs(x_acc) > EPS) {
            res += x_acc;
            x_acc *= x;
            x_acc /= div;
            div++;
        }
        return res;
    }

    /** Natural logarithm (Taylor expansion). */
    static double ln(final double x) {
        if (x < 0) return Double.NaN;
        if (x == 0) return Double.NEGATIVE_INFINITY;
        if (Double.isInfinite(x)) return Double.POSITIVE_INFINITY;
        boolean negative = false;
        double xx = x;
        if (xx < 1) {
            negative = true;
            xx = 1.0d / xx;
        }
        int multiplier = 1;
        while (xx >= 1.1d) {
            multiplier *= 2;
            xx = Math.sqrt(xx);
        }
        double t = 1.0d - xx;
        double tpow = t;
        int divisor = 1;
        double result = 0.0d;
        double toSubtract;
        while (Math.abs(toSubtract = tpow / divisor) > EPS) {
            result -= toSubtract;
            tpow *= t;
            divisor++;
        }
        final double res = multiplier * result;
        return negative ? -res : res;
    }

    static double asin(final double arg) {
        double a = arg;
        int sign = 0;
        if (a < 0) {
            a = -a;
            sign++;
        }
        if (a > 1) return Double.NaN;
        double temp = Math.sqrt(1.0d - a * a);
        temp = a > 0.7d ? PIO2 - atan(temp / a) : atan(a / temp);
        return sign > 0 ? -temp : temp;
    }

    static double acos(final double arg) {
        if (arg > 1 || arg < -1) return Double.NaN;
        return PIO2 - asin(arg);
    }

    static double atan(final double arg) {
        return arg > 0 ? msatan(arg) : -msatan(-arg);
    }

    private static double fpow(final double base, final double exponent) {
        if (base < 0) return Double.NaN;
        return exp(exponent * ln(base));
    }

    private static double mxatan(final double arg) {
        final double argsq = arg * arg;
        double value = ((((P4 * argsq + P3) * argsq + P2) * argsq + P1) * argsq + P0);
        value = value / (((((argsq + Q4) * argsq + Q3) * argsq + Q2) * argsq + Q1) * argsq + Q0);
        return value * arg;
    }

    private static double msatan(final double arg) {
        if (arg < SQ2M1) return mxatan(arg);
        if (arg > SQ2P1) return PIO2 - mxatan(1.0d / arg);
        return PIO2 / 2.0d + mxatan((arg - 1.0d) / (arg + 1.0d));
    }
}

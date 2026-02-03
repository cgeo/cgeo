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

import androidx.annotation.NonNull;

import java.util.Locale;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

public enum StringLib implements JavaFunction {
    SUB,
    CHAR,
    BYTE,
    LOWER,
    UPPER,
    REVERSE,
    FORMAT,
    FIND,
    MATCH,
    GSUB;

    private static final boolean[] SPECIALS = new boolean[256];
    static {
        String s = "^$*+?.([%-";
        for (int i = 0; i < s.length(); i++) {
            SPECIALS[(int) s.charAt(i)] = true;
        }
    }

    private static final int LUA_MAXCAPTURES = 32;
    private static final char L_ESC = '%';
    private static final int CAP_UNFINISHED = ( -1 );
    private static final int CAP_POSITION = ( -2 );

    // NOTE: String.class won't work in J2ME - so this is used as a workaround
    public static final Class<?> STRING_CLASS = "".getClass();

    private static final char[] digits = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final String name;

    StringLib() {
        this.name = name().toLowerCase();
    }

    public static void register(LuaState state) {
        LuaTable string = new LuaTableImpl();
        state.getEnvironment().rawset("string", string);
        for (StringLib function : StringLib.values()) {
            string.rawset(function.name, function);
        }

        string.rawset("__index", string);
        state.setClassMetatable(STRING_CLASS, string);
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return switch (this) {
            case SUB -> sub(callFrame, nArguments);
            case CHAR -> stringChar(callFrame, nArguments);
            case BYTE -> stringByte(callFrame, nArguments);
            case LOWER -> lower(callFrame, nArguments);
            case UPPER -> upper(callFrame, nArguments);
            case REVERSE -> reverse(callFrame, nArguments);
            case FORMAT -> format(callFrame);
            case FIND -> findAux(callFrame, true);
            case MATCH -> findAux(callFrame, false);
            case GSUB -> gsub(callFrame);
        };
    }

    static long unsigned(long vv) {
        long v = vv;
        if (v < 0L) {
            v += (1L << 32);
        }
        return v;
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    static int format(LuaCallFrame callFrame) {
        String f = (String) BaseLib.getArg(callFrame, 1, BaseLib.TYPE_STRING, FORMAT.name);

        int len = f.length();
        int argc = 2;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = f.charAt(i);
            if (c == '%') {
                i++;
                BaseLib.luaAssert(i < len, "incomplete option to 'format'");
                c = f.charAt(i);
                if (c == '%') {
                    result.append('%');
                } else {
                    // Detect flags
                    boolean repr = false;
                    boolean zeroPadding = false;
                    boolean leftJustify = false;
                    boolean showPlus = false;
                    boolean spaceForSign = false;
                    flagLoop: while (true) {
                        switch (c) {
                        case '-':
                            leftJustify = true;
                            break;
                        case '+':
                            showPlus = true;
                            break;
                        case ' ':
                            spaceForSign = true;
                            break;
                        case '#':
                            repr = true;
                            break;
                        case '0':
                            zeroPadding = true;
                            break;
                        default:
                            break flagLoop;
                        }
                        i++;
                        BaseLib.luaAssert(i < len, "incomplete option to 'format'");
                        c = f.charAt(i);
                    }

                    // Detect width
                    int width = 0;
                    while (c >= '0' && c <= '9') {
                        width = 10 * width + (int) (c - '0');
                        i++;
                        BaseLib.luaAssert(i < len, "incomplete option to 'format'");
                        c = f.charAt(i);
                    }

                    // Detect precision
                    int precision = 0;
                    boolean hasPrecision = false;
                    if (c == '.') {
                        hasPrecision = true;
                        i++;
                        BaseLib.luaAssert(i < len, "incomplete option to 'format'");
                        c = f.charAt(i);

                        while (c >= '0' && c <= '9') {
                            precision = 10 * precision + (int) (c - '0');
                            i++;
                            BaseLib.luaAssert(i < len, "incomplete option to 'format'");
                            c = f.charAt(i);
                        }
                    }

                    if (leftJustify) {
                        zeroPadding = false;
                    }

                    // This will be overriden to space for the appropiate specifiers

                    // Pass 1: set up various variables needed for each specifier
                    // This simplifies the second pass by being able to combine several specifiers.

                    int base = 10;
                    boolean upperCase = false;
                    int defaultPrecision = 6; // This is the default for all float numerics
                    String basePrepend = "";
                    switch (c) {
                    // Simple character
                    case 'c':
                        zeroPadding = false;
                        break;
                    // change base
                    case 'o':
                        base = 8;
                        defaultPrecision = 1;
                        basePrepend = "0";
                        break;
                    case 'x':
                        base = 16;
                        defaultPrecision = 1;
                        basePrepend = "0x";
                        break;
                    case 'X':
                        base = 16;
                        defaultPrecision = 1;
                        upperCase = true;
                        basePrepend = "0X";
                        break;
                    // unsigned integer and signed integer
                    case 'u':
                        defaultPrecision = 1;
                        break;
                    case 'd':
                    case 'i':
                        defaultPrecision = 1;
                        break;
                    case 'e':
                        break;
                    case 'E':
                        upperCase = true;
                        break;
                    case 'g':
                        break;
                    case 'G':
                        upperCase = true;
                        break;
                    case 'f':
                        break;
                    case 's':
                        zeroPadding = false;
                        break;
                    case 'q':
                        // %q neither needs nor supports width
                        width = 0;
                        break;
                    default:
                        throw new IllegalStateException("invalid option '%" + c +
                        "' to 'format'");
                    }

                    // Set precision
                    if (!hasPrecision) {
                        precision = defaultPrecision;
                    }

                    if (hasPrecision && base != 10) {
                        zeroPadding = false;
                    }
                    char padCharacter = zeroPadding ? '0' : ' ';

                    // extend the string by "width" characters, and delete a subsection of them later to get the correct padding width
                    int resultStartLength = result.length();
                    if (!leftJustify) {
                        extend(result, width, padCharacter);
                    }

                    // Detect specifier and compute result
                    switch (c) {
                    case 'c':
                        result.append((char)(getDoubleArg(callFrame, argc)).shortValue());
                        break;
                    case 'o':
                    case 'x':
                    case 'X':
                    case 'u': {
                        long vLong = getDoubleArg(callFrame, argc).longValue();
                        vLong = unsigned(vLong);

                        if (repr) {
                            if (base == 8) {
                                int digits = 0;
                                long vLong2 = vLong;
                                while (vLong2 > 0) {
                                    vLong2 /= 8;
                                    digits++;
                                }
                                if (precision <= digits) {
                                    result.append(basePrepend);
                                }
                            } else if (base == 16 && vLong != 0) {
                                result.append(basePrepend);
                            }
                        }

                        if (vLong != 0 || precision > 0) {
                            stringBufferAppend(result, vLong, base, false, precision);
                        }
                        break;
                    }
                    case 'd':
                    case 'i': {
                        Double v = getDoubleArg(callFrame, argc);
                        long vLong = v.longValue();
                        if (vLong < 0) {
                            result.append('-');
                            vLong = -vLong;
                        } else if (showPlus) {
                            result.append('+');
                        } else if (spaceForSign) {
                            result.append(' ');
                        }
                        if (vLong != 0 || precision > 0) {
                            stringBufferAppend(result, vLong, base, false, precision);
                        }
                        break;
                    }
                    case 'e':
                    case 'E':
                    case 'f': {
                        Double v = getDoubleArg(callFrame, argc);
                        boolean isNaN = v.isInfinite() || v.isNaN();

                        double vDouble = v.doubleValue();
                        if (MathLib.isNegative(vDouble)) {
                            if (!isNaN) {
                                result.append('-');
                            }
                            vDouble = -vDouble;
                        } else if (showPlus) {
                            result.append('+');
                        } else if (spaceForSign) {
                            result.append(' ');
                        }
                        if (isNaN) {
                            result.append(BaseLib.numberToString(v));
                        } else {
                            if (c == 'f') {
                                appendPrecisionNumber(result, vDouble, precision, repr);
                            } else {
                                appendScientificNumber(result, vDouble, precision, repr, false);
                            }
                        }
                        break;
                    }
                    case 'g':
                    case 'G':
                    {
                        // Precision is significant digits for %g
                        if (precision <= 0) {
                            precision = 1;
                        }

                        // first round to correct significant digits (precision),
                        // then check which formatting to be used.
                        Double v = getDoubleArg(callFrame, argc);
                        boolean isNaN = v.isInfinite() || v.isNaN();
                        double vDouble = v.doubleValue();
                        if (MathLib.isNegative(vDouble)) {
                            if (!isNaN) {
                                result.append('-');
                            }
                            vDouble = -vDouble;
                        } else if (showPlus) {
                            result.append('+');
                        } else if (spaceForSign) {
                            result.append(' ');
                        }
                        if (isNaN) {
                            result.append(BaseLib.numberToString(v));
                        } else {
                            double x = MathLib.roundToSignificantNumbers(vDouble, precision);

                            /*
                             * Choose %f version if:
                             *     |v| >= 10^(-4)
                             * AND
                             *     |v| < 10^(precision)
                             *
                             * otherwise, choose %e
                             */
                            if (x == 0 || (x >= 1e-4 && x < MathLib.ipow(10, precision))) {
                                int iPartSize;
                                if (x == 0) {
                                    iPartSize = 1;
                                } else if (Math.floor(x) == 0) {
                                    iPartSize = 0;
                                } else {
                                    double longValue = x;
                                    iPartSize = 1;
                                    while (longValue >= 10.0) {
                                        longValue /= 10.0;
                                        iPartSize++;
                                    }
                                }
                                // format with %f, with precision significant numbers
                                appendSignificantNumber(result, x, precision - iPartSize, repr);
                            } else {
                                // format with %e, with precision significant numbers, i.e. precision -1 digits
                                // but skip trailing zeros unless repr
                                appendScientificNumber(result, x, precision - 1, repr, true);
                            }
                        }
                        break;
                    }
                    case 's': {
                        String s = getStringArg(callFrame, argc);
                        int n = s.length();
                        if (hasPrecision) {
                            n = Math.min(precision, s.length());
                        }
                        append(result, s, 0, n);
                        break;
                    }
                    case 'q':
                        String q = getStringArg(callFrame, argc);
                        result.append('"');
                        for (int j = 0; j < q.length(); j++) {
                            char d = q.charAt(j);
                            switch (d) {
                            case '\\': result.append("\\"); break;
                            case '\n': result.append("\\\n"); break;
                            case '\r': result.append("\\r"); break;
                            case '"': result.append("\\\""); break;
                            default: result.append(d);
                            }
                        }
                        result.append('"');
                        break;
                    default:
                        throw new IllegalStateException("Internal error");
                    }
                    if (leftJustify) {
                        int currentResultLength = result.length();
                        int d = width - (currentResultLength - resultStartLength);
                        if (d > 0) {
                            extend(result, d, ' ');
                        }
                    } else {
                        int currentResultLength = result.length();
                        int d = currentResultLength - resultStartLength - width;
                        d = Math.min(d, width);
                        if (d > 0) {
                            result.delete(resultStartLength, resultStartLength + d);
                        }
                        if (zeroPadding) {
                            int signPos = resultStartLength + (width - d);
                            char ch = result.charAt(signPos);
                            if (ch == '+' || ch == '-' || ch == ' ') {
                                result.setCharAt(signPos, '0');
                                result.setCharAt(resultStartLength, ch);
                            }
                        }
                    }
                    if (upperCase) {
                        stringBufferUpperCase(result, resultStartLength);
                    }
                    argc++;
                }
            } else {
                result.append(c);
            }
        }
        callFrame.push(result.toString());
        return 1;
    }

    static void append(StringBuilder buffer, String s, int start, int end) {
        for (int i = start; i < end; i++) {
            buffer.append(s.charAt(i));
        }
    }

    static void extend(StringBuilder buffer, int extraWidth, char padCharacter) {
        int preLength = buffer.length();
        buffer.setLength(preLength + extraWidth);
        for (int i = extraWidth - 1; i >= 0; i--) {
            buffer.setCharAt(preLength + i, padCharacter);
        }
    }

    static void stringBufferUpperCase(StringBuilder buffer, int start) {
        int length = buffer.length();
        for (int i = start; i < length; i++) {
            char c = buffer.charAt(i);
            if (c >= 'a' && c <= 'z') {
                buffer.setCharAt(i, (char) (c - 32));
            }
        }
    }

    /**
     * Precondition: value >= 0
     * Precondition: 2 <= base <= 16
     * @param sb the stringbuffer to append to
     * @param pValue the value to append
     * @param base the base to use when formatting (typically 8, 10 or 16)
     * @param mminDigits min digits
     */
    private static void stringBufferAppend(StringBuilder sb, double pValue, int base, boolean printZero, int mminDigits) {
        double value = pValue;
        int startPos = sb.length();
        int minDigits = mminDigits;
        while (value > 0 || minDigits > 0) {
            double newValue = Math.floor(value / base);
            sb.append(digits[(int) (value - (newValue * base))]);
            value = newValue;
            minDigits--;
        }
        int endPos = sb.length() - 1;
        if (startPos > endPos && printZero) {
            sb.append('0');
        } else {
            // Note that the digits are in reverse order now, so we need to correct it.
            // We can't use StringBuilder.reverse because that reverses the entire string

            int swapCount = (1 + endPos - startPos) / 2;
            for (int i = swapCount - 1; i >= 0; i--) {
                int leftPos = startPos + i;
                int rightPos = endPos - i;
                char left = sb.charAt(leftPos);
                char right = sb.charAt(rightPos);
                sb.setCharAt(leftPos, right);
                sb.setCharAt(rightPos, left);
            }
        }
    }

    /**
     * Only works with non-negative numbers
     */
    static void appendPrecisionNumber(StringBuilder buffer, double pNumber, int precision, boolean requirePeriod) {
        double number = MathLib.roundToPrecision(pNumber, precision);
        double iPart = Math.floor(number);
        double fPart = number - iPart;

        for (int i = 0; i < precision; i++) {
            fPart *= 10.0;
        }
        fPart = MathLib.round(iPart + fPart) - iPart;

        stringBufferAppend(buffer, iPart, 10, true, 0);

        if (requirePeriod || precision > 0) {
            buffer.append('.');
        }

        stringBufferAppend(buffer, fPart, 10, false, precision);
    }

    /**
     * Only works with non-negative numbers
     */
    static void appendSignificantNumber(StringBuilder buffer, double number, int pSignificantDecimals, boolean includeTrailingZeros) {
        int significantDecimals = pSignificantDecimals;
        double iPart = Math.floor(number);

        stringBufferAppend(buffer, iPart, 10, true, 0);

        double fPart = MathLib.roundToSignificantNumbers(number - iPart, significantDecimals);

        boolean hasNotStarted = iPart == 0 && fPart != 0;
        int zeroPaddingBefore = 0;
        int scanLength = significantDecimals;
        for (int i = 0; i < scanLength; i++) {
            fPart *= 10.0;
            if (Math.floor(fPart) == 0 && fPart != 0) {
                zeroPaddingBefore++;
                if (hasNotStarted) {
                    scanLength++;
                }
            }
        }
        fPart = MathLib.round(fPart);

        if (!includeTrailingZeros) {
            while (fPart > 0 && (fPart % 10) == 0) {
                fPart /= 10;
                significantDecimals--;
            }
        }

        buffer.append('.');
        int periodPos = buffer.length();
        extend(buffer, zeroPaddingBefore, '0');
        int prePos = buffer.length();
        stringBufferAppend(buffer, fPart, 10, false, 0);
        int postPos = buffer.length();

        int len = postPos - prePos;
        if (includeTrailingZeros && len < significantDecimals) {
            int padRightSize = significantDecimals - len - zeroPaddingBefore;
            extend(buffer, padRightSize, '0');
        }

        if (!includeTrailingZeros && periodPos == buffer.length()) {
            buffer.delete(periodPos - 1, buffer.length());
        }
    }

    static void appendScientificNumber(StringBuilder buffer, double xx, int precision, boolean repr, boolean useSignificantNumbers) {
        int exponent = 0;
        double x = xx;

        // Run two passes to handle cases such as %.2e with the value 95.
        for (int i = 0; i < 2; i++) {
            if (x >= 1.0) {
                while (x >= 10.0) {
                    x /= 10.0;
                    exponent++;
                }
            } else {
                while (x > 0 && x < 1.0) {
                    x *= 10.0;
                    exponent--;
                }
            }
            x = MathLib.roundToPrecision(x, precision);
        }
        int absExponent = Math.abs(exponent);
        char expSign;
        if (exponent >= 0) {
            expSign = '+';
        } else {
            expSign = '-';
        }
        if (useSignificantNumbers) {
            appendSignificantNumber(buffer, x, precision, repr);
        } else {
            appendPrecisionNumber(buffer, x, precision, repr);
        }
        buffer.append('e');
        buffer.append(expSign);
        stringBufferAppend(buffer, absExponent, 10, true, 2);
    }

    static String getStringArg(LuaCallFrame callFrame, int argc) {
        return getStringArg(callFrame, argc, FORMAT.name);
    }

    static String getStringArg(LuaCallFrame callFrame, int argc, String funcname) {
        return (String) BaseLib.getArg(callFrame, argc, BaseLib.TYPE_STRING, funcname);
    }

    static Double getDoubleArg(LuaCallFrame callFrame, int argc) {
        return getDoubleArg(callFrame, argc, FORMAT.name);
    }

    static Double getDoubleArg(LuaCallFrame callFrame, int argc, String funcname) {
        return (Double)BaseLib.getArg(callFrame, argc, BaseLib.TYPE_NUMBER, funcname);
    }

    static int lower(LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        String s = getStringArg(callFrame,1,LOWER.name);

        callFrame.push(s.toLowerCase(Locale.getDefault()));
        return 1;
    }

    static int upper(LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        String s = getStringArg(callFrame,1,UPPER.name);

        callFrame.push(s.toUpperCase(Locale.getDefault()));
        return 1;
    }

    static int reverse(LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        String s = getStringArg(callFrame, 1, REVERSE.name);
        s = new StringBuilder(s).reverse().toString();
        callFrame.push(s);
        return 1;
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    static int stringByte(LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        String s = getStringArg(callFrame, 1, BYTE.name);

        Double di = null;
        Double dj = null;
        if (nArguments >= 2) {
            di = getDoubleArg(callFrame, 2, BYTE.name);
            if (nArguments >= 3) {
                dj = getDoubleArg(callFrame, 3, BYTE.name);
            }
        }
        double di2 = 1;
        if (di != null) {
            di2 = LuaState.fromDouble(di);
        }
        double dj2 = di2;
        if (dj != null) {
            dj2 = LuaState.fromDouble(dj);
        }

        int ii = (int) di2;
        int ij = (int) dj2;

        int len = s.length();
        if (ii < 0) {
            ii += len + 1;
        }
        if (ii <= 0) {
            ii = 1;
        }
        if (ij < 0) {
            ij += len + 1;
        } else if (ij > len) {
            ij = len;
        }
        int nReturns = 1 +ij - ii;

        if (nReturns <= 0) {
            return 0;
        }
        callFrame.setTop(nReturns);
        int offset = ii - 1;
        for (int i = 0; i < nReturns; i++) {
            char c = s.charAt(offset + i);
            callFrame.set(i, new Double((double) c));
        }
        return nReturns;
    }

    static int stringChar(LuaCallFrame callFrame, int nArguments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nArguments; i++) {
            int num = getDoubleArg(callFrame, i + 1, CHAR.name).intValue();
            sb.append((char) num);
        }
        return callFrame.push(sb.toString());
    }

    static int sub(LuaCallFrame callFrame, int nArguments) {
        String s = getStringArg(callFrame, 1, SUB.name);
        double start = getDoubleArg(callFrame, 2, SUB.name).doubleValue();
        double end = -1;
        if (nArguments >= 3) {
            end = getDoubleArg(callFrame, 3, SUB.name).doubleValue();
        }
        String res;
        int istart = (int) start;
        int iend = (int) end;

        int len = s.length();
        if (istart < 0) {
            istart = Math.max(len + istart + 1, 1);
        } else if (istart == 0) {
            istart = 1;
        }


        if (iend < 0) {
            iend = Math.max(0, iend + len + 1);
        } else if (iend > len) {
            iend = len;
        }

        if (istart > iend) {
            return callFrame.push("");
        }
        res = s.substring(istart - 1, iend);

        return callFrame.push(res);
    }

    /* Pattern Matching
     * Original code that this was adapted from is copyright (c) 2008 groundspeak, inc.
     */

    public static class MatchState {

        public StringPointer src_init;  /* init of source string */

        public int endIndex; /* end (`\0') of source string */

        public LuaCallFrame callFrame;
        public int level;  /* total number of captures (finished or unfinished) */

        public Capture[] capture;

        public static class Capture {

            public StringPointer init;
            public int len;
        }

        public MatchState () {
            capture = new Capture[ LUA_MAXCAPTURES ];
            for ( int i = 0; i < LUA_MAXCAPTURES; i ++ ) {
                capture[i] = new Capture ();
            }
        }

        public Object[] getCaptures() {
            if (level <= 0) {
                return null;
            }
            Object[] caps = new String[level];
            for (int i = 0; i < level; i++) {
                if (capture[i].len == CAP_POSITION) {
                    caps[i] = new Double(src_init.length() - capture[i].init.length() + 1);
                } else {
                    caps[i] = capture[i].init.getString().substring(0, capture[i].len);
                }
            }
            return caps;
        }
    }

    public static class StringPointer {

        private String string;
        private int index = 0;

        public StringPointer(String original) {
            this.string = original;
        }

        public StringPointer(String original, int index) {
            this.string = original;
            this.index = index;
        }

        public StringPointer getClone() {
            StringPointer newSP = new StringPointer( this.getOriginalString(), this.getIndex() );
            return newSP;
        }

        public int getIndex () {
            return index;
        }

        public void setIndex ( int ind ) {
            index = ind;
        }

        public String getOriginalString () {
            return string;
        }

        public void setOriginalString(String orStr) {
            string = orStr;
        }

        public String getString() {
            return getString(0);
        }

        public String getString(int i) {
            return string.substring ( index + i, string.length () );
        }

        public char getChar() {
            return getChar(0);
        }

        public char getChar(int strIndex) {
            if ( index + strIndex >= string.length () )
                return '\0';
            else
                return string.charAt ( index + strIndex );
        }

        public int length() {
            return string.length () - index;
        }

        public int postIncrStringI ( int num ) {
            int oldIndex = index;
            index += num;
            return oldIndex;
        }

        public int preIncrStringI ( int num ) {
            index += num;
            return index;
        }

        public char postIncrString ( int num ) {
            char c = getChar();
            index += num;
            return c;
        }

        public char preIncrString ( int num ) {
            index += num;
            return getChar();
        }

        public int compareTo(StringPointer cmp, int len) {
            return this.string.substring(this.index,this.index+len).compareTo(
                    cmp.string.substring(cmp.index, cmp.index+len));
        }
    }

    private static Object pushOnecapture(MatchState ms, int i, StringPointer s, StringPointer e ) {
        if (i >= ms.level) {
            if ( i == 0 ) { // ms->level == 0, too
                String res = s.string.substring(s.index, e.index);
                ms.callFrame.push(res);
                return res;
            } else {
                throw new IllegalStateException("invalid capture index");
            }
        } else {
            int l = ms.capture[i].len;
            if (l == CAP_UNFINISHED) {
                throw new IllegalStateException("unfinished capture");
            } else if (l == CAP_POSITION) {
                Double res = new Double(ms.src_init.length() - ms.capture[i].init.length() + 1);
                ms.callFrame.push(res);
                return res;
            } else {
                int index = ms.capture[i].init.index;
                String res = ms.capture[i].init.string.substring(index, index+l);
                ms.callFrame.push(res);
                return res;
            }
        }
    }

    private static int pushCaptures(MatchState ms, StringPointer s, StringPointer e ) {
        int nlevels = ( ms.level == 0 && s != null ) ? 1 : ms.level;
        BaseLib.luaAssert(nlevels <= LUA_MAXCAPTURES, "too many captures");
        for (int i = 0; i < nlevels; i++) {
            pushOnecapture(ms, i, s, e);
        }
        return nlevels;  // number of strings pushed
    }

    private static boolean noSpecialChars(String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c < 256 && SPECIALS[c]) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private static int findAux (LuaCallFrame callFrame, boolean find ) {
        String f = find ? FIND.name : MATCH.name;
        String source = (String) BaseLib.getArg(callFrame, 1, BaseLib.TYPE_STRING, f);
        String pattern = (String) BaseLib.getArg(callFrame, 2, BaseLib.TYPE_STRING, f);
        Double i = ((Double)(BaseLib.getOptArg(callFrame, 3, BaseLib.TYPE_NUMBER)));
        boolean plain = LuaState.boolEval(BaseLib.getOptArg(callFrame, 4, BaseLib.TYPE_BOOLEAN));
        int init = (i == null ? 0 : i.intValue() - 1);

        if ( init < 0 ) {
            // negative numbers count back from the end of the string.
            init += source.length();
            if ( init < 0 ) {
                init = 0; // if we are still negative, just start at the beginning.
            }
        } else if ( init > source.length() ) {
            init = source.length();
        }

        if ( find && ( plain || noSpecialChars(pattern) ) ) { // explicit plain request or no special characters?
            // do a plain search
            int pos = source.indexOf(pattern, init);
            if ( pos > -1 ) {
                return callFrame.push(LuaState.toDouble(pos + 1), LuaState.toDouble(pos + pattern.length()));
            }
        } else {
            StringPointer s = new StringPointer(source);
            StringPointer p = new StringPointer(pattern);

            MatchState ms = new MatchState ();
            boolean anchor = false;
            if ( p.getChar () == '^' ) {
                anchor = true;
                p.postIncrString ( 1 );
            }
            StringPointer s1 = s.getClone();
            s1.postIncrString ( init );

            ms.callFrame = callFrame;
            ms.src_init = s.getClone();
            ms.endIndex = s.getString().length();
            do {
                StringPointer res;
                ms.level = 0;
                if ( ( res = match ( ms, s1, p ) ) != null ) {
                    if ( find ) {
                        return callFrame.push(new Double(s.length () - s1.length () + 1), new Double(s.length () - res.length ())) +
                        pushCaptures( ms, null, null );
                    } else {
                        return pushCaptures( ms, s1, res );
                    }
                }

            } while ( s1.postIncrStringI ( 1 ) < ms.endIndex && !anchor );
        }
        return callFrame.pushNil();  // not found
    }

    private static StringPointer startCapture ( MatchState ms, StringPointer s, StringPointer p, int what ) {
        StringPointer res;
        int level = ms.level;
        BaseLib.luaAssert(level < LUA_MAXCAPTURES, "too many captures");

        ms.capture[level].init = s.getClone();
        ms.capture[level].init.setIndex ( s.getIndex () );
        ms.capture[level].len = what;
        ms.level = level + 1;
        if ( ( res = match ( ms, s, p ) ) == null ) /* match failed? */ {
            ms.level --;  /* undo capture */
        }
        return res;
    }

    private static int captureToClose ( MatchState ms ) {
        int level = ms.level;
        for ( level --; level >= 0; level -- ) {
            if ( ms.capture[level].len == CAP_UNFINISHED ) {
                return level;
            }
        }
        throw new IllegalStateException("invalid pattern capture");
    }

    private static StringPointer endCapture ( MatchState ms, StringPointer s, StringPointer p ) {
        int l = captureToClose ( ms );
        StringPointer res;
        ms.capture[l].len = ms.capture[l].init.length () - s.length ();  /* close capture */
        if ( ( res = match ( ms, s, p ) ) == null ) /* match failed? */ {
            ms.capture[l].len = CAP_UNFINISHED;  /* undo capture */
        }
        return res;
    }

    private static int checkCapture ( MatchState ms, int ll ) {
        int l = ll - '1'; // convert chars 1-9 to actual ints 1-9
        BaseLib.luaAssert(l < 0 || l >= ms.level || ms.capture[l].len == CAP_UNFINISHED,
        "invalid capture index");
        return l;
    }

    private static StringPointer matchCapture ( MatchState ms, StringPointer s, int ll ) {
        int l = checkCapture ( ms, ll );
        int len = ms.capture[l].len;
        if ( ( ms.endIndex - s.length () ) >= len && ms.capture[l].init.compareTo(s, len) == 0 ) {
            StringPointer sp = s.getClone();
            sp.postIncrString ( len );
            return sp;
        }
        else {
            return null;
        }
    }

    private static StringPointer matchBalance ( MatchState ms, StringPointer ss, StringPointer p ) {

        BaseLib.luaAssert(!(p.getChar () == 0 || p.getChar ( 1 ) == 0), "unbalanced pattern");

        StringPointer s = ss.getClone();
        if ( s.getChar () != p.getChar () ) {
            return null;
        } else {
            int b = p.getChar ();
            int e = p.getChar ( 1 );
            int cont = 1;

            while ( s.preIncrStringI ( 1 ) < ms.endIndex ) {
                if ( s.getChar () == e ) {
                    if (  -- cont == 0 ) {
                        StringPointer sp = s.getClone();
                        sp.postIncrString ( 1 );
                        return sp;
                    }
                } else if ( s.getChar () == b ) {
                    cont ++;
                }
            }
        }
        return null;  /* string ends out of balance */
    }

    private static StringPointer classEnd ( StringPointer pp ) {
        StringPointer p = pp.getClone();
        switch ( p.postIncrString ( 1 ) ) {
        case L_ESC: {
            BaseLib.luaAssert(p.getChar () != '\0', "malformed pattern (ends with '%')");
            p.postIncrString ( 1 );
            return p;
        }
        case '[': {
            if ( p.getChar () == '^' ) {
                p.postIncrString ( 1 );
            }
            do { // look for a `]'
                BaseLib.luaAssert(p.getChar () != '\0', "malformed pattern (missing ']')");

                if ( p.postIncrString ( 1 ) == L_ESC && p.getChar () != '\0' ) {
                    p.postIncrString ( 1 );  // skip escapes (e.g. `%]')
                }

            } while ( p.getChar () != ']' );

            p.postIncrString ( 1 );
            return p;
        }
        default: {
            return p;
        }
        }
    }

    private static boolean singleMatch ( char c, StringPointer p, StringPointer ep ) {
        switch ( p.getChar () ) {
        case '.':
            return true;  // matches any char
        case L_ESC:
            return matchClass ( p.getChar ( 1 ), c );
        case '[': {
            StringPointer sp = ep.getClone();
            sp.postIncrString ( -1 );
            return matchBracketClass ( c, p, sp );
        }
        default:
            return ( p.getChar () == c );
        }
    }

    private static StringPointer minExpand ( MatchState ms, StringPointer ss, StringPointer p, StringPointer ep ) {
        StringPointer sp = ep.getClone();
        StringPointer s = ss.getClone();

        sp.postIncrString ( 1 );
        while (true) {
            StringPointer res = match ( ms, s, sp );
            if ( res != null ) {
                return res;
            } else if ( s.getIndex () < ms.endIndex && singleMatch ( s.getChar (), p, ep ) ) {
                s.postIncrString ( 1 );  // try with one more repetition
            } else {
                return null;
            }
        }
    }

    private static StringPointer maxExpand(MatchState ms, StringPointer s, StringPointer p, StringPointer ep) {
        int i = 0;  // counts maximum expand for item
        while (s.getIndex () + i < ms.endIndex && singleMatch(s.getChar(i), p, ep)) {
            i ++;
        }
        // keeps trying to match with the maximum repetitions
        while (i >= 0) {
            StringPointer sp1 = s.getClone();
            sp1.postIncrString(i);
            StringPointer sp2 = ep.getClone();
            sp2.postIncrString(1);
            StringPointer res = match(ms, sp1, sp2);
            if (res != null) {
                return res;
            }
            i --;  // else didn't match; reduce 1 repetition to try again
        }
        return null;
    }

    private static boolean matchBracketClass(char c, StringPointer pp, StringPointer ecc) {
        StringPointer p = pp.getClone();
        StringPointer ec = ecc.getClone();
        boolean sig = true;
        if (p.getChar(1) == '^') {
            sig = false;
            p.postIncrString(1);  // skip the `^'
        }
        while (p.preIncrStringI(1) < ec.getIndex()) {
            if (p.getChar() == L_ESC) {
                p.postIncrString(1);
                if (matchClass(p.getChar(), c)) {
                    return sig;
                }
            } else if ((p.getChar(1) == '-') && (p.getIndex() + 2 < ec.getIndex())) {
                p.postIncrString(2);
                if (p.getChar(-2) <= c && c <= p.getChar()) {
                    return sig;
                }
            } else if (p.getChar () == c) {
                return sig;
            }
        }
        return !sig;
    }

    private static StringPointer match(MatchState ms, StringPointer ss, StringPointer pp) {
        StringPointer s = ss.getClone();
        StringPointer p = pp.getClone();
        boolean isContinue = true;
        boolean isDefault = false;
        while (isContinue) {
            isContinue = false;
            isDefault = false;
            switch (p.getChar()) {
            case '(': { // start capture
                StringPointer p1 = p.getClone();
                if (p.getChar(1) == ')') { // position capture?
                    p1.postIncrString(2);
                    return startCapture(ms, s, p1, CAP_POSITION);
                } else {
                    p1.postIncrString(1);
                    return startCapture(ms, s, p1, CAP_UNFINISHED);
                }
            }
            case ')': { // end capture
                StringPointer p1 = p.getClone();
                p1.postIncrString(1);
                return endCapture(ms, s, p1);
            }
            case L_ESC: {
                switch (p.getChar(1)) {
                case 'b': { // balanced string?
                    StringPointer p1 = p.getClone();
                    p1.postIncrString(2);
                    s = matchBalance(ms, s, p1);
                    if (s == null) {
                        return null;
                    }
                    p.postIncrString(4);
                    isContinue = true;
                    continue; // else return match(ms, s, p+4);
                }
                case 'f': { // frontier?
                    p.postIncrString (2);
                    BaseLib.luaAssert(p.getChar() == '[' , "missing '[' after '%%f' in pattern");

                    StringPointer ep = classEnd(p);  // points to what is next
                    char previous = (s.getIndex() == ms.src_init.getIndex()) ? '\0' : s.getChar(-1);

                    StringPointer ep1 = ep.getClone();
                    ep1.postIncrString(-1);
                    if (matchBracketClass(previous, p, ep1) || !matchBracketClass(s.getChar(), p, ep1)) {
                        return null;
                    }
                    p = ep;
                    isContinue = true;
                    continue; // else return match(ms, s, ep);
                }
                default: {
                    if (Character.isDigit(p.getChar(1))) { // capture results (%0-%9)?
                        s = matchCapture(ms, s, p.getChar(1));
                        if (s == null) {
                            return null;
                        }
                        p.postIncrString(2);
                        isContinue = true;
                        continue; // else return match(ms, s, p+2)
                    }
                    isDefault = true; // case default
                }
                }
                break;
            }
            case '\0': {  // end of pattern
                return s;  // match succeeded
            }
            case '$': {
                if (p.getChar(1) == '\0') { // is the `$' the last char in pattern?
                    return (s.getIndex() == ms.endIndex) ? s : null;  // check end of string
                }
            }
            default: { // it is a pattern item
                isDefault = true;
            }
            }

            if (isDefault) { // it is a pattern item
                isDefault = false;
                StringPointer ep = classEnd(p);  // points to what is next
                boolean m = (s.getIndex () < ms.endIndex && singleMatch(s.getChar(), p, ep));
                switch (ep.getChar()) {
                case '?':  { // optional
                    StringPointer res;
                    StringPointer s1 = s.getClone();
                    s1.postIncrString ( 1 );
                    StringPointer ep1 = ep.getClone();
                    ep1.postIncrString ( 1 );

                    if (m && ((res = match(ms, s1, ep1)) != null)) {
                        return res;
                    }
                    p = ep;
                    p.postIncrString(1);
                    isContinue = true;
                    continue; // else return match(ms, s, ep+1);
                }
                case '*': { // 0 or more repetitions
                    return maxExpand(ms, s, p, ep);
                }
                case '+': { // 1 or more repetitions
                    StringPointer s1 = s.getClone();
                    s1.postIncrString(1);
                    return (m ? maxExpand(ms, s1, p, ep) : null);
                }
                case '-': { // 0 or more repetitions (minimum)
                    return minExpand(ms, s, p, ep);
                }
                default: {
                    if (!m) {
                        return null;
                    }
                    s.postIncrString(1);

                    p = ep;
                    isContinue = true;
                    continue; // else return match(ms, s+1, ep);
                }
                }
            }
        }
        return null;
    }

    private static boolean matchClass(char classIdentifier, char c) {
        boolean res;
        char lowerClassIdentifier = Character.toLowerCase(classIdentifier);
        switch (lowerClassIdentifier) {
        case 'a': res = Character.isLowerCase(c) || Character.isUpperCase(c); break;
        case 'c': res = isControl(c); break;
        case 'd': res = Character.isDigit(c); break;
        case 'l': res = Character.isLowerCase(c); break;
        case 'p': res = isPunct(c); break;
        case 's': res = isSpace(c); break;
        case 'u': res = Character.isUpperCase(c); break;
        case 'w': res = Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c); break;
        case 'x': res = isHex(c); break;
        case 'z': res = (c == 0); break;
        default: return (classIdentifier == c);
        }
        return (lowerClassIdentifier == classIdentifier) == res;
    }

    private static boolean isPunct(char c) {
        return ( c >= 0x21 && c <= 0x2F ) ||
        ( c >= 0x3a && c <= 0x40 ) ||
        ( c >= 0x5B && c <= 0x60 ) ||
        ( c >= 0x7B && c <= 0x7E );
    }

    private static boolean isSpace(char c) {
        return ( c >= 0x09 && c <= 0x0D ) || c == 0x20 ;
    }

    private static boolean isControl(char c) {
        return ( c >= 0x00 && c <= 0x1f ) || c == 0x7f;
    }

    private static boolean isHex(char c) {
        return ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'f' ) || ( c >= 'A' && c <= 'F' );
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private static int gsub(LuaCallFrame cf) {
        String srcTemp = (String)BaseLib.getArg(cf, 1, BaseLib.TYPE_STRING, GSUB.name);
        String pTemp = (String)BaseLib.getArg(cf, 2, BaseLib.TYPE_STRING, GSUB.name);
        Object repl = BaseLib.getArg(cf, 3, null, GSUB.name);
        {
            String tmp = BaseLib.rawTostring(repl);
            if (tmp != null) {
                repl = tmp;
            }
        }
        Double num = (Double)BaseLib.getOptArg(cf, 4, BaseLib.TYPE_NUMBER);
        // if i isn't supplied, we want to substitute all occurrences of the pattern
        int maxSubstitutions = (num == null) ? Integer.MAX_VALUE : num.intValue();

        StringPointer pattern = new StringPointer (pTemp);
        StringPointer src = new StringPointer (srcTemp);

        boolean anchor = false;
        if (pattern.getChar() == '^') {
            anchor = true;
            pattern.postIncrString ( 1 );
        }

        String replType = BaseLib.type(repl);
        if (!(replType == BaseLib.TYPE_FUNCTION ||
                        replType == BaseLib.TYPE_STRING ||
                        replType == BaseLib.TYPE_TABLE)) {
            BaseLib.fail(("string/function/table expected, got "+replType));
        }

        MatchState ms = new MatchState ();
        ms.callFrame = cf;
        ms.src_init = src.getClone();
        ms.endIndex = src.length();

        int n = 0;
        StringBuilder b = new StringBuilder();
        StringPointer e = null;
        while (n < maxSubstitutions) {
            ms.level = 0;
            e = match(ms, src, pattern);
            if (e != null) {
                n++;
                addValue(ms, repl, b, src, e);
            }

            if (e != null && e.getIndex() > src.getIndex()) { // non empty match?
                src.setIndex (e.getIndex());  // skip it
            } else if (src.getIndex() < ms.endIndex) {
                b.append(src.postIncrString(1));
            } else {
                break;
            }

            if (anchor) {
                break;
            }
        }
        return cf.push(b.append(src.getString()).toString(), new Double(n));
    }

    private static void addValue(MatchState ms, Object repl, StringBuilder b, StringPointer src, StringPointer e) {
        String type = BaseLib.type(repl);
        if (type == BaseLib.TYPE_NUMBER || type == BaseLib.TYPE_STRING) {
            b.append(addString (ms, repl, src, e));
        } else {
            String match = src.getString().substring(0, e.getIndex() - src.getIndex());
            Object[] captures = ms.getCaptures();
            if (captures != null) {
                match = BaseLib.rawTostring(captures[0]);
            }
            Object res = null;
            if (type == BaseLib.TYPE_FUNCTION) {
                res = ms.callFrame.thread.state.call(repl, match, null, null);
            } else if (type == BaseLib.TYPE_TABLE) {
                res = ((LuaTable)repl).rawget(match);
            }
            if (res == null) {
                res = match;
            }
            b.append(BaseLib.rawTostring(res));
        }
    }

    private static String addString(MatchState ms, Object repl, StringPointer s, StringPointer e) {
        String replTemp = BaseLib.tostring(repl, ms.callFrame.thread.state);
        StringPointer replStr = new StringPointer (replTemp);
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < replTemp.length(); i++) {
            if (replStr.getChar ( i ) != L_ESC) {
                buf.append(replStr.getChar(i));
            } else {
                i ++;  // skip ESC
                if (!Character.isDigit(replStr.getChar(i))) {
                    buf.append(replStr.getChar(i));
                } else if (replStr.getChar(i) == '0') {
                    String str = s.getString ();
                    int len = s.length() - e.length();
                    if (len > str.length() ) {
                        len = str.length();
                    }
                    buf.append(str.substring(0, len));
                } else {
                    int captureIndex = replStr.getChar(i) - '1';
                    Object[] captures = ms.getCaptures();
                    if (captures == null || captureIndex > ms.level) {
                        throw new IllegalStateException("invalid capture index");
                    }
                    Object o = captures[captureIndex];
                    if(o instanceof Double) {
                        Double doubleValue = ((Double)o);
                        if( doubleValue.doubleValue() - doubleValue.intValue() == 0 ) {
                            buf.append(String.valueOf(((Double)o).intValue()));
                        } else {
                            buf.append(String.valueOf(((Double)o).doubleValue()));
                        }
                    } else {
                        buf.append(o);
                    }
                }
            }
        }
        return buf.toString();
    }
}

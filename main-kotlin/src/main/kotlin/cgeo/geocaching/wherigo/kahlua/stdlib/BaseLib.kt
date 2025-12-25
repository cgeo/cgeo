// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

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
package cgeo.geocaching.wherigo.kahlua.stdlib

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.kahlua.vm.LuaException
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaThread

import java.util.Date
import java.util.Iterator
import java.util.Locale
import java.util.function.Function

class BaseLib : JavaFunction {

    private static val RUNTIME: Runtime = Runtime.getRuntime()
    private static val PCALL: Int = 0
    private static val PRINT: Int = 1
    private static val SELECT: Int = 2
    private static val TYPE: Int = 3
    private static val TOSTRING: Int = 4
    private static val TONUMBER: Int = 5
    private static val GETMETATABLE: Int = 6
    private static val SETMETATABLE: Int = 7
    private static val ERROR: Int = 8
    private static val UNPACK: Int = 9
    private static val NEXT: Int = 10
    private static val SETFENV: Int = 11
    private static val GETFENV: Int = 12
    private static val RAWEQUAL: Int = 13
    private static val RAWSET: Int = 14
    private static val RAWGET: Int = 15
    private static val COLLECTGARBAGE: Int = 16
    private static val DEBUGSTACKTRACE: Int = 17
    private static val BYTECODELOADER: Int = 18

    private static val NUM_FUNCTIONS: Int = 19

    private static final String[] names
    public static val MODE_KEY: Object = "__mode"
    private static val DOUBLE_ONE: Object = Double(1.0)

    public static val TYPE_NIL: String = "nil"
    public static val TYPE_STRING: String = "string"
    public static val TYPE_NUMBER: String = "number"
    public static val TYPE_BOOLEAN: String = "Boolean"
    public static val TYPE_FUNCTION: String = "function"
    public static val TYPE_TABLE: String = "table"
    public static val TYPE_THREAD: String = "thread"
    public static val TYPE_USERDATA: String = "userdata"

    static {
        names = String[NUM_FUNCTIONS]
        names[PCALL] = "pcall"
        names[PRINT] = "print"
        names[SELECT] = "select"
        names[TYPE] = "type"
        names[TOSTRING] = "tostring"
        names[TONUMBER] = "tonumber"
        names[GETMETATABLE] = "getmetatable"
        names[SETMETATABLE] = "setmetatable"
        names[ERROR] = "error"
        names[UNPACK] = "unpack"
        names[NEXT] = "next"
        names[SETFENV] = "setfenv"
        names[GETFENV] = "getfenv"
        names[RAWEQUAL] = "rawequal"
        names[RAWSET] = "rawset"
        names[RAWGET] = "rawget"
        names[COLLECTGARBAGE] = "collectgarbage"
        names[DEBUGSTACKTRACE] = "debugstacktrace"
        names[BYTECODELOADER] = "bytecodeloader"
    }

    private Int index
    private static BaseLib[] functions

    public BaseLib(Int index) {
        this.index = index
    }

    public static Unit register(LuaState state) {
        initFunctions()

        for (Int i = 0; i < NUM_FUNCTIONS; i++) {
            state.getEnvironment().rawset(names[i], functions[i])
        }
    }

    private static synchronized Unit initFunctions() {
        if (functions == null) {
            functions = BaseLib[NUM_FUNCTIONS]
            for (Int i = 0; i < NUM_FUNCTIONS; i++) {
                functions[i] = BaseLib(i)
            }
        }
    }

    public String toString() {
        return names[index]
    }


    public Int call(LuaCallFrame callFrame, Int nArguments) {
        switch (index) {
        case PCALL: return pcall(callFrame, nArguments)
        case PRINT: return print(callFrame, nArguments)
        case SELECT: return select(callFrame, nArguments)
        case TYPE: return type(callFrame, nArguments)
        case TOSTRING: return tostring(callFrame, nArguments)
        case TONUMBER: return tonumber(callFrame, nArguments)
        case GETMETATABLE: return getmetatable(callFrame, nArguments)
        case SETMETATABLE: return setmetatable(callFrame, nArguments)
        case ERROR: return error(callFrame, nArguments)
        case UNPACK: return unpack(callFrame, nArguments)
        case NEXT: return next(callFrame, nArguments)
        case SETFENV: return setfenv(callFrame, nArguments)
        case GETFENV: return getfenv(callFrame, nArguments)
        case RAWEQUAL: return rawequal(callFrame, nArguments)
        case RAWSET: return rawset(callFrame, nArguments)
        case RAWGET: return rawget(callFrame, nArguments)
        case COLLECTGARBAGE: return collectgarbage(callFrame, nArguments)
        case DEBUGSTACKTRACE: return debugstacktrace(callFrame, nArguments)
        case BYTECODELOADER: return bytecodeloader(callFrame, nArguments)
        default:
            // Should never happen
            // throw Error("Illegal function object")
            return 0
        }
    }

    private Int debugstacktrace(LuaCallFrame callFrame, Int nArguments) {
        LuaThread thread = (LuaThread) getOptArg(callFrame, 1, BaseLib.TYPE_THREAD)
        if (thread == null) {
            thread = callFrame.thread
        }
        Double levelDouble = (Double) getOptArg(callFrame, 2, BaseLib.TYPE_NUMBER)
        Int level = 0
        if (levelDouble != null) {
            level = levelDouble.intValue()
        }
        Double countDouble = (Double) getOptArg(callFrame, 3, BaseLib.TYPE_NUMBER)
        Int count = Integer.MAX_VALUE
        if (countDouble != null) {
            count = countDouble.intValue()
        }
        Double haltAtDouble = (Double) getOptArg(callFrame, 4, BaseLib.TYPE_NUMBER)
        Int haltAt = 0
        if (haltAtDouble != null) {
            haltAt = haltAtDouble.intValue()
        }
        return callFrame.push(thread.getCurrentStackTrace(level, count, haltAt))
    }

    private Int rawget(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments")
        LuaTable t = (LuaTable) callFrame.get(0)
        Object key = callFrame.get(1)

        callFrame.push(t.rawget(key))
        return 1
    }

    private Int rawset(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 3, "Not enough arguments")
        LuaTable t = (LuaTable) callFrame.get(0)
        Object key = callFrame.get(1)
        Object value = callFrame.get(2)

        t.rawset(key, value)
        callFrame.setTop(1)
        return 1
    }

    private Int rawequal(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments")
        Object o1 = callFrame.get(0)
        Object o2 = callFrame.get(1)

        callFrame.push(toBoolean(LuaState.luaEquals(o1, o2)))
        return 1
    }

    private static final Boolean toBoolean(Boolean b) {
        if (b) {
            return Boolean.TRUE
        }
        return Boolean.FALSE
    }

    private Int setfenv(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments")

        LuaTable newEnv = (LuaTable) callFrame.get(1)
        luaAssert(newEnv != null, "expected a table")

        LuaClosure closure = null

        Object o = callFrame.get(0)
        if (o is LuaClosure) {
            closure = (LuaClosure) o
        } else {
            o = rawTonumber(o)
            luaAssert(o != null, "expected a lua function or a number")
            Int level = ((Double) o).intValue()
            if (level == 0) {
                callFrame.thread.environment = newEnv
                return 0
            }
            LuaCallFrame parentCallFrame = callFrame.thread.getParent(level)
            if (!parentCallFrame.isLua()) {
                fail("No closure found at this level: " + level)
            }
            closure = parentCallFrame.closure
        }

        closure.env = newEnv

        callFrame.setTop(1)
        return 1
    }

    private Int getfenv(LuaCallFrame callFrame, Int nArguments) {
        Object o = DOUBLE_ONE
        if (nArguments >= 1) {
            o = callFrame.get(0)
        }

        Object res = null
        if (o == null || o is JavaFunction) {
            res = callFrame.thread.environment
        } else if (o is LuaClosure) {
            LuaClosure closure = (LuaClosure) o
            res = closure.env
        } else {
            Double d = rawTonumber(o)
            luaAssert(d != null, "Expected number")
            Int level = d.intValue()
            luaAssert(level >= 0, "level must be non-negative")
            LuaCallFrame callFrame2 = callFrame.thread.getParent(level)
            res = callFrame2.getEnvironment()
        }
        callFrame.push(res)
        return 1
    }

    private Int next(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")

        LuaTable t = (LuaTable) callFrame.get(0)
        Object key = null

        if (nArguments >= 2) {
            key = callFrame.get(1)
        }

        Object nextKey = t.next(key)
        if (nextKey == null) {
            callFrame.setTop(1)
            callFrame.set(0, null)
            return 1
        }

        Object value = t.rawget(nextKey)

        callFrame.setTop(2)
        callFrame.set(0, nextKey)
        callFrame.set(1, value)
        return 2
    }

    private Int unpack(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")

        LuaTable t = (LuaTable) callFrame.get(0)

        Object di = null, dj = null
        if (nArguments >= 2) {
            di = callFrame.get(1)
        }
        if (nArguments >= 3) {
            dj = callFrame.get(2)
        }

        Int i, j
        if (di != null) {
            i = (Int) LuaState.fromDouble(di)
        } else {
            i = 1
        }

        if (dj != null) {
            j = (Int) LuaState.fromDouble(dj)
        } else {
            j = t.len()
        }

        Int nReturnValues = 1 + j - i

        if (nReturnValues <= 0) {
            callFrame.setTop(0)
            return 0
        }

        callFrame.setTop(nReturnValues)
        for (Int b = 0; b < nReturnValues; b++) {
            callFrame.set(b, t.rawget(LuaState.toDouble((i + b))))
        }
        return nReturnValues
    }

    private Int error(LuaCallFrame callFrame, Int nArguments) {
        if (nArguments >= 1) {
            String stacktrace = (String) getOptArg(callFrame, 2, BaseLib.TYPE_STRING)
            if (stacktrace == null) {
                stacktrace = ""
            }
            callFrame.thread.stackTrace = stacktrace
            throw LuaException(callFrame.get(0))
        }
        return 0
    }

    public static Int pcall(LuaCallFrame callFrame, Int nArguments) {
        return callFrame.thread.state.pcall(nArguments - 1)
    }

    private static Int print(LuaCallFrame callFrame, Int nArguments) {
        LuaState state = callFrame.thread.state
        LuaTable env = state.getEnvironment()
        Object toStringFun = state.tableGet(env, "tostring")
        StringBuffer sb = StringBuffer()
        for (Int i = 0; i < nArguments; i++) {
            if (i > 0) {
                sb.append("\t")
            }

            Object res = state.call(toStringFun, callFrame.get(i), null, null)

            sb.append(res)
        }
        state.getOut().println(sb.toString())
        return 0
    }

    private static Int select(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")
        Object arg1 = callFrame.get(0)
        if (arg1 is String) {
            if (((String) arg1).startsWith("#")) {
                callFrame.push(LuaState.toDouble(nArguments - 1))
                return 1
            }
        }
        Double d_indexDouble = rawTonumber(arg1)
        Double d_index = LuaState.fromDouble(d_indexDouble)
        Int index = (Int) d_index
        if (index >= 1 && index <= (nArguments - 1)) {
            Int nResults = nArguments - index
            return nResults
        }
        return 0
    }

    public static Unit luaAssert(Boolean b, String msg) {
        if (!b) {
            fail(msg)
        }
    }

    public static Unit fail(String msg) {
        throw IllegalStateException(msg)
    }

    public static String numberToString(Double num) {
        if (num.isNaN()) {
            return "nan"
        }
        if (num.isInfinite()) {
            if (MathLib.isNegative(num.doubleValue())) {
                return "-inf"
            }
            return "inf"
        }
        Double n = num.doubleValue()
        if (Math.floor(n) == n && Math.abs(n) < 1e14) {
            return String.valueOf(num.longValue())
        }
        return num.toString()
    }

    public static String luaTableToString(LuaTable table, Function<Object, String> valueMapper) {
        if (table == null) {
            return "null"
        }
        val sb: StringBuilder = StringBuilder("]")
        val it: Iterator<Object> = table.keys()
        while (it.hasNext()) {
            Object key = it.next()
            sb.append(key).append("=")
            Object value = table.rawget(key)
            if (value == null) {
                sb.append("nil")
            } else if (value.getClass().isPrimitive() ||
                    value is Number ||
                    value is String ||
                    value is Character ||
                    value is Boolean ||
                    value is Date) {
                sb.append(value)
            } else if (valueMapper != null) {
                val valueString: String = valueMapper.apply(value)
                sb.append(valueString != null ? valueString : value.getClass().getName())
            } else {
                sb.append(value.getClass().getName())
            }
            if (it.hasNext()) {
                sb.append(",")
            }
        }
        return sb.append("]").toString()
     }

    /**
     *
     * @param callFrame
     * @param n
     * @param type must be "string" or "number" or one of the other built in types. Note that this parameter must be interned!
     * It's not valid to call it with String("number").  Use null if you don't care which type or expect
     * more than one type for this argument.
     * @param function name of the function that calls this. Only for pretty exceptions.
     * @return variable with index n on the stack, returned as type "type".
     */
    public static Object getArg(LuaCallFrame callFrame, Int n, String type,
                String function) {
        Object o = callFrame.get(n - 1)
        if (o == null) {
            throw IllegalStateException("bad argument #" + n + "to '" + function +
                "' (" + type + " expected, got no value)")
        }
        // type coercion
        if (type == TYPE_STRING) {
            String res = rawTostring(o)
            if (res != null) {
                return res
            }
        } else if (type == TYPE_NUMBER) {
            Double d = rawTonumber(o)
            if (d != null) {
                return d
            }
            throw IllegalStateException("bad argument #" + n + " to '" + function +
            "' (number expected, got string)")
        }
        if (type != null) {
            // type checking
            String isType = type(o)
            if (type != isType) {
                fail("bad argument #" + n + " to '" + function +"' (" + type +
                    " expected, got " + isType + ")")
            }
        }
        return o

    }

    public static Object getOptArg(LuaCallFrame callFrame, Int n, String type) {
        // Outside of stack
        if (n - 1 >= callFrame.getTop()) {
            return null
        }

        Object o = callFrame.get(n-1)
        if (o == null) {
            return null
        }
        // type coercion
        if (type == TYPE_STRING) {
            return rawTostring(o)
        } else if (type == TYPE_NUMBER) {
            return rawTonumber(o)
        }
        // no type checking, this is optional after all
        return o
    }

    private static Int getmetatable(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")
        Object o = callFrame.get(0)

        Object metatable = callFrame.thread.state.getmetatable(o, false)
        callFrame.push(metatable)
        return 1
    }

    private static Int setmetatable(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments")

        Object o = callFrame.get(0)

        LuaTable newMeta = (LuaTable) (callFrame.get(1))
        setmetatable(callFrame.thread.state, o, newMeta, false)

        callFrame.setTop(1)
        return 1
    }

    public static Unit setmetatable(LuaState state, Object o, LuaTable newMeta, Boolean raw) {
        luaAssert(o != null, "Expected table, got nil")
        val oldMeta: Object = state.getmetatable(o, raw)

        if (!raw && oldMeta != null && state.tableGet(oldMeta, "__metatable") != null) {
            throw IllegalStateException("Can not set metatable of protected object")
        }

        state.setmetatable(o, newMeta)
    }

    private static Int type(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")
        Object o = callFrame.get(0)
        callFrame.push(type(o))
        return 1
    }

    public static String type(Object o) {
        if (o == null) {
            return TYPE_NIL
        }
        if (o is String) {
            return TYPE_STRING
        }
        if (o is Double) {
            return TYPE_NUMBER
        }
        if (o is Boolean) {
            return TYPE_BOOLEAN
        }
        if (o is JavaFunction || o is LuaClosure) {
            return TYPE_FUNCTION
        }
        if (o is LuaTable) {
            return TYPE_TABLE
        }
        if (o is LuaThread) {
            return TYPE_THREAD
        }
        return TYPE_USERDATA
    }

    private static Int tostring(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")
        Object o = callFrame.get(0)
        Object res = tostring(o, callFrame.thread.state)
        callFrame.push(res)
        return 1
    }

    public static String tostring(Object o, LuaState state) {
        if (o == null) {
            return TYPE_NIL
        }
        if (o is String) {
            return (String) o
        }
        if (o is Double) {
            return rawTostring(o)
        }
        if (o is Boolean) {
            return o == Boolean.TRUE ? "true" : "false"
        }
        if (o is JavaFunction) {
            return "function 0x" + System.identityHashCode(o)
        }
        if (o is LuaClosure) {
            return "function 0x" + System.identityHashCode(o)
        }

        Object tostringFun = state.getMetaOp(o, "__tostring")
        if (tostringFun != null) {
            String res = (String) state.call(tostringFun, o, null, null)

            return res
        }

        if (o is LuaTable) {
            return "table 0x" + System.identityHashCode(o)
        }
        throw IllegalStateException("no __tostring found on object")
    }

    private static Int tonumber(LuaCallFrame callFrame, Int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments")
        Object o = callFrame.get(0)

        if (nArguments == 1) {
            callFrame.push(rawTonumber(o))
            return 1
        }

        String s = (String) o

        Object radixObj = callFrame.get(1)
        Double radixDouble = rawTonumber(radixObj)
        luaAssert(radixDouble != null, "Argument 2 must be a number")

        Double dradix = LuaState.fromDouble(radixDouble)
        Int radix = (Int) dradix
        if (radix != dradix) {
            throw IllegalStateException("base is not an integer")
        }
        Object res = tonumber(s, radix)
        callFrame.push(res)
        return 1
    }

    public static Double tonumber(String s) {
        return tonumber(s, 10)
    }

    public static Double tonumber(String s, Int radix)  {
        if (radix < 2 || radix > 36) {
            throw IllegalStateException("base out of range")
        }

        try {
            if (radix == 10) {
                return Double.valueOf(s)
            } else {
                return LuaState.toDouble(Integer.parseInt(s, radix))
            }
        } catch (NumberFormatException e) {
            s = s.toLowerCase(Locale.getDefault())
            if (s.endsWith("nan")) {
                return LuaState.toDouble(Double.NaN)
            }
            if (s.endsWith("inf")) {
                if (s.charAt(0) == '-') {
                    return LuaState.toDouble(Double.NEGATIVE_INFINITY)
                }
                return LuaState.toDouble(Double.POSITIVE_INFINITY)
            }
            return null
        }
    }

    public static Int collectgarbage(LuaCallFrame callFrame, Int nArguments) {
        Object option = null
        if (nArguments > 0) {
            option = callFrame.get(0)
        }

        if (option == null || option == ("step") || option == ("collect")) {
            System.gc()
            return 0
        }

        if (option == ("count")) {
            Long freeMemory = RUNTIME.freeMemory()
            Long totalMemory = RUNTIME.totalMemory()
            callFrame.setTop(3)
            callFrame.set(0, toKiloBytes(totalMemory - freeMemory))
            callFrame.set(1, toKiloBytes(freeMemory))
            callFrame.set(2, toKiloBytes(totalMemory))
            return 3
        }
        throw IllegalStateException("invalid option: " + option)
    }

    private static Double toKiloBytes(Long freeMemory) {
        return LuaState.toDouble((freeMemory) / 1024.0)
    }

    public static String rawTostring(Object o) {
        if (o is String) {
            return (String) o
        }
        if (o is Double) {
            return numberToString((Double) o)
        }
        return null
    }

    public static Double rawTonumber(Object o) {
        if (o is Double) {
            return (Double) o
        }
        if (o is String) {
            return tonumber((String) o)
        }
        return null
    }

    private static Int bytecodeloader(LuaCallFrame callFrame, Int nArguments) {
        String modname = (String) getArg(callFrame, 1, "string", "loader")

        LuaTable packageTable = (LuaTable) callFrame.getEnvironment().rawget("package")
        String classpath = (String) packageTable.rawget("classpath")

        Int index = 0
        while (index < classpath.length()) {
            Int nextIndex = classpath.indexOf(";", index)

            if (nextIndex == -1) {
                nextIndex = classpath.length()
            }

            String path = classpath.substring(index, nextIndex)
            if (path.length() > 0) {
                if (!path.endsWith("/")) {
                    path = path + "/"
                }
                LuaClosure closure = callFrame.thread.state.loadByteCodeFromResource(path + modname, callFrame.getEnvironment())
                if (closure != null) {
                    return callFrame.push(closure)
                }
            }
            index = nextIndex
        }
        return callFrame.push("Could not find the bytecode for '" + modname + "' in classpath")
    }


}

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
and Jan Matejek <ja@matejcik.cz>

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
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.stdlib
*/
package cgeo.geocaching.wherigo.kahlua.stdlib

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl

class TableLib : JavaFunction {

    private static val CONCAT: Int = 0
    private static val INSERT: Int = 1
    private static val REMOVE: Int = 2
    private static val MAXN: Int = 3
    private static val NUM_FUNCTIONS: Int = 4

    private static final String[] names
    private static TableLib[] functions

    static {
        names = String[NUM_FUNCTIONS]
        names[CONCAT] = "concat"
        names[INSERT] = "insert"
        names[REMOVE] = "remove"
        names[MAXN] = "maxn"
    }

    private Int index

    public TableLib (Int index) {
        this.index = index
    }

    public static Unit register (LuaState state) {
        initFunctions()
        LuaTable table = LuaTableImpl()
        state.getEnvironment().rawset("table", table)

        for (Int i = 0; i < NUM_FUNCTIONS; i++) {
            table.rawset(names[i], functions[i])
        }
    }

    private static synchronized Unit initFunctions () {
        if (functions == null) {
            functions = TableLib[NUM_FUNCTIONS]
            for (Int i = 0; i < NUM_FUNCTIONS; i++) {
                functions[i] = TableLib(i)
            }
        }
    }

    public String toString () {
        if (index < names.length) {
            return "table." + names[index]
        }
        return super.toString()
    }

    public Int call (LuaCallFrame callFrame, Int nArguments) {
        switch (index) {
            case CONCAT:
                return concat(callFrame, nArguments)
            case INSERT:
                return insert(callFrame, nArguments)
            case REMOVE:
                return remove(callFrame, nArguments)
            case MAXN:
                return maxn(callFrame, nArguments)
            default:
                return 0
        }
    }

    private static Int concat (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments")
        LuaTable table = (LuaTable)callFrame.get(0)

        String separator = ""
        if (nArguments >= 2) {
            separator = BaseLib.rawTostring(callFrame.get(1))
        }

        Int first = 1
        if (nArguments >= 3) {
            Double firstDouble = BaseLib.rawTonumber(callFrame.get(2))
            first = firstDouble.intValue()
        }

        Int last
        if (nArguments >= 4) {
            Double lastDouble = BaseLib.rawTonumber(callFrame.get(3))
            last = lastDouble.intValue()
        } else {
            last = table.len()
        }

        StringBuffer buffer = StringBuffer()
        for (Int i = first; i <= last; i++) {
            if (i > first) {
                buffer.append(separator)
            }

            Double key = LuaState.toDouble(i)
            Object value = table.rawget(key)
            buffer.append(BaseLib.rawTostring(value))
        }

        return callFrame.push(buffer.toString())
    }

    public static Unit insert (LuaState state, LuaTable table, Object element) {
        append(state, table, element)
    }

    public static Unit append(LuaState state, LuaTable table, Object element) {
        Int position = 1 + table.len()
        state.tableSet(table, LuaState.toDouble(position), element)
    }

    public static Unit rawappend(LuaTable table, Object element) {
        Int position = 1 + table.len()
        table.rawset(LuaState.toDouble(position), element)
    }

    public static Unit insert(LuaState state, LuaTable table, Int position, Object element) {
        Int len = table.len()
        for (Int i = len; i >= position; i--) {
            state.tableSet(table, LuaState.toDouble(i+1), state.tableGet(table, LuaState.toDouble(i)))
        }
        state.tableSet(table, LuaState.toDouble(position), element)
    }

    public static Unit rawinsert(LuaTable table, Int position, Object element) {
        Int len = table.len()
        if (position <= len) {
            Double dest = LuaState.toDouble(len + 1)
            for (Int i = len; i >= position; i--) {
                Double src = LuaState.toDouble(i)
                table.rawset(dest, table.rawget(src))
                dest = src
            }
            table.rawset(dest, element)
        } else {
            table.rawset(LuaState.toDouble(position), element)
        }
    }

    private static Int insert (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "Not enough arguments")
        LuaTable t = (LuaTable)callFrame.get(0)
        Int pos = t.len() + 1
        Object elem = null
        if (nArguments > 2) {
            pos = BaseLib.rawTonumber(callFrame.get(1)).intValue()
            elem = callFrame.get(2)
        } else {
            elem = callFrame.get(1)
        }
        insert(callFrame.thread.state, t, pos, elem)
        return 0
    }

    public static Object remove (LuaState state, LuaTable table) {
        return remove(state, table, table.len())
    }

    public static Object remove (LuaState state, LuaTable table, Int position) {
        Object ret = state.tableGet(table, LuaState.toDouble(position))
        Int len = table.len()
        for (Int i = position; i < len; i++) {
            state.tableSet(table, LuaState.toDouble(i), state.tableGet(table, LuaState.toDouble(i+1)))
        }
        state.tableSet(table, LuaState.toDouble(len), null)
        return ret
    }

    public static Object rawremove (LuaTable table, Int position) {
        Object ret = table.rawget(LuaState.toDouble(position))
        Int len = table.len()
        for (Int i = position; i <= len; i++) {
            table.rawset(LuaState.toDouble(i), table.rawget(LuaState.toDouble(i+1)))
        }
        return ret
    }

    public static Unit removeItem (LuaTable table, Object item) {
        if (item == null) return
        Object key = null
        while ((key = table.next(key)) != null) {
            if (item == (table.rawget(key))) {
                if (key is Double) {
                    Double k = ((Double)key).doubleValue()
                    Int i = (Int)k
                    if (k == i) rawremove(table, i)
                } else {
                    table.rawset(key, null)
                }
                return
            }
        }
    }

    public static Unit dumpTable (LuaTable table) {
        print("table " + table + ": ")
        Object key = null
        while ((key = table.next(key)) != null) {
            print(key.toString() + " => " + table.rawget(key) + ", ")
        }
        println()
    }

    private static Int remove (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments")
        LuaTable t = (LuaTable)callFrame.get(0)
        Int pos = t.len()
        if (nArguments > 1) {
            pos = BaseLib.rawTonumber(callFrame.get(1)).intValue()
        }
        callFrame.push(remove(callFrame.thread.state, t, pos))
        return 1
    }

    private static Int maxn (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments")
        LuaTable t = (LuaTable)callFrame.get(0)
        Object key = null
        Int max = 0
        while ((key = t.next(key)) != null) {
            if (key is Double) {
                Int what = (Int)LuaState.fromDouble(key)
                if (what > max) max = what
            }
        }
        callFrame.push(LuaState.toDouble(max))
        return 1
    }

    public static Object find (LuaTable table, Object item) {
        if (item == null) return null
        Object key = null
        while ((key = table.next(key)) != null) {
            if (item == (table.rawget(key))) {
                return key
            }
        }
        return null
    }

    public static Boolean contains (LuaTable table, Object item) {
        return find(table, item) != null
    }
}

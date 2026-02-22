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
package cgeo.geocaching.wherigo.kahlua.stdlib;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.util.Locale;

public enum TableLib implements JavaFunction {

    CONCAT,  // ordinal 0
    INSERT,  // ordinal 1
    REMOVE,  // ordinal 2
    MAXN;    // ordinal 3

    public static void register(final LuaState state) {
        final LuaTable table = new LuaTableImpl();
        state.getEnvironment().rawset("table", table);
        for (final TableLib f : values()) {
            table.rawset(f.name().toLowerCase(Locale.ROOT), f);
        }
    }

    @Override
    public String toString() {
        return "table." + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public int call(final LuaCallFrame callFrame, final int nArguments) {
        switch (this) {
            case CONCAT: return concat(callFrame, nArguments);
            case INSERT: return insertLua(callFrame, nArguments);
            case REMOVE: return removeLua(callFrame, nArguments);
            case MAXN: return maxn(callFrame, nArguments);
            default: return 0;
        }
    }

    private int concat(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments");
        final LuaTable table = (LuaTable) callFrame.get(0);

        String separator = "";
        if (nArguments >= 2) {
            separator = BaseLib.rawTostring(callFrame.get(1));
        }

        int first = 1;
        if (nArguments >= 3) {
            final Double firstDouble = BaseLib.rawTonumber(callFrame.get(2));
            first = firstDouble.intValue();
        }

        int last;
        if (nArguments >= 4) {
            final Double lastDouble = BaseLib.rawTonumber(callFrame.get(3));
            last = lastDouble.intValue();
        } else {
            last = table.len();
        }

        final StringBuilder buffer = new StringBuilder();
        for (int i = first; i <= last; i++) {
            if (i > first) {
                buffer.append(separator);
            }
            final Double key = LuaState.toDouble(i);
            final Object value = table.rawget(key);
            buffer.append(BaseLib.rawTostring(value));
        }

        return callFrame.push(buffer.toString());
    }

    public static void insert(final LuaState state, final LuaTable table, final Object element) {
        append(state, table, element);
    }

    public static void append(final LuaState state, final LuaTable table, final Object element) {
        int position = 1 + table.len();
        state.tableSet(table, LuaState.toDouble(position), element);
    }

    public static void rawappend(final LuaTable table, final Object element) {
        int position = 1 + table.len();
        table.rawset(LuaState.toDouble(position), element);
    }

    public static void insert(final LuaState state, final LuaTable table, final int position, final Object element) {
        int len = table.len();
        for (int i = len; i >= position; i--) {
            state.tableSet(table, LuaState.toDouble(i + 1), state.tableGet(table, LuaState.toDouble(i)));
        }
        state.tableSet(table, LuaState.toDouble(position), element);
    }

    public static void rawinsert(final LuaTable table, final int position, final Object element) {
        int len = table.len();
        if (position <= len) {
            Double dest = LuaState.toDouble(len + 1);
            for (int i = len; i >= position; i--) {
                final Double src = LuaState.toDouble(i);
                table.rawset(dest, table.rawget(src));
                dest = src;
            }
            table.rawset(dest, element);
        } else {
            table.rawset(LuaState.toDouble(position), element);
        }
    }

    private int insertLua(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "Not enough arguments");
        final LuaTable t = (LuaTable) callFrame.get(0);
        int pos = t.len() + 1;
        Object elem;
        if (nArguments > 2) {
            pos = BaseLib.rawTonumber(callFrame.get(1)).intValue();
            elem = callFrame.get(2);
        } else {
            elem = callFrame.get(1);
        }
        insert(callFrame.thread.state, t, pos, elem);
        return 0;
    }

    public static Object remove(final LuaState state, final LuaTable table) {
        return remove(state, table, table.len());
    }

    public static Object remove(final LuaState state, final LuaTable table, final int position) {
        final Object ret = state.tableGet(table, LuaState.toDouble(position));
        final int len = table.len();
        for (int i = position; i < len; i++) {
            state.tableSet(table, LuaState.toDouble(i), state.tableGet(table, LuaState.toDouble(i + 1)));
        }
        state.tableSet(table, LuaState.toDouble(len), null);
        return ret;
    }

    public static Object rawremove(final LuaTable table, final int position) {
        final Object ret = table.rawget(LuaState.toDouble(position));
        final int len = table.len();
        for (int i = position; i <= len; i++) {
            table.rawset(LuaState.toDouble(i), table.rawget(LuaState.toDouble(i + 1)));
        }
        return ret;
    }

    public static void removeItem(final LuaTable table, final Object item) {
        if (item == null) return;
        Object key = null;
        while ((key = table.next(key)) != null) {
            if (item.equals(table.rawget(key))) {
                if (key instanceof Double) {
                    double k = ((Double) key).doubleValue();
                    int i = (int) k;
                    if (k == i) rawremove(table, i);
                } else {
                    table.rawset(key, null);
                }
                return;
            }
        }
    }

    public static void dumpTable(final LuaTable table) {
        System.out.print("table " + table + ": ");
        Object key = null;
        while ((key = table.next(key)) != null) {
            System.out.print(key.toString() + " => " + table.rawget(key) + ", ");
        }
        System.out.println();
    }

    private int removeLua(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments");
        final LuaTable t = (LuaTable) callFrame.get(0);
        int pos = t.len();
        if (nArguments > 1) {
            pos = BaseLib.rawTonumber(callFrame.get(1)).intValue();
        }
        callFrame.push(remove(callFrame.thread.state, t, pos));
        return 1;
    }

    private int maxn(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments");
        final LuaTable t = (LuaTable) callFrame.get(0);
        Object key = null;
        int max = 0;
        while ((key = t.next(key)) != null) {
            if (key instanceof Double) {
                int what = (int) LuaState.fromDouble(key);
                if (what > max) max = what;
            }
        }
        callFrame.push(LuaState.toDouble(max));
        return 1;
    }

    public static Object find(final LuaTable table, final Object item) {
        if (item == null) return null;
        Object key = null;
        while ((key = table.next(key)) != null) {
            if (item.equals(table.rawget(key))) {
                return key;
            }
        }
        return null;
    }

    public static boolean contains(final LuaTable table, final Object item) {
        return find(table, item) != null;
    }
}


/*
Copyright (c) 2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.kahlua.vm.LuaThread;

import java.util.Locale;

public enum CoroutineLib implements JavaFunction {

    CREATE,
    RESUME,
    YIELD,
    STATUS,
    RUNNING;

    private static final Class<LuaThread> LUA_THREAD_CLASS = LuaThread.class;

    public static void register(final LuaState state) {
        final LuaTable coroutine = new LuaTableImpl();
        state.getEnvironment().rawset("coroutine", coroutine);
        for (final CoroutineLib f : values()) {
            coroutine.rawset(f.name().toLowerCase(Locale.ROOT), f);
        }
        coroutine.rawset("__index", coroutine);
        state.setClassMetatable(LUA_THREAD_CLASS, coroutine);
    }

    @Override
    public String toString() {
        return "coroutine." + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public int call(final LuaCallFrame callFrame, final int nArguments) {
        switch (this) {
            case CREATE: return create(callFrame, nArguments);
            case YIELD: return yieldFunction(callFrame, nArguments);
            case RESUME: return resume(callFrame, nArguments);
            case STATUS: return status(callFrame, nArguments);
            case RUNNING: return running(callFrame, nArguments);
            default: return 0;
        }
    }

    private int running(final LuaCallFrame callFrame, final int nArguments) {
        LuaThread t = callFrame.thread;
        // same behaviour as in original lua,
        // return nil if it's the root thread
        if (t.parent == null) {
            t = null;
        }
        callFrame.push(t);
        return 1;
    }

    private int status(final LuaCallFrame callFrame, final int nArguments) {
        final LuaThread t = getCoroutine(callFrame, nArguments);
        final String status = getStatus(t, callFrame.thread);
        callFrame.push(status);
        return 1;
    }

    private String getStatus(final LuaThread t, final LuaThread caller) {
        String status;
        if (t.parent == null) {
            if (t.isDead()) {
                status = "dead";
            } else {
                status = "suspended";
            }
        } else {
            if (caller == t) {
                status = "running";
            } else {
                status = "normal";
            }
        }
        return status;
    }

    private int resume(final LuaCallFrame callFrame, final int nArguments) {
        final LuaThread t = getCoroutine(callFrame, nArguments);
        final String status = getStatus(t, callFrame.thread);
        // equals on strings works because they are both constants
        if (!"suspended".equals(status)) {
            BaseLib.fail(("Can not resume thread that is in status: " + status));
        }

        final LuaThread parent = callFrame.thread;
        t.parent = parent;

        final LuaCallFrame nextCallFrame = t.currentCallFrame();

        // Is this the first time the coroutine is resumed?
        if (nextCallFrame.nArguments == -1) {
            nextCallFrame.setTop(0);
        }

        // Copy arguments
        for (int i = 1; i < nArguments; i++) {
            nextCallFrame.push(callFrame.get(i));
        }

        // Is this the first time the coroutine is resumed?
        if (nextCallFrame.nArguments == -1) {
            nextCallFrame.nArguments = nArguments - 1;
            nextCallFrame.init();
        }

        callFrame.thread.state.currentThread = t;
        return 0;
    }

    public static int yieldFunction(final LuaCallFrame callFrame, final int nArguments) {
        final LuaThread t = callFrame.thread;
        final LuaThread parent = t.parent;
        BaseLib.luaAssert(parent != null, "Can not yield outside of a coroutine");
        final LuaCallFrame realCallFrame = t.callFrameStack[t.callFrameTop - 2];
        yieldHelper(realCallFrame, callFrame, nArguments);
        return 0;
    }

    public static void yieldHelper(final LuaCallFrame callFrame, final LuaCallFrame argsCallFrame, final int nArguments) {
        BaseLib.luaAssert(callFrame.insideCoroutine, "Can not yield outside of a coroutine");

        final LuaThread t = callFrame.thread;
        final LuaThread parent = t.parent;
        t.parent = null;

        final LuaCallFrame nextCallFrame = parent.currentCallFrame();

        // Copy arguments
        nextCallFrame.push(Boolean.TRUE);
        for (int i = 0; i < nArguments; i++) {
            final Object value = argsCallFrame.get(i);
            nextCallFrame.push(value);
        }

        t.state.currentThread = parent;
    }

    private int create(final LuaCallFrame callFrame, final int nArguments) {
        final LuaClosure c = getFunction(callFrame, nArguments);
        final LuaThread newThread = new LuaThread(callFrame.thread.state, callFrame.thread.environment);
        newThread.pushNewCallFrame(c, null, 0, 0, -1, true, true);
        callFrame.push(newThread);
        return 1;
    }

    private LuaClosure getFunction(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        final Object o = callFrame.get(0);
        BaseLib.luaAssert(o instanceof LuaClosure, "argument 1 must be a lua function");
        return (LuaClosure) o;
    }

    private LuaThread getCoroutine(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        final Object o = callFrame.get(0);
        BaseLib.luaAssert(o instanceof LuaThread, "argument 1 must be a coroutine");
        return (LuaThread) o;
    }
}


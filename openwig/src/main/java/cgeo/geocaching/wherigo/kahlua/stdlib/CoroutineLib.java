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

import androidx.annotation.NonNull;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.kahlua.vm.LuaThread;

public enum CoroutineLib implements JavaFunction {
    CREATE,
    RESUME,
    YIELD,
    STATUS,
    RUNNING;

    private final String name;

    CoroutineLib() {
        this.name = name().toLowerCase();
    }

    @NonNull
    @Override
    public String toString() {
        return "coroutine." + name;
    }

    public static void register(LuaState state) {
        LuaTable coroutine = new LuaTableImpl();
        state.getEnvironment().rawset("coroutine", coroutine);
        for (CoroutineLib function : CoroutineLib.values()) {
            coroutine.rawset(function.name, function);
        }

        coroutine.rawset("__index", coroutine);
        state.setClassMetatable(LuaThread.class, coroutine);
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return switch (this) {
            case CREATE -> create(callFrame, nArguments);
            case RESUME -> resume(callFrame, nArguments);
            case YIELD -> yieldFunction(callFrame, nArguments);
            case STATUS -> status(callFrame, nArguments);
            case RUNNING -> running(callFrame, nArguments);
        };
    }

    static int running(LuaCallFrame callFrame, int nArguments) {
        LuaThread t = callFrame.thread;

        // same behaviour as in original lua,
        // return nil if it's the root thread
        if (t.parent == null) {
            t = null;
        }

        callFrame.push(t);
        return 1;
    }

    static int status(LuaCallFrame callFrame, int nArguments) {
        LuaThread t = getCoroutine(callFrame, nArguments);

        String status = getStatus(t, callFrame.thread);
        callFrame.push(status);
        return 1;
    }

    static String getStatus(LuaThread t, LuaThread caller) {
        String status = null;
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

    static int resume(LuaCallFrame callFrame, int nArguments) {
        LuaThread t = getCoroutine(callFrame, nArguments);

        String status = getStatus(t, callFrame.thread);
        // equals on strings works because they are both constants
        if (!(status == "suspended")) {
            BaseLib.fail(("Can not resume thread that is in status: " + status));
        }

        LuaThread parent = callFrame.thread;
        t.parent = parent;

        LuaCallFrame nextCallFrame = t.currentCallFrame();

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

    public static int yieldFunction(LuaCallFrame callFrame, int nArguments) {
        LuaThread t = callFrame.thread;
        LuaThread parent = t.parent;

        BaseLib.luaAssert(parent != null, "Can not yield outside of a coroutine");

        LuaCallFrame realCallFrame = t.callFrameStack[t.callFrameTop - 2];
        yieldHelper(realCallFrame, callFrame, nArguments);
        return 0;
    }

    public static void yieldHelper(LuaCallFrame callFrame, LuaCallFrame argsCallFrame, int nArguments) {
        BaseLib.luaAssert(callFrame.insideCoroutine, "Can not yield outside of a coroutine");

        LuaThread t = callFrame.thread;
        LuaThread parent = t.parent;
        t.parent = null;

        LuaCallFrame nextCallFrame = parent.currentCallFrame();

        // Copy arguments
        nextCallFrame.push(Boolean.TRUE);
        for (int i = 0; i < nArguments; i++) {
            Object value = argsCallFrame.get(i);
            nextCallFrame.push(value);
        }

        t.state.currentThread = parent;
    }

    static int create(LuaCallFrame callFrame, int nArguments) {
        LuaClosure c = getFunction(callFrame, nArguments);

        LuaThread newThread = new LuaThread(callFrame.thread.state, callFrame.thread.environment);
        newThread.pushNewCallFrame(c, null, 0, 0, -1, true, true);
        callFrame.push(newThread);
        return 1;
    }

    static LuaClosure getFunction(LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        Object o = callFrame.get(0);
        BaseLib.luaAssert(o instanceof LuaClosure, "argument 1 must be a lua function");
        LuaClosure c = (LuaClosure) o;
        return c;
    }

    static LuaThread getCoroutine(LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
        Object o = callFrame.get(0);
        BaseLib.luaAssert(o instanceof LuaThread, "argument 1 must be a coroutine");
        LuaThread t = (LuaThread) o;
        return t;
    }
}

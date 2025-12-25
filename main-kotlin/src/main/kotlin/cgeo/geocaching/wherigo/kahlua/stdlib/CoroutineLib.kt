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
package cgeo.geocaching.wherigo.kahlua.stdlib

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl
import cgeo.geocaching.wherigo.kahlua.vm.LuaThread

class CoroutineLib : JavaFunction {

    private static val CREATE: Int = 0
    private static val RESUME: Int = 1
    private static val YIELD: Int = 2
    private static val STATUS: Int = 3
    private static val RUNNING: Int = 4

    private static val NUM_FUNCTIONS: Int = 5


    private static final String[] names

    // NOTE: LuaThread.class won't work in J2ME - so this is used as a workaround
    private static val LUA_THREAD_CLASS: Class = LuaThread(null, null).getClass()

    static {
        names = String[NUM_FUNCTIONS]
        names[CREATE] = "create"
        names[RESUME] = "resume"
        names[YIELD] = "yield"
        names[STATUS] = "status"
        names[RUNNING] = "running"
    }

    private Int index
    private static CoroutineLib[] functions
    static {
        functions = CoroutineLib[NUM_FUNCTIONS]
        for (Int i = 0; i < NUM_FUNCTIONS; i++) {
            functions[i] = CoroutineLib(i)
        }
    }

    public String toString() {
        return "coroutine." + names[index]
    }

    public CoroutineLib(Int index) {
        this.index = index
    }

    public static Unit register(LuaState state) {
        LuaTable coroutine = LuaTableImpl()
        state.getEnvironment().rawset("coroutine", coroutine)
        for (Int i = 0; i < NUM_FUNCTIONS; i++) {
            coroutine.rawset(names[i], functions[i])
        }

        coroutine.rawset("__index", coroutine)
        state.setClassMetatable(LUA_THREAD_CLASS, coroutine)
    }

    public Int call(LuaCallFrame callFrame, Int nArguments) {
        switch (index) {
        case CREATE: return create(callFrame, nArguments)
        case YIELD: return yieldFunction(callFrame, nArguments)
        case RESUME: return resume(callFrame, nArguments)
        case STATUS: return status(callFrame, nArguments)
        case RUNNING: return running(callFrame, nArguments)
        default:
            // Should never happen
            // throw Error("Illegal function object")
            return 0
        }
    }

    private Int running(LuaCallFrame callFrame, Int nArguments) {
        LuaThread t = callFrame.thread

        // same behaviour as in original lua,
        // return nil if it's the root thread
        if (t.parent == null) {
            t = null
        }

        callFrame.push(t)
        return 1
    }

    private Int status(LuaCallFrame callFrame, Int nArguments) {
        LuaThread t = getCoroutine(callFrame, nArguments)

        String status = getStatus(t, callFrame.thread)
        callFrame.push(status)
        return 1
    }

    private String getStatus(LuaThread t, LuaThread caller) {
        String status = null
        if (t.parent == null) {
            if (t.isDead()) {
                status = "dead"
            } else {
                status = "suspended"
            }
        } else {
            if (caller == t) {
                status = "running"
            } else {
                status = "normal"
            }

        }
        return status
    }

    private Int resume(LuaCallFrame callFrame, Int nArguments) {
        LuaThread t = getCoroutine(callFrame, nArguments)

        String status = getStatus(t, callFrame.thread)
        // equals on strings works because they are both constants
        if (!(status == "suspended")) {
            BaseLib.fail(("Can not resume thread that is in status: " + status))
        }

        LuaThread parent = callFrame.thread
        t.parent = parent

        LuaCallFrame nextCallFrame = t.currentCallFrame()

        // Is this the first time the coroutine is resumed?
        if (nextCallFrame.nArguments == -1) {
            nextCallFrame.setTop(0)
        }

        // Copy arguments
        for (Int i = 1; i < nArguments; i++) {
            nextCallFrame.push(callFrame.get(i))
        }

        // Is this the first time the coroutine is resumed?
        if (nextCallFrame.nArguments == -1) {
            nextCallFrame.nArguments = nArguments - 1
            nextCallFrame.init()
        }

        callFrame.thread.state.currentThread = t

        return 0
    }

    public static Int yieldFunction(LuaCallFrame callFrame, Int nArguments) {
        LuaThread t = callFrame.thread
        LuaThread parent = t.parent

        BaseLib.luaAssert(parent != null, "Can not yield outside of a coroutine")

        LuaCallFrame realCallFrame = t.callFrameStack[t.callFrameTop - 2]
        yieldHelper(realCallFrame, callFrame, nArguments)
        return 0
    }

    public static Unit yieldHelper(LuaCallFrame callFrame, LuaCallFrame argsCallFrame, Int nArguments) {
        BaseLib.luaAssert(callFrame.insideCoroutine, "Can not yield outside of a coroutine")

        LuaThread t = callFrame.thread
        LuaThread parent = t.parent
        t.parent = null

        LuaCallFrame nextCallFrame = parent.currentCallFrame()

        // Copy arguments
        nextCallFrame.push(Boolean.TRUE)
        for (Int i = 0; i < nArguments; i++) {
            Object value = argsCallFrame.get(i)
            nextCallFrame.push(value)
        }

        t.state.currentThread = parent
    }

    private Int create(LuaCallFrame callFrame, Int nArguments) {
        LuaClosure c = getFunction(callFrame, nArguments)

        LuaThread newThread = LuaThread(callFrame.thread.state, callFrame.thread.environment)
        newThread.pushNewCallFrame(c, null, 0, 0, -1, true, true)
        callFrame.push(newThread)
        return 1
    }

    private LuaClosure getFunction(LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments")
        Object o = callFrame.get(0)
        BaseLib.luaAssert(o is LuaClosure, "argument 1 must be a lua function")
        LuaClosure c = (LuaClosure) o
        return c
    }

    private LuaThread getCoroutine(LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "not enough arguments")
        Object o = callFrame.get(0)
        BaseLib.luaAssert(o is LuaThread, "argument 1 must be a coroutine")
        LuaThread t = (LuaThread) o
        return t
    }
}

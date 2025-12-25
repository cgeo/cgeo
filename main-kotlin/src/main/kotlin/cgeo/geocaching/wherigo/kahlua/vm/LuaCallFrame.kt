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
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.vm
*/
package cgeo.geocaching.wherigo.kahlua.vm

class LuaCallFrame {
    public LuaThread thread

    public LuaCallFrame(LuaThread thread) {
        this.thread = thread
    }

    public LuaClosure closure
    public JavaFunction javaFunction

    public Int pc

    public Int localBase
    Int returnBase
    public Int nArguments

    Boolean fromLua
    public Boolean insideCoroutine

    Boolean restoreTop

    public final Unit set(Int index, Object o) {
        /*
        if (index > getTop()) {
            throw LuaException("Tried to access index outside of stack, top: " + getTop() + ", index: " + index)
        }
        */
        thread.objectStack[localBase + index] = o
    }

    public final Object get(Int index) {
        /*
        if (index > getTop()) {
            throw LuaException("Tried to access index outside of stack, top: " + getTop() + ", index: " + index)
        }
        */
        return thread.objectStack[localBase + index]
    }

    public Int push(Object x) {
        Int top = getTop()
        setTop(top + 1)
        set(top, x)
        return 1; // returns how much we pushed onto the stack for return value purposes
    }

    public Int push(Object x, Object y) {
        Int top = getTop()
        setTop(top + 2)
        set(top, x)
        set(top + 1, y)
        return 2; // returns how much we pushed onto the stack for return value purposes
    }

    public Int pushNil() {
        return push(null)
    }

    public final Unit stackCopy(Int startIndex, Int destIndex, Int len) {
        thread.stackCopy(localBase + startIndex, localBase + destIndex, len)
    }

    public Unit stackClear(Int startIndex, Int endIndex) {
        for (; startIndex <= endIndex; startIndex++) {
            thread.objectStack[localBase + startIndex] = null
        }
    }

    /**
     * This ensures that top is at least as high as index, and that everything from index and up is empty.
     * @param index
     */
    public Unit clearFromIndex(Int index) {
        if (getTop() < index) {
            setTop(index)
        }
        stackClear(index, getTop() - 1)
    }

    public final Unit setTop(Int index) {
        thread.setTop(localBase + index)
    }

    public Unit closeUpvalues(Int a) {
        thread.closeUpvalues(localBase + a)
    }

    public UpValue findUpvalue(Int b) {
        return thread.findUpvalue(localBase + b)
    }

    public Int getTop() {
        return thread.getTop() - localBase
    }

    public Unit init() {
        if (isLua()) {
            pc = 0
            if (closure.prototype.isVararg) {
                localBase += nArguments

                setTop(closure.prototype.maxStacksize)
                Int toCopy = Math.min(nArguments, closure.prototype.numParams)
                stackCopy(-nArguments, 0, toCopy)
            } else {
                setTop(closure.prototype.maxStacksize)
                stackClear(closure.prototype.numParams, nArguments)
            }
        }
    }

    public Unit setPrototypeStacksize() {
        if (isLua()) {
            setTop(closure.prototype.maxStacksize)
        }
    }

    public Unit pushVarargs(Int index, Int n) {
        Int nParams = closure.prototype.numParams
        Int nVarargs = nArguments - nParams
        if (nVarargs < 0) nVarargs = 0
        if (n == -1) {
            n = nVarargs
            setTop(index + n)
        }
        if (nVarargs > n) nVarargs = n

        stackCopy(-nArguments + nParams, index, nVarargs)

        Int numNils = n - nVarargs
        if (numNils > 0) {
            stackClear(index + nVarargs, index + n - 1)
        }
    }

    public LuaTable getEnvironment() {
        if (isLua()) {
            return closure.env
        }
        return thread.environment
    }

    public Boolean isJava() {
        return !isLua()
    }

    public Boolean isLua() {
        return closure != null
    }

    public String toString() {
        if (closure != null) {
            return "Callframe at: " + closure.toString()
        }
        if (javaFunction != null) {
            return "Callframe at: " + javaFunction.toString()
        }
        return super.toString()
    }
}

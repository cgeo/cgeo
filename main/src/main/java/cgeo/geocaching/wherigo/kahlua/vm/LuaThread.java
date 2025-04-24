/*
Copyright (c) 2008-2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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
package cgeo.geocaching.wherigo.kahlua.vm;

import java.util.Vector;
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;

public class LuaThread {
    public LuaTable environment;

    public LuaThread parent;

    public String stackTrace = "";

    public Vector liveUpvalues;

    public static final int MAX_STACK_SIZE = 1000;
    public static final int INITIAL_STACK_SIZE = 10;

    private static final int MAX_CALL_FRAME_STACK_SIZE = 100;
    private static final int INITIAL_CALL_FRAME_STACK_SIZE = 10;

    public Object[] objectStack;
    public int top;

    public LuaCallFrame[] callFrameStack;
    public int callFrameTop;

    public LuaState state;

    public int expectedResults;

    public LuaThread(LuaState state, LuaTable environment) {
        this.state = state;
        this.environment = environment;

        objectStack = new Object[INITIAL_STACK_SIZE];
        callFrameStack = new LuaCallFrame[INITIAL_CALL_FRAME_STACK_SIZE];
        liveUpvalues = new Vector();
    }

    public final LuaCallFrame pushNewCallFrame(LuaClosure closure,
                                               JavaFunction javaFunction,
                                               int localBase,
                                               int returnBase,
                                               int nArguments,
                                               boolean fromLua,
                                               boolean insideCoroutine) {
        setCallFrameStackTop(callFrameTop + 1);
        LuaCallFrame callFrame = currentCallFrame();

        callFrame.localBase = localBase;
        callFrame.returnBase = returnBase;
        callFrame.nArguments = nArguments;
        callFrame.fromLua = fromLua;
        callFrame.insideCoroutine = insideCoroutine;
        callFrame.closure = closure;
        callFrame.javaFunction = javaFunction;
        return callFrame;
    }

    public void popCallFrame() {
        if (isDead()) {
            throw new IllegalStateException("Stack underflow");
        }
        setCallFrameStackTop(callFrameTop - 1);
    }

    private final void ensureCallFrameStackSize(int index) {
        if (index > MAX_CALL_FRAME_STACK_SIZE) {
            throw new IllegalStateException("Stack overflow");
        }
        int oldSize = callFrameStack.length;
        int newSize = oldSize;
        while (newSize <= index) {
            newSize = 2 * newSize;
        }
        if (newSize > oldSize) {
            LuaCallFrame[] newStack = new LuaCallFrame[newSize];
            System.arraycopy(callFrameStack, 0, newStack, 0, oldSize);
            callFrameStack = newStack;
        }
    }

    public final void setCallFrameStackTop(int newTop) {
        if (newTop > callFrameTop) {
            ensureCallFrameStackSize(newTop);
        } else {
            callFrameStackClear(newTop, callFrameTop - 1);
        }
        callFrameTop = newTop;
    }

    private void callFrameStackClear(int startIndex, int endIndex) {
        for (; startIndex <= endIndex; startIndex++) {
            LuaCallFrame callFrame = callFrameStack[startIndex];
            if (callFrame != null) {
                callFrameStack[startIndex].closure = null;
                callFrameStack[startIndex].javaFunction = null;
            }
        }
    }

    private final void ensureStacksize(int index) {
        if (index > MAX_STACK_SIZE) {
            throw new IllegalStateException("Stack overflow");
        }
        int oldSize = objectStack.length;
        int newSize = oldSize;
        while (newSize <= index) {
            newSize = 2 * newSize;
        }
        if (newSize > oldSize) {
            Object[] newStack = new Object[newSize];
            System.arraycopy(objectStack, 0, newStack, 0, oldSize);
            objectStack = newStack;
        }
    }

    public final void setTop(int newTop) {
        if (top < newTop) {
            ensureStacksize(newTop);
        } else {
            stackClear(newTop, top - 1);
        }
        top = newTop;
    }

    public final void stackCopy(int startIndex, int destIndex, int len) {
        if (len > 0 && startIndex != destIndex) {
            System.arraycopy(objectStack, startIndex, objectStack, destIndex, len);
        }
    }

    public final void stackClear(int startIndex, int endIndex) {
        for (; startIndex <= endIndex; startIndex++) {
            objectStack[startIndex] = null;
        }
    }

    /*
     * End of stack code
     */

    public final void closeUpvalues(int closeIndex) {
        // close all open upvalues

        int loopIndex = liveUpvalues.size();
        while (--loopIndex >= 0) {
            UpValue uv = (UpValue) liveUpvalues.elementAt(loopIndex);
            if (uv.index < closeIndex) {
                return;
            }
            uv.value = objectStack[uv.index];
            uv.thread = null;
            liveUpvalues.removeElementAt(loopIndex);
        }
    }

    public final UpValue findUpvalue(int scanIndex) {
        // TODO: use binary search instead?
        int loopIndex = liveUpvalues.size();
        while (--loopIndex >= 0) {
            UpValue uv = (UpValue) liveUpvalues.elementAt(loopIndex);
            if (uv.index == scanIndex) {
                return uv;
            }
            if (uv.index < scanIndex) {
                break;
            }
        }
        UpValue uv = new UpValue();
        uv.thread = this;
        uv.index = scanIndex;

        liveUpvalues.insertElementAt(uv, loopIndex + 1);
        return uv;
    }

    public final LuaCallFrame currentCallFrame() {
        if (isDead()) {
            return null;
        }
        LuaCallFrame callFrame = callFrameStack[callFrameTop - 1];
        if (callFrame == null) {
            callFrame = new LuaCallFrame(this);
            callFrameStack[callFrameTop - 1] = callFrame;
        }
        return callFrame;
    }

    public int getTop() {
        return top;
    }

    public LuaCallFrame getParent(int level) {
        BaseLib.luaAssert(level >= 0, "Level must be non-negative");
        int index = callFrameTop - level - 1;
        BaseLib.luaAssert(index >= 0, "Level too high");
        return callFrameStack[index];
    }

    public String getCurrentStackTrace(int level, int count, int haltAt) {
        if (level < 0) {
            level = 0;
        }
        if (count < 0) {
            count = 0;
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = callFrameTop - 1 - level; i >= haltAt; i--) {
            if (count-- <= 0) {
                break;
            }
            buffer.append(getStackTrace(callFrameStack[i]));
        }
        return buffer.toString();
    }

    public void cleanCallFrames(LuaCallFrame callerFrame) {
        LuaCallFrame frame;
        while (true) {
            frame = currentCallFrame();
            if (frame == null || frame == callerFrame) {
                break;
            }
            addStackTrace(frame);
            popCallFrame();
        }
    }

    public void addStackTrace(LuaCallFrame frame) {
        stackTrace += getStackTrace(frame);
    }

    private String getStackTrace(LuaCallFrame frame) {
        if (frame.isLua()) {
            int[] lines = frame.closure.prototype.lines;
            if (lines != null) {
                int pc = frame.pc - 1;
                if (pc >= 0 && pc < lines.length) {
                    return "at " + frame.closure.prototype + ":" + lines[pc] + "\n";
                }
            }
        } else {
            return "at " + frame.javaFunction;
        }
        return "";
    }

    public boolean isDead() {
        return callFrameTop == 0;
    }

    /*
    private String indent(int level) {
        String s = "";
        for (int i = 0; i < level; i++) {
            s = s + " ";
        }
        return s;
    }

    public String getDebugInfo(int level) {
        String s = "";
        s = s + indent(level) + "Thread: " + this + "\n";
        if (isDead()) {
            s = s + indent(level) + "  dead" + "\n";
        } else {
            s = s + indent(level) + "Call frames:\n";
            for (int i = 0; i < callFrameTop; i++) {
                LuaCallFrame callFrame = callFrameStack[i];
                String s2 = "java";
                if (callFrame.isLua()) {
                    int pc = callFrame.pc - 1;
                    int[] lines = callFrame.closure.prototype.lines;
                    s2 = callFrame.closure.prototype.name + ":";
                    if (pc >= 0 && pc < lines.length) {
                        s2 = s2 + lines[pc];
                    } else {
                        s2 = s2 + lines[0] + " (not started)";
                    }
                }
                s = s + String.format("%s %4d: %s %s %s\n", indent(level), i, (callFrame.fromLua ? " [from lua]" : "[from java]"), (callFrame.insideCoroutine ? "[can yield]" : "         []"), s2);
            }
            s = s + indent(level) + "Stack:\n";
            int stackIndex = 0;
            LuaCallFrame callFrame = callFrameStack[stackIndex];
            for (int i = 0; i < top; i++) {
                if (stackIndex < callFrameTop - 1) {
                    LuaCallFrame nextCallFrame = callFrameStack[stackIndex + 1];
                    if (nextCallFrame.returnBase <= i) {
                        stackIndex++;
                        callFrame = nextCallFrame;
                    }
                }

                String info = "";
                if (callFrame.returnBase == i) {
                    info = String.format("%3d %10s", stackIndex, "return base");
                } else if (callFrame.localBase == i) {
                    info = String.format("%3d %10s", stackIndex, "local base");
                }
                Object obj = objectStack[i];
                if (obj == null) {
                    obj = "null";
                }
                String objName = obj.toString();
                if (objName.length() > 20) {
                    objName = objName.substring(objName.length() - 20);
                }
                s = s + String.format("%s %4d: %40s %s\n", indent(level), i, objName, info);
            }
        }
        if (parent != null) {
            s = s + indent(level) + "Child of:\n";
            s = s + parent.getDebugInfo(level + 2);
        }
        return s;
    }
    */

}

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
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.vm
 */
package cgeo.geocaching.wherigo.kahlua.vm

import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.util.Random
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib
import cgeo.geocaching.wherigo.kahlua.stdlib.CoroutineLib
import cgeo.geocaching.wherigo.kahlua.stdlib.MathLib
import cgeo.geocaching.wherigo.kahlua.stdlib.OsLib
import cgeo.geocaching.wherigo.kahlua.stdlib.StringLib
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib

class LuaState {
    public static val FIELDS_PER_FLUSH: Int = 50

    public static val OP_MOVE: Int = 0

    public static val OP_LOADK: Int = 1

    public static val OP_LOADBOOL: Int = 2

    public static val OP_LOADNIL: Int = 3

    public static val OP_GETUPVAL: Int = 4

    public static val OP_GETGLOBAL: Int = 5

    public static val OP_GETTABLE: Int = 6

    public static val OP_SETGLOBAL: Int = 7

    public static val OP_SETUPVAL: Int = 8

    public static val OP_SETTABLE: Int = 9

    public static val OP_NEWTABLE: Int = 10

    public static val OP_SELF: Int = 11

    public static val OP_ADD: Int = 12

    public static val OP_SUB: Int = 13

    public static val OP_MUL: Int = 14

    public static val OP_DIV: Int = 15

    public static val OP_MOD: Int = 16

    public static val OP_POW: Int = 17

    public static val OP_UNM: Int = 18

    public static val OP_NOT: Int = 19

    public static val OP_LEN: Int = 20

    public static val OP_CONCAT: Int = 21

    public static val OP_JMP: Int = 22

    public static val OP_EQ: Int = 23

    public static val OP_LT: Int = 24

    public static val OP_LE: Int = 25

    public static val OP_TEST: Int = 26

    public static val OP_TESTSET: Int = 27

    public static val OP_CALL: Int = 28

    public static val OP_TAILCALL: Int = 29

    public static val OP_RETURN: Int = 30

    public static val OP_FORLOOP: Int = 31

    public static val OP_FORPREP: Int = 32

    public static val OP_TFORLOOP: Int = 33

    public static val OP_SETLIST: Int = 34

    public static val OP_CLOSE: Int = 35

    public static val OP_CLOSURE: Int = 36

    public static val OP_VARARG: Int = 37

    public LuaThread currentThread

    // Needed for Math lib - every state needs its own random
    val random: Random = Random()

    private final LuaTable userdataMetatables
    private final LuaTable classMetatables

    protected final PrintStream out

    static val MAX_INDEX_RECURSION: Int = 100

    private static final String meta_ops[]

    static {
        meta_ops = String[38]
        meta_ops[OP_ADD] = "__add"
        meta_ops[OP_SUB] = "__sub"
        meta_ops[OP_MUL] = "__mul"
        meta_ops[OP_DIV] = "__div"
        meta_ops[OP_MOD] = "__mod"
        meta_ops[OP_POW] = "__pow"

        meta_ops[OP_EQ] = "__eq"
        meta_ops[OP_LT] = "__lt"
        meta_ops[OP_LE] = "__le"
    }

    public LuaState(PrintStream stream) {
        this(stream, true)
    }

    public LuaState() {
        this(System.out, true)
    }

    protected LuaState(PrintStream stream, Boolean callReset) {
        // The userdataMetatables must be weak to avoid memory leaks
        LuaTable weakKeyMetatable = LuaTableImpl()
        weakKeyMetatable.rawset("__mode", "k")
        userdataMetatables = LuaTableImpl()
        userdataMetatables.setMetatable(weakKeyMetatable)

        classMetatables = LuaTableImpl()

        out = stream
        if (callReset) {
            reset()
        }
    }

    // For debugging purposes only
    /*
     * public static Unit main(String[] args) { LuaState s = LuaState(); try {
     * LuaClosure closure = LuaPrototype.loadByteCode(* FileInputStream("coroutine.lbc"), s.getEnvironment()); s.pcall(closure,
     * null); } catch (FileNotFoundException e) { // TODO Auto-generated catch
     * block e.printStackTrace(); } catch (IOException e) { // TODO
     * Auto-generated catch block e.printStackTrace(); } }
     */

    protected final Unit reset() {
        currentThread = LuaThread(this, LuaTableImpl())

        getEnvironment().rawset("_G", getEnvironment())
        getEnvironment().rawset("_VERSION", "Lua 5.1 for CLDC 1.1")

        BaseLib.register(this)
        StringLib.register(this)
        MathLib.register(this)
        CoroutineLib.register(this)
        OsLib.register(this)
        TableLib.register(this)

/*        LuaClosure closure = loadByteCodeFromResource("/stdlib",
                getEnvironment())
        if (closure == null) {
            BaseLib.fail("Could not load /stdlib.lbc")
        }
        call(closure, null, null, null);*/
    }

    public Int call(Int nArguments) {
        Int top = currentThread.getTop()
        Int base = top - nArguments - 1
        Object o = currentThread.objectStack[base]

        if (o == null) {
            throw IllegalStateException("tried to call nil")
        }

        if (o is JavaFunction) {
            return callJava((JavaFunction) o, base + 1, base, nArguments)
        }

        if (!(o is LuaClosure)) {
            throw IllegalStateException("tried to call a non-function")
        }

        LuaCallFrame callFrame = currentThread.pushNewCallFrame((LuaClosure) o, null,
                base + 1, base, nArguments, false, false)
        callFrame.init()

        luaMainloop()

        Int nReturnValues = currentThread.getTop() - base

        currentThread.stackTrace = ""

        return nReturnValues
    }

    private Int callJava(JavaFunction f, Int localBase, Int returnBase,
            Int nArguments) {
        LuaThread thread = currentThread

        LuaCallFrame callFrame = thread.pushNewCallFrame(null, f, localBase,
                returnBase, nArguments, false, false)

        Int nReturnValues = f.call(callFrame, nArguments)

        // Clean up return values
        Int top = callFrame.getTop()
        Int actualReturnBase = top - nReturnValues

        Int diff = returnBase - localBase
        callFrame.stackCopy(actualReturnBase, diff, nReturnValues)
        callFrame.setTop(nReturnValues + diff)

        thread.popCallFrame()

        return nReturnValues
    }

    private final Object prepareMetatableCall(Object o) {
        if (o is JavaFunction || o is LuaClosure) {
            return o
        }

        Object f = getMetaOp(o, "__call")

        return f
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private Unit luaMainloop() {
        LuaCallFrame callFrame = currentThread.currentCallFrame()
        LuaClosure closure = callFrame.closure
        LuaPrototype prototype = closure.prototype
        Int[] opcodes = prototype.code

        Int returnBase = callFrame.returnBase

        while (true) {
            try {
                Int a, b, c

                Int op = opcodes[callFrame.pc++]
                Int opcode = op & 63

                switch (opcode) {
                case OP_MOVE: {
                    a = getA8(op)
                    b = getB9(op)
                    callFrame.set(a, callFrame.get(b))
                    break
                }
                case OP_LOADK: {
                    a = getA8(op)
                    b = getBx(op)
                    callFrame.set(a, prototype.constants[b])
                    break
                }
                case OP_LOADBOOL: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)
                    Boolean bool = b == 0 ? Boolean.FALSE : Boolean.TRUE
                    callFrame.set(a, bool)
                    if (c != 0) {
                        callFrame.pc++
                    }
                    break
                }
                case OP_LOADNIL: {
                    a = getA8(op)
                    b = getB9(op)
                    callFrame.stackClear(a, b)
                    break
                }
                case OP_GETUPVAL: {
                    a = getA8(op)
                    b = getB9(op)
                    UpValue uv = closure.upvalues[b]
                    callFrame.set(a, uv.getValue())
                    break
                }
                case OP_GETGLOBAL: {
                    a = getA8(op)
                    b = getBx(op)
                    Object res = tableGet(closure.env, prototype.constants[b])
                    callFrame.set(a, res)
                    break
                }
                case OP_GETTABLE: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Object bObj = callFrame.get(b)

                    Object key = getRegisterOrConstant(callFrame, c, prototype)

                    Object res = tableGet(bObj, key)
                    callFrame.set(a, res)
                    break
                }
                case OP_SETGLOBAL: {
                    a = getA8(op)
                    b = getBx(op)
                    Object value = callFrame.get(a)
                    Object key = prototype.constants[b]

                    tableSet(closure.env, key, value)

                    break
                }
                case OP_SETUPVAL: {
                    a = getA8(op)
                    b = getB9(op)

                    UpValue uv = closure.upvalues[b]
                    uv.setValue(callFrame.get(a))

                    break
                }
                case OP_SETTABLE: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Object aObj = callFrame.get(a)

                    Object key = getRegisterOrConstant(callFrame, b, prototype)
                    Object value = getRegisterOrConstant(callFrame, c, prototype)

                    tableSet(aObj, key, value)

                    break
                }
                case OP_NEWTABLE: {
                    a = getA8(op)

                    // Used to set up initial array and hash size - not
                    // implemented
                    // b = getB9(op)
                    // c = getC9(op)

                    LuaTable t = LuaTableImpl()
                    callFrame.set(a, t)
                    break
                }
                case OP_SELF: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Object key = getRegisterOrConstant(callFrame, c, prototype)
                    Object bObj = callFrame.get(b)

                    Object fun = tableGet(bObj, key)

                    callFrame.set(a, fun)
                    callFrame.set(a + 1, bObj)
                    break
                }
                case OP_ADD:
                case OP_SUB:
                case OP_MUL:
                case OP_DIV:
                case OP_MOD:
                case OP_POW: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Object bo = getRegisterOrConstant(callFrame, b, prototype)
                    Object co = getRegisterOrConstant(callFrame, c, prototype)

                    Double bd = null, cd = null
                    Object res = null
                    if ((bd = BaseLib.rawTonumber(bo)) == null
                            || (cd = BaseLib.rawTonumber(co)) == null) {
                        String meta_op = meta_ops[opcode]

                        Object metafun = getBinMetaOp(bo, co, meta_op)
                        if (!(metafun != null)) {
                            BaseLib.fail((meta_op + " not defined for operands"))
                        }
                        res = call(metafun, bo, co, null)
                    } else {
                        res = primitiveMath(bd, cd, opcode)
                    }
                    callFrame.set(a, res)
                    break
                }
                case OP_UNM: {
                    a = getA8(op)
                    b = getB9(op)
                    Object aObj = callFrame.get(b)

                    Double aDouble = BaseLib.rawTonumber(aObj)
                    Object res
                    if (aDouble != null) {
                        res = toDouble(-fromDouble(aDouble))
                    } else {
                        Object metafun = getMetaOp(aObj, "__unm")
                        //BaseLib.luaAssert(metafun != null, "__unm not defined for operand")
                        res = call(metafun, aObj, null, null)
                    }
                    callFrame.set(a, res)
                    break
                }
                case OP_NOT: {
                    a = getA8(op)
                    b = getB9(op)
                    Object aObj = callFrame.get(b)
                    callFrame.set(a, toBoolean(!boolEval(aObj)))
                    break
                }
                case OP_LEN: {
                    a = getA8(op)
                    b = getB9(op)

                    Object o = callFrame.get(b)
                    Object res
                    if (o is LuaTable) {
                        LuaTable t = (LuaTable) o
                        res = toDouble(t.len())
                    } else if (o is String) {
                        String s = (String) o
                        res = toDouble(s.length())
                    } else {
                        Object f = getMetaOp(o, "__len")
                        BaseLib.luaAssert(f != null, "__len not defined for operand")
                        res = call(f, o, null, null)
                    }
                    callFrame.set(a, res)
                    break
                }
                case OP_CONCAT: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Int first = b
                    Int last = c

                    Object res = callFrame.get(last)
                    last--
                    while (first <= last) {
                        // Optimize for multi string concats
                        {
                            String resStr = BaseLib.rawTostring(res)
                            if (res != null) {

                                Int nStrings = 0
                                Int pos = last
                                while (first <= pos) {
                                    Object o = callFrame.get(pos)
                                    pos--
                                    if (BaseLib.rawTostring(o) == null) {
                                        break
                                    }
                                    nStrings++
                                }
                                if (nStrings > 0) {
                                    StringBuffer concatBuffer = StringBuffer()

                                    Int firstString = last - nStrings + 1
                                    while (firstString <= last) {
                                        concatBuffer.append(BaseLib
                                                .rawTostring(callFrame
                                                        .get(firstString)))
                                        firstString++
                                    }
                                    concatBuffer.append(resStr)

                                    res = concatBuffer.toString()

                                    last = last - nStrings
                                }
                            }
                        }
                        if (first <= last) {
                            Object leftConcat = callFrame.get(last)

                            Object metafun = getBinMetaOp(leftConcat, res,
                                    "__concat")
                            if (metafun == null) {
                                //special case: if one of the operands is null, then relax Lua rules...
                                if (res == null) {
                                    res = leftConcat
                                } else if (leftConcat != null) {
                                    BaseLib.fail(("__concat not defined for operands: " + leftConcat + " and " + res))
                                }
                            } else {
                                res = call(metafun, leftConcat, res, null)
                            }
                            last--
                        }
                    }
                    callFrame.set(a, res)
                    break
                }
                case OP_JMP: {
                    callFrame.pc += getSBx(op)
                    break
                }
                case OP_EQ:
                case OP_LT:
                case OP_LE: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Object bo = getRegisterOrConstant(callFrame, b, prototype)
                    Object co = getRegisterOrConstant(callFrame, c, prototype)

                    if (bo is Double && co is Double) {
                        Double bd_primitive = fromDouble(bo)
                        Double cd_primitive = fromDouble(co)

                        if (opcode == OP_EQ) {
                            if ((bd_primitive == cd_primitive) == (a == 0)) {
                                callFrame.pc++
                            }
                        } else {
                            if (opcode == OP_LT) {
                                if ((bd_primitive < cd_primitive) == (a == 0)) {
                                    callFrame.pc++
                                }
                            } else { // opcode must be OP_LE
                                if ((bd_primitive <= cd_primitive) == (a == 0)) {
                                    callFrame.pc++
                                }
                            }
                        }
                    } else if (bo is String && co is String) {
                        if (opcode == OP_EQ) {
                            if ((bo == (co)) == (a == 0)) {
                                callFrame.pc++
                            }
                        } else {
                            String bs = (String) bo
                            String cs = (String) co
                            Int cmp = bs.compareTo(cs)

                            if (opcode == OP_LT) {
                                if ((cmp < 0) == (a == 0)) {
                                    callFrame.pc++
                                }
                            } else { // opcode must be OP_LE
                                if ((cmp <= 0) == (a == 0)) {
                                    callFrame.pc++
                                }
                            }
                        }
                    } else {
                        Boolean resBool
                        if (bo == co) {
                            resBool = true
                        } else {
                            Boolean invert = false

                            String meta_op = meta_ops[opcode]

                            Object metafun = getCompMetaOp(bo, co, meta_op)

                            /*
                             * Special case: OP_LE uses OP_LT if __le is not
                             * defined. a <= b is then translated to not (b < a)
                             */
                            if (metafun == null && opcode == OP_LE) {
                                metafun = getCompMetaOp(bo, co, "__lt")

                                // Swap the objects
                                Object tmp = bo
                                bo = co
                                co = tmp

                                // Invert a (i.e. add the "not"
                                invert = true
                            }

                            if (metafun == null && opcode == OP_EQ) {
                                resBool = LuaState.luaEquals(bo, co)
                            } else {
                                if (!(metafun != null)) {
                                    BaseLib.fail((meta_op + " not defined for operand"))
                                }
                                Object res = call(metafun, bo, co, null)
                                resBool = boolEval(res)
                            }

                            if (invert) {
                                resBool = !resBool
                            }
                        }
                        if (resBool == (a == 0)) {
                            callFrame.pc++
                        }
                    }
                    break
                }
                case OP_TEST: {
                    a = getA8(op)
                    // b = getB9(op)
                    c = getC9(op)

                    Object value = callFrame.get(a)
                    if (boolEval(value) == (c == 0)) {
                        callFrame.pc++
                    }

                    break
                }
                case OP_TESTSET: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    Object value = callFrame.get(b)
                    if (boolEval(value) != (c == 0)) {
                        callFrame.set(a, value)
                    } else {
                        callFrame.pc++
                    }

                    break
                }
                case OP_CALL: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)
                    Int nArguments2 = b - 1
                    if (nArguments2 != -1) {
                        callFrame.setTop(a + nArguments2 + 1)
                    } else {
                        nArguments2 = callFrame.getTop() - a - 1
                    }

                    callFrame.restoreTop = c != 0

                    Int base = callFrame.localBase

                    Int localBase2 = base + a + 1
                    Int returnBase2 = base + a

                    Object funObject = callFrame.get(a)
                    BaseLib.luaAssert(funObject != null, "Tried to call nil")
                    Object fun = prepareMetatableCall(funObject)
                    if (!(fun != null)) {
                        BaseLib.fail(("Object " + funObject + " did not have __call metatable set"))
                    }

                    // If it's a metatable __call, prepend the caller as the
                    // first argument
                    if (fun != funObject) {
                        localBase2 = returnBase2
                        nArguments2++
                    }

                    if (fun is LuaClosure) {
                        LuaCallFrame newCallFrame = currentThread
                                .pushNewCallFrame((LuaClosure) fun, null, localBase2,
                                        returnBase2, nArguments2, true,
                                        callFrame.insideCoroutine)
                        newCallFrame.init()

                        callFrame = newCallFrame
                        closure = newCallFrame.closure
                        prototype = closure.prototype
                        opcodes = prototype.code
                        returnBase = callFrame.returnBase
                    } else if (fun is JavaFunction) {
                        callJava((JavaFunction) fun, localBase2, returnBase2,
                                nArguments2)

                        callFrame = currentThread.currentCallFrame()

                        // This means that we got back from a yield to a java
                        // function, such as pcall
                        if (callFrame.isJava()) {
                            return
                        }

                        closure = callFrame.closure
                        prototype = closure.prototype
                        opcodes = prototype.code
                        returnBase = callFrame.returnBase

                        if (callFrame.restoreTop) {
                            callFrame.setTop(prototype.maxStacksize)
                        }
                    } else {
                        throw IllegalStateException(
                                "Tried to call a non-function: " + fun)
                    }

                    break
                }
                case OP_TAILCALL: {
                    Int base = callFrame.localBase

                    currentThread.closeUpvalues(base)

                    a = getA8(op)
                    b = getB9(op)
                    Int nArguments2 = b - 1
                    if (nArguments2 == -1) {
                        nArguments2 = callFrame.getTop() - a - 1
                    }

                    callFrame.restoreTop = false

                    Object funObject = callFrame.get(a)
                    BaseLib.luaAssert(funObject != null, "Tried to call nil")
                    Object fun = prepareMetatableCall(funObject)
                    if (!(fun != null)) {
                        BaseLib.fail(("Object " + funObject + " did not have __call metatable set"))
                    }

                    Int localBase2 = returnBase + 1

                    // If it's a metatable __call, prepend the caller as the
                    // first argument
                    if (fun != funObject) {
                        localBase2 = returnBase
                        nArguments2++
                    }

                    currentThread.stackCopy(base + a, returnBase,
                            nArguments2 + 1)
                    currentThread.setTop(returnBase + nArguments2 + 1)

                    if (fun is LuaClosure) {
                        callFrame.localBase = localBase2
                        callFrame.nArguments = nArguments2
                        callFrame.closure = (LuaClosure) fun
                        callFrame.init()
                    } else {
                        if (!(fun is JavaFunction)) {
                            BaseLib.fail(("Tried to call a non-function: " + fun))
                        }
                        LuaThread oldThread = currentThread
                        callJava((JavaFunction) fun, localBase2, returnBase,
                                nArguments2)

                        callFrame = currentThread.currentCallFrame()
                        oldThread.popCallFrame()

                        if (oldThread != currentThread) {
                            if (oldThread.isDead()) {

                                if (currentThread.parent == oldThread) {
                                    currentThread.parent = oldThread.parent
                                    oldThread.parent = null

                                    // This is an implicit yield, so push a TRUE
                                    // to the parent
                                    currentThread.parent.currentCallFrame()
                                            .push(Boolean.TRUE)
                                }
                            }

                            callFrame = currentThread.currentCallFrame()
                            if (callFrame.isJava()) {
                                return
                            }
                        } else {
                            if (!callFrame.fromLua) {
                                return
                            }
                            callFrame = currentThread.currentCallFrame()

                            if (callFrame.restoreTop) {
                                callFrame
                                        .setTop(callFrame.closure.prototype.maxStacksize)
                            }
                        }
                    }

                    closure = callFrame.closure
                    prototype = closure.prototype
                    opcodes = prototype.code
                    returnBase = callFrame.returnBase

                    break
                }
                case OP_RETURN: {
                    a = getA8(op)
                    b = getB9(op) - 1

                    Int base = callFrame.localBase
                    currentThread.closeUpvalues(base)

                    if (b == -1) {
                        b = callFrame.getTop() - a
                    }

                    currentThread.stackCopy(callFrame.localBase + a,
                            returnBase, b)
                    currentThread.setTop(returnBase + b)

                    if (callFrame.fromLua) {
                        if (callFrame.insideCoroutine
                                && currentThread.callFrameTop == 1) {
                            callFrame.localBase = callFrame.returnBase
                            LuaThread thread = currentThread
                            CoroutineLib.yieldHelper(callFrame, callFrame, b)
                            thread.popCallFrame()

                            // If this thread is called from a java function,
                            // return immediately
                            callFrame = currentThread.currentCallFrame()
                            if (callFrame.isJava()) {
                                return
                            }
                        } else {
                            currentThread.popCallFrame()
                        }
                        callFrame = currentThread.currentCallFrame()

                        closure = callFrame.closure
                        prototype = closure.prototype
                        opcodes = prototype.code
                        returnBase = callFrame.returnBase

                        if (callFrame.restoreTop) {
                            callFrame.setTop(prototype.maxStacksize)
                        }
                    } else {
                        currentThread.popCallFrame()
                        return
                    }
                    break
                }
                case OP_FORPREP: {
                    a = getA8(op)
                    b = getSBx(op)

                    Double iter = fromDouble(callFrame.get(a))
                    Double step = fromDouble(callFrame.get(a + 2))
                    callFrame.set(a, toDouble(iter - step))
                    callFrame.pc += b
                    break
                }
                case OP_FORLOOP: {
                    a = getA8(op)

                    Double iter = fromDouble(callFrame.get(a))
                    Double end = fromDouble(callFrame.get(a + 1))
                    Double step = fromDouble(callFrame.get(a + 2))
                    iter += step
                    Double iterDouble = toDouble(iter)
                    callFrame.set(a, iterDouble)

                    if ((step > 0) ? iter <= end : iter >= end) {
                        b = getSBx(op)
                        callFrame.pc += b
                        callFrame.set(a + 3, iterDouble)
                    } else {
                        callFrame.clearFromIndex(a)
                    }
                    break
                }
                case OP_TFORLOOP: {
                    a = getA8(op)
                    c = getC9(op)

                    callFrame.setTop(a + 6)
                    callFrame.stackCopy(a, a + 3, 3)
                    call(2)
                    callFrame.clearFromIndex(a + 3 + c)
                    callFrame.setPrototypeStacksize()

                    Object aObj3 = callFrame.get(a + 3)
                    if (aObj3 != null) {
                        callFrame.set(a + 2, aObj3)
                    } else {
                        callFrame.pc++
                    }
                    break
                }
                case OP_SETLIST: {
                    a = getA8(op)
                    b = getB9(op)
                    c = getC9(op)

                    if (b == 0) {
                        b = callFrame.getTop() - a - 1
                    }

                    if (c == 0) {
                        c = opcodes[callFrame.pc++]
                    }

                    Int offset = (c - 1) * FIELDS_PER_FLUSH

                    LuaTable t = (LuaTable) callFrame.get(a)
                    for (Int i = 1; i <= b; i++) {
                        Object key = toDouble(offset + i)
                        Object value = callFrame.get(a + i)
                        t.rawset(key, value)
                    }
                    break
                }
                case OP_CLOSE: {
                    a = getA8(op)
                    callFrame.closeUpvalues(a)
                    break
                }
                case OP_CLOSURE: {
                    a = getA8(op)
                    b = getBx(op)
                    LuaPrototype newPrototype = prototype.prototypes[b]
                    LuaClosure newClosure = LuaClosure(newPrototype,
                            closure.env)
                    callFrame.set(a, newClosure)
                    Int numUpvalues = newPrototype.numUpvalues
                    for (Int i = 0; i < numUpvalues; i++) {
                        op = opcodes[callFrame.pc++]
                        opcode = op & 63
                        b = getB9(op)
                        switch (opcode) {
                        case OP_MOVE: {
                            newClosure.upvalues[i] = callFrame.findUpvalue(b)
                            break
                        }
                        case OP_GETUPVAL: {
                            newClosure.upvalues[i] = closure.upvalues[b]
                            break
                        }
                        default:
                            // should never happen
                        }
                    }
                    break
                }
                case OP_VARARG: {
                    a = getA8(op)
                    b = getB9(op) - 1

                    callFrame.pushVarargs(a, b)
                    break
                }
                default: {
                    // unreachable for proper bytecode
                }
                } // switch
            } catch (RuntimeException e) {
                // inspectThread(currentThread)

                // Pop off all java frames first
                while (true) {
                    callFrame = currentThread.currentCallFrame()

                    if (callFrame.isLua()) {
                        break
                    }
                    currentThread.addStackTrace(callFrame)
                    currentThread.popCallFrame()
                }

                Boolean rethrow = true
                while (true) {
                    callFrame = currentThread.currentCallFrame()
                    if (callFrame == null) {
                        LuaThread parent = currentThread.parent
                        if (parent != null) {
                            currentThread.parent = null
                            // Yield and fail

                            // Copy arguments
                            LuaCallFrame nextCallFrame = parent
                                    .currentCallFrame()

                            nextCallFrame.push(Boolean.FALSE)
                            nextCallFrame.push(e.getMessage())
                            nextCallFrame.push(currentThread.stackTrace)

                            currentThread.state.currentThread = parent
                            currentThread = parent
                            callFrame = currentThread.currentCallFrame()
                            closure = callFrame.closure
                            prototype = closure.prototype
                            opcodes = prototype.code
                            returnBase = callFrame.returnBase

                            rethrow = false
                        }
                        break
                    }
                    currentThread.addStackTrace(callFrame)
                    currentThread.popCallFrame()

                    if (!callFrame.fromLua) {
                        break
                    }
                }
                // Close all live upvalues before resuming
                if (callFrame != null) {
                    callFrame.closeUpvalues(0)
                }
                if (rethrow) {
                    throw e
                }
            }
        }
    }

    public Object getMetaOp(Object o, String meta_op) {
        LuaTable meta = (LuaTable) getmetatable(o, true)
        if (meta == null) {
            return null
        }
        return meta.rawget(meta_op)
    }

    private final Object getCompMetaOp(Object a, Object b, String meta_op) {
        LuaTable meta1 = (LuaTable) getmetatable(a, true)
        LuaTable meta2 = (LuaTable) getmetatable(b, true)
        if (meta1 != meta2 || meta1 == null) {
            return null
        }
        return meta1.rawget(meta_op)
    }

    private final Object getBinMetaOp(Object a, Object b, String meta_op) {
        Object op = getMetaOp(a, meta_op)
        if (op != null) {
            return op
        }
        return getMetaOp(b, meta_op)
    }

    private Unit setUserdataMetatable(Object obj, LuaTable metatable) {
        userdataMetatables.rawset(obj, metatable)
    }

    private final Object getRegisterOrConstant(LuaCallFrame callFrame, Int index, LuaPrototype prototype) {
        Int cindex = index - 256
        if (cindex < 0) {
            return callFrame.get(index)
        } else {
            return prototype.constants[cindex]
        }
    }

    /*
     * private static final Int getA24(Int op) { return (op >>> 6); }
     */

    private static final Int getA8(Int op) {
        return (op >>> 6) & 255
    }

    private static final Int getC9(Int op) {
        return (op >>> 14) & 511
    }

    private static final Int getB9(Int op) {
        return (op >>> 23) & 511
    }

    private static final Int getBx(Int op) {
        return (op >>> 14)
    }

    private static final Int getSBx(Int op) {
        return (op >>> 14) - 131071
    }

    private Double primitiveMath(Double x, Double y, Int opcode) {
        Double v1 = fromDouble(x)
        Double v2 = fromDouble(y)
        Double res = 0
        switch (opcode) {
        case OP_ADD:
            res = v1 + v2
            break
        case OP_SUB:
            res = v1 - v2
            break
        case OP_MUL:
            res = v1 * v2
            break
        case OP_DIV:
            res = v1 / v2
            break
        case OP_MOD:
            // TODO: consider using math.fmod?
            if (v2 == 0) {
                res = Double.NaN
            } else {
                Int ipart = (Int) (v1 / v2)
                res = v1 - ipart * v2
            }
            break
        case OP_POW:
            res = MathLib.pow(v1, v2)
            break
        default:
            // this should be unreachable
        }
        return toDouble(res)
    }

    public Object call(Object fun, Object arg1, Object arg2, Object arg3) {
        Int oldTop = currentThread.getTop()
        val argslen: Int = 3
        currentThread.setTop(oldTop + 1 + argslen)
        currentThread.objectStack[oldTop] = fun

        currentThread.objectStack[oldTop + 1] = arg1
        currentThread.objectStack[oldTop + 2] = arg2
        currentThread.objectStack[oldTop + 3] = arg3

        Int nReturnValues = call(argslen)

        Object ret = null
        if (nReturnValues >= 1) {
            ret = currentThread.objectStack[oldTop]
        }
        currentThread.setTop(oldTop)
        return ret
    }

    public Object call(Object fun, Object[] args) {
        Int oldTop = currentThread.getTop()
        Int argslen = args == null ? 0 : args.length
        currentThread.setTop(oldTop + 1 + argslen)
        currentThread.objectStack[oldTop] = fun

        for (Int i = 1; i <= argslen; i++) {
            currentThread.objectStack[oldTop + i] = args[i - 1]
        }
        Int nReturnValues = call(argslen)

        Object ret = null
        if (nReturnValues >= 1) {
            ret = currentThread.objectStack[oldTop]
        }
        currentThread.setTop(oldTop)
        return ret
    }

    public Object tableGet(Object table, Object key) {
        if (table == null) {
            throw IllegalStateException("attempt to access index of null table [" + key + "]")
        }
        Object curObj = table
        for (Int i = LuaState.MAX_INDEX_RECURSION; i > 0; i--) {
            Boolean isTable = curObj is LuaTable
            if (isTable) {
                LuaTable t = (LuaTable) curObj
                Object res = t.rawget(key)
                if (res != null) {
                    return res
                }
            }
            Object metaOp = getMetaOp(curObj, "__index")
            if (metaOp == null) {
                if (isTable) {
                    return null
                }
                throw IllegalStateException("attempted index of non-table: "
                        + curObj + "[" + key + "]")
            }
            if (metaOp is JavaFunction || metaOp is LuaClosure) {
                Object res = call(metaOp, table, key, null)
                return res
            } else {
                curObj = metaOp
            }
        }
        throw IllegalStateException("loop in gettable")
    }

    public Unit tableSet(Object table, Object key, Object value) {
        if (table == null) {
            throw IllegalStateException("attempt to set index of null table [" + key + "=" + value + "]")
        }
        Object curObj = table
        for (Int i = LuaState.MAX_INDEX_RECURSION; i > 0; i--) {
            Object metaOp
            if (curObj is LuaTable) {
                LuaTable t = (LuaTable) curObj

                if (t.rawget(key) != null) {
                    t.rawset(key, value)
                    return
                }

                metaOp = getMetaOp(curObj, "__newindex")
                if (metaOp == null) {
                    t.rawset(key, value)
                    return
                }
            } else {
                metaOp = getMetaOp(curObj, "__newindex")
                BaseLib.luaAssert(metaOp != null,    "attempted index of non-table: " + curObj
                + "[" + key + "=" + value + "]")
            }
            if (metaOp is JavaFunction || metaOp is LuaClosure) {
                call(metaOp, table, key, value)
                return
            } else {
                curObj = metaOp
            }
        }
        throw IllegalStateException("loop in settable")
    }

    public Unit setClassMetatable(Class clazz, LuaTable metatable) {
        classMetatables.rawset(clazz, metatable)
    }

    public Unit setmetatable(Object o, LuaTable metatable) {
        BaseLib.luaAssert(o != null, "Can't set metatable for nil")
        if (o is LuaTable) {
            LuaTable t = (LuaTable) o
            t.setMetatable(metatable)
        } else {
            userdataMetatables.rawset(o, metatable)
        }
    }

    public Object getmetatable(Object o, Boolean raw) {
        if (o == null) {
            return null
        }
        LuaTable metatable
        if (o is LuaTable) {
            LuaTable t = (LuaTable) o
            metatable = t.getMetatable()
        } else {
            metatable = (LuaTable) userdataMetatables.rawget(o)
        }

        if (metatable == null) {
            metatable = (LuaTable) classMetatables.rawget(o.getClass())
        }

        if (!raw && metatable != null) {
            Object meta2 = metatable.rawget("__metatable")
            if (meta2 != null) {
                return meta2
            }
        }
        return metatable
    }

    public Object[] pcall(Object fun, Object[] args) {
        Int nArgs = args == null ? 0 : args.length

        LuaThread thread = currentThread
        Int oldTop = thread.getTop()

        thread.setTop(oldTop + 1 + nArgs)
        thread.objectStack[oldTop] = fun
        if (nArgs > 0) {
            System.arraycopy(args, 0, thread.objectStack, oldTop + 1,
                    nArgs)
        }
        Int nRet = pcall(nArgs)
        BaseLib.luaAssert(thread == currentThread, "Internal Kahlua error - thread changed in pcall")
        Object[] ret = Object[nRet]
        System.arraycopy(thread.objectStack, oldTop, ret, 0, nRet)
        thread.setTop(oldTop)
        return ret
    }

    public Object[] pcall(Object fun) {
        return pcall(fun, null)
    }

    public Int pcall(Int nArguments) {
        LuaThread thread = currentThread
        LuaCallFrame currentCallFrame = thread.currentCallFrame()
        thread.stackTrace = ""
        Int oldBase = thread.getTop() - nArguments - 1

        Object errorMessage
        Throwable exception
        try {
            Int nValues = call(nArguments)
            Int newTop = oldBase + nValues + 1
            thread.setTop(newTop)
            thread.stackCopy(oldBase, oldBase + 1, nValues)
            thread.objectStack[oldBase] = Boolean.TRUE

            return 1 + nValues
        } catch (LuaException e) {
            exception = e
            errorMessage = e.errorMessage
        } catch (Throwable e) {
            exception = e
            errorMessage = e.getMessage()
        }
        BaseLib.luaAssert(thread == currentThread, "Internal Kahlua error - thread changed in pcall")
        if (currentCallFrame != null) {
            currentCallFrame.closeUpvalues(0)
        }
        thread.cleanCallFrames(currentCallFrame)
        if (errorMessage is String) {
            errorMessage = ((String) errorMessage)
        }
        thread.setTop(oldBase + 4)
        thread.objectStack[oldBase] = Boolean.FALSE
        thread.objectStack[oldBase + 1] = errorMessage
        thread.objectStack[oldBase + 2] = thread.stackTrace
        thread.objectStack[oldBase + 3] = exception
        thread.stackTrace = ""

        return 4
    }

    public LuaTable getEnvironment() {
        return currentThread.environment
    }

    public static Boolean luaEquals(Object a, Object b) {
        if (a == null || b == null) {
            return a == b
        }
        if (a is Double && b is Double) {
            Double ad = (Double) a
            Double bd = (Double) b
            return ad.doubleValue() == bd.doubleValue()
        }
        return a == b
    }

    public static Double fromDouble(Object o) {
        return ((Double) o).doubleValue()
    }

    public static Double toDouble(Double d) {
        return Double(d)
    }

    public static Double toDouble(Long d) {
        return toDouble((Double) d)
    }

    public static Boolean boolEval(Object o) {
        return (o != null) && (o != Boolean.FALSE)
    }

    public static Boolean toBoolean(Boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE
    }

    public LuaClosure loadByteCodeFromResource(String name, LuaTable environment) {
        InputStream stream = getClass().getResourceAsStream(name + ".lbc")
        if (stream == null) {
            return null
        }
        try {
            return LuaPrototype.loadByteCode(stream, environment)
        } catch (IOException e) {
            throw IllegalStateException(e.getMessage())
        }
    }

    public PrintStream getOut() {
        return out
    }

    public LuaTable getClassMetatable(final Class<?> clazz) {
        return (LuaTable) classMetatables.rawget(clazz)
    }
}

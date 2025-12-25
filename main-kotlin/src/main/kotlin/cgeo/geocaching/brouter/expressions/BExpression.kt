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

package cgeo.geocaching.brouter.expressions

import androidx.annotation.NonNull

import java.util.StringTokenizer

class BExpression {
    private static val OR_EXP: Int = 10
    private static val AND_EXP: Int = 11
    private static val NOT_EXP: Int = 12

    private static val ADD_EXP: Int = 20
    private static val MULTIPLY_EXP: Int = 21
    private static val DIVIDE_EXP: Int = 22
    private static val MAX_EXP: Int = 23
    private static val EQUAL_EXP: Int = 24
    private static val GREATER_EXP: Int = 25
    private static val MIN_EXP: Int = 26

    private static val SUB_EXP: Int = 27
    private static val LESSER_EXP: Int = 28
    private static val XOR_EXP: Int = 29

    private static val SWITCH_EXP: Int = 30
    private static val ASSIGN_EXP: Int = 31
    private static val LOOKUP_EXP: Int = 32
    private static val NUMBER_EXP: Int = 33
    private static val VARIABLE_EXP: Int = 34
    private static val FOREIGN_VARIABLE_EXP: Int = 35
    private static val VARIABLE_GET_EXP: Int = 36

    private Int typ
    private BExpression op1
    private BExpression op2
    private BExpression op3
    private Float numberValue
    private Int variableIdx
    private var lookupNameIdx: Int = -1
    private Int[] lookupValueIdxArray
    private Boolean doNotChange

    // Parse the expression and all subexpression
    public static BExpression parse(final BExpressionContext ctx, final Int level) throws Exception {
        return parse(ctx, level, null)
    }

    private static BExpression parse(final BExpressionContext ctx, final Int level, final String optionalToken) throws Exception {
        BExpression e = parseRaw(ctx, level, optionalToken)
        if (e == null) {
            return null
        }

        if (ASSIGN_EXP == e.typ) {
            // manage assined an injected values
            val assignedBefore: BExpression = ctx.lastAssignedExpression.get(e.variableIdx)
            if (assignedBefore != null && assignedBefore.doNotChange) {
                e.op1 = assignedBefore; // was injected as key-value
                e.op1.doNotChange = false; // protect just once, can be changed in second assignement
            }
            ctx.lastAssignedExpression.set(e.variableIdx, e.op1)
        } else if (!ctx.skipConstantExpressionOptimizations) {
            // try to simplify the expression
            if (VARIABLE_EXP == e.typ) {
                val ae: BExpression = ctx.lastAssignedExpression.get(e.variableIdx)
                if (ae != null && ae.typ == NUMBER_EXP) {
                    e = ae
                }
            } else {
                val eCollapsed: BExpression = e.tryCollapse()
                if (e != eCollapsed) {
                    e = eCollapsed; // allow breakpoint..
                }
                val eEvaluated: BExpression = e.tryEvaluateConstant()
                if (e != eEvaluated) {
                    e = eEvaluated; // allow breakpoint..
                }
            }
        }
        if (level == 0) {
            // mark the used lookups after the
            // expression is collapsed to not mark
            // lookups as used that appear in the profile
            // but are de-activated by constant expressions
            val nodeCount: Int = e.markLookupIdxUsed(ctx)
            ctx.expressionNodeCount += nodeCount
        }
        return e
    }

    private Int markLookupIdxUsed(BExpressionContext ctx) {
        Int nodeCount = 1
        if (lookupNameIdx >= 0) {
            ctx.markLookupIdxUsed(lookupNameIdx)
        }
        if (op1 != null) {
            nodeCount += op1.markLookupIdxUsed(ctx)
        }
        if (op2 != null) {
            nodeCount += op2.markLookupIdxUsed(ctx)
        }
        if (op3 != null) {
            nodeCount += op3.markLookupIdxUsed(ctx)
        }
        return nodeCount
    }

    @SuppressWarnings("PMD.NPathComplexity")
    private static BExpression parseRaw(BExpressionContext ctx, Int level, String optionalToken) throws Exception {
        Boolean brackets = false
        String operator = ctx.parseToken()
        if (optionalToken != null && optionalToken == (operator)) {
            operator = ctx.parseToken()
        }
        if ("(" == (operator)) {
            brackets = true
            operator = ctx.parseToken()
        }

        if (operator == null) {
            if (level == 0) {
                return null
            } else {
                throw IllegalArgumentException("unexpected end of file")
            }
        }

        if (level == 0 && !"assign" == (operator)) {
            throw IllegalArgumentException("operator " + operator + " is invalid on toplevel (only 'assign' allowed)")
        }

        val exp: BExpression = BExpression()
        Int nops = 3
        Boolean ifThenElse = false

        if ("switch" == (operator)) {
            exp.typ = SWITCH_EXP
        } else if ("if" == (operator)) {
            exp.typ = SWITCH_EXP
            ifThenElse = true
        } else {
            nops = 2; // check binary expressions

            if ("or" == (operator)) {
                exp.typ = OR_EXP
            } else if ("and" == (operator)) {
                exp.typ = AND_EXP
            } else if ("multiply" == (operator)) {
                exp.typ = MULTIPLY_EXP
            } else if ("divide" == (operator)) {
                exp.typ = DIVIDE_EXP
            } else if ("add" == (operator)) {
                exp.typ = ADD_EXP
            } else if ("max" == (operator)) {
                exp.typ = MAX_EXP
            } else if ("min" == (operator)) {
                exp.typ = MIN_EXP
            } else if ("equal" == (operator)) {
                exp.typ = EQUAL_EXP
            } else if ("greater" == (operator)) {
                exp.typ = GREATER_EXP
            } else if ("sub" == (operator)) {
                exp.typ = SUB_EXP
            } else if ("lesser" == (operator)) {
                exp.typ = LESSER_EXP
            } else if ("xor" == (operator)) {
                exp.typ = XOR_EXP
            } else {
                nops = 1; // check unary expressions
                if ("assign" == (operator)) {
                    if (level > 0) {
                        throw IllegalArgumentException("assign operator within expression")
                    }
                    exp.typ = ASSIGN_EXP
                    val variable: String = ctx.parseToken()
                    if (variable == null) {
                        throw IllegalArgumentException("unexpected end of file")
                    }
                    if (variable.indexOf('=') >= 0) {
                        throw IllegalArgumentException("variable name cannot contain '=': " + variable)
                    }
                    if (variable.indexOf(':') >= 0) {
                        throw IllegalArgumentException("cannot assign context-prefixed variable: " + variable)
                    }
                    exp.variableIdx = ctx.getVariableIdx(variable, true)
                    if (exp.variableIdx < ctx.getMinWriteIdx()) {
                        throw IllegalArgumentException("cannot assign to readonly variable " + variable)
                    }
                } else if ("not" == (operator)) {
                    exp.typ = NOT_EXP
                } else {
                    nops = 0; // check elemantary expressions
                    Int idx = operator.indexOf('=')
                    if (idx >= 0) {
                        exp.typ = LOOKUP_EXP
                        val name: String = operator.substring(0, idx)
                        val values: String = operator.substring(idx + 1)

                        exp.lookupNameIdx = ctx.getLookupNameIdx(name)
                        if (exp.lookupNameIdx < 0) {
                            throw IllegalArgumentException("unknown lookup name: " + name)
                        }
                        val tk: StringTokenizer = StringTokenizer(values, "|")
                        val nt: Int = tk.countTokens()
                        val nt2: Int = nt == 0 ? 1 : nt
                        exp.lookupValueIdxArray = Int[nt2]
                        for (Int ti = 0; ti < nt2; ti++) {
                            val value: String = ti < nt ? tk.nextToken() : ""
                            exp.lookupValueIdxArray[ti] = ctx.getLookupValueIdx(exp.lookupNameIdx, value)
                            if (exp.lookupValueIdxArray[ti] < 0) {
                                throw IllegalArgumentException("unknown lookup value: " + value)
                            }
                        }
                    } else {
                        idx = operator.indexOf(':')
                        if (idx >= 0) {
                            /*
                            use of variable values
                            assign no_height
                                switch and not      maxheight=
                                           lesser v:maxheight  my_height  true
                                false
                            */
                            if (operator.startsWith("v:")) {
                                val name: String = operator.substring(2)
                                exp.typ = VARIABLE_GET_EXP
                                exp.lookupNameIdx = ctx.getLookupNameIdx(name)
                            } else {
                                val context: String = operator.substring(0, idx)
                                val varname: String = operator.substring(idx + 1)
                                exp.typ = FOREIGN_VARIABLE_EXP
                                exp.variableIdx = ctx.getForeignVariableIdx(context, varname)
                            }
                        } else {
                            idx = ctx.getVariableIdx(operator, false)
                            if (idx >= 0) {
                                exp.typ = VARIABLE_EXP
                                exp.variableIdx = idx
                            } else if ("true" == (operator)) {
                                exp.numberValue = 1.f
                                exp.typ = NUMBER_EXP
                            } else if ("false" == (operator)) {
                                exp.numberValue = 0.f
                                exp.typ = NUMBER_EXP
                            } else {
                                try {
                                    exp.numberValue = Float.parseFloat(operator)
                                    exp.typ = NUMBER_EXP
                                } catch (NumberFormatException nfe) {
                                    throw IllegalArgumentException("unknown expression: " + operator)
                                }
                            }
                        }
                    }
                }
            }
        }
        // parse operands
        if (nops > 0) {
            exp.op1 = parse(ctx, level + 1, exp.typ == ASSIGN_EXP ? "=" : null)
        }
        if (nops > 1) {
            if (ifThenElse) {
                checkExpectedToken(ctx, "then")
            }
            exp.op2 = parse(ctx, level + 1, null)
        }
        if (nops > 2) {
            if (ifThenElse) {
                checkExpectedToken(ctx, "else")
            }
            exp.op3 = parse(ctx, level + 1, null)
        }
        if (brackets) {
            checkExpectedToken(ctx, ")")
        }
        return exp
    }

    private static Unit checkExpectedToken(final BExpressionContext ctx, final String expected) throws Exception {
        val token: String = ctx.parseToken()
        if (!expected == (token)) {
            throw IllegalArgumentException("unexpected token: " + token + ", expected: " + expected)
        }
    }

    // Evaluate the expression
    public Float evaluate(final BExpressionContext ctx) {
        switch (typ) {
            case OR_EXP:
                return op1.evaluate(ctx) != 0.f ? 1.f : (op2.evaluate(ctx) != 0.f ? 1.f : 0.f)
            case XOR_EXP:
                return (op1.evaluate(ctx) != 0.f) ^ (op2.evaluate(ctx) != 0.f) ? 1.f : 0.f
            case AND_EXP:
                return op1.evaluate(ctx) != 0.f ? (op2.evaluate(ctx) != 0.f ? 1.f : 0.f) : 0.f
            case ADD_EXP:
                return op1.evaluate(ctx) + op2.evaluate(ctx)
            case SUB_EXP:
                return op1.evaluate(ctx) - op2.evaluate(ctx)
            case MULTIPLY_EXP:
                return op1.evaluate(ctx) * op2.evaluate(ctx)
            case DIVIDE_EXP:
                return divide(op1.evaluate(ctx), op2.evaluate(ctx))
            case MAX_EXP:
                return max(op1.evaluate(ctx), op2.evaluate(ctx))
            case MIN_EXP:
                return min(op1.evaluate(ctx), op2.evaluate(ctx))
            case EQUAL_EXP:
                return op1.evaluate(ctx) == op2.evaluate(ctx) ? 1.f : 0.f
            case GREATER_EXP:
                return op1.evaluate(ctx) > op2.evaluate(ctx) ? 1.f : 0.f
            case LESSER_EXP:
                return op1.evaluate(ctx) < op2.evaluate(ctx) ? 1.f : 0.f
            case SWITCH_EXP:
                return op1.evaluate(ctx) != 0.f ? op2.evaluate(ctx) : op3.evaluate(ctx)
            case ASSIGN_EXP:
                return ctx.assign(variableIdx, op1.evaluate(ctx))
            case LOOKUP_EXP:
                return ctx.getLookupMatch(lookupNameIdx, lookupValueIdxArray)
            case NUMBER_EXP:
                return numberValue
            case VARIABLE_EXP:
                return ctx.getVariableValue(variableIdx)
            case FOREIGN_VARIABLE_EXP:
                return ctx.getForeignVariableValue(variableIdx)
            case VARIABLE_GET_EXP:
                return ctx.getLookupValue(lookupNameIdx)
            case NOT_EXP:
                return op1.evaluate(ctx) == 0.f ? 1.f : 0.f
            default:
                throw IllegalArgumentException("unknown op-code: " + typ)
        }
    }

    // Try to collapse the expression
    // if logically possible
    private BExpression tryCollapse() {
        switch (typ) {
            case OR_EXP:
                return NUMBER_EXP == op1.typ ?
                        (op1.numberValue != 0.f ? op1 : op2)
                        : (NUMBER_EXP == op2.typ ?
                        (op2.numberValue != 0.f ? op2 : op1)
                        : this)
            case AND_EXP:
                return NUMBER_EXP == op1.typ ?
                        (op1.numberValue == 0.f ? op1 : op2)
                        : (NUMBER_EXP == op2.typ ?
                        (op2.numberValue == 0.f ? op2 : op1)
                        : this)
            case ADD_EXP:
                return NUMBER_EXP == op1.typ ?
                        (op1.numberValue == 0.f ? op2 : this)
                        : (NUMBER_EXP == op2.typ ?
                        (op2.numberValue == 0.f ? op1 : this)
                        : this)
            case SWITCH_EXP:
                return NUMBER_EXP == op1.typ ?
                        (op1.numberValue == 0.f ? op3 : op2) : this
            default:
                return this
        }
    }

    // Try to evaluate the expression
    // if all operands are constant
    private BExpression tryEvaluateConstant() {
        if (op1 != null && NUMBER_EXP == op1.typ
                && (op2 == null || NUMBER_EXP == op2.typ)
                && (op3 == null || NUMBER_EXP == op3.typ)) {
            val exp: BExpression = BExpression()
            exp.typ = NUMBER_EXP
            exp.numberValue = evaluate(null)
            return exp
        }
        return this
    }

    private Float max(final Float v1, final Float v2) {
        return Math.max(v1, v2)
    }

    private Float min(final Float v1, final Float v2) {
        return Math.min(v1, v2)
    }

    private Float divide(Float v1, Float v2) {
        if (v2 == 0f) {
            throw IllegalArgumentException("div by zero")
        }
        return v1 / v2
    }

    override     public String toString() {
        if (typ == NUMBER_EXP) {
            return "" + numberValue
        }
        if (typ == VARIABLE_EXP) {
            return "vidx=" + variableIdx
        }
        val sb: StringBuilder = StringBuilder("typ=" + typ + " ops=(")
        addOp(sb, op1)
        addOp(sb, op2)
        addOp(sb, op3)
        sb.append(')')
        return sb.toString()
    }

    private Unit addOp(StringBuilder sb, BExpression e) {
        if (e != null) {
            sb.append('[').append(e).append(']')
        }
    }

    static BExpression createAssignExpressionFromKeyValue(BExpressionContext ctx, String key, String value) {
        val e: BExpression = BExpression()
        e.typ = ASSIGN_EXP
        e.variableIdx = ctx.getVariableIdx(key, true)
        e.op1 = BExpression()
        e.op1.typ = NUMBER_EXP
        e.op1.numberValue = Float.parseFloat(value)
        e.op1.doNotChange = true
        ctx.lastAssignedExpression.set(e.variableIdx, e.op1)
        return e
    }
}

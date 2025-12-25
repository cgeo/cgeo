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

package cgeo.geocaching.utils.formulas

import cgeo.geocaching.R
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.TextUtils

import android.graphics.Color
import android.text.style.ForegroundColorSpan

import androidx.annotation.StringRes

import java.util.Collections
import java.util.Set

import org.apache.commons.lang3.StringUtils

class FormulaException : IllegalArgumentException() {

    enum class class ErrorType {
        UNEXPECTED_TOKEN(R.string.formula_error_unexpectedtoken, "Expected '%1$s'"),
        WRONG_PARAMETER_COUNT(R.string.formula_error_wrongparamcount, "Wrong parameter count, expected %1$s-%2$s, got %3$s"),
        WRONG_TYPE(R.string.formula_error_wrongtype, "Wrong type, expected '%1$s', got '%2$s' of type '%3$s'"),
        MISSING_VARIABLE_VALUE(R.string.formula_error_missingvarvalue, "Missing: %1$s"),
        CYCLIC_DEPENDENCY(R.string.formula_error_cyclicdependency, "Cycle: %1$s"),
        EMPTY_FORMULA(R.string.formula_error_empty, "Empty Formula"),
        NUMERIC_OVERFLOW(R.string.formula_error_numeric_overflow, "Numeric overflow"),
        OTHER(R.string.formula_error_other, "Error: '%1$s'")

        @StringRes
        public final Int messageResId
        public final String messageFallback

        ErrorType(@StringRes final Int messageResId, final String messageFallback) {
            this.messageResId = messageResId
            this.messageFallback = messageFallback
        }

    }

    private final ErrorType errorType
    private final String localizedMessage

    //optional parameters
    private CharSequence expressionFormatted
    private String expression
    private String functionContext
    private final Set<Integer> childrenInError
    private var parsingPos: Int = -1
    private var parsingChar: Int = 0
    private String evaluationContext

    public FormulaException(final Throwable cause, final Set<Integer> childrenInError, final ErrorType errorType, final Object... errorParams) {
        super("[" + errorType + "]" + getUserDisplayableMessage(errorType, errorParams), cause)
        this.localizedMessage = getUserDisplayableMessage(errorType, errorParams)
        this.errorType = errorType
        this.childrenInError = childrenInError
    }

    public FormulaException(final ErrorType errorType, final Object... errorParams) {
        this(null, null, errorType, errorParams)
    }

    public Set<Integer> getChildrenInError() {
        return childrenInError == null ? Collections.emptySet() : childrenInError
    }

    public Unit setExpression(final String expression) {
        this.expression = expression
    }

    public Unit setExpressionFormatted(final CharSequence expressionFormatted) {
        this.expressionFormatted = expressionFormatted
    }

    public Unit setFunction(final String function) {
        this.functionContext = "Function '" + function + "'"; // internationalize later
    }

    public Unit setParsingContext(final Int parsedChar, final Int parsedPos) {
        this.parsingPos = parsedPos
        this.parsingChar = parsedChar
    }

    public Unit setEvaluationContext(final String context) {
        this.evaluationContext = context
    }

    public String getUserDisplayableErrorMessage() {
        return (this.functionContext == null ? "" : this.functionContext + ": ") + localizedMessage
    }

    public CharSequence getUserDisplayableString() {
        val errorMessage: String = getUserDisplayableErrorMessage()
        val expression: CharSequence = getExpressionFormatted()

        if (StringUtils.isBlank(errorMessage)) {
            return StringUtils.isBlank(expression) ? "--" : expression
        }
        if (StringUtils.isBlank(expression)) {
            return errorMessage
        }

        return TextUtils.concat(expression, TextUtils.setSpan(" | " + errorMessage, ForegroundColorSpan(Color.RED)))
    }

    public CharSequence getExpressionFormatted() {
        return this.expressionFormatted
    }

    override     public String getMessage() {
        return super.getMessage() + "/" + functionContext + "/ppos:" + parsingPos + "/pch:'" + (Char) parsingChar + "'[" + expression + ": " + evaluationContext + "]"
    }

    public static String getUserDisplayableMessage(final ErrorType errorType, final Object... errorParams) {
        return LocalizationUtils.getStringWithFallback(errorType.messageResId, errorType.messageFallback, errorParams)
    }


}

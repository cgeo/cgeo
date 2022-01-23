package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;

public class FormulaException extends IllegalArgumentException {

    public enum ErrorType {
        //internationalize later
        UNEXPECTED_TOKEN(0, "Expected '%1$s'"),
        WRONG_PARAMETER_COUNT(0, "Wrong parameter count, expected %1$s-%2$s, got %3$s"),
        WRONG_TYPE(0, "Wrong type, expected '%1$s', got '%2$s' of type '%3$s'"),
        MISSING_VARIABLE_VALUE(0, "Missing: %1$s"),
        CYCLIC_DEPENDENCY(0, "Cycle: %1$s"),
        EMPTY_FORMULA(0, "Empty Formula"),
        OTHER(0, "Error: '%1$s'");

        @StringRes
        public final int messageResId;
        public final String messageFallback;

        ErrorType(@StringRes final int messageResId, final String messageFallback) {
            this.messageResId = messageResId;
            this.messageFallback = messageFallback;
        }

    }

    private final ErrorType errorType;
    private final String localizedMessage;

    //optional parameters
    private CharSequence expressionFormatted;
    private String expression;
    private String functionContext;
    private int parsingPos = -1;
    private int parsingChar = 0;
    private String evaluationContext;

    public FormulaException(final Throwable cause, final ErrorType errorType, final Object ... errorParams) {
        super("[" + errorType + "]" + getUserDisplayableMessage(errorType, errorParams), cause);
        this.localizedMessage = getUserDisplayableMessage(errorType, errorParams);
        this.errorType = errorType;
    }

    public FormulaException(final ErrorType errorType, final Object ...errorParams) {
        this(null, errorType, errorParams);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setExpression(final String expression) {
        this.expression = expression;
    }

    public void setExpressionFormatted(final CharSequence expressionFormatted) {
        this.expressionFormatted = expressionFormatted;
    }

    public void setFunction(final String function) {
        this.functionContext = "Function '" + function + "'"; // internationalize later
    }

    public void setParsingContext(final int parsedChar, final int parsedPos) {
        this.parsingPos = parsedPos;
        this.parsingChar = parsedChar;
    }

    public void setEvaluationContext(final String context) {
        this.evaluationContext = context;
    }

    public String getUserDisplayableString() {
        return (this.functionContext == null ? "" : this.functionContext + ": ") + localizedMessage;
    }

    public CharSequence getExpressionFormatted() {
        return this.expressionFormatted;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "/" + functionContext + "/ppos:" + parsingPos + "/pch:" + parsingChar + "[" + expression + ": " + evaluationContext + "]";
    }

    public static String getUserDisplayableMessage(final ErrorType errorType, final Object ... errorParams) {
        return LocalizationUtils.getStringWithFallback(errorType.messageResId, errorType.messageFallback, errorParams);
    }



}

package cgeo.geocaching.utils.calc;

import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;

public class CalculatorException extends IllegalArgumentException {

    public enum ErrorType {
        //internationalize later
        UNEXPECTED_TOKEN(0, "Invalid formula, expected '%1$s'"),
        WRONG_PARAMETER_COUNT(0, "Wrong parameter count, expected min=%1$s, max=%2$s, got %3$s"),
        WRONG_TYPE(0, "Wrong value type, expected '%1$s', got '%2$s' of type '%3$s'"),
        MISSING_VARIABLE_VALUE(0, "Value missing for variable(s): %1$s"),
        CYCLIC_DEPENDENCY(0, "Cyclic dependency between variables: %1$s"),
        EMPTY_FORMULA(0, "Empty formula"),
        OTHER(0, "Problem during evaluation: '%1$s'");

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
    private String expression;
    private String functionContext;
    private String parsingContext;
    private String evaluationContext;

    public CalculatorException(final Throwable cause, final ErrorType errorType, final Object ... errorParams) {
        super("[" + errorType + "]" + getUserDisplayableMessage(errorType, errorParams), cause);
        this.localizedMessage = getUserDisplayableMessage(errorType, errorParams);
        this.errorType = errorType;
    }

    public CalculatorException(final ErrorType errorType, final Object ...errorParams) {
        this(null, errorType, errorParams);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setExpression(final String expression) {
        this.expression = expression;
    }

    public void setFunction(final String function) {
        this.functionContext = "Function '" + function + "'"; // internationalize later
    }

    public void setParsingContext(final int parsedChar, final int parsedPos) {
        if (parsedChar <= 0 && parsedPos < 0) {
            this.parsingContext = null;
        } else {
            this.parsingContext = "(near '" + (parsedChar <= 0 ? ' ' : (char) parsedChar) + "' at position " + parsedPos + ")"; //internationalize later
        }
    }

    public void setEvaluationContext(final String context) {
        this.evaluationContext = context;
    }

    public String getUserDisplayableString() {
        return (this.functionContext == null ? "" : this.functionContext + ": ") + localizedMessage +
            (this.parsingContext == null ? "" : " " + parsingContext);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "/" + functionContext + "/" + parsingContext + "[" + expression + ": " + evaluationContext + "]";
    }

    public static String getUserDisplayableMessage(final ErrorType errorType, final Object ... errorParams) {
        return LocalizationUtils.getStringWithFallback(errorType.messageResId, errorType.messageFallback, errorParams);
    }



}

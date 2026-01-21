package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import java.util.Set;

/**
 * Utility class for error and warning handling in formula evaluation and display.
 * <p>
 * FormulaError provides static helper methods and types to mark and visualize errors and warnings
 * during formula processing and rendering. This includes methods to create error and warning spans
 * for text display, as well as the {@link ErrorValue} class to represent error states in calculations.
 * The class also offers methods to convert value lists to displayable character sequences, highlighting
 * error states visually if needed.
 * <p>
 * Instantiation is not intended (private constructor).
 */
final class FormulaError {

    private FormulaError() {
        // Utility class, no instantiation
    }

    static class ErrorValue extends Value {
        protected ErrorValue(final CharSequence errorString) {
            super(errorString);
        }

        static ErrorValue of(final CharSequence cs) {
            return new ErrorValue(cs);
        }

        static boolean isError(final Value v) {
            return v instanceof ErrorValue;
        }
    }

    static ForegroundColorSpan createErrorSpan() {
        return new ForegroundColorSpan(Color.RED);
    }

    static ForegroundColorSpan createWarningSpan() {
        return new ForegroundColorSpan(Color.GRAY);
    }

    static CharSequence optionalError(final CharSequence value, final Set<Integer> childrenInError) {
        return childrenInError != null && childrenInError.isEmpty() ? TextUtils.setSpan(value, createErrorSpan()) : value;
    }

    static CharSequence valueListToCharSequence(final ValueList valueList) {
        return valueListToCharSequence(valueList, null);
    }

    static CharSequence valueListToCharSequence(final ValueList valueList, final CharSequence delim) {
        return valueListToCharSequence(valueList, delim, null, false);
    }

    static CharSequence valueListToCharSequence(final ValueList valueList, final CharSequence delim, final Set<Integer> childrenInError, final boolean quoteTypes) {
        final int[] idx = new int[]{0};
        return TextUtils.join(valueList, v -> ErrorValue.isError(v) || childrenInError == null || !childrenInError.contains(idx[0]++) ?
                        v.getAsTypedCharSequence(quoteTypes && !ErrorValue.isError(v)) : TextUtils.setSpan(TextUtils.concat("<", v.getAsTypedCharSequence(quoteTypes), ">"), createErrorSpan()),
                delim == null ? "" : delim);
    }
}

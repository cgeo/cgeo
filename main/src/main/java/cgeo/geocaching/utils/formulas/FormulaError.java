package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import java.util.Set;

final class FormulaError {

    private FormulaError() {
        // Utility class, no instantiation
    }

    public static Object createErrorSpan() {
        return new ForegroundColorSpan(Color.RED);
    }

    public static Object createWarningSpan() {
        return new ForegroundColorSpan(Color.GRAY);
    }

    public static class ErrorValue extends Value {
        protected ErrorValue(final CharSequence errorString) {
            super(errorString);
        }

        public static ErrorValue of(final CharSequence cs) {
            return new ErrorValue(cs);
        }

        public static boolean isError(final Value v) {
            return v instanceof ErrorValue;
        }
    }

    public static CharSequence optionalError(final CharSequence value, final Set<Integer> childrenInError) {
        return childrenInError != null && childrenInError.isEmpty() ? TextUtils.setSpan(value, createErrorSpan()) : value;
    }

    public static CharSequence valueListToCharSequence(final ValueList valueList) {
        return valueListToCharSequence(valueList, null);
    }

    public static CharSequence valueListToCharSequence(final ValueList valueList, final CharSequence delim) {
        return valueListToCharSequence(valueList, delim, null, false);
    }

    public static CharSequence valueListToCharSequence(final ValueList valueList, final CharSequence delim, final Set<Integer> childrenInError, final boolean quoteTypes) {
        final int[] idx = new int[]{0};
        return TextUtils.join(valueList, v -> ErrorValue.isError(v) || childrenInError == null || !childrenInError.contains(idx[0]++) ?
                        v.getAsTypedCharSequence(quoteTypes && !ErrorValue.isError(v)) : TextUtils.setSpan(TextUtils.concat("<", v.getAsTypedCharSequence(quoteTypes), ">"), createErrorSpan()),
                delim == null ? "" : delim);
    }
}

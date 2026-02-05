package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

final class FormulaError {

    private FormulaError() {
        // Utility class, no instantiation
    }

    static class ErrorValue extends Value {
        protected ErrorValue(@NonNull final CharSequence errorString) {
            super(errorString);
        }

        public static ErrorValue of(final CharSequence cs) {
            return new ErrorValue(cs);
        }

        static boolean isError(@NonNull final Value v) {
            return v instanceof ErrorValue;
        }
    }


    @NonNull
    static ForegroundColorSpan createErrorSpan() {
        return new ForegroundColorSpan(Color.RED);
    }

    @NonNull
    static ForegroundColorSpan createWarningSpan() {
        return new ForegroundColorSpan(Color.GRAY);
    }

    @NonNull
    static CharSequence optionalError(@NonNull final CharSequence value, @Nullable final Set<Integer> childrenInError) {
        return childrenInError != null && childrenInError.isEmpty() ? TextUtils.setSpan(value, createErrorSpan()) : value;
    }

    @NonNull
    static CharSequence valueListToCharSequence(@NonNull final ValueList valueList) {
        return valueListToCharSequence(valueList, null);
    }

    @NonNull
    static CharSequence valueListToCharSequence(@NonNull final ValueList valueList, @Nullable final CharSequence delim) {
        return valueListToCharSequence(valueList, delim, null, false);
    }

    @NonNull
    static CharSequence valueListToCharSequence(@NonNull final ValueList valueList, @Nullable final CharSequence delim, @Nullable final Set<Integer> childrenInError, final boolean quoteTypes) {
        final int[] idx = new int[]{0};
        return TextUtils.join(valueList, v -> ErrorValue.isError(v) || childrenInError == null || !childrenInError.contains(idx[0]++) ?
                        v.getAsTypedCharSequence(quoteTypes && !ErrorValue.isError(v)) : TextUtils.setSpan(TextUtils.concat("<", v.getAsTypedCharSequence(quoteTypes), ">"), createErrorSpan()),
                delim == null ? "" : delim);
    }
}

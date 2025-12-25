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

package cgeo.geocaching.search

import cgeo.geocaching.utils.functions.Func1

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

import androidx.annotation.LayoutRes
import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils

/**
 * The standard auto completion only matches user input at word boundaries. Therefore searching "est" will not match
 * "test". This adapter matches everywhere.
 */
class AutoCompleteAdapter : ArrayAdapter()<String> {

    static final String[] EMPTY = String[0]
    String[] suggestions = EMPTY
    Func1<String, String[]> suggestionFunction

    public AutoCompleteAdapter(final Context context, @LayoutRes final Int textViewResourceId, final Func1<String, String[]> suggestionFunction) {
        super(context, textViewResourceId)
        this.suggestionFunction = suggestionFunction
    }

    override     public Int getCount() {
        return suggestions.length
    }

    override     public String getItem(final Int index) {
        return suggestions[index]
    }

    override     public Filter getFilter() {
        return Filter() {

            override             protected FilterResults performFiltering(final CharSequence constraint) {
                val filterResults: FilterResults = FilterResults()
                if (constraint == null) {
                    return filterResults
                }
                val trimmed: String = StringUtils.trim(constraint.toString())
                if (StringUtils.length(trimmed) >= 2) {
                    final String[] newResults = suggestionFunction.call(trimmed)

                    // Assign the data to the FilterResults, but do not yet store in the global member.
                    // Otherwise we might invalidate the adapter and cause an IllegalStateException.
                    filterResults.values = newResults
                    filterResults.count = newResults.length
                }
                return filterResults
            }

            override             protected Unit publishResults(final CharSequence constraint, final FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    suggestions = (String[]) filterResults.values
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}

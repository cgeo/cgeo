package cgeo.geocaching.search;

import cgeo.geocaching.utils.functions.Func1;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * The standard auto completion only matches user input at word boundaries. Therefore searching "est" will not match
 * "test". This adapter matches everywhere.
 */
public class AutoCompleteAdapter extends ArrayAdapter<String> {

    private static final String[] EMPTY = new String[0];
    private String[] suggestions = EMPTY;
    private final Func1<String, String[]> suggestionFunction;

    public AutoCompleteAdapter(final Context context, @LayoutRes final int textViewResourceId, final Func1<String, String[]> suggestionFunction) {
        super(context, textViewResourceId);
        this.suggestionFunction = suggestionFunction;
    }

    @Override
    public int getCount() {
        return suggestions.length;
    }

    @Override
    public String getItem(final int index) {
        return suggestions[index];
    }

    @Override
    @NonNull
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();
                if (constraint == null) {
                    return filterResults;
                }
                final String trimmed = StringUtils.trim(constraint.toString());
                if (StringUtils.length(trimmed) >= 2) {
                    final String[] newResults = suggestionFunction.call(trimmed);

                    // Assign the data to the FilterResults, but do not yet store in the global member.
                    // Otherwise we might invalidate the adapter and cause an IllegalStateException.
                    filterResults.values = newResults;
                    filterResults.count = newResults.length;
                }
                return filterResults;
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    suggestions = (String[]) filterResults.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}

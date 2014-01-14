package cgeo.geocaching.search;

import org.apache.commons.lang3.StringUtils;

import rx.util.functions.Func1;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

/**
 * The standard auto completion only matches user input at word boundaries. Therefore searching "est" will not match
 * "test". This adapter matches everywhere.
 *
 */
public class AutoCompleteAdapter extends ArrayAdapter<String> {

    private String[] results;
    private final Func1<String, String[]> suggestionFunction;

    public AutoCompleteAdapter(Context context, int textViewResourceId, final Func1<String, String[]> suggestionFunction) {
        super(context, textViewResourceId);
        this.suggestionFunction = suggestionFunction;
    }

    @Override
    public int getCount() {
        return results.length;
    }

    @Override
    public String getItem(int index) {
        return results[index];
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint == null) {
                    return filterResults;
                }
                String trimmed = StringUtils.trim(constraint.toString());
                if (StringUtils.length(trimmed) >= 2) {
                    results = suggestionFunction.call(trimmed);

                    // Assign the data to the FilterResults
                    filterResults.values = results;
                    filterResults.count = results.length;
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
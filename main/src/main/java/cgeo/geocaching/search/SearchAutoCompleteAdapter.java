package cgeo.geocaching.search;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.functions.Func0;
import cgeo.geocaching.utils.functions.Func1;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public class SearchAutoCompleteAdapter extends AutoCompleteAdapter {

    int suggestionIcon;
    int historyIcon = R.drawable.ic_menu_recent_history;
    Context context;
    final Func0<String[]> historyFunction;
    String searchTerm;
    boolean isShowingResultsFromHistory;

    public SearchAutoCompleteAdapter(final Context context, final int textViewResourceId, final Func1<String, String[]> suggestionFunction, final int suggestionIcon, final Func0<String[]> historyFunction) {
        super(context, textViewResourceId, suggestionFunction);
        if (null != suggestionFunction) {
            this.suggestionIcon = suggestionIcon;
        } else {
            this.suggestionIcon = historyIcon;
        }
        this.context = context;
        this.historyFunction = historyFunction;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
        final View v = getOrCreateView(context, convertView, parent);
        final TextView textView = v.findViewById(R.id.text);
        final ImageView iconView = v.findViewById(R.id.icon);

        textView.setText(getItem(position));
        setHighLightedText(textView, searchTerm);
        iconView.setImageResource(isShowingResultsFromHistory ? historyIcon : suggestionIcon);

        return v;
    }

    public static View getOrCreateView(final Context context, final View convertView, final ViewGroup parent) {
        final View view = convertView == null ? LayoutInflater.from(context).inflate(R.layout.search_suggestion, parent, false) : convertView;
        ((TextView) view.findViewById(R.id.text)).setText(new SpannableString(""));
        return view;
    }

    /**
     * Highlight the selected text (case insensitive) in the given TextView
     */
    void setHighLightedText(final TextView tv, final String textToHighlight) {
        final String text = tv.getText().toString().toLowerCase();
        int ofe = text.indexOf(textToHighlight, 0);
        final Spannable wordToSpan = new SpannableString(tv.getText());
        for (int ofs = 0; ofs < text.length() && ofe != -1; ofs = ofe + 1) {
            ofe = text.indexOf(textToHighlight, ofs);
            if (ofe == -1) {
                break;
            } else {
                wordToSpan.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.colorAccent)), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
            }
        }
    }

    public boolean queryTriggersSearch(final String query) {
        return StringUtils.length(query) >= 2;
    }

    @Override
    @NonNull
    public Filter getFilter() {
        final SearchAutoCompleteAdapter adapter = this;
        return new Filter() {

            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();
                if (constraint == null) {
                    return filterResults;
                }
                String[] newResults = new String[0];
                final String trimmed = StringUtils.trimToEmpty(constraint.toString());
                if (null != suggestionFunction && adapter.queryTriggersSearch(trimmed)) {
                    newResults = suggestionFunction.call(trimmed);
                    adapter.isShowingResultsFromHistory = false;
                }
                if (newResults.length == 0 && null != historyFunction) {
                    newResults = Arrays.stream(historyFunction.call()).filter(s -> s.contains(trimmed)).toArray(String[]::new);
                    adapter.isShowingResultsFromHistory = true;
                }
                filterResults.values = newResults;
                filterResults.count = newResults.length;
                return filterResults;
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    searchTerm = constraint.toString().toLowerCase();
                    suggestions = (String[]) filterResults.values;
                    notifyDataSetChanged();
                } else {
                    suggestions = new String[0];
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}

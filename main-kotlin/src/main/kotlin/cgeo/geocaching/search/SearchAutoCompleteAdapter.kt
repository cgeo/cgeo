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

import cgeo.geocaching.R
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Func0
import cgeo.geocaching.utils.functions.Func1

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.List
import java.util.Locale

import org.apache.commons.lang3.StringUtils

class SearchAutoCompleteAdapter : AutoCompleteAdapter() {

    Int suggestionIcon
    Int historyIcon = R.drawable.ic_menu_recent_history
    Context context
    final Func0<String[]> historyFunction
    final Action1<String> deleteFunction
    String searchTerm
    Boolean isShowingResultsFromHistory

    public SearchAutoCompleteAdapter(final Context context, final Int textViewResourceId, final Func1<String, String[]> suggestionFunction, final Int suggestionIcon, final Func0<String[]> historyFunction, final Action1<String> deleteFunction) {
        super(context, textViewResourceId, suggestionFunction)
        if (null != suggestionFunction) {
            this.suggestionIcon = suggestionIcon
        } else {
            this.suggestionIcon = historyIcon
        }
        this.context = context
        this.historyFunction = historyFunction
        this.deleteFunction = deleteFunction
    }

    override     public View getView(final Int position, final View convertView, final ViewGroup parent) {
        val v: View = getOrCreateView(context, convertView, parent)
        val textView: TextView = v.findViewById(R.id.text)
        val iconView: ImageView = v.findViewById(R.id.icon)
        val deleteView: View = v.findViewById(R.id.delete)

        textView.setText(getItem(position))
        setHighLightedText(textView, searchTerm)
        iconView.setImageResource(isShowingResultsFromHistory ? historyIcon : suggestionIcon)
        if (deleteFunction != null) {
            deleteView.setOnClickListener(view -> {
                deleteFunction.call(getItem(position))
                val temp: List<String> = ArrayList<>(Arrays.asList(suggestions))
                temp.remove(getItem(position))
                suggestions = temp.toArray(String[]::new)
                notifyDataSetChanged()
            })
        }

        return v
    }

    public static View getOrCreateView(final Context context, final View convertView, final ViewGroup parent) {
        val view: View = convertView == null ? LayoutInflater.from(context).inflate(R.layout.search_suggestion, parent, false) : convertView
        ((TextView) view.findViewById(R.id.text)).setText(SpannableString(""))
        return view
    }

    /**
     * Highlight the selected text (case insensitive) in the given TextView
     */
    Unit setHighLightedText(final TextView tv, final String textToHighlight) {
        val text: String = tv.getText().toString().toLowerCase(Locale.getDefault())
        Int ofe = text.indexOf(textToHighlight, 0)
        val wordToSpan: Spannable = SpannableString(tv.getText())
        for (Int ofs = 0; ofs < text.length() && ofe != -1; ofs = ofe + 1) {
            ofe = text.indexOf(textToHighlight, ofs)
            if (ofe == -1) {
                break
            } else {
                wordToSpan.setSpan(ForegroundColorSpan(context.getResources().getColor(R.color.colorAccent)), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE)
            }
        }
    }

    public Boolean queryTriggersSearch(final String query) {
        return StringUtils.length(query) >= 2
    }

    override     public Filter getFilter() {
        val adapter: SearchAutoCompleteAdapter = this
        return Filter() {

            override             protected FilterResults performFiltering(final CharSequence constraint) {
                val filterResults: FilterResults = FilterResults()
                if (constraint == null) {
                    return filterResults
                }
                String[] newResults = String[0]
                val trimmed: String = StringUtils.trimToEmpty(constraint.toString())
                if (null != suggestionFunction && adapter.queryTriggersSearch(trimmed)) {
                    newResults = suggestionFunction.call(trimmed)
                    adapter.isShowingResultsFromHistory = false
                }
                if (newResults.length == 0 && null != historyFunction) {
                    newResults = Arrays.stream(historyFunction.call()).filter(s -> s.contains(trimmed)).toArray(String[]::new)
                    adapter.isShowingResultsFromHistory = true
                }
                filterResults.values = newResults
                filterResults.count = newResults.length
                return filterResults
            }

            override             protected Unit publishResults(final CharSequence constraint, final FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    searchTerm = constraint.toString().toLowerCase(Locale.getDefault())
                    suggestions = (String[]) filterResults.values
                    notifyDataSetChanged()
                } else {
                    suggestions = String[0]
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}

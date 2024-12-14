package cgeo.geocaching.search;

import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.utils.functions.Func1;

public class SearchAutoCompleteAdapter extends AutoCompleteAdapter {

    int icon;
    Context context;

    public SearchAutoCompleteAdapter(Context context, int textViewResourceId, int icon, Func1<String, String[]> suggestionFunction) {
        super(context, textViewResourceId, suggestionFunction);
        this.icon = icon;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
        View v = getOrCreateView(context, convertView, parent);
        final String geocode = getItem(position);
        TextView textView = v.findViewById(R.id.text);
        ImageView iconView = v.findViewById(R.id.icon);
        textView.setText(geocode);
        iconView.setImageResource(icon);
        return v;
    }

    public static View getOrCreateView(final Context context, final View convertView, final ViewGroup parent) {
        final View view = convertView == null ? LayoutInflater.from(context).inflate(R.layout.search_suggestion, parent, false) : convertView;
        ((TextView) view.findViewById(R.id.text)).setText(new SpannableString(""));
        return view;
    }

}

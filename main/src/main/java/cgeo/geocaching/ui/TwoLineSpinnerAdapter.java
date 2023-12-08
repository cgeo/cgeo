package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class TwoLineSpinnerAdapter extends ArrayAdapter<TwoLineSpinnerAdapter.TextSpinnerData> {

    private final WeakReference<Context> contextRef;

    public static class TextSpinnerData {
        public String title;        // will be displayed in the upper line, larger font size
        public String subtitle;     // will be displayed in the lower line, smaller font size
        public int reference;       // some user-defined reference value

        public TextSpinnerData(final String title, final String subtitle, final int reference) {
            this.title = title;
            this.subtitle = subtitle;
            this.reference = reference;
        }
    }

    public TwoLineSpinnerAdapter(@NonNull final Context context, final List<TextSpinnerData> data) {
        super(context, R.layout.cachelist_spinner_actionbar);
        contextRef = new WeakReference<>(context);
        addAll(data);
    }

    @NonNull
    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        return getCustomView(position, convertView, parent, R.layout.cachelist_spinner_actionbar);
    }

    @NonNull
    @Override
    public View getDropDownView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        return getCustomView(position, convertView, parent, R.layout.cachelist_spinner_dropdownitem);
    }

    public View getCustomView(final int position, final View convertView, final ViewGroup parent, final @LayoutRes int layoutRes) {
        final View resultView = convertView != null ? convertView : LayoutInflater.from(contextRef.get()).inflate(layoutRes, parent, false);
        final TextSpinnerData temp = getItem(position);
        ((TextView) resultView.findViewById(android.R.id.text1)).setText(temp.title);
        ((TextView) resultView.findViewById(android.R.id.text2)).setText(temp.subtitle);
        return resultView;
    }

    /** updates title or subtitle of given element; silently ignores unknown references */
    public void setTextByReference(final int reference, final boolean title, final String text) {
        final int position = getPositionFromReference(reference);
        if (position < 0) {
            return;
        }
        if (title) {
            Objects.requireNonNull(getItem(position)).title = text;
        } else {
            Objects.requireNonNull(getItem(position)).subtitle = text;
        }
        notifyDataSetChanged();
    }

    /** returns position for given reference (-1, if not found) */
    public int getPositionFromReference(final int reference) {
        final int max = getCount();
        for (int position = 0; position < max; position++) {
            if (Objects.requireNonNull(getItem(position)).reference == reference) {
                return position;
            }
        }
        return -1;
    }

}

package cgeo.geocaching.log;

import cgeo.geocaching.ImagesActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.network.SmileyImage;
import cgeo.geocaching.ui.AbstractCachingListViewPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;

import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

public abstract class LogsViewCreator extends AbstractCachingListViewPageViewCreator {

    protected final AbstractActionBarActivity activity;

    public LogsViewCreator(final AbstractActionBarActivity activity) {
        this.activity = activity;
    }

    @Override
    public ListView getDispatchedView(final ViewGroup parentView) {
        if (!isValid()) {
            return null;
        }

        final List<LogEntry> logs = getLogs();

        view = (ListView) activity.getLayoutInflater().inflate(R.layout.logs_page, parentView, false);
        addHeaderView();
        view.setAdapter(new ArrayAdapter<LogEntry>(activity, R.layout.logs_item, logs) {

            @Override
            @NonNull
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View rowView = convertView;
                if (rowView == null) {
                    rowView = activity.getLayoutInflater().inflate(R.layout.logs_item, parent, false);
                }
                LogViewHolder holder = (LogViewHolder) rowView.getTag();
                if (holder == null) {
                    holder = new LogViewHolder(rowView);
                }
                holder.setPosition(position);

                final LogEntry log = getItem(position);
                fillViewHolder(convertView, holder, log);
                final View logView = rowView.findViewById(R.id.log);
                activity.addContextMenu(logView);
                return rowView;
            }
        });

        return view;
    }

    protected void fillViewHolder(@SuppressWarnings("unused") final View convertView, final LogViewHolder holder, final LogEntry log) {
        if (log.date > 0) {
            holder.date.setText(Formatter.formatShortDateVerbally(log.date));
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }

        holder.type.setText(log.getType().getL10n());
        holder.author.setText(StringEscapeUtils.unescapeHtml4(log.author));

        fillCountOrLocation(holder, log);

        // log text, avoid parsing HTML if not necessary
        if (TextUtils.containsHtml(log.log)) {
            final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
            holder.text.setText(TextUtils.trimSpanned(Html.fromHtml(log.getDisplayText(), new SmileyImage(getGeocode(), holder.text), unknownTagsHandler)), TextView.BufferType.SPANNABLE);
        } else {
            holder.text.setText(log.log, TextView.BufferType.SPANNABLE);
        }

        // images
        if (log.hasLogImages()) {
            holder.images.setText(log.getImageTitles());
            holder.images.setVisibility(View.VISIBLE);
            holder.images.setOnClickListener(v -> ImagesActivity.startActivity(activity, getGeocode(), new ArrayList<>(log.getLogImages())));
        } else {
            holder.images.setVisibility(View.GONE);
        }

        // colored marker
        final int marker = log.getType().markerId;
        if (marker != 0) {
            holder.marker.setVisibility(View.VISIBLE);
            holder.marker.setImageResource(marker);
        } else {
            holder.marker.setVisibility(View.GONE);
        }

        holder.author.setOnClickListener(createUserActionsListener(log));
        holder.text.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
        holder.text.setOnClickListener(new DecryptTextClickListener(holder.text));
        activity.registerForContextMenu(holder.text);
    }

    protected abstract View.OnClickListener createUserActionsListener(LogEntry log);

    protected abstract String getGeocode();

    protected abstract List<LogEntry> getLogs();

    protected abstract void addHeaderView();

    protected abstract void fillCountOrLocation(LogViewHolder holder, LogEntry log);

    protected abstract boolean isValid();

}

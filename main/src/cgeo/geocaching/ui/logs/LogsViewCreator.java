package cgeo.geocaching.ui.logs;

import cgeo.geocaching.ImagesActivity;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.ui.AbstractCachingListViewPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.ui.UserActionsClickListener;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;

import org.apache.commons.lang3.StringEscapeUtils;

import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public abstract class LogsViewCreator extends AbstractCachingListViewPageViewCreator {

    protected final AbstractActivity activity;

    public LogsViewCreator(final AbstractActivity activity) {
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
            public View getView(final int position, final View convertView, final android.view.ViewGroup parent) {
                View rowView = convertView;
                if (null == rowView) {
                    rowView = activity.getLayoutInflater().inflate(R.layout.logs_item, parent, false);
                }
                LogViewHolder holder = (LogViewHolder) rowView.getTag();
                if (null == holder) {
                    holder = new LogViewHolder(rowView);
                }
                holder.setPosition(position);

                final LogEntry log = getItem(position);
                fillViewHolder(convertView, holder, log);
                return rowView;
            }
        });

        return view;
    }

    protected void fillViewHolder(final View convertView, final LogViewHolder holder, final LogEntry log) {
        if (log.date > 0) {
            holder.date.setText(Formatter.formatShortDateVerbally(log.date));
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }

        holder.type.setText(log.type.getL10n());
        holder.author.setText(StringEscapeUtils.unescapeHtml4(log.author));

        fillCountOrLocation(holder, log);

        // log text, avoid parsing HTML if not necessary
        String logText = log.log;
        if (TextUtils.containsHtml(logText)) {
            logText = log.getDisplayText();
            final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
            holder.text.setText(Html.fromHtml(logText, new HtmlImage(getGeocode(), false, StoredList.STANDARD_LIST_ID, false, holder.text),
                    unknownTagsHandler), TextView.BufferType.SPANNABLE);
        } else {
            holder.text.setText(logText, TextView.BufferType.SPANNABLE);
        }

        // images
        if (log.hasLogImages()) {
            holder.images.setText(log.getImageTitles());
            holder.images.setVisibility(View.VISIBLE);
            holder.images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    ImagesActivity.startActivityLogImages(activity, getGeocode(), new ArrayList<>(log.getLogImages()));
                }
            });
        } else {
            holder.images.setVisibility(View.GONE);
        }

        // colored marker
        final int marker = log.type.markerId;
        if (marker != 0) {
            holder.marker.setVisibility(View.VISIBLE);
            holder.marker.setImageResource(marker);
        }
        else {
            holder.marker.setVisibility(View.GONE);
        }

        if (null == convertView) {
            holder.author.setOnClickListener(createUserActionsListener());
            holder.text.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            holder.text.setOnClickListener(new DecryptTextClickListener(holder.text));
            activity.registerForContextMenu(holder.text);
        }
    }

    abstract protected UserActionsClickListener createUserActionsListener();

    abstract protected String getGeocode();

    abstract protected List<LogEntry> getLogs();

    abstract protected void addHeaderView();

    abstract protected void fillCountOrLocation(LogViewHolder holder, final LogEntry log);

    abstract protected boolean isValid();

}

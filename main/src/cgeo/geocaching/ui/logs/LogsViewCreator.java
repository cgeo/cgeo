package cgeo.geocaching.ui.logs;

import cgeo.geocaching.Image;
import cgeo.geocaching.ImagesActivity;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.ui.HtmlImageCounter;
import cgeo.geocaching.ui.UserActionsClickListener;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;

import org.apache.commons.lang3.StringEscapeUtils;

import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public abstract class LogsViewCreator extends AbstractCachingPageViewCreator<ListView> {

    protected final AbstractActivity activity;

    public LogsViewCreator(AbstractActivity activity) {
        this.activity = activity;
    }

    @Override
    public ListView getDispatchedView() {
        if (!isValid()) {
            return null;
        }

        final List<LogEntry> logs = getLogs();

        view = (ListView) activity.getLayoutInflater().inflate(R.layout.logs_page, null);
        addHeaderView();
        view.setAdapter(new ArrayAdapter<LogEntry>(activity, R.layout.logs_item, logs) {

            @Override
            public View getView(final int position, final View convertView, final android.view.ViewGroup parent) {
                View rowView = convertView;
                if (null == rowView) {
                    rowView = activity.getLayoutInflater().inflate(R.layout.logs_item, null);
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

    protected void fillViewHolder(final View convertView, LogViewHolder holder, final LogEntry log) {
        if (log.date > 0) {
            holder.date.setText(Formatter.formatShortDateVerbally(log.date));
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }

        holder.type.setText(log.type.getL10n());
        holder.author.setText(StringEscapeUtils.unescapeHtml4(log.author));

        fillCountOrLocation(holder, log);

        // logtext, avoid parsing HTML if not necessary
        String logText = log.log;
        if (TextUtils.containsHtml(logText)) {
            logText = log.getDisplayText();
            // Fast preview: parse only HTML without loading any images
            final HtmlImageCounter imageCounter = new HtmlImageCounter();
            final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
            holder.text.setText(Html.fromHtml(logText, imageCounter, unknownTagsHandler), TextView.BufferType.SPANNABLE);
            if (imageCounter.getImageCount() > 0) {
                // Complete view: parse again with loading images - if necessary ! If there are any images causing problems the user can see at least the preview
                final LogImageLoader loader = new LogImageLoader(holder);
                loader.execute(logText);
            }
        }
        else {
            holder.text.setText(logText, TextView.BufferType.SPANNABLE);
        }

        // images
        if (log.hasLogImages()) {
            holder.images.setText(log.getImageTitles());
            holder.images.setVisibility(View.VISIBLE);
            holder.images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImagesActivity.startActivityLogImages(activity, getGeocode(), new ArrayList<Image>(log.getLogImages()));
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

    /** Loads the Log Images outside the ui thread. */

    private class LogImageLoader extends AsyncTask<String, Progress, Spanned> {
        final private LogViewHolder holder;
        final private int position;

        public LogImageLoader(LogViewHolder holder) {
            this.holder = holder;
            this.position = holder.getPosition();
        }

        @Override
        protected Spanned doInBackground(String... logtext) {
            return Html.fromHtml(logtext[0], new HtmlImage(getGeocode(), false, StoredList.STANDARD_LIST_ID, false), null); //, TextView.BufferType.SPANNABLE)
        }

        @Override
        protected void onPostExecute(Spanned result) {
            // Ensure that this holder and its view still references the right item before updating the text.
            if (position == holder.getPosition()) {
                holder.text.setText(result);
            }
        }

    }

}

package cgeo.geocaching.log;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;

import android.view.View;

import androidx.core.text.HtmlCompat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class TrackableLogsViewCreator extends LogsViewCreator {

    private Trackable trackable;
    private final TrackableActivity trackableActivity;

    /**
     */
    public TrackableLogsViewCreator() {
        this.trackableActivity = (TrackableActivity) getActivity();
        trackable = trackableActivity.getTrackable();
    }

    @Override
    public long getPageId() {
        return CacheDetailActivity.Page.LOGS.id;
    }

    @Override
    protected boolean isValid() {
        return trackable != null;
    }

    @Override
    protected List<LogEntry> getLogs() {
        trackable = trackableActivity.getTrackable();
        return trackable.getLogs();
    }

    @Override
    protected void addHeaderView() {
        // empty
    }

    @Override
    protected void fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        if (StringUtils.isNotBlank(log.cacheName)) {
            holder.binding.gcinfo.setText(HtmlCompat.fromHtml(log.cacheName, HtmlCompat.FROM_HTML_MODE_LEGACY));
            holder.binding.gcinfo.setVisibility(View.VISIBLE);
            final String cacheGuid = log.cacheGuid;
            final String cacheName = log.cacheName;
            holder.binding.gcinfo.setOnClickListener(arg0 -> {
                if (StringUtils.isNotBlank(cacheGuid)) {
                    CacheDetailActivity.startActivityGuid(getActivity(), cacheGuid, TextUtils.stripHtml(cacheName));
                } else {
                    // for GeoKrety we only know the cache geocode
                    final String cacheGeocode = log.cacheGeocode;
                    if (ConnectorFactory.canHandle(cacheGeocode)) {
                        CacheDetailActivity.startActivity(getActivity(), cacheGeocode);
                    }
                }
            });
            holder.binding.countOrLocation.setVisibility(View.GONE);
            holder.binding.gcinfo.setVisibility(View.VISIBLE);
        } else {
            holder.binding.countOrLocation.setVisibility(View.GONE);
            holder.binding.gcinfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected String getGeocode() {
        return trackable.getGeocode();
    }

    @Override
    protected View.OnClickListener createUserActionsListener(final LogEntry log) {
        return UserClickListener.forUser(trackable, StringEscapeUtils.unescapeHtml4(log.author), log.authorGuid);
    }

    @Override
    protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, final LogEntry log) {
        if (trackable.canShareLog(log)) {
            ctxMenu.addItem(trackableActivity.getString(R.string.cache_menu_browser),
                R.drawable.ic_menu_info_details, it -> ShareUtils.openUrl(trackableActivity, trackable.getServiceSpecificLogUrl(log)));
        }
        return ctxMenu;
    }

}

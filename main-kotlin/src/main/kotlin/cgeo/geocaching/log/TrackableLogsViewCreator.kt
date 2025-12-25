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

package cgeo.geocaching.log

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.R
import cgeo.geocaching.TrackableActivity
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.ui.UserClickListener
import cgeo.geocaching.ui.dialog.ContextMenuDialog
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils

import android.view.View

import androidx.core.text.HtmlCompat

import java.util.Collections
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

class TrackableLogsViewCreator : LogsViewCreator() {

    private Trackable getTrackable() {
        val activity: TrackableActivity = (TrackableActivity) getActivity()
        return activity != null ? activity.getTrackable() : null
    }

    override     public Long getPageId() {
        return TrackableActivity.Page.LOGS.id
    }

    override     protected Boolean isValid() {
        return getTrackable() != null
    }

    override     protected List<LogEntry> getLogs() {
        val trackable: Trackable = getTrackable()
        return trackable != null ? trackable.getLogs() : Collections.emptyList()
    }

    override     protected Unit addHeaderView() {
        // empty
    }

    override     protected Unit fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        if (StringUtils.isNotBlank(log.cacheName)) {
            holder.binding.gcinfo.setText(HtmlCompat.fromHtml(log.cacheName, HtmlCompat.FROM_HTML_MODE_LEGACY))
            holder.binding.gcinfo.setVisibility(View.VISIBLE)
            val cacheGuid: String = log.cacheGuid
            val cacheName: String = log.cacheName
            holder.binding.gcinfo.setOnClickListener(arg0 -> {
                if (StringUtils.isNotBlank(cacheGuid)) {
                    CacheDetailActivity.startActivityGuid(getActivity(), cacheGuid, TextUtils.stripHtml(cacheName))
                } else {
                    // for GeoKrety we only know the cache geocode
                    val cacheGeocode: String = log.cacheGeocode
                    if (ConnectorFactory.canHandle(cacheGeocode)) {
                        CacheDetailActivity.startActivity(getActivity(), cacheGeocode)
                    }
                }
            })
            holder.binding.countOrLocation.setVisibility(View.GONE)
            holder.binding.gcinfo.setVisibility(View.VISIBLE)
        } else {
            holder.binding.countOrLocation.setVisibility(View.GONE)
            holder.binding.gcinfo.setVisibility(View.GONE)
        }
    }

    override     protected String getGeocode() {
        val trackable: Trackable = getTrackable()
        return trackable != null ? trackable.getGeocode() : ""
    }

    override     protected View.OnClickListener createUserActionsListener(final LogEntry log) {
        val trackable: Trackable = getTrackable()
        return UserClickListener.forUser(trackable, StringEscapeUtils.unescapeHtml4(log.author), log.authorGuid)
    }

    override     protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, final LogEntry log) {
        val trackable: Trackable = getTrackable()
        if (trackable != null && trackable.canShareLog(log)) {
            ctxMenu.addItem(getActivity().getString(R.string.cache_menu_browser),
                    R.drawable.ic_menu_open_in_browser, it -> ShareUtils.openUrl(getActivity(), trackable.getServiceSpecificLogUrl(log)))
        }
        return ctxMenu
    }

}

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
import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.TabbedViewPagerFragment
import cgeo.geocaching.databinding.LogsPageBinding
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.UserClickListener
import cgeo.geocaching.ui.dialog.ContextMenuDialog
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.ShareUtils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.appcompat.widget.TooltipCompat

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.List
import java.util.Map
import java.util.Map.Entry
import java.util.stream.Collectors

import com.google.android.material.chip.Chip
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

class CacheLogsViewCreator : LogsViewCreator() {
    private final Boolean allLogs
    private val res: Resources = CgeoApplication.getInstance().getResources()
    private var countview1: LinearLayout = null
    private var countview2: TextView = null

    public CacheLogsViewCreator(final Boolean allLogs) {
        this.allLogs = allLogs
    }

    public CacheLogsViewCreator() {
        this.allLogs = false
    }

    public static TabbedViewPagerFragment<LogsPageBinding> newInstance(final Boolean allLogs) {
        val fragment: CacheLogsViewCreator = CacheLogsViewCreator(allLogs)
        val bundle: Bundle = Bundle()
        fragment.setArguments(bundle)
        return fragment
    }

    override     public Long getPageId() {
        return allLogs ? CacheDetailActivity.Page.LOGS.id : CacheDetailActivity.Page.LOGSFRIENDS.id
    }

    private Geocache getCache() {
        val activity: CacheDetailActivity = (CacheDetailActivity) getActivity()
        return activity.getCache()
    }

    override     protected List<LogEntry> getLogs() {
        val cache: Geocache = getCache()
        if (allLogs) {
            return addOwnOfflineLog(cache, cache.getLogs())
        } else {
            val logs: List<LogEntry> = addOwnOfflineLog(cache, cache.getFriendsLogs())
            val ownLogs: List<LogEntry> = addOwnOfflineLog(cache, logs.stream().filter(LogEntry::isOwn).collect(Collectors.toList()))
            val ownerLogs: List<LogEntry> = logs.stream().filter(log -> log.authorGuid == (getCache().getOwnerGuid())).collect(Collectors.toList())
            val friendsLogs: List<LogEntry> = ArrayList<>(cache.getFriendsLogs())
            friendsLogs.removeAll(ownLogs)
            friendsLogs.removeAll(ownerLogs)

            binding.chipOwn.setVisibility(ownLogs.isEmpty() ? View.GONE : View.VISIBLE)
            binding.chipFriends.setVisibility(friendsLogs.isEmpty() ? View.GONE : View.VISIBLE)
            binding.chipOwner.setVisibility(ownerLogs.isEmpty() ? View.GONE : View.VISIBLE)

            if (!binding.chipOwn.isChecked()) {
                logs.removeAll(ownLogs)
            }
            if (!binding.chipFriends.isChecked()) {
                logs.removeAll(friendsLogs)
            }
            if (!binding.chipOwner.isChecked()) {
                logs.removeAll(ownerLogs)
            }
            return logs
        }
    }

    private List<LogEntry> addOwnOfflineLog(final Geocache cache, final List<LogEntry> logsIn) {
        val log: LogEntry = DataStore.loadLogOffline(cache.getGeocode())
        val logs: List<LogEntry> = ArrayList<>(logsIn)
        if (log != null) {
            logs.add(0, log.buildUpon().setAuthor(res.getString(R.string.log_your_saved_log)).build())
        }
        return logs
    }

    override     protected Unit addHeaderView() {
        if (binding != null) {
            addLogCountsHeader()
            addEmptyLogsHeader()

            for (Int i = 0; i < binding.filterChips.getChildCount(); i++) {
                ((Chip) binding.filterChips.getChildAt(i)).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    setContent()
                })
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private Unit addLogCountsHeader() {
        if (countview1 != null) {
            binding.logsItems.removeHeaderView(countview1)
            countview1 = null
        }

        val logCounts: Map<LogType, Integer> = allLogs ? getCache().getLogCounts() : getLogs().stream().collect(Collectors.groupingBy(log -> log.logType, Collectors.summingInt(log -> 1)))
        if (logCounts != null) {
            final List<Entry<LogType, Integer>> sortedLogCounts = ArrayList<>(logCounts.size())
            for (final Entry<LogType, Integer> entry : logCounts.entrySet()) {
                // it may happen that the label is unknown -> then avoid any output for this type
                if (entry.getKey() != LogType.PUBLISH_LISTING && entry.getValue() != 0) {
                    sortedLogCounts.add(entry)
                }
            }

            if (!sortedLogCounts.isEmpty()) {
                // sort the log counts by type id ascending. that way the FOUND, DNF log types are the first and most visible ones
                Collections.sort(sortedLogCounts, Comparator.comparing(Entry::getKey))

                val labels: List<String> = ArrayList<>(sortedLogCounts.size())
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    labels.add(pair.getValue() + "× " + pair.getKey().getL10n())
                }

                countview1 = LinearLayout(getActivity())
                val logtypes: TextView = TextView(getActivity())
                logtypes.setText(res.getString(R.string.cache_log_types) + ": ")
                countview1.addView(logtypes)
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    val tv: TextView = TextView(getActivity())
                    tv.setText(pair.getValue().toString())
                    tv.setCompoundDrawablesWithIntrinsicBounds(pair.getKey().getLogOverlay(), 0, 0, 0)
                    tv.setCompoundDrawablePadding(4)
                    tv.setPadding(0, 0, 10, 0)
                    TooltipCompat.setTooltipText(tv, pair.getKey().getL10n())
                    countview1.addView(tv)
                }
                binding.logsItems.addHeaderView(countview1, null, false)
            }
        }
    }

    private Unit addEmptyLogsHeader() {
        if (countview2 != null) {
            binding.logsItems.removeHeaderView(countview2)
            countview2 = null
        }

        if (getLogs().isEmpty()) {
            countview2 = TextView(getActivity())
            countview2.setText(allLogs ? res.getString(R.string.log_empty_logbook) : res.getString(R.string.log_empty_logbook_filtered))
            binding.logsItems.addHeaderView(countview2, null, false)
        }
    }

    override     protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, final LogEntry log) {
        val activity: CacheDetailActivity = (CacheDetailActivity) getActivity()
        val logUrl: String = getCache().getLogUrl(log)
        val canShareLog: Boolean = StringUtils.isNotBlank(logUrl)
        if (canShareLog) {
            ctxMenu.addItem(R.string.context_share_as_link, R.drawable.ic_menu_share, it -> ShareUtils.shareLink(activity, getCache().getShareSubject(), logUrl))
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_menu_browser),
                    R.drawable.ic_menu_open_in_browser, it -> ShareUtils.openUrl(activity, logUrl, true))
        }
        if (isOfflineLog(log)) {
            ctxMenu.setTitle(LocalizationUtils.getString(R.string.log_your_saved_log))
            ctxMenu.addItem(0, R.string.cache_log_menu_edit, R.drawable.ic_menu_edit, it -> EditOfflineLogListener(getCache(), activity).onClick(null))
            ctxMenu.addItem(1, R.string.cache_log_menu_delete, R.drawable.ic_menu_delete, it -> deleteOfflineLogEntry(activity, getCache(), log))
        }
        return ctxMenu
    }

    private Unit deleteOfflineLogEntry(final CacheDetailActivity activity, final Geocache cache, final LogEntry entry) {
        SimpleDialog.ofContext(activity)
            .setTitle(TextParam.id(R.string.cache_log_menu_delete))
            .setMessage(TextParam.id(R.string.log_offline_delete_confirm,
                entry.logType.getL10n(), Formatter.formatShortDateVerbally(entry.date)))
            .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
            .confirm(() -> {
                DataStore.clearLogOffline(cache.getGeocode())
                cache.notifyChange()
            })
    }

    override     protected Unit fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        // finds count
        if (log.found == -1) {
            holder.binding.countOrLocation.setVisibility(View.GONE)
        } else {
            holder.binding.countOrLocation.setVisibility(View.VISIBLE)
            holder.binding.countOrLocation.setText(res.getQuantityString(R.plurals.cache_counts, log.found, log.found))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override     protected Unit fillViewHolder(final View convertView, final LogViewHolder holder, final LogEntry log) {
        super.fillViewHolder(convertView, holder, log)
        if (isOfflineLog(log)) {
            holder.binding.author.setOnClickListener(EditOfflineLogListener(getCache(), (CacheDetailActivity) getActivity()))
            holder.binding.logMark.setVisibility(View.VISIBLE)
            holder.binding.logMark.setImageResource(R.drawable.mark_orange)
        }
        // avoid the need for a Double tap to edit item (offline log) resp. display context menu (all other log types)
        // see stackoverflow.com/questions/22653641/using-onclick-on-textview-with-selectable-text-how-to-avoid-Double-click
        holder.binding.log.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.requestFocus()
            }
            return false
        })
    }

    private Boolean isOfflineLog(final LogEntry log) {
        return log.author == (getString(R.string.log_your_saved_log))
    }

    override     protected Boolean isValid() {
        return getActivity() != null && getCache() != null
    }

    override     protected String getGeocode() {
        return getCache().getGeocode()
    }

    override     protected UserClickListener createUserActionsListener(final LogEntry log) {
        val userName: String = StringEscapeUtils.unescapeHtml4(log.author)
        return UserClickListener.forUser(getCache(), userName, userName, log.authorGuid)
    }

}

package cgeo.geocaching.log;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.databinding.LogsPageBinding;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ChipChoiceGroup;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.ShareUtils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class CacheLogsViewCreator extends LogsViewCreator {

    /**
     * Which logs are shown on a logbook page.
     */
    public enum Mode {
        /** all logs without filter chips (used if friends'/own logs are shown on a separate page) */
        ALL_PLAIN,
        /** only friends', own and owner logs, filterable by combinable chips (separate friends'/own logs page) */
        SPECIAL_ONLY,
        /** all logs, filterable by single choice chips for all logs or only friends', own or owner logs */
        INTEGRATED
    }

    /**
     * Category of a log. The ordinal of the special categories is the index of the according filter chip.
     * The special categories are shown in this order as filter chips and in the page title.
     */
    private enum LogCategory {
        OWN(R.string.cache_logs_own, R.drawable.ic_menu_edit, R.string.cache_logs_count_own),
        FRIEND(R.string.cache_logs_friends, R.drawable.ic_menu_face, R.string.cache_logs_count_friends),
        OWNER(R.string.cache_logs_owner, R.drawable.star_off, R.string.cache_logs_count_owner),
        OTHER(0, 0, 0);

        private static final LogCategory[] SPECIAL = {OWN, FRIEND, OWNER};

        private final @StringRes int chipTextId;
        private final @DrawableRes int chipIconId;
        private final @StringRes int countTextId;

        LogCategory(final @StringRes int chipTextId, final @DrawableRes int chipIconId, final @StringRes int countTextId) {
            this.chipTextId = chipTextId;
            this.chipIconId = chipIconId;
            this.countTextId = countTextId;
        }
    }

    private static final String ARG_MODE = "mode";
    private static final String STATE_CHECKED_FILTER_CHIPS = "checkedFilterChips";

    private LinearLayout countview1 = null;
    private TextView countview2 = null;

    /**
     * checked filter chips, null for the initial state: all chips (combinable chips) or "All" (single choice chips).
     * Kept on configuration changes, but not persisted.
     */
    @Nullable
    private int[] checkedFilterChips = null;

    public static TabbedViewPagerFragment<LogsPageBinding> newInstance(@NonNull final Mode mode) {
        final CacheLogsViewCreator fragment = new CacheLogsViewCreator();
        final Bundle bundle = new Bundle();
        bundle.putString(ARG_MODE, mode.name());
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    private Mode getMode() {
        final Bundle arguments = getArguments();
        return arguments == null ? Mode.ALL_PLAIN : EnumUtils.getEnum(Mode.class, arguments.getString(ARG_MODE), Mode.ALL_PLAIN);
    }

    @Override
    public LogsPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final LogsPageBinding binding = super.createView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            restoreFilterState(savedInstanceState);
        }
        final Mode mode = getMode();
        if (mode != Mode.ALL_PLAIN) {
            final ChipChoiceGroup filterChips = binding.filterChips;
            if (mode == Mode.INTEGRATED) {
                filterChips.setSingleChoice(true);
                filterChips.setSelectAllText(TextParam.id(R.string.cache_filter_status_select_all));
            } else {
                filterChips.setWithSelectAllChip(false);
            }
            final List<TextParam> chipTexts = new ArrayList<>();
            for (final LogCategory category : LogCategory.SPECIAL) {
                chipTexts.add(TextParam.id(category.chipTextId));
            }
            filterChips.addChips(chipTexts);
            for (final LogCategory category : LogCategory.SPECIAL) {
                filterChips.setButtonIconByIndex(category.chipIconId, category.ordinal());
            }
            if (checkedFilterChips == null) {
                filterChips.setSelectAllChecked(true);
            } else {
                filterChips.setSelectAllChecked(false);
                filterChips.setCheckedButtonByIndex(true, checkedFilterChips);
            }
            filterChips.setOnSelectionChangedListener(this::onFilterChipsChanged);
        }
        return binding;
    }

    private void onFilterChipsChanged() {
        storeCheckedFilterChips();
        setContent();
    }

    private void storeCheckedFilterChips() {
        checkedFilterChips = binding.filterChips.getCheckedButtonIndexes().stream().mapToInt(Integer::intValue).toArray();
    }

    private void restoreFilterState(@NonNull final Bundle savedInstanceState) {
        checkedFilterChips = savedInstanceState.getIntArray(STATE_CHECKED_FILTER_CHIPS);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(STATE_CHECKED_FILTER_CHIPS, checkedFilterChips);
    }

    @Override
    public long getPageId() {
        return getMode() == Mode.SPECIAL_ONLY ? CacheDetailActivity.Page.LOGSFRIENDS.id : CacheDetailActivity.Page.LOGS.id;
    }

    private Geocache getCache() {
        final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
        return activity.getCache();
    }

    /**
     * Whether the separate friends'/own logs page has logs to show (including a saved offline log)
     */
    public static boolean hasLogsForSeparateTab(@NonNull final Geocache cache) {
        if (cache.hasLogOffline()) {
            return true;
        }
        for (final LogEntry log : cache.getFriendsLogs()) {
            if (isShownInSeparateTab(log, cache)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isShownInSeparateTab(@NonNull final LogEntry log, @NonNull final Geocache cache) {
        final LogCategory category = getCategory(log, cache);
        return category != LogCategory.OTHER && isCategoryEnabled(category);
    }

    /**
     * Separate counts of own logs (including a saved offline log), friends' logs and owner logs shown in the logbook page title, e.g. "1✎/3⚇/2☆".
     * Friends' and owner logs are only counted if they are loaded additionally.
     * Counted categories are also shown without logs, e.g. "0✎/3⚇/0☆".
     */
    @NonNull
    public static String getSpecialLogsCountText(@NonNull final Geocache cache) {
        final Map<LogCategory, Integer> categoryCounts = new EnumMap<>(LogCategory.class);
        if (cache.hasLogOffline()) {
            categoryCounts.put(LogCategory.OWN, 1);
        }
        for (final LogEntry log : cache.getLogs()) {
            categoryCounts.merge(getCategory(log, cache), 1, Integer::sum);
        }
        final List<String> counts = new ArrayList<>();
        for (final LogCategory category : LogCategory.SPECIAL) {
            if (isCategoryEnabled(category)) {
                final Integer count = ObjectUtils.getIfNull(categoryCounts.get(category), 0);
                counts.add(LocalizationUtils.getString(category.countTextId, count));
            }
        }
        return StringUtils.join(counts, '/');
    }

    @NonNull
    private static LogCategory getCategory(@NonNull final LogEntry log, @NonNull final Geocache cache) {
        if (isOfflineLog(log) || LogUtils.isOwnLog(log, cache)) {
            return LogCategory.OWN;
        }
        if (LogUtils.isOwnerLog(log, cache)) {
            return LogCategory.OWNER;
        }
        return log.friend ? LogCategory.FRIEND : LogCategory.OTHER;
    }

    /**
     * All logs of this page, ignoring the filter chips.
     */
    public static List<LogEntry> getUnfilteredLogs(@NonNull final Geocache cache, final Mode mode) {
        if (mode != Mode.SPECIAL_ONLY) {
            return addOwnOfflineLog(cache, cache.getLogs());
        }
        final List<LogEntry> logs = addOwnOfflineLog(cache, cache.getFriendsLogs());
        logs.removeIf(log -> !isShownInSeparateTab(log, cache));
        return logs;
    }

    /**
     * @return true if the complete logbook of the cache is shown (no filter active)
     */
    private boolean isCompleteLogbook() {
        final Mode mode = getMode();
        return mode == Mode.ALL_PLAIN || (mode == Mode.INTEGRATED && binding.filterChips.getCheckedButtonIndexes().isEmpty());
    }

    @Override
    protected List<LogEntry> getLogs() {
        final Geocache cache = getCache();
        final List<LogEntry> logs = getUnfilteredLogs(cache, getMode());
        if (isCompleteLogbook()) {
            return logs;
        }
        final Set<Integer> checkedCategories = binding.filterChips.getCheckedButtonIndexes();
        logs.removeIf(log -> !checkedCategories.contains(getCategory(log, cache).ordinal()));
        return logs;
    }

    private static List<LogEntry> addOwnOfflineLog(final Geocache cache, final List<LogEntry> logsIn) {
        final LogEntry log = DataStore.loadLogOffline(cache.getGeocode());
        final List<LogEntry> logs = new ArrayList<>(logsIn);
        if (log != null) {
            logs.add(0, log.buildUpon().setAuthor(LocalizationUtils.getString(R.string.log_your_saved_log)).build());
        }
        return logs;
    }

    @Override
    protected void addHeaderView() {
        if (binding != null) {
            updateFilter();
            addLogCountsHeader();
            addEmptyLogsHeader();
        }
    }

    private void updateFilter() {
        if (getMode() == Mode.ALL_PLAIN) {
            return;
        }
        final Geocache cache = getCache();
        final Set<LogCategory> presentCategories = EnumSet.noneOf(LogCategory.class);
        for (final LogEntry log : getUnfilteredLogs(cache, getMode())) {
            presentCategories.add(getCategory(log, cache));
        }
        updateFilterChips(presentCategories);
    }

    /**
     * Friends' and owner logs may also be part of the regular logbook,
     * but are counted in the page title and filterable only if they are loaded additionally
     */
    private static boolean isCategoryEnabled(@NonNull final LogCategory category) {
        switch (category) {
            case FRIEND:
                return Settings.isFriendLogsWanted();
            case OWNER:
                return Settings.isLoadOwnerLogs();
            default:
                return true;
        }
    }

    /**
     * Shows only the filter chips of enabled categories which are present in the logs of this page
     * (single choice: if the checked chip gets hidden, "All" gets checked)
     */
    private void updateFilterChips(@NonNull final Set<LogCategory> presentCategories) {
        boolean anyChipVisible = false;
        for (final LogCategory category : LogCategory.SPECIAL) {
            final boolean visible = isCategoryEnabled(category) && presentCategories.contains(category);
            binding.filterChips.setButtonVisibleByIndex(visible, category.ordinal());
            anyChipVisible |= visible;
        }
        binding.filterChips.setVisibility(anyChipVisible ? View.VISIBLE : View.GONE);
        storeCheckedFilterChips();
    }

    @SuppressLint("SetTextI18n")
    private void addLogCountsHeader() {
        if (countview1 != null) {
            binding.logsItems.removeHeaderView(countview1);
            countview1 = null;
        }

        final Map<LogType, Integer> logCounts = isCompleteLogbook() ? getCache().getLogCounts() : getLogs().stream().collect(Collectors.groupingBy(log -> log.logType, Collectors.summingInt(log -> 1)));
        if (logCounts != null) {
            final List<Entry<LogType, Integer>> sortedLogCounts = new ArrayList<>(logCounts.size());
            for (final Entry<LogType, Integer> entry : logCounts.entrySet()) {
                // it may happen that the label is unknown -> then avoid any output for this type
                if (entry.getKey() != LogType.PUBLISH_LISTING && entry.getValue() != 0) {
                    sortedLogCounts.add(entry);
                }
            }

            if (!sortedLogCounts.isEmpty()) {
                // sort the log counts by type id ascending. that way the FOUND, DNF log types are the first and most visible ones
                Collections.sort(sortedLogCounts, Comparator.comparing(Entry::getKey));

                final List<String> labels = new ArrayList<>(sortedLogCounts.size());
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    labels.add(pair.getValue() + "× " + pair.getKey().getL10n());
                }

                countview1 = new LinearLayout(getActivity());
                final TextView logtypes = new TextView(getActivity());
                logtypes.setText(LocalizationUtils.getString(R.string.cache_log_types) + ": ");
                countview1.addView(logtypes);
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    final TextView tv = new TextView(getActivity());
                    tv.setText(pair.getValue().toString());
                    tv.setCompoundDrawablesWithIntrinsicBounds(pair.getKey().getLogOverlay(), 0, 0, 0);
                    tv.setCompoundDrawablePadding(4);
                    tv.setPadding(0, 0, 10, 0);
                    TooltipCompat.setTooltipText(tv, pair.getKey().getL10n());
                    countview1.addView(tv);
                }
                binding.logsItems.addHeaderView(countview1);
            }
        }
    }

    private void addEmptyLogsHeader() {
        if (countview2 != null) {
            binding.logsItems.removeHeaderView(countview2);
            countview2 = null;
        }

        if (getLogs().isEmpty()) {
            countview2 = new TextView(getActivity());
            countview2.setText(isCompleteLogbook() ? LocalizationUtils.getString(R.string.log_empty_logbook) : LocalizationUtils.getString(R.string.log_empty_logbook_filtered));
            binding.logsItems.addHeaderView(countview2);
        }
    }

    @Override
    protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, @NonNull final LogEntry log) {
        final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
        final String logUrl = getCache().getLogUrl(log);
        final boolean canShareLog = StringUtils.isNotBlank(logUrl);
        if (canShareLog) {
            ctxMenu.addItem(R.string.context_share_as_link, R.drawable.ic_menu_share, it -> ShareUtils.shareLink(activity, getCache().getShareSubject(), logUrl));
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_menu_browser),
                    R.drawable.ic_menu_open_in_browser, it -> ShareUtils.openUrl(activity, logUrl, true));
        }
        if (isOfflineLog(log)) {
            ctxMenu.setTitle(LocalizationUtils.getString(R.string.log_your_saved_log));
            ctxMenu.addItem(0, R.string.cache_log_menu_edit, R.drawable.ic_menu_edit, it -> new EditOfflineLogListener(getCache(), activity).onClick(null));
            ctxMenu.addItem(1, R.string.cache_log_menu_delete, R.drawable.ic_menu_delete, it -> deleteOfflineLogEntry(activity, getCache(), log));
        }
        return ctxMenu;
    }

    private void deleteOfflineLogEntry(final CacheDetailActivity activity, final Geocache cache, final LogEntry entry) {
        SimpleDialog.ofContext(activity)
            .setTitle(TextParam.id(R.string.cache_log_menu_delete))
            .setMessage(TextParam.id(R.string.log_offline_delete_confirm,
                entry.logType.getL10n(), Formatter.formatShortDateVerbally(entry.date)))
            .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
            .confirm(() -> {
                DataStore.clearLogOffline(cache.getGeocode());
                cache.notifyChange();
            });
    }

    @Override
    protected void fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        // finds count
        if (log.found == -1) {
            holder.binding.countOrLocation.setVisibility(View.GONE);
        } else {
            holder.binding.countOrLocation.setVisibility(View.VISIBLE);
            holder.binding.countOrLocation.setText(LocalizationUtils.getPlural(R.plurals.cache_counts, log.found));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void fillViewHolder(final @NonNull LogViewHolder holder, final LogEntry log) {
        super.fillViewHolder(holder, log);
        if (isOfflineLog(log)) {
            holder.binding.author.setOnClickListener(new EditOfflineLogListener(getCache(), (CacheDetailActivity) getActivity()));
            holder.binding.logMark.setVisibility(View.VISIBLE);
            holder.binding.logMark.setImageResource(R.drawable.mark_orange);
        }
        // avoid the need for a double tap to edit item (offline log) resp. display context menu (all other log types)
        // see stackoverflow.com/questions/22653641/using-onclick-on-textview-with-selectable-text-how-to-avoid-double-click
        holder.binding.log.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.requestFocus();
            }
            return false;
        });
    }

    private static boolean isOfflineLog(final LogEntry log) {
        return log.author.equals(LocalizationUtils.getString(R.string.log_your_saved_log));
    }

    @Override
    protected boolean isValid() {
        return getActivity() != null && getCache() != null;
    }

    @Override
    protected String getGeocode() {
        return getCache().getGeocode();
    }

    @Override
    protected UserClickListener createUserActionsListener(final LogEntry log) {
        final String userName = StringEscapeUtils.unescapeHtml4(log.author);
        return UserClickListener.forUser(getCache(), userName, userName, log.authorGuid);
    }

}

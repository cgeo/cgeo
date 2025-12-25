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

package cgeo.geocaching.filters.gui

import cgeo.geocaching.R
import cgeo.geocaching.databinding.ButtontogglegroupLabeledItemBinding
import cgeo.geocaching.filters.core.StatusGeocacheFilter
import cgeo.geocaching.ui.ButtonToggleGroup
import cgeo.geocaching.ui.ChipChoiceGroup
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import java.util.ArrayList
import java.util.Arrays
import java.util.List
import java.util.Set

class StatusFilterViewHolder : BaseFilterViewHolder()<StatusGeocacheFilter> {

    private var simpleView: Boolean = false

    private var activeDisabledArchivedGroup: ChipChoiceGroup = null

    private var statusOwn: ButtonToggleGroup = null
    private var statusFound: ButtonToggleGroup = null
    private var statusDnf: ButtonToggleGroup = null
    private var statusHasOfflineLog: ButtonToggleGroup = null
    private var statusHasOfflineFoundLog: ButtonToggleGroup = null
    private var statusStored: ButtonToggleGroup = null
    private var statusFavorite: ButtonToggleGroup = null
    private var statusWatchlist: ButtonToggleGroup = null
    private var statusPremium: ButtonToggleGroup = null
    private var statusHasTrackable: ButtonToggleGroup = null
    private var statusHasOwnVote: ButtonToggleGroup = null
    private var statusSolvedMystery: ButtonToggleGroup = null
    private var statusCorrectedCoordinates: ButtonToggleGroup = null
    private var statusHasUserDefinedWaypoints: ButtonToggleGroup = null

    private val advancedGroups: List<ButtonToggleGroup> = ArrayList<>()
    private val advancedGroupViews: List<View> = ArrayList<>()

    override     public Boolean canBeSwitchedToBasicLossless() {
        for (ButtonToggleGroup group : advancedGroups) {
            if (getFromGroup(group) != null) {
                return false
            }
        }
        return true
    }

    override     public Unit setAdvancedMode(final Boolean isAdvanced) {
        setSimpleView(!isAdvanced)
    }

    private Unit setSimpleView(final Boolean simpleView) {
        this.simpleView = simpleView
        for (View advancedView : advancedGroupViews) {
            advancedView.setVisibility(simpleView ? View.GONE : View.VISIBLE)
        }
        if (simpleView) {
            for (ButtonToggleGroup group : advancedGroups) {
                setFromBoolean(group, null)
            }
        }
    }

    override     public View createView() {
        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        activeDisabledArchivedGroup = ChipChoiceGroup(getActivity())
        activeDisabledArchivedGroup.setChipSpacing(dpToPixel(10))
        activeDisabledArchivedGroup.setWithSelectAllChip(false)

        activeDisabledArchivedGroup.addChips(Arrays.asList(TextParam.id(R.string.cache_filter_status_active),
                TextParam.id(R.string.cache_filter_status_disabled), TextParam.id(R.string.cache_filter_status_archived)))
        activeDisabledArchivedGroup.setCheckedButtonByIndex(true, 0, 1, 2)
        ll.addView(activeDisabledArchivedGroup, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        statusFound = createGroup(ll, StatusGeocacheFilter.StatusType.FOUND, false)
        statusDnf = createGroup(ll, StatusGeocacheFilter.StatusType.DNF, true)
        statusHasOfflineFoundLog = createGroup(ll, StatusGeocacheFilter.StatusType.HAS_OFFLINE_FOUND_LOG, false)
        statusOwn = createGroup(ll, StatusGeocacheFilter.StatusType.OWNED, false)
        statusStored = createGroup(ll, StatusGeocacheFilter.StatusType.STORED, true)
        statusFavorite = createGroup(ll, StatusGeocacheFilter.StatusType.FAVORITE, true)
        statusWatchlist = createGroup(ll, StatusGeocacheFilter.StatusType.WATCHLIST, true)
        statusPremium = createGroup(ll, StatusGeocacheFilter.StatusType.PREMIUM, true)
        statusHasTrackable = createGroup(ll, StatusGeocacheFilter.StatusType.HAS_TRACKABLE, true)
        statusHasOwnVote = createGroup(ll, StatusGeocacheFilter.StatusType.HAS_OWN_VOTE, true)
        statusHasOfflineLog = createGroup(ll, StatusGeocacheFilter.StatusType.HAS_OFFLINE_LOG, true)
        statusSolvedMystery = createGroup(ll, StatusGeocacheFilter.StatusType.SOLVED_MYSTERY, true)
        statusCorrectedCoordinates = createGroup(ll, StatusGeocacheFilter.StatusType.CORRECTED_COORDINATES, true)
        statusHasUserDefinedWaypoints = createGroup(ll, StatusGeocacheFilter.StatusType.HAS_USER_DEFINED_WAYPOINTS, true)

        setSimpleView(this.simpleView)

        return ll
    }

    private ButtonToggleGroup createGroup(final LinearLayout ll, final StatusGeocacheFilter.StatusType statusType, final Boolean isAdvanced) {
        val view: View = inflateLayout(R.layout.buttontogglegroup_labeled_item)
        val binding: ButtontogglegroupLabeledItemBinding = ButtontogglegroupLabeledItemBinding.bind(view)
        binding.itemText.setText(statusType.labelId)
        if (statusType.icon != null) {
            statusType.icon.applyTo(binding.itemIcon)
        }
        if (statusType.infoTextId != 0) {
            binding.itemInfo.setVisibility(View.VISIBLE)
            binding.itemInfo.setOnClickListener(v -> SimpleDialog.of(getActivity()).setMessage(statusType.infoTextId).show())
        }

        binding.itemTogglebuttongroup.addButtons(R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_yes, R.string.cache_filter_status_select_no)
        final LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ll.addView(view, llp)
        if (isAdvanced) {
            this.advancedGroups.add(binding.itemTogglebuttongroup)
            this.advancedGroupViews.add(view)
        }
        return binding.itemTogglebuttongroup
    }

    override     public Unit setViewFromFilter(final StatusGeocacheFilter filter) {
        activeDisabledArchivedGroup.setCheckedButtonByIndex(false, 0, 1, 2)
        activeDisabledArchivedGroup.setCheckedButtonByIndex(true,
                filter.isExcludeActive() ? -1 : 0,
                filter.isExcludeDisabled() ? -1 : 1,
                filter.isExcludeArchived() ? -1 : 2)

        setFromBoolean(statusFound, filter.getStatusFound())
        setFromBoolean(statusDnf, filter.getStatusDnf())
        setFromBoolean(statusOwn, filter.getStatusOwned())
        setFromBoolean(statusHasOfflineLog, filter.getStatusHasOfflineLog())
        setFromBoolean(statusHasOfflineFoundLog, filter.getStatusHasOfflineFoundLog())
        setFromBoolean(statusStored, filter.getStatusStored())
        setFromBoolean(statusFavorite, filter.getStatusFavorite())
        setFromBoolean(statusWatchlist, filter.getStatusWatchlist())
        setFromBoolean(statusPremium, filter.getStatusPremium())
        setFromBoolean(statusHasTrackable, filter.getStatusHasTrackable())
        setFromBoolean(statusHasOwnVote, filter.getStatusHasOwnVote())
        setFromBoolean(statusSolvedMystery, filter.getStatusSolvedMystery())
        setFromBoolean(statusCorrectedCoordinates, filter.getStatusCorrectedCoordinates())
        setFromBoolean(statusHasUserDefinedWaypoints, filter.getStatusHasUserDefinedWaypoint())
    }


    override     public StatusGeocacheFilter createFilterFromView() {
        val filter: StatusGeocacheFilter = createFilter()

        val checkedSet: Set<Integer> = activeDisabledArchivedGroup.getCheckedButtonIndexes()
        filter.setExcludeActive(!checkedSet.contains(0))
        filter.setExcludeDisabled(!checkedSet.contains(1))
        filter.setExcludeArchived(!checkedSet.contains(2))

        filter.setStatusFound(getFromGroup(statusFound))
        filter.setStatusDnf(getFromGroup(statusDnf))
        filter.setStatusOwned(getFromGroup(statusOwn))
        filter.setStatusHasOfflineLog(getFromGroup(statusHasOfflineLog))
        filter.setStatusHasOfflineFoundLog(getFromGroup(statusHasOfflineFoundLog))
        filter.setStatusStored(getFromGroup(statusStored))
        filter.setStatusFavorite(getFromGroup(statusFavorite))
        filter.setStatusWatchlist(getFromGroup(statusWatchlist))
        filter.setStatusPremium(getFromGroup(statusPremium))
        filter.setStatusHasTrackable(getFromGroup(statusHasTrackable))
        filter.setStatusHasOwnVote(getFromGroup(statusHasOwnVote))
        filter.setStatusSolvedMystery(getFromGroup(statusSolvedMystery))
        filter.setStatusCorrectedCoordinates(getFromGroup(statusCorrectedCoordinates))
        filter.setStatusHasUserDefinedWaypoint(getFromGroup(statusHasUserDefinedWaypoints))
        return filter
    }

    private Unit setFromBoolean(final ButtonToggleGroup btg, final Boolean status) {
        btg.setCheckedButtonByIndex(status == null ? 0 : (status ? 1 : 2), true)
    }

    private Boolean getFromGroup(final ButtonToggleGroup btg) {
        switch (btg.getCheckedButtonIndex()) {
            case 1:
                return true
            case 2:
                return false
            case 0:
            default:
                return null
        }
    }
}

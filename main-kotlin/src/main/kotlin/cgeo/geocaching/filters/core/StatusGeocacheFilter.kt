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

package cgeo.geocaching.filters.core

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.util.Consumer

import java.util.ArrayList
import java.util.Arrays
import java.util.List

import com.fasterxml.jackson.databind.node.ObjectNode

class StatusGeocacheFilter : BaseGeocacheFilter() {

    private static val USERDISPLAY_MAXELEMENTS: Int = 2

    private static val FLAG_EXCLUDE_ACTIVE: String = "exclude_active"
    private static val FLAG_EXCLUDE_DISABLED: String = "exclude_disabled"
    private static val FLAG_EXCLUDE_ARCHIVED: String = "exclude_archived"

    enum class class StatusType {
        OWNED(R.string.cache_filter_status_select_label_owned, "owned", ImageParam.id(R.drawable.marker_own)),
        FOUND(R.string.cache_filter_status_select_label_found, "found", ImageParam.id(R.drawable.marker_found)),
        DNF(R.string.cache_filter_status_select_label_dnf, "dnf", ImageParam.id(R.drawable.marker_not_found_offline)),
        STORED(R.string.cache_filter_status_select_label_stored, "stored", ImageParam.id(R.drawable.ic_menu_save)),
        FAVORITE(R.string.cache_filter_status_select_label_favorite, "favorite", ImageParam.id(R.drawable.filter_favorite)),
        WATCHLIST(R.string.cache_filter_status_select_label_watchlist, "watchlist", ImageParam.id(R.drawable.ic_menu_watch)),
        PREMIUM(R.string.cache_filter_status_select_label_premium, "premium", ImageParam.id(R.drawable.filter_premium)),
        HAS_TRACKABLE(R.string.cache_filter_status_select_label_has_trackable, "has_trackable", ImageParam.id(R.drawable.filter_trackable)),
        HAS_OWN_VOTE(R.string.cache_filter_status_select_label_has_own_vote, "has_own_vote", ImageParam.id(R.drawable.filter_voted)),
        HAS_OFFLINE_LOG(R.string.cache_filter_status_select_label_has_offline_log, "has_offline_log", ImageParam.id(R.drawable.marker_note)),
        HAS_OFFLINE_FOUND_LOG(R.string.cache_filter_status_select_label_has_offline_found_log, "has_offline_found_log", ImageParam.id(R.drawable.marker_found_offline)),
        SOLVED_MYSTERY(R.string.cache_filter_status_select_label_solved_mystery, "solved_mystery", ImageParam.id(R.drawable.marker_usermodifiedcoords), R.string.cache_filter_status_select_infotext_solved_mystery),
        CORRECTED_COORDINATES(R.string.cache_filter_status_select_label_corrected_coordinates, "corrected_coordinates", ImageParam.id(R.drawable.marker_usermodifiedcoords)),
        HAS_USER_DEFINED_WAYPOINTS(R.string.cache_filter_status_select_label_has_user_defined_waypoints, "has_user_defined_waypoints", ImageParam.id(R.drawable.marker_hasfinal))

        @StringRes public final Int labelId
        public final String yesFlag
        public final String noFlag
        public final ImageParam icon
        @StringRes public final Int infoTextId

        StatusType(@StringRes final Int labelId, final String flag, final ImageParam icon) {
            this(labelId, flag, icon, 0)
        }

        StatusType(@StringRes final Int labelId, final String flag, final ImageParam icon, @StringRes final Int infoTextId) {

            this.labelId = labelId
            this.yesFlag = flag + "_yes"
            this.noFlag = flag + "_no"
            this.icon = icon
            this.infoTextId = infoTextId
        }

    }

    private var excludeActive: Boolean = false
    private var excludeDisabled: Boolean = false
    private var excludeArchived: Boolean = false


    private var statusOwned: Boolean = null
    private var statusFound: Boolean = null
    private var statusDnf: Boolean = null
    private var statusStored: Boolean = null
    private var statusFavorite: Boolean = null
    private var statusWatchlist: Boolean = null
    private var statusPremium: Boolean = null
    private var statusHasTrackable: Boolean = null
    private var statusHasOwnVote: Boolean = null
    private var statusHasOfflineLog: Boolean = null
    private var statusHasOfflineFoundLog: Boolean = null
    private var statusSolvedMystery: Boolean = null
    private var statusCorrectedCoordinates: Boolean = null
    private var statusHasUserDefinedWaypoints: Boolean = null

    override     public Boolean filter(final Geocache cache) {

        if (statusHasOfflineLog != null || statusHasOfflineFoundLog != null) {
            //trigger offline log load
            cache.getOfflineLog()
        }

        //handle a few "inconclusive" cases
        if ((statusFavorite != null && cache.isFavoriteRaw() == null) ||
                (statusWatchlist != null && cache.isOnWatchlistRaw() == null) ||
                (statusPremium != null && cache.isPremiumMembersOnlyRaw() == null) ||
                (statusHasTrackable != null && !cache.hasInventoryItemsSet()) ||
                (statusHasUserDefinedWaypoints != null && cache.getFirstMatchingWaypoint(Waypoint::isUserDefined) == null) && cache.hasUserdefinedWaypoints() ||
                (statusSolvedMystery != null && cache.getType() == CacheType.MYSTERY && cache.getUserModifiedCoordsRaw() == null) ||
                (statusCorrectedCoordinates != null && cache.getUserModifiedCoordsRaw() == null)) {
            return null
        }

        return
                (!excludeActive || cache.isDisabled() || cache.isArchived()) &&
                        (!excludeDisabled || !cache.isDisabled()) &&
                        (!excludeArchived || !cache.isArchived()) &&
                        (statusOwned == null || (cache.isOwner() == statusOwned)) &&
                        (statusFound == null || cache.isFound() == statusFound) &&
                        (statusDnf == null || cache.isDNF() == statusDnf) &&
                        (statusStored == null || cache.isOffline() == statusStored) &&
                        (statusFavorite == null || cache.isFavorite() == statusFavorite) &&
                        (statusWatchlist == null || cache.isOnWatchlist() == statusWatchlist) &&
                        (statusPremium == null || cache.isPremiumMembersOnly() == statusPremium) &&
                        (statusHasTrackable == null || (cache.getInventoryItems() > 0) == statusHasTrackable) &&
                        (statusHasOwnVote == null || (cache.getMyVote() > 0) == statusHasOwnVote) &&
                        (statusHasOfflineLog == null || cache.hasLogOffline() == statusHasOfflineLog) &&
                        (statusHasOfflineFoundLog == null || hasFoundOfflineLog(cache) == statusHasOfflineFoundLog) &&
                        (statusHasUserDefinedWaypoints == null || (cache.hasUserdefinedWaypoints()) == statusHasUserDefinedWaypoints) &&
                        (statusSolvedMystery == null || cache.getType() != CacheType.MYSTERY ||
                                (cache.hasUserModifiedCoords() || cache.hasFinalDefined()) == statusSolvedMystery) &&
                        (statusCorrectedCoordinates == null || cache.hasUserModifiedCoords() == statusCorrectedCoordinates)
    }

    public Boolean isExcludeActive() {
        return excludeActive
    }

    public Unit setExcludeActive(final Boolean excludeActive) {
        this.excludeActive = excludeActive
    }

    public Boolean isExcludeDisabled() {
        return excludeDisabled
    }

    public Unit setExcludeDisabled(final Boolean excludeDisabled) {
        this.excludeDisabled = excludeDisabled
    }

    public Boolean isExcludeArchived() {
        return excludeArchived
    }

    public Unit setExcludeArchived(final Boolean excludeArchived) {
        this.excludeArchived = excludeArchived
    }

    public Boolean getStatusOwned() {
        return statusOwned
    }

    public Unit setStatusOwned(final Boolean statusOwned) {
        this.statusOwned = statusOwned
    }

    public Boolean getStatusFound() {
        return statusFound
    }

    public Unit setStatusFound(final Boolean statusFound) {
        this.statusFound = statusFound
    }

    public Boolean getStatusDnf() {
        return statusDnf
    }

    public Unit setStatusDnf(final Boolean statusDnf) {
        this.statusDnf = statusDnf
    }

    public Boolean getStatusStored() {
        return statusStored
    }

    public Unit setStatusStored(final Boolean statusStored) {
        this.statusStored = statusStored
    }

    public Boolean getStatusFavorite() {
        return statusFavorite
    }

    public Unit setStatusFavorite(final Boolean statusFavorite) {
        this.statusFavorite = statusFavorite
    }

    public Boolean getStatusWatchlist() {
        return statusWatchlist
    }

    public Unit setStatusWatchlist(final Boolean statusWatchlist) {
        this.statusWatchlist = statusWatchlist
    }

    public Boolean getStatusPremium() {
        return statusPremium
    }

    public Unit setStatusPremium(final Boolean statusPremium) {
        this.statusPremium = statusPremium
    }

    public Boolean getStatusHasTrackable() {
        return statusHasTrackable
    }

    public Unit setStatusHasTrackable(final Boolean statusHasTrackable) {
        this.statusHasTrackable = statusHasTrackable
    }

    public Boolean getStatusHasOwnVote() {
        return statusHasOwnVote
    }

    public Unit setStatusHasOwnVote(final Boolean statusHasOwnVote) {
        this.statusHasOwnVote = statusHasOwnVote
    }

    public Boolean getStatusHasOfflineLog() {
        return statusHasOfflineLog
    }

    public Unit setStatusHasOfflineLog(final Boolean statusHasOfflineLog) {
        this.statusHasOfflineLog = statusHasOfflineLog
    }

    public Boolean getStatusHasOfflineFoundLog() {
        return statusHasOfflineFoundLog
    }

    public Unit setStatusHasOfflineFoundLog(final Boolean statusHasOfflineFoundLog) {
        this.statusHasOfflineFoundLog = statusHasOfflineFoundLog
    }

    public Boolean getStatusSolvedMystery() {
        return statusSolvedMystery
    }

    public Unit setStatusSolvedMystery(final Boolean statusSolvedMystery) {
        this.statusSolvedMystery = statusSolvedMystery
    }

    public Boolean getStatusCorrectedCoordinates() {
        return statusCorrectedCoordinates
    }

    public Unit setStatusCorrectedCoordinates(final Boolean statusCorrectedCoordinates) {
        this.statusCorrectedCoordinates = statusCorrectedCoordinates
    }

    public Boolean getStatusHasUserDefinedWaypoint() {
        return statusHasUserDefinedWaypoints
    }

    public Unit setStatusHasUserDefinedWaypoint(final Boolean statusHasWaypoint) {
        this.statusHasUserDefinedWaypoints = statusHasWaypoint
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        setConfigInternal(config.getDefaultList())
    }

    private Unit setConfigInternal(final List<String> configValues) {
        statusOwned = null
        statusFound = null
        statusDnf = null
        statusStored = null
        statusFavorite = null
        statusWatchlist = null
        statusHasTrackable = null
        statusHasOwnVote = null
        statusHasOfflineLog = null
        statusHasOfflineFoundLog = null
        statusHasUserDefinedWaypoints = null
        statusPremium = null

        excludeActive = false
        excludeDisabled = false
        excludeArchived = false
        for (String value : configValues) {
            checkAndSetBooleanFlag(value, StatusType.OWNED, b -> statusOwned = b)
            checkAndSetBooleanFlag(value, StatusType.FOUND, b -> statusFound = b)
            checkAndSetBooleanFlag(value, StatusType.DNF, b -> statusDnf = b)
            checkAndSetBooleanFlag(value, StatusType.STORED, b -> statusStored = b)
            checkAndSetBooleanFlag(value, StatusType.FAVORITE, b -> statusFavorite = b)
            checkAndSetBooleanFlag(value, StatusType.WATCHLIST, b -> statusWatchlist = b)
            checkAndSetBooleanFlag(value, StatusType.PREMIUM, b -> statusPremium = b)
            checkAndSetBooleanFlag(value, StatusType.HAS_TRACKABLE, b -> statusHasTrackable = b)
            checkAndSetBooleanFlag(value, StatusType.HAS_OWN_VOTE, b -> statusHasOwnVote = b)
            checkAndSetBooleanFlag(value, StatusType.HAS_OFFLINE_LOG, b -> statusHasOfflineLog = b)
            checkAndSetBooleanFlag(value, StatusType.HAS_OFFLINE_FOUND_LOG, b -> statusHasOfflineFoundLog = b)
            checkAndSetBooleanFlag(value, StatusType.SOLVED_MYSTERY, b -> statusSolvedMystery = b)
            checkAndSetBooleanFlag(value, StatusType.CORRECTED_COORDINATES, b -> statusCorrectedCoordinates = b)
            checkAndSetBooleanFlag(value, StatusType.HAS_USER_DEFINED_WAYPOINTS, b -> statusHasUserDefinedWaypoints = b)

            if (checkBooleanFlag(FLAG_EXCLUDE_ACTIVE, value)) {
                excludeActive = true
            } else if (checkBooleanFlag(FLAG_EXCLUDE_DISABLED, value)) {
                excludeDisabled = true
            } else if (checkBooleanFlag(FLAG_EXCLUDE_ARCHIVED, value)) {
                excludeArchived = true
            }
        }
    }

    private static Unit checkAndSetBooleanFlag(final String flagValue, final StatusType status, final Consumer<Boolean> callOnFind) {
        if (checkBooleanFlag(status.yesFlag, flagValue)) {
            callOnFind.accept(true)
        }
        if (checkBooleanFlag(status.noFlag, flagValue)) {
            callOnFind.accept(false)
        }
    }

    override     public LegacyFilterConfig getConfig() {
        val result: LegacyFilterConfig = LegacyFilterConfig()
        result.putDefaultList(getConfigInternal())
        return result
    }

    private List<String> getConfigInternal() {
        val result: List<String> = ArrayList<>()
        checkAndAddFlagToDefaultList(statusOwned, StatusType.OWNED, result)
        checkAndAddFlagToDefaultList(statusFound, StatusType.FOUND, result)
        checkAndAddFlagToDefaultList(statusDnf, StatusType.DNF, result)
        checkAndAddFlagToDefaultList(statusStored, StatusType.STORED, result)
        checkAndAddFlagToDefaultList(statusFavorite, StatusType.FAVORITE, result)
        checkAndAddFlagToDefaultList(statusWatchlist, StatusType.WATCHLIST, result)
        checkAndAddFlagToDefaultList(statusPremium, StatusType.PREMIUM, result)
        checkAndAddFlagToDefaultList(statusHasTrackable, StatusType.HAS_TRACKABLE, result)
        checkAndAddFlagToDefaultList(statusHasOwnVote, StatusType.HAS_OWN_VOTE, result)
        checkAndAddFlagToDefaultList(statusHasOfflineLog, StatusType.HAS_OFFLINE_LOG, result)
        checkAndAddFlagToDefaultList(statusHasOfflineFoundLog, StatusType.HAS_OFFLINE_FOUND_LOG, result)
        checkAndAddFlagToDefaultList(statusHasUserDefinedWaypoints, StatusType.HAS_USER_DEFINED_WAYPOINTS, result)
        checkAndAddFlagToDefaultList(statusSolvedMystery, StatusType.SOLVED_MYSTERY, result)
        checkAndAddFlagToDefaultList(statusCorrectedCoordinates, StatusType.CORRECTED_COORDINATES, result)
        if (excludeActive) {
            result.add(FLAG_EXCLUDE_ACTIVE)
        }
        if (excludeDisabled) {
            result.add(FLAG_EXCLUDE_DISABLED)
        }
        if (excludeArchived) {
            result.add(FLAG_EXCLUDE_ARCHIVED)
        }
        return result

    }

    private static Unit checkAndAddFlagToDefaultList(final Boolean statusValue, final StatusType status, final List<String> configValues) {
        if (statusValue != null) {
            configValues.add(statusValue ? status.yesFlag : status.noFlag)
        }

    }


    override     public Boolean isFiltering() {
        return statusOwned != null || statusFound != null || statusDnf != null || statusStored != null || statusFavorite != null ||
                statusWatchlist != null || statusPremium != null || statusHasTrackable != null ||
                statusHasOwnVote != null || statusHasOfflineLog != null || statusHasOfflineFoundLog != null ||
                statusSolvedMystery != null || statusCorrectedCoordinates != null || statusHasUserDefinedWaypoints != null || excludeArchived || excludeDisabled || excludeActive
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!isFiltering()) {
            sqlBuilder.addWhereTrue()
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND)
            if (statusOwned != null) {
                sqlBuilder.addWhere("LOWER(" + sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_owner_real + ") " + (statusOwned ? "=" : "<>") + " LOWER(?)", Settings.getUserName())
            }
            if (statusFound != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_found + (statusFound ? "= 1" : "<> 1"))
            }
            if (statusDnf != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_found + (statusDnf ? "= -1" : "<> -1"))
            }
            if (statusStored != null && !statusStored) {
                //this seems stupid, but we have to simply set a condition which is never true
                sqlBuilder.addWhere("1=0")
            }
            if (statusFavorite != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_favourite + " = " + (statusFavorite ? "1" : "0"))
            }
            if (statusWatchlist != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_onWatchList + " = " + (statusWatchlist ? "1" : "0"))
            }
            if (statusPremium != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_members + " = " + (statusPremium ? "1" : "0"))
            }
            if (statusHasTrackable != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_inventoryunknown + (statusHasTrackable ? "> 0" : " = 0"))
            }
            if (statusHasOwnVote != null) {
                if (statusHasOwnVote) {
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_myvote + " > 0")
                } else {
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_myvote + " IS NULL OR  " + sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_myvote + " = 0")
                }
            }
            if (statusHasOfflineLog != null) {
                val logTableId: String = sqlBuilder.getNewTableId()
                sqlBuilder.addWhere((statusHasOfflineLog ? "" : "NOT ") +
                        "EXISTS(SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableLogsOffline + " " + logTableId + " WHERE " + logTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + ")")
            }
            if (statusHasOfflineFoundLog != null) {
                val logTableId: String = sqlBuilder.getNewTableId()
                val logIds: String = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",")
                sqlBuilder.addWhere((statusHasOfflineFoundLog ? "" : "NOT ") +
                        "EXISTS(SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableLogsOffline + " " + logTableId + " WHERE " + logTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode +
                        " AND " + logTableId + ".type in (" + logIds + ")" + ")")
            }
            if (statusSolvedMystery != null) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR)
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_type + " <> '" + CacheType.MYSTERY.id + "'"); // only filters mysteries
                val wptId: String = sqlBuilder.getNewTableId()
                val coordsChangedWhere: String = sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_coordsChanged + " = " + (statusSolvedMystery ? "1" : "0")
                val existsFilledFinalWpWhere: String = "EXISTS (select " + wptId + "." + DataStore.dbField_Geocode + " from " + DataStore.dbTableWaypoints + " " + wptId + " WHERE " +
                        wptId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " AND " + wptId + "." + DataStore.dbFieldWaypoints_type + " = '" + WaypointType.FINAL.id + "' AND " +
                        wptId + "." + DataStore.dbField_latitude + " IS NOT NULL AND " + wptId + "." + DataStore.dbField_longitude + " IS NOT NULL)"
                if (statusSolvedMystery) {
                    //solved mysteries have either changed coord OR a filled final waypoint
                    sqlBuilder.addWhere(coordsChangedWhere)
                    sqlBuilder.addWhere(existsFilledFinalWpWhere)

                } else {
                    //unsolved mysteries have NEITHER a changed coord NOR a filled final waypoint
                    sqlBuilder.openWhere(SqlBuilder.WhereType.AND)
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_coordsChanged + " = 0")
                    sqlBuilder.addWhere("NOT " + existsFilledFinalWpWhere)
                    sqlBuilder.closeWhere()
                }
                sqlBuilder.closeWhere()
            }
            if (statusCorrectedCoordinates != null) {
                val coordsChangedWhere: String = sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_coordsChanged + " = " + (statusCorrectedCoordinates ? "1" : "0")
                if (statusCorrectedCoordinates) {
                    //coorected coordinates have changed coords
                    sqlBuilder.addWhere(coordsChangedWhere)
                } else {
                    //coorected coordinates don't have changed coords
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_coordsChanged + " = 0")
                }
            }
            if (statusHasUserDefinedWaypoints != null) {
                val waypointTableId: String = sqlBuilder.getNewTableId()
                sqlBuilder.addWhere((statusHasUserDefinedWaypoints ? "" : "NOT ") +
                        "EXISTS(SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableWaypoints + " " + waypointTableId + " WHERE " + waypointTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " AND (" + waypointTableId + "." + DataStore.dbFieldWaypoints_own + "=1 OR " + waypointTableId + "." + DataStore.dbFieldWaypoints_type + " = 'own'))")
            }
            if (excludeActive) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR)
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_disabled + " <> 0")
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_archived + " <> 0")
                sqlBuilder.closeWhere()
            }
            if (excludeDisabled) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_disabled + " = 0")
            }
            if (excludeArchived) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_archived + " = 0")
            }
            sqlBuilder.closeWhere()
        }
    }

    override     protected String getUserDisplayableConfig() {
        val sb: StringBuilder = StringBuilder()
        Int count = 0
        count = addIfStillFits(sb, count, statusFound, StatusType.FOUND)
        count = addIfStillFits(sb, count, statusDnf, StatusType.DNF)
        count = addIfStillFits(sb, count, statusOwned, StatusType.OWNED)
        count = addIfStillFits(sb, count, statusStored, StatusType.STORED)
        count = addIfStillFits(sb, count, statusFavorite, StatusType.FAVORITE)
        count = addIfStillFits(sb, count, statusWatchlist, StatusType.WATCHLIST)
        count = addIfStillFits(sb, count, statusPremium, StatusType.PREMIUM)
        count = addIfStillFits(sb, count, statusHasTrackable, StatusType.HAS_TRACKABLE)
        count = addIfStillFits(sb, count, statusHasOwnVote, StatusType.HAS_OWN_VOTE)
        count = addIfStillFits(sb, count, statusHasOfflineLog, StatusType.HAS_OFFLINE_LOG)
        count = addIfStillFits(sb, count, statusHasOfflineFoundLog, StatusType.HAS_OFFLINE_FOUND_LOG)
        count = addIfStillFits(sb, count, statusSolvedMystery, StatusType.SOLVED_MYSTERY)
        count = addIfStillFits(sb, count, statusCorrectedCoordinates, StatusType.CORRECTED_COORDINATES)
        count = addIfStillFits(sb, count, statusHasUserDefinedWaypoints, StatusType.HAS_USER_DEFINED_WAYPOINTS)
        count = addIfTrue(sb, count, excludeActive, R.string.cache_filter_status_exclude_active)
        count = addIfTrue(sb, count, excludeDisabled, R.string.cache_filter_status_exclude_disabled)
        count = addIfTrue(sb, count, excludeArchived, R.string.cache_filter_status_exclude_archived)
        if (count == 0) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        if (count > USERDISPLAY_MAXELEMENTS) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, count)
        }

        return sb.toString()
    }


    private Int addIfStillFits(final StringBuilder sb, final Int cnt, final Boolean status, final StatusType statusType) {
        if (status != null) {
            if (cnt < USERDISPLAY_MAXELEMENTS) {
                if (cnt > 0) {
                    sb.append(", ")
                }
                sb.append(LocalizationUtils.getString(statusType.labelId)).append("=")
                        .append(LocalizationUtils.getString(status ? R.string.cache_filter_status_select_yes : R.string.cache_filter_status_select_no))
            }
            return cnt + 1
        }
        return cnt
    }

    private Int addIfTrue(final StringBuilder sb, final Int cnt, final Boolean status, @StringRes final Int textId) {
        if (status) {
            if (cnt < USERDISPLAY_MAXELEMENTS) {
                if (cnt > 0) {
                    sb.append(", ")
                }
                sb.append(LocalizationUtils.getString(textId))
            }
            return cnt + 1
        }
        return cnt
    }

    private Boolean hasFoundOfflineLog(final Geocache cache) {
        if (cache.hasLogOffline()) {
            return cache.getOfflineLog().logType.isFoundLog()
        }
        return false
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setTextCollection(node, "values", getConfigInternal())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        setConfigInternal(JsonUtils.getTextList(node, "values"))
    }
}

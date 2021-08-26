package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

import java.util.Arrays;

public class StatusGeocacheFilter extends BaseGeocacheFilter {

    private static final int USERDISPLAY_MAXELEMENTS = 2;

    private static final String FLAG_EXCLUDE_ACTIVE = "exclude_active";
    private static final String FLAG_EXCLUDE_DISABLED = "exclude_disabled";
    private static final String FLAG_EXCLUDE_ARCHIVED = "exclude_archived";

    public enum StatusType {
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
        HAS_USER_DEFINED_WAYPOINTS(R.string.cache_filter_status_select_label_has_user_defined_waypoints, "has_user_defined_waypoints", ImageParam.id(R.drawable.marker_hasfinal));

        @StringRes public final int labelId;
        public final String yesFlag;
        public final String noFlag;
        public final ImageParam icon;
        @StringRes public final int infoTextId;

        StatusType(@StringRes final int labelId, final String flag, final ImageParam icon) {
            this(labelId, flag, icon, 0);
        }

        StatusType(@StringRes final int labelId, final String flag, final ImageParam icon, @StringRes final int infoTextId) {

            this.labelId = labelId;
            this.yesFlag = flag + "_yes";
            this.noFlag = flag + "_no";
            this.icon = icon;
            this.infoTextId = infoTextId;
        }

    }

    private boolean excludeActive = false;
    private boolean excludeDisabled = false;
    private boolean excludeArchived = false;


    private Boolean statusOwned = null;
    private Boolean statusFound = null;
    private Boolean statusDnf = null;
    private Boolean statusStored = null;
    private Boolean statusFavorite = null;
    private Boolean statusWatchlist = null;
    private Boolean statusPremium = null;
    private Boolean statusHasTrackable = null;
    private Boolean statusHasOwnVote = null;
    private Boolean statusHasOfflineLog = null;
    private Boolean statusHasOfflineFoundLog = null;
    private Boolean statusSolvedMystery = null;
    private Boolean statusHasUserDefinedWaypoints = null;

    @Override
    public Boolean filter(final Geocache cache) {

        if (statusHasOfflineLog != null || statusHasOfflineFoundLog != null) {
            //trigger offline log load
            cache.getOfflineLog();
        }

        //handle a few "inconclusive" cases
        if ((statusFavorite != null && cache.isFavoriteRaw() == null) ||
                (statusWatchlist != null && cache.isOnWatchlistRaw() == null) ||
                (statusPremium != null && cache.isPremiumMembersOnlyRaw() == null) ||
                (statusHasTrackable != null && !cache.hasInventoryItemsSet()) ||
                (statusHasUserDefinedWaypoints != null && cache.getFirstMatchingWaypoint(Waypoint::isUserDefined) == null) && cache.hasUserdefinedWaypoints() ||
                (statusSolvedMystery != null && cache.getType() == CacheType.MYSTERY && cache.getUserModifiedCoordsRaw() == null)) {
            return null;
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
                                (cache.hasUserModifiedCoords() || cache.hasFinalDefined()) == statusSolvedMystery);
    }

    public boolean isExcludeActive() {
        return excludeActive;
    }

    public void setExcludeActive(final boolean excludeActive) {
        this.excludeActive = excludeActive;
    }

    public boolean isExcludeDisabled() {
        return excludeDisabled;
    }

    public void setExcludeDisabled(final boolean excludeDisabled) {
        this.excludeDisabled = excludeDisabled;
    }

    public boolean isExcludeArchived() {
        return excludeArchived;
    }

    public void setExcludeArchived(final boolean excludeArchived) {
        this.excludeArchived = excludeArchived;
    }

    public Boolean getStatusOwned() {
        return statusOwned;
    }

    public void setStatusOwned(final Boolean statusOwned) {
        this.statusOwned = statusOwned;
    }

    public Boolean getStatusFound() {
        return statusFound;
    }

    public void setStatusFound(final Boolean statusFound) {
        this.statusFound = statusFound;
    }

    public Boolean getStatusDnf() {
        return statusDnf;
    }

    public void setStatusDnf(final Boolean statusDnf) {
        this.statusDnf = statusDnf;
    }

    public Boolean getStatusStored() {
        return statusStored;
    }

    public void setStatusStored(final Boolean statusStored) {
        this.statusStored = statusStored;
    }

    public Boolean getStatusFavorite() {
        return statusFavorite;
    }

    public void setStatusFavorite(final Boolean statusFavorite) {
        this.statusFavorite = statusFavorite;
    }

    public Boolean getStatusWatchlist() {
        return statusWatchlist;
    }

    public void setStatusWatchlist(final Boolean statusWatchlist) {
        this.statusWatchlist = statusWatchlist;
    }

    public Boolean getStatusPremium() {
        return statusPremium;
    }

    public void setStatusPremium(final Boolean statusPremium) {
        this.statusPremium = statusPremium;
    }

    public Boolean getStatusHasTrackable() {
        return statusHasTrackable;
    }

    public void setStatusHasTrackable(final Boolean statusHasTrackable) {
        this.statusHasTrackable = statusHasTrackable;
    }

    public Boolean getStatusHasOwnVote() {
        return statusHasOwnVote;
    }

    public void setStatusHasOwnVote(final Boolean statusHasOwnVote) {
        this.statusHasOwnVote = statusHasOwnVote;
    }

    public Boolean getStatusHasOfflineLog() {
        return statusHasOfflineLog;
    }

    public void setStatusHasOfflineLog(final Boolean statusHasOfflineLog) {
        this.statusHasOfflineLog = statusHasOfflineLog;
    }

    public Boolean getStatusHasOfflineFoundLog() {
        return statusHasOfflineFoundLog;
    }

    public void setStatusHasOfflineFoundLog(final Boolean statusHasOfflineFoundLog) {
        this.statusHasOfflineFoundLog = statusHasOfflineFoundLog;
    }

    public Boolean getStatusSolvedMystery() {
        return statusSolvedMystery;
    }

    public void setStatusSolvedMystery(final Boolean statusSolvedMystery) {
        this.statusSolvedMystery = statusSolvedMystery;
    }

    public Boolean getStatusHasUserDefinedWaypoint() {
        return statusHasUserDefinedWaypoints;
    }

    public void setStatusHasUserDefinedWaypoint(final Boolean statusHasWaypoint) {
        this.statusHasUserDefinedWaypoints = statusHasWaypoint;
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        statusOwned = null;
        statusFound = null;
        statusDnf = null;
        statusStored = null;
        statusFavorite = null;
        statusWatchlist = null;
        statusHasTrackable = null;
        statusHasOwnVote = null;
        statusHasOfflineLog = null;
        statusHasOfflineFoundLog = null;
        statusHasUserDefinedWaypoints = null;
        statusPremium = null;

        excludeActive = false;
        excludeDisabled = false;
        excludeArchived = false;
        for (String value : config.getDefaultList()) {
            checkAndSetBooleanFlag(value, StatusType.OWNED, b -> statusOwned = b);
            checkAndSetBooleanFlag(value, StatusType.FOUND, b -> statusFound = b);
            checkAndSetBooleanFlag(value, StatusType.DNF, b -> statusDnf = b);
            checkAndSetBooleanFlag(value, StatusType.STORED, b -> statusStored = b);
            checkAndSetBooleanFlag(value, StatusType.FAVORITE, b -> statusFavorite = b);
            checkAndSetBooleanFlag(value, StatusType.WATCHLIST, b -> statusWatchlist = b);
            checkAndSetBooleanFlag(value, StatusType.PREMIUM, b -> statusPremium = b);
            checkAndSetBooleanFlag(value, StatusType.HAS_TRACKABLE, b -> statusHasTrackable = b);
            checkAndSetBooleanFlag(value, StatusType.HAS_OWN_VOTE, b -> statusHasOwnVote = b);
            checkAndSetBooleanFlag(value, StatusType.HAS_OFFLINE_LOG, b -> statusHasOfflineLog = b);
            checkAndSetBooleanFlag(value, StatusType.HAS_OFFLINE_FOUND_LOG, b -> statusHasOfflineFoundLog = b);
            checkAndSetBooleanFlag(value, StatusType.SOLVED_MYSTERY, b -> statusSolvedMystery = b);
            checkAndSetBooleanFlag(value, StatusType.HAS_USER_DEFINED_WAYPOINTS, b -> statusHasUserDefinedWaypoints = b);

            if (checkBooleanFlag(FLAG_EXCLUDE_ACTIVE, value)) {
                excludeActive = true;
            } else if (checkBooleanFlag(FLAG_EXCLUDE_DISABLED, value)) {
                excludeDisabled = true;
            } else if (checkBooleanFlag(FLAG_EXCLUDE_ARCHIVED, value)) {
                excludeArchived = true;
            }
        }
    }

    private static void checkAndSetBooleanFlag(final String flagValue, final StatusType status, final Consumer<Boolean> callOnFind) {
        if (checkBooleanFlag(status.yesFlag, flagValue)) {
            callOnFind.accept(true);
        }
        if (checkBooleanFlag(status.noFlag, flagValue)) {
            callOnFind.accept(false);
        }
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig result = new ExpressionConfig();
        checkAndAddFlagToDefaultList(statusOwned, StatusType.OWNED, result);
        checkAndAddFlagToDefaultList(statusFound, StatusType.FOUND, result);
        checkAndAddFlagToDefaultList(statusDnf, StatusType.DNF, result);
        checkAndAddFlagToDefaultList(statusStored, StatusType.STORED, result);
        checkAndAddFlagToDefaultList(statusFavorite, StatusType.FAVORITE, result);
        checkAndAddFlagToDefaultList(statusWatchlist, StatusType.WATCHLIST, result);
        checkAndAddFlagToDefaultList(statusPremium, StatusType.PREMIUM, result);
        checkAndAddFlagToDefaultList(statusHasTrackable, StatusType.HAS_TRACKABLE, result);
        checkAndAddFlagToDefaultList(statusHasOwnVote, StatusType.HAS_OWN_VOTE, result);
        checkAndAddFlagToDefaultList(statusHasOfflineLog, StatusType.HAS_OFFLINE_LOG, result);
        checkAndAddFlagToDefaultList(statusHasOfflineFoundLog, StatusType.HAS_OFFLINE_FOUND_LOG, result);
        checkAndAddFlagToDefaultList(statusHasUserDefinedWaypoints, StatusType.HAS_USER_DEFINED_WAYPOINTS, result);
        checkAndAddFlagToDefaultList(statusSolvedMystery, StatusType.SOLVED_MYSTERY, result);
        if (excludeActive) {
            result.addToDefaultList(FLAG_EXCLUDE_ACTIVE);
        }
        if (excludeDisabled) {
            result.addToDefaultList(FLAG_EXCLUDE_DISABLED);
        }
        if (excludeArchived) {
            result.addToDefaultList(FLAG_EXCLUDE_ARCHIVED);
        }
        return result;

    }

    private static void checkAndAddFlagToDefaultList(final Boolean statusValue, final StatusType status, final ExpressionConfig config) {
        if (statusValue != null) {
            config.addToDefaultList(statusValue ? status.yesFlag : status.noFlag);
        }

    }


    @Override
    public boolean isFiltering() {
        return statusOwned != null || statusFound != null || statusDnf != null || statusStored != null || statusFavorite != null ||
                statusWatchlist != null || statusPremium != null || statusHasTrackable != null ||
                statusHasOwnVote != null || statusHasOfflineLog != null || statusHasOfflineFoundLog != null ||
                statusSolvedMystery != null || statusHasUserDefinedWaypoints != null || excludeArchived || excludeDisabled || excludeActive;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!isFiltering()) {
            sqlBuilder.addWhereTrue();
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (statusOwned != null) {
                sqlBuilder.addWhere("LOWER(" + sqlBuilder.getMainTableId() + ".owner_real) " + (statusOwned ? "=" : "<>") + " LOWER(?)", Settings.getUserName());
            }
            if (statusFound != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".found " + (statusFound ? "= 1" : "<> 1"));
            }
            if (statusDnf != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".found " + (statusDnf ? "= -1" : "<> -1"));
            }
            if (statusStored != null && !statusStored) {
                //this seems stupid, but we have to simply set a condition which is never true
                sqlBuilder.addWhere("1=0");
            }
            if (statusFavorite != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".favourite = " + (statusFavorite ? "1" : "0"));
            }
            if (statusWatchlist != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".onWatchlist = " + (statusWatchlist ? "1" : "0"));
            }
            if (statusPremium != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".members = " + (statusPremium ? "1" : "0"));
            }
            if (statusHasTrackable != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".inventoryunknown " + (statusHasTrackable ? "> 0" : " = 0"));
            }
            if (statusHasOwnVote != null) {
                if (statusHasOwnVote) {
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".myvote > 0");
                } else {
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".myvote IS NULL OR  " + sqlBuilder.getMainTableId() + ".myvote = 0");
                }
            }
            if (statusHasOfflineLog != null) {
                final String logTableId = sqlBuilder.getNewTableId();
                sqlBuilder.addWhere((statusHasOfflineLog ? "" : "NOT ") +
                        "EXISTS(SELECT geocode FROM cg_logs_offline " + logTableId + " WHERE " + logTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode)");
            }
            if (statusHasOfflineFoundLog != null) {
                final String logTableId = sqlBuilder.getNewTableId();
                final String logIds = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",");
                sqlBuilder.addWhere((statusHasOfflineFoundLog ? "" : "NOT ") +
                        "EXISTS(SELECT geocode FROM cg_logs_offline " + logTableId + " WHERE " + logTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode" +
                        " AND " + logTableId + ".type in (" + logIds + ")" + ")");
            }
            if (statusSolvedMystery != null) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".type <> '" + CacheType.MYSTERY.id + "'"); // only filters mysteries
                final String wptId = sqlBuilder.getNewTableId();
                final String coordsChangedWhere = sqlBuilder.getMainTableId() + ".coordsChanged = " + (statusSolvedMystery ? "1" : "0");
                final String existsFilledFinalWpWhere = "EXISTS (select " + wptId + ".geocode from cg_waypoints " + wptId + " WHERE " +
                        wptId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode AND " + wptId + ".type = '" + WaypointType.FINAL.id + "' AND " +
                        wptId + ".latitude IS NOT NULL AND " + wptId + ".longitude IS NOT NULL)";
                if (statusSolvedMystery) {
                    //solved mysteries have either changed coord OR a filled final waypoint
                    sqlBuilder.addWhere(coordsChangedWhere);
                    sqlBuilder.addWhere(existsFilledFinalWpWhere);

                } else {
                    //unsolved mysteries have NEITHER a changed coord NOR a filled final waypoint
                    sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".coordsChanged = 0");
                    sqlBuilder.addWhere("NOT " + existsFilledFinalWpWhere);
                    sqlBuilder.closeWhere();
                }
                sqlBuilder.closeWhere();
            }
            if (statusHasUserDefinedWaypoints != null) {
                final String waypointTableId = sqlBuilder.getNewTableId();
                sqlBuilder.addWhere((statusHasUserDefinedWaypoints ? "" : "NOT ") +
                        "EXISTS(SELECT geocode FROM cg_waypoints " + waypointTableId + " WHERE " + waypointTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode AND (" + waypointTableId + ".own=1 OR " + waypointTableId + ".type = 'own'))");
            }
            if (excludeActive) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled <> 0");
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived <> 0");
                sqlBuilder.closeWhere();
            }
            if (excludeDisabled) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled = 0");
            }
            if (excludeArchived) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived = 0");
            }
            sqlBuilder.closeWhere();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        count = addIfStillFits(sb, count, statusFound, StatusType.FOUND);
        count = addIfStillFits(sb, count, statusDnf, StatusType.DNF);
        count = addIfStillFits(sb, count, statusOwned, StatusType.OWNED);
        count = addIfStillFits(sb, count, statusStored, StatusType.STORED);
        count = addIfStillFits(sb, count, statusFavorite, StatusType.FAVORITE);
        count = addIfStillFits(sb, count, statusWatchlist, StatusType.WATCHLIST);
        count = addIfStillFits(sb, count, statusPremium, StatusType.PREMIUM);
        count = addIfStillFits(sb, count, statusHasTrackable, StatusType.HAS_TRACKABLE);
        count = addIfStillFits(sb, count, statusHasOwnVote, StatusType.HAS_OWN_VOTE);
        count = addIfStillFits(sb, count, statusHasOfflineLog, StatusType.HAS_OFFLINE_LOG);
        count = addIfStillFits(sb, count, statusHasOfflineFoundLog, StatusType.HAS_OFFLINE_FOUND_LOG);
        count = addIfStillFits(sb, count, statusSolvedMystery, StatusType.SOLVED_MYSTERY);
        count = addIfStillFits(sb, count, statusHasUserDefinedWaypoints, StatusType.HAS_USER_DEFINED_WAYPOINTS);
        count = addIfTrue(sb, count, excludeActive, R.string.cache_filter_status_exclude_active);
        count = addIfTrue(sb, count, excludeDisabled, R.string.cache_filter_status_exclude_disabled);
        count = addIfTrue(sb, count, excludeArchived, R.string.cache_filter_status_exclude_archived);
        if (count == 0) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (count > USERDISPLAY_MAXELEMENTS) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, count);
        }

        return sb.toString();
    }


    private int addIfStillFits(final StringBuilder sb, final int cnt, final Boolean status, final StatusType statusType) {
        if (status != null) {
            if (cnt < USERDISPLAY_MAXELEMENTS) {
                if (cnt > 0) {
                    sb.append(", ");
                }
                sb.append(LocalizationUtils.getString(statusType.labelId)).append("=")
                        .append(LocalizationUtils.getString(status ? R.string.cache_filter_status_select_yes : R.string.cache_filter_status_select_no));
            }
            return cnt + 1;
        }
        return cnt;
    }

    private int addIfTrue(final StringBuilder sb, final int cnt, final boolean status, @StringRes final int textId) {
        if (status) {
            if (cnt < USERDISPLAY_MAXELEMENTS) {
                if (cnt > 0) {
                    sb.append(", ");
                }
                sb.append(LocalizationUtils.getString(textId));
            }
            return cnt + 1;
        }
        return cnt;
    }

    private boolean hasFoundOfflineLog(final Geocache cache) {
        if (cache.hasLogOffline()) {
            return cache.getOfflineLog().logType.isFoundLog();
        }
        return false;
    }
}

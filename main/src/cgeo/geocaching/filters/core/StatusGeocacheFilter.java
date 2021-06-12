package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

public class StatusGeocacheFilter extends BaseGeocacheFilter {

    private static final int USERDISPLAY_MAXELEMENTS = 2;

    private static final String FLAG_EXCLUDE_ACTIVE = "exclude_active";
    private static final String FLAG_EXCLUDE_DISABLED = "exclude_disabled";
    private static final String FLAG_EXCLUDE_ARCHIVED = "exclude_archived";

    public enum StatusType {
        OWNED(R.string.cache_filter_status_select_label_owned, "owned", ImageParam.id(R.drawable.ic_menu_myplaces)),
        FOUND(R.string.cache_filter_status_select_label_found, "found", ImageParam.id(R.drawable.ic_menu_found)),
        STORED(R.string.cache_filter_status_select_label_stored, "stored", ImageParam.id(R.drawable.ic_menu_save)),
        FAVORITE(R.string.cache_filter_status_select_label_favorite, "favorite", ImageParam.emoji(EmojiUtils.SMILEY_LOVE)),
        WATCHLIST(R.string.cache_filter_status_select_label_watchlist, "watchlist", ImageParam.emoji(EmojiUtils.SMILEY_MONOCLE)),
        PREMIUM(R.string.cache_filter_status_select_label_premium, "premium", ImageParam.emoji(EmojiUtils.SPARKLES)),
        SOLVED_MYSTERY(R.string.cache_filter_status_select_label_solved_mystery, "solved_mystery", ImageParam.id(R.drawable.waypoint_puzzle), R.string.cache_filter_status_select_infotext_solved_mystery);

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
    private Boolean statusStored = null;
    private Boolean statusFavorite = null;
    private Boolean statusWatchlist = null;
    private Boolean statusPremium = null;
    private Boolean statusSolvedMystery = null;


    @Override
    public Boolean filter(final Geocache cache) {
        //handle a few "inconclusive" cases
        if ((statusFavorite != null && cache.isFavoriteRaw() == null) ||
            (statusWatchlist != null && cache.isOnWatchlistRaw() == null) ||
            (statusPremium != null && cache.isPremiumMembersOnlyRaw() == null) ||
            (statusSolvedMystery != null && cache.getUserModifiedCoordsRaw() == null)) {
            return null;
        }

        return
            ((!excludeActive && !cache.isDisabled() && !cache.isArchived()) ||
                (!excludeDisabled && cache.isDisabled()) ||
                (!excludeArchived && cache.isArchived())) &&
            (statusOwned == null || (cache.isOwner() == statusOwned)) &&
            (statusFound == null || cache.isFound() == statusFound) &&
            (statusStored == null || cache.isOffline() == statusStored) &&
            (statusFavorite == null || cache.isFavorite() == statusFavorite) &&
            (statusWatchlist == null || cache.isOnWatchlist() == statusWatchlist) &&
            (statusPremium == null || cache.isPremiumMembersOnly() == statusPremium) &&
            (statusSolvedMystery == null || cache.getType() != CacheType.MYSTERY ||
                (cache.hasUserModifiedCoords() ||
                    cache.getFirstMatchingWaypoint(wp -> wp.getWaypointType() == WaypointType.FINAL && wp.getCoords() != null) != null)
                    == statusSolvedMystery);
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

    public Boolean getStatusSolvedMystery() {
        return statusSolvedMystery;
    }

    public void setStatusSolvedMystery(final Boolean statusSolvedMystery) {
        this.statusSolvedMystery = statusSolvedMystery;
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        statusOwned = null;
        statusFound = null;
        statusStored = null;
        statusFavorite = null;
        statusWatchlist = null;
        statusPremium = null;

        excludeActive = false;
        excludeDisabled = false;
        excludeArchived = false;
        for (String value : config.getDefaultList()) {
            checkAndSetBooleanFlag(value, StatusType.OWNED, b -> statusOwned = b);
            checkAndSetBooleanFlag(value, StatusType.FOUND, b -> statusFound = b);
            checkAndSetBooleanFlag(value, StatusType.STORED, b -> statusStored = b);
            checkAndSetBooleanFlag(value, StatusType.FAVORITE, b -> statusFavorite = b);
            checkAndSetBooleanFlag(value, StatusType.WATCHLIST, b -> statusWatchlist = b);
            checkAndSetBooleanFlag(value, StatusType.PREMIUM, b -> statusPremium = b);
            checkAndSetBooleanFlag(value, StatusType.SOLVED_MYSTERY, b -> statusSolvedMystery = b);

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
        checkAndAddFlagToDefaultList(statusStored, StatusType.STORED, result);
        checkAndAddFlagToDefaultList(statusFavorite, StatusType.FAVORITE, result);
        checkAndAddFlagToDefaultList(statusWatchlist, StatusType.WATCHLIST, result);
        checkAndAddFlagToDefaultList(statusPremium, StatusType.PREMIUM, result);
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
        return statusOwned != null || statusFound != null || statusStored != null || statusFavorite != null ||
            statusWatchlist != null || statusPremium != null || statusSolvedMystery != null ||
            excludeArchived || excludeDisabled || excludeActive;
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
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".found = " + (statusFound ? "1" : "0"));
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
            if (excludeActive) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".disabled = 1");
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".archived = 1");
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
        count = addIfStillFits(sb, count, statusOwned, StatusType.OWNED);
        count = addIfStillFits(sb, count, statusStored, StatusType.STORED);
        count = addIfStillFits(sb, count, statusFavorite, StatusType.FAVORITE);
        count = addIfStillFits(sb, count, statusWatchlist, StatusType.WATCHLIST);
        count = addIfStillFits(sb, count, statusPremium, StatusType.PREMIUM);
        count = addIfStillFits(sb, count, statusSolvedMystery, StatusType.SOLVED_MYSTERY);
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
                    sb.append(",");
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
                    sb.append(",");
                }
                sb.append(LocalizationUtils.getString(textId));
            }
            return cnt + 1;
        }
        return cnt;
    }
}

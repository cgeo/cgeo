package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;

import org.junit.Test;

public class StatusGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void activeDisabledArchived() {
        singleActiveDisabledArchived(false, false, false, false, false, true);
        singleActiveDisabledArchived(true, true, false, false, false, true);

        //exclude active
        singleActiveDisabledArchived(false, false, true, false, false, false);
        singleActiveDisabledArchived(true, false, true, false, false, true);
        singleActiveDisabledArchived(false, true, true, false, false, true);
        singleActiveDisabledArchived(true, true, true, false, false, true);

        //exclude disabled
        singleActiveDisabledArchived(false, false, false, true, false, true);
        singleActiveDisabledArchived(true, false, false, true, false, false);
        singleActiveDisabledArchived(false, true, false, true, false, true);
        singleActiveDisabledArchived(true, true, false, true, false, true); // archived does win over disabled

        //exclude archived
        singleActiveDisabledArchived(false, false, false, false, true, true);
        singleActiveDisabledArchived(true, false, false, false, true, true);
        singleActiveDisabledArchived(false, true, false, false, true, false);
        singleActiveDisabledArchived(true, true, false, false, true, false); // archived does win over disabled

    }

    private void singleActiveDisabledArchived(final boolean cacheDisabled, final boolean cacheArchived, final boolean excludeActive, final boolean excludeDisabled, final boolean excludeArchived, final Boolean expectedResult) {

        singleStatus(cache -> {
            cache.setDisabled(cacheDisabled);
            cache.setArchived(cacheArchived);
        }, filter -> {
            filter.setExcludeActive(excludeActive);
            filter.setExcludeDisabled(excludeDisabled);
            filter.setExcludeArchived(excludeArchived);
        }, expectedResult);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void found() {
        singleStandardStatus(Geocache::setFound, StatusGeocacheFilter::setStatusFound);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void owned() {
        singleStandardStatus((c, b) -> {
            c.setOwnerUserId(b ? Settings.getUserName() : "");
            c.setGeocode("GCFAKE");
        }, StatusGeocacheFilter::setStatusOwned);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void favorite() {
        singleStandardStatus(Geocache::setFavorite, StatusGeocacheFilter::setStatusFavorite);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void watchlist() {
        singleStandardStatus(Geocache::setOnWatchlist, StatusGeocacheFilter::setStatusWatchlist);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void premium() {
        singleStandardStatus(Geocache::setPremiumMembersOnly, StatusGeocacheFilter::setStatusPremium);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void inventory() {
        singleStandardStatus((c, b) -> c.setInventoryItems(b ? 1 : 0), StatusGeocacheFilter::setStatusHasTrackable);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void ownvote() {
        singleStandardStatus((c, b) -> c.setMyVote(b ? 1 : 0), StatusGeocacheFilter::setStatusHasOwnVote);
    }

    private void singleStandardStatus(final Action2<Geocache, Boolean> cacheSetter, final Action2<StatusGeocacheFilter, Boolean> filterSetter) {
        singleStatus(c -> cacheSetter.call(c, false), f -> filterSetter.call(f, null), true);
        singleStatus(c -> cacheSetter.call(c, true), f -> filterSetter.call(f, null), true);

        singleStatus(c -> cacheSetter.call(c, false), f -> filterSetter.call(f, true), false);
        singleStatus(c -> cacheSetter.call(c, true), f -> filterSetter.call(f, true), true);

        singleStatus(c -> cacheSetter.call(c, false), f -> filterSetter.call(f, false), true);
        singleStatus(c -> cacheSetter.call(c, true), f -> filterSetter.call(f, false), false);
    }

    private void singleStatus(final Action1<Geocache> cacheSetter, final Action1<StatusGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.STATUS, cacheSetter, filterSetter, expectedResult);
    }

}

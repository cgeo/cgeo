package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CalendarUtils;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * compares caches by hidden date
 */
class DateComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        final Date date1 = cache1.getHiddenDate();
        final Date date2 = cache2.getHiddenDate();
        if (date1 != null && date2 != null) {
            final int dateDifference = date1.compareTo(date2);
            if (dateDifference == 0) {
                return sortSameDate(cache1, cache2);
            }
            return dateDifference;
        }
        if (date1 != null) {
            return -1;
        }
        if (date2 != null) {
            return 1;
        }
        return 0;
    }

    protected int sortSameDate(final Geocache cache1, final Geocache cache2) {
        final Geopoint gps = Sensors.getInstance().currentGeo().getCoords();
        final Float d1 = gps.distanceTo(cache1.getCoords());
        final Float d2 = gps.distanceTo(cache2.getCoords());
        return d1.compareTo(d2);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return CalendarUtils.yearMonth(cache.getHiddenDate());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".hidden", sortDesc);
    }

}

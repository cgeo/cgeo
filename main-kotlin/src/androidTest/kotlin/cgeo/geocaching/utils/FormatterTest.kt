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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.models.Waypoint

import android.annotation.SuppressLint
import android.text.format.DateUtils

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.List
import java.util.Locale

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat


class FormatterTest  {

    /**
     * The pattern we get from {@link Formatter#getShortDateFormat()} should be the one used by {@link Formatter#formatShortDate(Long)}.
     */
    @Test
    public Unit testShortDateFormat() {
        val currentTimeMillis: Long = System.currentTimeMillis()
        val formattedDate: String = Formatter.formatShortDate(currentTimeMillis)
        val pattern: String = Formatter.getShortDateFormat()
        assertThat(SimpleDateFormat(pattern, Locale.getDefault()).format(currentTimeMillis)).isEqualTo(formattedDate)
    }

    @Test
    public Unit testParkingWaypoint() {
        assertFormatting(Waypoint("you can park here", WaypointType.PARKING, false), WaypointType.PARKING.getL10n())
    }

    @Test
    public Unit testOriginalWaypoint() {
        assertFormatting(Waypoint("an original", WaypointType.ORIGINAL, false), WaypointType.ORIGINAL.getL10n())
    }

    @Test
    public Unit testOwnWaypoint() {
        val own: Waypoint = Waypoint("my own", WaypointType.OWN, true)
        own.setPrefix(Waypoint.PREFIX_OWN)
        assertFormatting(own, CgeoApplication.getInstance().getString(R.string.waypoint_custom))
    }

    private static Unit assertFormatting(final Waypoint waypoint, final String expected) {
        assertThat(Formatter.formatWaypointInfo(waypoint)).isEqualTo(expected)
    }

    @SuppressLint("SdCardPath")
    @Test
    public Unit testTruncateCommonSubdir() {
        val dirs: List<CharSequence> = ArrayList<>()
        dirs.add("/sdcard/Android/data/cgeo.geocaching/files")
        dirs.add("/storage/emulated/0/Android/data/cgeo.geocaching/files")
        dirs.add("/storage/extSdCard/Android/data/cgeo.geocaching/files")
        dirs.add("/mnt/sdcard/Android/data/cgeo.geocaching/files")
        dirs.add("/storage/sdcard1/Android/data/cgeo.geocaching/files")

        val truncated: List<CharSequence> = Formatter.truncateCommonSubdir(dirs)
        assertThat(truncated.get(0)).isEqualTo("/sdcard/\u2026")
        assertThat(truncated.get(1)).isEqualTo("/storage/emulated/0/\u2026")
        assertThat(truncated.get(2)).isEqualTo("/storage/extSdCard/\u2026")
        assertThat(truncated.get(3)).isEqualTo("/mnt/sdcard/\u2026")
        assertThat(truncated.get(4)).isEqualTo("/storage/sdcard1/\u2026")
    }

    @SuppressLint("SdCardPath")
    @Test
    public Unit testTruncateDuplicate() {
        val dirs: List<CharSequence> = ArrayList<>()
        dirs.add("/data/data/cgeo.geocaching/files")
        dirs.add("/data/data/cgeo.geocaching/files")

        val truncated: List<CharSequence> = Formatter.truncateCommonSubdir(dirs)
        assertThat(truncated.get(0)).isEqualTo("/\u2026")
    }

    @Test
    public Unit testTruncateNothingInCommon() {
        val dirs: List<CharSequence> = ArrayList<>()
        dirs.add("/one/directory/files")
        dirs.add("/some/other/directory")

        val truncated: List<CharSequence> = Formatter.truncateCommonSubdir(dirs)
        assertThat(truncated.get(0)).isEqualTo("/one/directory/files")
        assertThat(truncated.get(1)).isEqualTo("/some/other/directory")
    }

    @SuppressLint("SdCardPath")
    @Test
    public Unit testTruncateCommonSubdirSingleEntry() {
        val dirs: List<CharSequence> = ArrayList<>()
        dirs.add("/data/data/cgeo.geocaching/files")

        val truncated: List<CharSequence> = Formatter.truncateCommonSubdir(dirs)
        assertThat(truncated.get(0)).isEqualTo("/data/data/cgeo.geocaching/files")
    }

    @Test
    public Unit testFormatStoredAgo() {
        // skip test on non english device
        if (!StringUtils == (Locale.getDefault().getLanguage(), Locale.ENGLISH.getLanguage())) {
            return
        }
        assertThat(Formatter.formatStoredAgo(0)).isEqualTo("Stored ")
        assertFormatStoredAgo(DateUtils.MINUTE_IN_MILLIS * 10, "Stored a few minutes ago")
        assertFormatStoredAgo(DateUtils.MINUTE_IN_MILLIS * 20, "Stored about 20 minutes ago")
        assertFormatStoredAgo(DateUtils.MINUTE_IN_MILLIS * 65, "Stored about 1 hour ago")
        assertFormatStoredAgo(DateUtils.HOUR_IN_MILLIS * 45, "Stored about 45 hours ago")
        assertFormatStoredAgo(DateUtils.HOUR_IN_MILLIS * 50, "Stored about 2 days ago")
        assertFormatStoredAgo(DateUtils.DAY_IN_MILLIS * 30, "Stored about 30 days ago")
        assertFormatStoredAgo(DateUtils.DAY_IN_MILLIS * 31, "Stored about 1 month ago")
        assertFormatStoredAgo(DateUtils.DAY_IN_MILLIS * 66, "Stored about 2 months ago")
        assertFormatStoredAgo(DateUtils.DAY_IN_MILLIS * 366, "Stored over a year ago")
    }

    private static Unit assertFormatStoredAgo(final Long agoInMillis, final String expected) {
        assertThat(Formatter.formatStoredAgo(System.currentTimeMillis() - agoInMillis)).isEqualTo(expected)
    }

}

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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.bettercacher.BetterCacherConnector
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.location.Units
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.ICoordinate
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.bettercacher.Category
import cgeo.geocaching.models.bettercacher.Tier
import cgeo.geocaching.network.SmileyImage
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.dialog.ContextMenuDialog
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.html.UnknownTagsHandler
import cgeo.geocaching.utils.Formatter.SEPARATOR
import cgeo.geocaching.utils.MapMarkerUtils.createDTRatingMarker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.view.View.GONE

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.text.HtmlCompat

import java.util.ArrayList
import java.util.List
import java.util.Locale

import org.apache.commons.lang3.StringUtils

class CacheDetailsCreator {
    private final Activity activity
    private final ViewGroup parentView
    private final Resources res

    /**
     * Immutable pair holder for name value line views.
     */
    public static class NameValueLine {
        public final View layout
        public final TextView valueView

        NameValueLine(final View layout, final TextView value) {
            this.layout = layout
            this.valueView = value
        }
    }

    public CacheDetailsCreator(final Activity activity, final ViewGroup parentView) {
        this.activity = activity
        this.res = activity.getResources()
        this.parentView = parentView
        parentView.removeAllViews()
    }

    /**
     * Create a "name: value" line.
     *
     * @param nameId the resource of the name field
     * @param value  the initial value
     * @return a pair made of the whole "name: value" line (to be able to hide it for example) and of the value (to update it)
     */
    public NameValueLine add(final Int nameId, final CharSequence value) {
        val nameValue: NameValueLine = createNameValueLine(nameId)
        nameValue.valueView.setText(value)
        return nameValue
    }

    /**
     * Create a "name: value" line with html content.
     *
     * @param nameId  the resource of the name field
     * @param value   the initial value
     * @param geocode the geocode for image getter
     * @return a pair made of the whole "name: value" line (to be able to hide it for example) and of the value (to update it)
     */
    public NameValueLine addHtml(final Int nameId, final CharSequence value, final String geocode) {
        val nameValue: NameValueLine = createNameValueLine(nameId)
        val valueView: TextView = nameValue.valueView
        valueView.setText(HtmlCompat.fromHtml(value.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY, SmileyImage(geocode, valueView), UnknownTagsHandler()), TextView.BufferType.SPANNABLE)
        return nameValue
    }

    public Unit addWideHtml(final Int nameId, final CharSequence value, final String geocode) {
        val nameValue: NameValueLine = createNameValueLine(nameId)
        nameValue.layout.findViewById(R.id.name).setVisibility(GONE)
        val valueView: TextView = nameValue.valueView
        val label: String = nameId > 0 ? res.getString(nameId) + ": " : ""
        valueView.setText(HtmlCompat.fromHtml(label + value.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY, SmileyImage(geocode, valueView), UnknownTagsHandler()), TextView.BufferType.SPANNABLE)
    }

    private NameValueLine createNameValueLine(final Int nameId) {
        val layout: View = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false)
        val nameView: TextView = layout.findViewById(R.id.name)
        if (nameId > 0) {
            nameView.setText(res.getString(nameId))
        }
        val valueView: TextView = layout.findViewById(R.id.value)
        parentView.addView(layout)
        return NameValueLine(layout, valueView)
    }

    private LinearLayout createNameLinearLayoutLine(final Int nameId) {
        val layout: View = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false)
        parentView.addView(layout)
        val nameView: TextView = layout.findViewById(R.id.name)
        nameView.setText(res.getString(nameId))
        layout.findViewById(R.id.value).setVisibility(GONE)
        layout.findViewById(R.id.addition).setVisibility(GONE)
        return layout.findViewById(R.id.linearlayout)
    }

    public View addStars(final Int nameId, final Float value) {
        return addStars(nameId, value, 5)
    }

    private View addStars(final Int nameId, final Float value, final Int max) {
        val layout: View = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false)
        val nameView: TextView = layout.findViewById(R.id.name)
        val valueView: TextView = layout.findViewById(R.id.value)

        nameView.setText(activity.getString(nameId))
        valueView.setText(String.format(Locale.getDefault(), activity.getString(R.string.cache_rating_of_new), value, max))

        val layoutStars: RatingBar = layout.findViewById(R.id.stars)
        layoutStars.setNumStars(max)
        layoutStars.setRating(value)
        layoutStars.setVisibility(View.VISIBLE)

        parentView.addView(layout)
        return layout
    }

    public Unit addCacheState(final Geocache cache) {
        val states: List<String> = ArrayList<>(5)
        String date = getVisitedDate(cache)
        if (cache.hasLogOffline()) {
            states.add(res.getString(R.string.cache_status_offline_log) + date)
            // reset the found date, to avoid showing it twice
            date = ""
        }
        if (cache.isFound()) {
            states.add(res.getString(cache.isEventCache() ? R.string.cache_status_attended : R.string.cache_status_found) + date)
        } else if (cache.isDNF()) {
            states.add(res.getString(R.string.cache_not_status_found) + date)
        }
        if (cache.isEventCache() && states.isEmpty()) {
            for (final LogEntry log : cache.getLogs()) {
                if (log.logType == LogType.WILL_ATTEND && log.isOwn()) {
                    states.add(LogType.WILL_ATTEND.getL10n())
                }
            }
        }
        if (cache.isArchived()) {
            states.add(res.getString(R.string.cache_status_archived))
        }
        if (cache.isDisabled()) {
            states.add(res.getString(R.string.cache_status_disabled))
        }
        if (cache.isPremiumMembersOnly()) {
            states.add(res.getString(R.string.cache_status_premium))
        }
        if (!states.isEmpty()) {
            add(R.string.cache_status, StringUtils.join(states, ", "))
        }
    }

    private static String getVisitedDate(final Geocache cache) {
        val visited: Long = cache.getVisitedDate()
        return visited != 0 ? " (" + Formatter.formatShortDate(visited) + ")" : ""
    }

    private static Float distanceNonBlocking(final ICoordinate target) {
        if (target.getCoords() == null) {
            return null
        }
        return LocationDataProvider.getInstance().currentGeo().getCoords().distanceTo(target)
    }

    @SuppressLint("SetTextI18n")
    public Unit addRating(final Geocache cache) {
        if (cache.getRating() > 0) {
            val itemLayout: View = addStars(R.string.cache_rating, cache.getRating())
            if (cache.getVotes() > 0) {
                val itemAddition: TextView = itemLayout.findViewById(R.id.addition)
                itemAddition.setText(" (" + cache.getVotes() + ')')
                itemAddition.setVisibility(View.VISIBLE)
            }
        }
    }

    public Unit addSize(final Geocache cache) {
        if (cache.showSize()) {
            add(R.string.cache_size, cache.getSize().getL10n())
        }
    }

    public Unit addAlcMode(final Geocache cache) {
        Log.d("_AL add mode to view: " + cache.isLinearAlc())
        if (cache.isLinearAlc()) {
            add(R.string.cache_mode, res.getString(R.string.cache_mode_linear))
        } else {
            add(R.string.cache_mode, res.getString(R.string.cache_mode_random))
        }
    }

    public Unit addDifficulty(final Geocache cache) {
        if (cache.getDifficulty() > 0) {
            addStars(R.string.cache_difficulty, cache.getDifficulty())
        }
    }

    public Unit addTerrain(final Geocache cache) {
        if (cache.getTerrain() > 0) {
            addStars(R.string.cache_terrain, cache.getTerrain(), ConnectorFactory.getConnector(cache).getMaxTerrain())
        }
    }

    public Unit addDifficultyTerrain(final Geocache cache) {
        val context: Context = parentView.getContext()

        val layout: View = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false)
        layout.findViewById(R.id.addition).setVisibility(GONE)
        parentView.addView(layout)

        val nameView: TextView = layout.findViewById(R.id.name)
        nameView.setText(R.string.cache_difficulty_terrain)

        val valueView: TextView = layout.findViewById(R.id.value)
        val sb: StringBuilder = StringBuilder()
        if (cache.getDifficulty() > 0) {
            sb.append("D ").append(cache.getDifficulty())
        }
        if (cache.getTerrain() > 0) {
            sb.append(sb.length() > 0 ? SEPARATOR : "").append("T ").append(cache.getTerrain())
        }
        valueView.setText(sb)

        val linearLayout: LinearLayout = layout.findViewById(R.id.linearlayout)

        val catIcon: ImageView = ImageView(context)
        catIcon.setBackground(createDTRatingMarker(res, cache.supportsDifficultyTerrain(), cache.getDifficulty(), cache.getTerrain(), 1.47f)); // 1.47 = scaling factor required to make D/T marker the same size as log smileys
        linearLayout.addView(catIcon)
    }

    public Unit addBetterCacher(final Geocache cache) {
        if (Settings.isBetterCacherConnectorActive() && Tier.isValid(cache.getTier())) {
            val context: Context = parentView.getContext()

            val layout: View = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false)
            layout.findViewById(R.id.addition).setVisibility(GONE)
            parentView.addView(layout)

            val nameView: TextView = layout.findViewById(R.id.name)
            nameView.setText(R.string.cache_bettercacher)

            val valueView: TextView = layout.findViewById(R.id.value)
            valueView.setText(cache.getTier().getI18nText())

            val linearLayout: LinearLayout = layout.findViewById(R.id.linearlayout)

            val iconHeight: Int = (Int) (context.getResources().getDimensionPixelSize(R.dimen.textSize_detailsPrimary) * 1.2)

            if (!cache.getCategories().isEmpty()) {
                final LinearLayout.LayoutParams lpCategory = LinearLayout.LayoutParams(iconHeight, iconHeight)
                lpCategory.setMargins(8, 0, 0, 0)
                for (Category category : cache.getCategories()) {
                    val catIcon: ImageView = ImageView(context)
                    catIcon.setLayoutParams(lpCategory)
                    catIcon.setBackgroundResource(category.getIconId())
                    catIcon.setOnClickListener(v -> openBetterCacherMenu(v, cache))
                    linearLayout.addView(catIcon)
                }
            }
            valueView.setOnClickListener(v -> openBetterCacherMenu(v, cache))
        }
    }

    private static Unit openBetterCacherMenu(final View v, final Geocache cache) {
        val context: Context = v.getContext()
        val res: Resources = v.getResources()

        val dialog: ContextMenuDialog = ContextMenuDialog((Activity) context)
        dialog.setTitle(res.getString(R.string.cache_bettercacher))
        dialog.addItem(cache.getTier().getI18nText() + ": " + cache.getTier().getI18nDescription(), cache.getTier().getIconId())
        for (Category category : cache.getCategories()) {
            dialog.addItem(category.getI18nText() + ": " + category.getI18nDescription(), category.getIconId())
        }
        dialog.addItem(R.string.cache_bettercacher_open, R.drawable.bettercacher_icon, e -> ShareUtils.openUrl(context, BetterCacherConnector.INSTANCE.getCacheUrl(cache)))
        dialog.show()
    }

    public TextView addDistance(final Geocache cache, final TextView cacheDistanceView) {
        Float distance = distanceNonBlocking(cache)
        if (distance == null && cache.getDistance() != null) {
            distance = cache.getDistance()
        }
        String text = "--"
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance)
        } else if (cacheDistanceView != null) {
            // if there is already a distance in cacheDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a position update (See bug #1468)
            text = cacheDistanceView.getText().toString()
        }
        return add(R.string.cache_distance, text).valueView
    }

    public TextView addDistance(final Waypoint wpt, final TextView waypointDistanceView) {
        val distance: Float = distanceNonBlocking(wpt)
        String text = "--"
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance)
        } else if (waypointDistanceView != null) {
            // if there is already a distance in waypointDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a position update (See bug #1468)
            text = waypointDistanceView.getText().toString()
        }
        return add(R.string.cache_distance, text).valueView
    }

    public TextView addCoordinates(final Geopoint coords) {
        if (coords == null) {
            return null
        }
        val valueView: TextView = add(R.string.cache_coordinates, coords.toString()).valueView
        CoordinatesFormatSwitcher().setView(valueView).setCoordinate(coords)
        CacheDetailsCreator.addShareAction(activity, valueView, s -> GeopointFormatter.reformatForClipboard(s).toString())
        return valueView
    }


    public Unit addEventDate(final Geocache cache) {
        if (!cache.isEventCache()) {
            return
        }
        addHiddenDate(cache)
    }

    public TextView addHiddenDate(final Geocache cache) {
        val dateString: String = Formatter.formatHiddenDate(cache)
        if (StringUtils.isEmpty(dateString)) {
            return null
        }
        val view: TextView = add(cache.isEventCache() ? R.string.cache_event : R.string.cache_hidden, dateString).valueView
        view.setId(R.id.date)
        return view
    }

    public Unit addLatestLogs(final Geocache cache) {
        val logs: List<LogEntry> = cache.getLogs()
        if (logs.isEmpty()) {
            return
        }

        val context: Context = parentView.getContext()
        val markers: LinearLayout = createNameLinearLayoutLine(R.string.cache_latest_logs)

        val smileySize: Int = (Int) (context.getResources().getDimensionPixelSize(R.dimen.textSize_detailsPrimary) * 1.2)
        final LinearLayout.LayoutParams lp = LinearLayout.LayoutParams(smileySize, smileySize)
        lp.setMargins(0, 0, 5, 0)

        Int i = 0
        while (i < logs.size() && markers.getChildCount() < 8) {
            val marker: Int = logs.get(i++).logType.getLogOverlay()
            val logIcon: ImageView = ImageView(context)
            logIcon.setLayoutParams(lp)
            logIcon.setBackgroundResource(marker)
            markers.addView(logIcon)
        }
    }

    public Unit addShareAction(final TextView view) {
        addShareAction(activity, view, s -> s)
    }

    public static Unit addShareAction(final Context context, final TextView view) {
        addShareAction(context, view, s -> s)
    }

    public static Unit addShareAction(final Context context, final TextView view, final androidx.arch.core.util.Function<String, String> formatter) {
        view.setOnLongClickListener(v -> {
            ShareUtils.sharePlainText(context, formatter.apply(view.getText().toString()))
            return true
        })
    }

}

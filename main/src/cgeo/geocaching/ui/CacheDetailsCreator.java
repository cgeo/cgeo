package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinates;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.SmileyImage;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UnknownTagsHandler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import static android.view.View.GONE;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public final class CacheDetailsCreator {
    private final Activity activity;
    private final ViewGroup parentView;
    private final Resources res;

    /**
     * Immutable pair holder for name value line views.
     */
    public static final class NameValueLine {
        public final View layout;
        public final TextView valueView;

        NameValueLine(final View layout, final TextView value) {
            this.layout = layout;
            this.valueView = value;
        }
    }

    public CacheDetailsCreator(final Activity activity, final ViewGroup parentView) {
        this.activity = activity;
        this.res = activity.getResources();
        this.parentView = parentView;
        parentView.removeAllViews();
    }

    /**
     * Create a "name: value" line.
     *
     * @param nameId the resource of the name field
     * @param value  the initial value
     * @return a pair made of the whole "name: value" line (to be able to hide it for example) and of the value (to update it)
     */
    public NameValueLine add(final int nameId, final CharSequence value) {
        final NameValueLine nameValue = createNameValueLine(nameId);
        nameValue.valueView.setText(value);
        return nameValue;
    }

    /**
     * Create a "name: value" line with html content.
     *
     * @param nameId  the resource of the name field
     * @param value   the initial value
     * @param geocode the geocode for image getter
     * @return a pair made of the whole "name: value" line (to be able to hide it for example) and of the value (to update it)
     */
    public NameValueLine addHtml(final int nameId, final CharSequence value, final String geocode) {
        final NameValueLine nameValue = createNameValueLine(nameId);
        final TextView valueView = nameValue.valueView;
        valueView.setText(HtmlCompat.fromHtml(value.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY, new SmileyImage(geocode, valueView), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
        return nameValue;
    }

    @NonNull
    private NameValueLine createNameValueLine(final int nameId) {
        final View layout = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false);
        final TextView nameView = layout.findViewById(R.id.name);
        nameView.setText(res.getString(nameId));
        final TextView valueView = layout.findViewById(R.id.value);
        parentView.addView(layout);
        return new NameValueLine(layout, valueView);
    }

    public View addStars(final int nameId, final float value) {
        return addStars(nameId, value, 5);
    }

    private View addStars(final int nameId, final float value, final int max) {
        final View layout = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false);
        final TextView nameView = layout.findViewById(R.id.name);
        final TextView valueView = layout.findViewById(R.id.value);

        nameView.setText(activity.getString(nameId));
        valueView.setText(String.format(Locale.getDefault(), activity.getString(R.string.cache_rating_of_new), value, max));

        final RatingBar layoutStars = layout.findViewById(R.id.stars);
        layoutStars.setNumStars(max);
        layoutStars.setRating(value);
        layoutStars.setVisibility(View.VISIBLE);

        parentView.addView(layout);
        return layout;
    }

    public void addCacheState(final Geocache cache) {
        final List<String> states = new ArrayList<>(5);
        String date = getVisitedDate(cache);
        if (cache.hasLogOffline()) {
            states.add(res.getString(R.string.cache_status_offline_log) + date);
            // reset the found date, to avoid showing it twice
            date = "";
        }
        if (cache.isFound()) {
            states.add(res.getString(R.string.cache_status_found) + date);
        } else if (cache.isDNF()) {
            states.add(res.getString(R.string.cache_not_status_found) + date);
        }
        if (cache.isEventCache() && states.isEmpty()) {
            for (final LogEntry log : cache.getLogs()) {
                if (log.logType == LogType.WILL_ATTEND && log.isOwn()) {
                    states.add(LogType.WILL_ATTEND.getL10n());
                }
            }
        }
        if (cache.isArchived()) {
            states.add(res.getString(R.string.cache_status_archived));
        }
        if (cache.isDisabled()) {
            states.add(res.getString(R.string.cache_status_disabled));
        }
        if (cache.isPremiumMembersOnly()) {
            states.add(res.getString(R.string.cache_status_premium));
        }
        if (!states.isEmpty()) {
            add(R.string.cache_status, StringUtils.join(states, ", "));
        }
    }

    private static String getVisitedDate(final Geocache cache) {
        final long visited = cache.getVisitedDate();
        return visited != 0 ? " (" + Formatter.formatShortDate(visited) + ")" : "";
    }

    private static Float distanceNonBlocking(final ICoordinates target) {
        if (target.getCoords() == null) {
            return null;
        }
        return Sensors.getInstance().currentGeo().getCoords().distanceTo(target);
    }

    @SuppressLint("SetTextI18n")
    public void addRating(final Geocache cache) {
        if (cache.getRating() > 0) {
            final View itemLayout = addStars(R.string.cache_rating, cache.getRating());
            if (cache.getVotes() > 0) {
                final TextView itemAddition = itemLayout.findViewById(R.id.addition);
                itemAddition.setText(" (" + cache.getVotes() + ')');
                itemAddition.setVisibility(View.VISIBLE);
            }
        }
    }

    public void addSize(final Geocache cache) {
        if (cache.showSize()) {
            add(R.string.cache_size, cache.getSize().getL10n());
        }
    }

    public void addAlcMode(final Geocache cache) {
        Log.d("_AL add mode to view: " + cache.isLinearAlc());
        if (cache.isLinearAlc()) {
            add(R.string.cache_mode, res.getString(R.string.cache_mode_linear));
        } else {
            add(R.string.cache_mode, res.getString(R.string.cache_mode_random));
        }
    }

    public void addDifficulty(final Geocache cache) {
        if (cache.getDifficulty() > 0) {
            addStars(R.string.cache_difficulty, cache.getDifficulty());
        }
    }

    public void addTerrain(final Geocache cache) {
        if (cache.getTerrain() > 0) {
            addStars(R.string.cache_terrain, cache.getTerrain(), ConnectorFactory.getConnector(cache).getMaxTerrain());
        }
    }

    public TextView addDistance(final Geocache cache, final TextView cacheDistanceView) {
        Float distance = distanceNonBlocking(cache);
        if (distance == null && cache.getDistance() != null) {
            distance = cache.getDistance();
        }
        String text = "--";
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance);
        } else if (cacheDistanceView != null) {
            // if there is already a distance in cacheDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a new position update (See bug #1468)
            text = cacheDistanceView.getText().toString();
        }
        return add(R.string.cache_distance, text).valueView;
    }

    public TextView addDistance(final Waypoint wpt, final TextView waypointDistanceView) {
        final Float distance = distanceNonBlocking(wpt);
        String text = "--";
        if (distance != null) {
            text = Units.getDistanceFromKilometers(distance);
        } else if (waypointDistanceView != null) {
            // if there is already a distance in waypointDistance, use it instead of resetting to default.
            // this prevents displaying "--" while waiting for a new position update (See bug #1468)
            text = waypointDistanceView.getText().toString();
        }
        return add(R.string.cache_distance, text).valueView;
    }

    public void addEventDate(@NonNull final Geocache cache) {
        if (!cache.isEventCache()) {
            return;
        }
        addHiddenDate(cache);
    }

    public TextView addHiddenDate(@NonNull final Geocache cache) {
        final String dateString = Formatter.formatHiddenDate(cache);
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }
        final TextView view = add(cache.isEventCache() ? R.string.cache_event : R.string.cache_hidden, dateString).valueView;
        view.setId(R.id.date);
        return view;
    }

    public void addLatestLogs(final Geocache cache) {
        final Context context = parentView.getContext();

        final View layout = activity.getLayoutInflater().inflate(R.layout.cache_information_item, parentView, false);
        final TextView nameView = layout.findViewById(R.id.name);
        nameView.setText(res.getString(R.string.cache_latest_logs));
        final LinearLayout markers = layout.findViewById(R.id.linearlayout);

        final int smileySize = (int) (context.getResources().getDimensionPixelSize(R.dimen.textSize_detailsPrimary) * 1.2);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(smileySize, smileySize);
        lp.setMargins(0, 0, 5, 0);

        final List<LogEntry> logs = cache.getLogs();
        int i = 0;
        while (i < logs.size() && markers.getChildCount() < 8) {
            final int marker = logs.get(i++).logType.getLogOverlay();
            final ImageView logIcon = new ImageView(context);
            logIcon.setLayoutParams(lp);
            logIcon.setBackgroundResource(marker);
            markers.addView(logIcon);
        }
        if (markers.getChildCount() > 0) {
            parentView.addView(layout);
            layout.findViewById(R.id.value).setVisibility(GONE);
            layout.findViewById(R.id.addition).setVisibility(GONE);
        }
    }
}

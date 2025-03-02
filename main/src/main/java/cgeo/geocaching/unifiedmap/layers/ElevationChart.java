package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.MenuUtils;
import static cgeo.geocaching.unifiedmap.LayerHelper.ZINDEX_ELEVATIONCHARTMARKERPOSITION;
import static cgeo.geocaching.utils.DisplayUtils.getDimensionInDp;

import android.content.res.Resources;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

public class ElevationChart {

    private static final String ELEVATIONCHART_MARKER = "ELEVATIONCHARTMARKER";
    private final View chartBlock;
    private final LineChart chart;
    private final Resources res;
    private final GeoItemLayer<String> geoItemLayer;
    final Toolbar toolbar;
    private final List<Entry> entries = new ArrayList<>();
    private float offset = 0f;
    private Geopoint lastShownPosition = null;

    public ElevationChart(final AppCompatActivity activity, final GeoItemLayer<String> geoItemLayer) {
        chartBlock = activity.findViewById(R.id.elevation_block);
        chart = activity.findViewById(R.id.elevation_chart);
        this.geoItemLayer = geoItemLayer;
        toolbar = activity.findViewById(R.id.toolbar);
        res = activity.getResources();
    }

    public void removeElevationChart() {
        if (chart == null) {
            return;
        }
        if (chartBlock.getVisibility() == View.VISIBLE) {
            closeChart(geoItemLayer);
        }
    }

    public void showElevationChart(final Route route, final RouteTrackUtils routeTrackUtils) {
        if (chart == null) {
            return;
        }

        // initialize chart
        if (chartBlock.getVisibility() != View.VISIBLE) {
            chartBlock.setVisibility(View.VISIBLE);

            chart.setNoDataText(chart.getContext().getString(R.string.init_elevation_notAvailable));

            // follow tap on elevation chart in route on map
            chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(final Entry e, final Highlight h) {
                    final Geopoint center = (Geopoint) e.getData();
                    // update marker if position found
                    if (center != null) {
                        final GeoItem marker = GeoPrimitive.createMarker(center, GeoIcon.builder().setBitmap(ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.circle, null))).build()).buildUpon().setZLevel(ZINDEX_ELEVATIONCHARTMARKERPOSITION).build();
                        geoItemLayer.put(ELEVATIONCHART_MARKER, marker);
                    }
                }

                @Override
                public void onNothingSelected() {
                    geoItemLayer.remove(ELEVATIONCHART_MARKER);
                }
            });

            toolbar.setNavigationIcon(R.drawable.expand_more);
            toolbar.setNavigationOnClickListener(v -> closeChart(geoItemLayer));
        }

        if (routeTrackUtils != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.map_routetrack_context);
            RouteTrackUtils.configureContextMenu(toolbar.getMenu(), false, route, true);
            toolbar.setOnMenuItemClickListener(item -> routeTrackUtils.handleContextMenuClick(item, null, route, null));
            MenuUtils.enableIconsInOverflowMenu(toolbar.getMenu());
        }

        // set/update data
        synchronized (entries) {
            collectData(route);
            formatChart(res);
            chart.invalidate();
            toolbar.setTitle(route.getName().isEmpty() ? CgeoApplication.getInstance().getString(R.string.individual_route) : route.getName());
            geoItemLayer.remove(ELEVATIONCHART_MARKER);
        }
    }

    /** collect entries for line chart from route */
    private void collectData(final Route route) {
        float distance = 0.0f;
        Geopoint lastPoint = null;
        entries.clear();
        if (route == null || route.getSegments() == null) {
            return;
        }
        for (RouteSegment segment : route.getSegments()) {
            final ArrayList<Float> elevation = segment.getElevation();
            if (elevation == null) {
                return;
            }
            final Iterator<Float> it = elevation.iterator();
            for (Geopoint point : segment.getPoints()) {
                if (lastPoint != null) {
                    distance += lastPoint.distanceTo(point);
                }
                lastPoint = point;
                final float elev = it.hasNext() ? it.next() : Float.NaN;
                if (!Float.isNaN(elev)) {
                    entries.add(new Entry(distance, elev, point));
                }
            }
        }
    }

    /** format line chart (lines, axes etc.) */
    private void formatChart(final Resources res) {
        chart.setData(null);
        if (!entries.isEmpty()) {
            final LineDataSet dataSet = new LineDataSet(entries, null);
            dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            dataSet.setLineWidth(2f);
            final int color = res.getColor(R.color.colorAccent);
            dataSet.setColor(color);
            dataSet.setHighLightColor(res.getColor(R.color.colorTextHint));
            dataSet.setHighlightLineWidth(2.5f);
            dataSet.setValueTextColor(res.getColor(R.color.colorText));
            dataSet.setDrawValues(false);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(color);
            dataSet.setDrawCircles(false);

            final LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
        }

        chart.setExtraOffsets(0, -30, 0, 10);
        chart.setBackgroundColor(res.getColor(R.color.colorBackground));

        final Legend legend = chart.getLegend();
        legend.setEnabled(false);
        chart.getDescription().setEnabled(false);

        final XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextSize(getDimensionInDp(res, R.dimen.textSize_detailsSecondary));
        xAxis.setTextColor(res.getColor(R.color.colorText));
        xAxis.setAxisMinimum(0.0f);
        xAxis.setValueFormatter((value, axis) -> Units.getDistanceFromKilometers(value - offset));

        final YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(true);
        yAxis.setTextSize(getDimensionInDp(res, R.dimen.textSize_detailsSecondary));
        yAxis.setTextColor(res.getColor(R.color.colorText));

        final YAxis yAxis2 = chart.getAxisRight();
        yAxis2.setEnabled(false);
    }

    /** find position on track closest to given coords (max 100m) and highlight it */
    public void showPositionOnTrack(final Geopoint coords) {
        // calculate new position on track only if minimum distance reached
        if (lastShownPosition != null && coords.distanceTo(lastShownPosition) < 0.05f) {
            return;
        }
        lastShownPosition = coords;
        // find position
        float minDistance = 0.1f;
        int x = -1;
        int index = 0;
        for (Entry entry : entries) {
            final float distance = ((Geopoint) entry.getData()).distanceTo(coords);
            if (distance < minDistance) {
                x = index;
                minDistance = distance;
            }
            index++;
        }
        // display hairpin line for found position + adapt x axis labelling
        offset = x < 0 ? 0f : entries.get(x).getX();
        chart.highlightValue(offset, x < 0 ? -1 : 0);
        chart.invalidate();
    }

    /** hides chart and map marker */
    private void closeChart(final GeoItemLayer<String> geoItemLayer) {
        chartBlock.setVisibility(View.GONE);
        geoItemLayer.remove(ELEVATIONCHART_MARKER);
        LifecycleAwareBroadcastReceiver.sendBroadcast(chart.getContext(), Intents.ACTION_ELEVATIONCHART_CLOSED);
    }
}

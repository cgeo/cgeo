package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.ImageUtils;
import static cgeo.geocaching.unifiedmap.LayerHelper.ZINDEX_ELEVATIONCHARTMARKERPOSITION;
import static cgeo.geocaching.utils.DisplayUtils.getDimensionInDp;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

public class ElevationChartUtils {

    private static final String ELEVATIONCHART_MARKER = "ELEVATIONCHARTMARKER";

    private ElevationChartUtils() {
        // utility class
    }

    public static void toggleElevationChart(@NonNull final AppCompatActivity activity, final Route route, final GeoItemLayer<String> geoItemLayer, final String title) {
        final View chartBlock = activity.findViewById(R.id.elevation_block);
        final LineChart chart = activity.findViewById(R.id.elevation_chart);
        if (chart == null) {
            return;
        }
        if (chartBlock.getVisibility() == View.VISIBLE || route == null) {
            chartBlock.setVisibility(View.GONE);
        } else {
            chartBlock.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.elevation_title)).setText(title);

            final List<Entry> entries = new ArrayList<>();
            collectData(route, entries);

            final Resources res = activity.getResources();
            formatChart (res, chart, entries);

            // follow tap on elevation chart in route on map
            chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(final Entry e, final Highlight h) {
                    final Geopoint center = findClosestByDistance(route, e.getX());
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

            activity.findViewById(R.id.elevation_close).setOnClickListener(v -> chartBlock.setVisibility(View.GONE));

            chart.invalidate(); // draw chart
        }
    }

    /** collect entries for line chart from route */
    private static void collectData(final Route route, final List<Entry> entries) {
        float distance = 0.0f;
        Geopoint lastPoint = null;
        for (RouteSegment segment : route.getSegments()) {
            final ArrayList<Float> elevation = segment.getElevation();
            final Iterator<Float> it = elevation.iterator();
            for (Geopoint point : segment.getPoints()) {
                if (lastPoint != null) {
                    distance += lastPoint.distanceTo(point);
                }
                lastPoint = point;
                final float elev = it.hasNext() ? it.next() : Float.NaN;
                if (!Float.isNaN(elev)) {
                    entries.add(new Entry(distance, elev));
                }
            }
        }
    }

    /** format line chart (lines, axes etc.) */
    private static void formatChart(final Resources res, final LineChart chart, final List<Entry> entries) {
        final LineDataSet dataSet = new LineDataSet(entries, "Individual Route");
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setLineWidth(2f);
        final int color = res.getColor(R.color.colorAccent);
        dataSet.setColor(color);
        dataSet.setHighLightColor(color);
        dataSet.setValueTextColor(res.getColor(R.color.colorText));
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setDrawCircles(false);

        final LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

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
        xAxis.setValueFormatter((value, axis) -> Units.getDistanceFromKilometers(value));

        final YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(true);
        yAxis.setTextSize(getDimensionInDp(res, R.dimen.textSize_detailsSecondary));
        yAxis.setTextColor(res.getColor(R.color.colorText));

        final YAxis yAxis2 = chart.getAxisRight();
        yAxis2.setEnabled(false);
    }

    /** find position in route corresponding to distance from start */
    @Nullable
    private static Geopoint findClosestByDistance(final Route route, final float distance) {
        float done = 0.0f;
        Geopoint lastPoint = null;
        for (RouteSegment segment : route.getSegments()) {
            for (Geopoint point : segment.getPoints()) {
                if (lastPoint != null) {
                    done += lastPoint.distanceTo(point);
                }
                if (done >= distance) {
                    return point;
                }
                lastPoint = point;
            }
        }
        return null;
    }
}

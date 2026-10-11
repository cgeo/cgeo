package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.ui.ViewUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import info.appdev.charting.components.MarkerView;
import info.appdev.charting.data.EntryFloat;
import info.appdev.charting.highlight.Highlight;
import info.appdev.charting.utils.PointF;
import info.appdev.charting.utils.ViewPortHandler;

@SuppressLint("ViewConstructor")
public class ElevationChartMarkerView extends MarkerView {

    private final TextView tvText, tvDistance, tvUp, tvDown;

    public ElevationChartMarkerView(final Context context, final int layoutResource) {
        super(context, layoutResource);

        tvText = findViewById(R.id.elevationText);
        tvDistance = findViewById(R.id.distanceText);
        tvUp = findViewById(R.id.upText);
        tvDown = findViewById(R.id.downText);
    }

    // Run custom text updates when a data point is selected
    @Override
    public void refreshContent(final EntryFloat entry, @NonNull final Highlight highlight) {
        final ElevationChart.Data data = (ElevationChart.Data) entry.getData();
        ViewUtils.setText(tvText, Units.formatElevation(entry.getY()));
        ViewUtils.setText(tvDistance, data == null ? "" : Units.getDistanceFromMeters(data.distanceRemaining / 100f));
        ViewUtils.setText(tvUp, data == null ? "" : Units.formatElevation(data.upRemaining / 100f));
        ViewUtils.setText(tvDown, data == null ? "" : Units.formatElevation(data.downRemaining / 100f));

        // Force the layout to recalculate its width and height based on the text
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());

        super.refreshContent(entry, highlight);
    }

    @NonNull
    @Override
    public PointF getOffsetForDrawingAtPoint(final float posX, final float posY) {
        final int markerWidth = getWidth();
        final int markerHeight = getHeight();

        // get Viewport handler to determine exact positions of axes
        final ViewPortHandler viewPort = getChartView().getViewPortHandler();
        final float axisLeft = viewPort.contentLeft();
        final float axisRight = viewPort.contentRight();
        final float axisTop = viewPort.contentTop();
        final float axisBottom = viewPort.contentBottom();

        // horizontal placement (default: left of data point, with spacing)
        // flip to right of data point if infobox is cut off or hitting vertical axis
        final float spacingX = 15f;
        float offsetX = -markerWidth - spacingX;
        if (posX + offsetX < axisLeft) {
            offsetX = spacingX;
            if (posX + offsetX + markerWidth > axisRight) {
                offsetX = axisRight - posX - markerWidth;
            }
        }

        // vertical placement (default: vertically centered)
        float offsetY = -(markerHeight / 2f);
        if (posY + offsetY < axisTop) {
            offsetY = axisTop - posY;
        } else if (posY + offsetY + markerHeight > axisBottom) {
            offsetY = axisBottom - posY - markerHeight;
        }

        return new PointF(offsetX, offsetY);
    }

}

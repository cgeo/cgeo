package cgeo.geocaching.mapcommon;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;
import cgeo.geocaching.R;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;

public class cgMapMyOverlay implements OverlayBase {
	private cgSettings settings = null;
	private Location coordinates = null;
	private GeoPointImpl location = null;
	private Double heading = Double.valueOf(0);
	private Paint accuracyCircle = null;
	private Paint historyLine = null;
	private Paint historyLineShadow = null;
	private Point center = new Point();
	private Point left = new Point();
	private Bitmap arrow = null;
	private int widthArrowHalf = 0;
	private int heightArrowHalf = 0;
	private PaintFlagsDrawFilter setfil = null;
	private PaintFlagsDrawFilter remfil = null;
	private Location historyRecent = null;
	private List<Location> history = new ArrayList<Location>();
	private Point historyPointN = new Point();
	private Point historyPointP = new Point();
	private Activity activity;
	private MapFactory mapFactory = null;
	private OverlayImpl ovlImpl = null;

	public cgMapMyOverlay(cgSettings settingsIn, Activity activity, OverlayImpl ovlImpl) {
		settings = settingsIn;
		this.activity = activity;
		this.mapFactory = settings.getMapFactory();
		this.ovlImpl = ovlImpl;
	}

	public void setCoordinates(Location coordinatesIn) {
		coordinates = coordinatesIn;
		location = settings.getMapFactory().getGeoPointBase(new Geopoint(coordinates));
	}

	public void setHeading(Double headingIn) {
		heading = headingIn;
	}

	@Override
	public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
			MapProjectionImpl projection, byte drawZoomLevel) {

		drawInternal(canvas, projection);
	}

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {

    	drawInternal(canvas, mapView.getMapProjection());
    }

    private void drawInternal(Canvas canvas, MapProjectionImpl projection) {

		if (coordinates == null || location == null) return;

		if (accuracyCircle == null) {
			accuracyCircle = new Paint();
			accuracyCircle.setAntiAlias(true);
			accuracyCircle.setStrokeWidth(1.0f);
		}

		if (historyLine == null) {
			historyLine = new Paint();
			historyLine.setAntiAlias(true);
			historyLine.setStrokeWidth(3.0f);
			historyLine.setColor(0xFFFFFFFF);
		}

		if (historyLineShadow == null) {
			historyLineShadow = new Paint();
			historyLineShadow.setAntiAlias(true);
			historyLineShadow.setStrokeWidth(7.0f);
			historyLineShadow.setColor(0x66000000);
		}

		if (setfil == null) setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
		if (remfil == null) remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);

		canvas.setDrawFilter(setfil);

		double latitude = coordinates.getLatitude();
		double longitude = coordinates.getLongitude();
		float accuracy = coordinates.getAccuracy();

		float[] result = new float[1];

		Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
		float longitudeLineDistance = result[0];

		final Geopoint leftCoords = new Geopoint(latitude, longitude - accuracy / longitudeLineDistance);
		GeoPointImpl leftGeo = mapFactory.getGeoPointBase(leftCoords);
		projection.toPixels(leftGeo, left);
		projection.toPixels(location, center);
		int radius = center.x - left.x;

		accuracyCircle.setColor(0x66000000);
		accuracyCircle.setStyle(Style.STROKE);
		canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

		accuracyCircle.setColor(0x08000000);
		accuracyCircle.setStyle(Style.FILL);
		canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

		if (coordinates.getAccuracy() < 50f && ((historyRecent != null && cgBase.getDistance(new Geopoint(historyRecent), new Geopoint(coordinates)) > 0.005) || historyRecent == null)) {
			if (historyRecent != null) history.add(historyRecent);
			historyRecent = coordinates;

			int toRemove = history.size() - 700;

			if (toRemove > 0) {
				for (int cnt = 0; cnt < toRemove; cnt ++) {
					history.remove(cnt);
				}
			}
		}

		if (settings.maptrail == 1) {
			int size = history.size();
			if (size > 1) {
				int alpha = 0;
				int alphaCnt = size - 201;
				if (alphaCnt < 1) alphaCnt = 1;

				for (int cnt = 1; cnt < size; cnt ++) {
					Location prev = history.get(cnt - 1);
					Location now = history.get(cnt);

					if (prev != null && now != null) {
						projection.toPixels(mapFactory.getGeoPointBase(new Geopoint(prev)), historyPointP);
						projection.toPixels(mapFactory.getGeoPointBase(new Geopoint(now)), historyPointN);

						if ((alphaCnt - cnt) > 0) {
							alpha = 255 / (alphaCnt - cnt);
						}
						else {
							alpha = 255;
						}

						historyLineShadow.setAlpha(alpha);
						historyLine.setAlpha(alpha);

						canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLineShadow);
						canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLine);
					}
				}
			}

			if (size > 0) {
				Location prev = history.get(size - 1);
				Location now = coordinates;

				if (prev != null && now != null) {
					projection.toPixels(mapFactory.getGeoPointBase(new Geopoint(prev)), historyPointP);
					projection.toPixels(mapFactory.getGeoPointBase(new Geopoint(now)), historyPointN);

					historyLineShadow.setAlpha(255);
					historyLine.setAlpha(255);

					canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLineShadow);
					canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLine);
				}
			}
		}

		if (arrow == null) {
			arrow = BitmapFactory.decodeResource(activity.getResources(), R.drawable.my_location_chevron);
			widthArrowHalf = arrow.getWidth() / 2;
			heightArrowHalf = arrow.getHeight() / 2;
		}

		int marginLeft;
		int marginTop;

		marginLeft = center.x - widthArrowHalf;
		marginTop = center.y - heightArrowHalf;
		
		Matrix matrix = new Matrix();
		matrix.setRotate(heading.floatValue(), widthArrowHalf, heightArrowHalf);
		matrix.postTranslate(marginLeft, marginTop);

		canvas.drawBitmap(arrow, matrix, null);

		canvas.setDrawFilter(remfil);

		//super.draw(canvas, mapView, shadow);
	}

	@Override
	public OverlayImpl getOverlayImpl() {
		return this.ovlImpl;
	}
}
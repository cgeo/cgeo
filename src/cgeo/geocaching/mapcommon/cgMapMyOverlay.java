package cgeo.geocaching.mapcommon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.location.Location;
import cgeo.geocaching.R;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.MapViewImpl;

import java.util.ArrayList;

public class cgMapMyOverlay implements OverlayBase {
	private cgSettings settings = null;
	private Location coordinates = null;
	private GeoPointImpl location = null;
	private Double heading = new Double(0);
	private Paint accuracyCircle = null;
	private Paint historyLine = null;
	private Paint historyLineShadow = null;
	private Point center = new Point();
	private Point left = new Point();
	private Bitmap arrow = null;
	private int widthArrow = 0;
	private int heightArrow = 0;
	private PaintFlagsDrawFilter setfil = null;
	private PaintFlagsDrawFilter remfil = null;
	private Location historyRecent = null;
	private ArrayList<Location> history = new ArrayList<Location>();
	private Point historyPointN = new Point();
	private Point historyPointP = new Point();

	public cgMapMyOverlay(cgSettings settingsIn) {
		settings = settingsIn;
	}

	public void setCoordinates(Location coordinatesIn) {
		coordinates = coordinatesIn;
		location = settings.getMapFactory().getGeoPointBase((int)(coordinatesIn.getLatitude() * 1e6), (int)(coordinatesIn.getLongitude() * 1e6));
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
		
		MapFactory mapFactory = settings.getMapFactory();
		
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

		GeoPointImpl leftGeo = mapFactory.getGeoPointBase((int)(latitude * 1e6), (int)((longitude - accuracy / longitudeLineDistance) * 1e6));
		projection.toPixels(leftGeo, left);
		projection.toPixels(location, center);
		int radius = center.x - left.x;

		accuracyCircle.setColor(0x66000000);
		accuracyCircle.setStyle(Style.STROKE);
		canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

		accuracyCircle.setColor(0x08000000);
		accuracyCircle.setStyle(Style.FILL);
		canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

		if (coordinates.getAccuracy() < 50f && ((historyRecent != null && cgBase.getDistance(historyRecent.getLatitude(), historyRecent.getLongitude(), coordinates.getLatitude(), coordinates.getLongitude()) > 0.005) || historyRecent == null)) {
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
						projection.toPixels(mapFactory.getGeoPointBase((int)(prev.getLatitude() * 1e6), (int)(prev.getLongitude() * 1e6)), historyPointP);
						projection.toPixels(mapFactory.getGeoPointBase((int)(now.getLatitude() * 1e6), (int)(now.getLongitude() * 1e6)), historyPointN);

						if ((alphaCnt - cnt) > 0) alpha = Math.round(255 / (alphaCnt - cnt));
						else alpha = 255;

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
					projection.toPixels(mapFactory.getGeoPointBase((int)(prev.getLatitude() * 1e6), (int)(prev.getLongitude() * 1e6)), historyPointP);
					projection.toPixels(mapFactory.getGeoPointBase((int)(now.getLatitude() * 1e6), (int)(now.getLongitude() * 1e6)), historyPointN);

					historyLineShadow.setAlpha(255);
					historyLine.setAlpha(255);

					canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLineShadow);
					canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLine);
				}
			}
		}

		if (arrow == null) {
			arrow = BitmapFactory.decodeResource(settings.getContext().getResources(), R.drawable.my_location_chevron);
			widthArrow = arrow.getWidth();
			heightArrow = arrow.getHeight();
		}

		int marginLeft;
		int marginTop;

		marginLeft = center.x - (widthArrow / 2);
		marginTop = center.y - (heightArrow / 2);

		canvas.rotate(new Float(heading), center.x, center.y);
		canvas.drawBitmap(arrow, marginLeft, marginTop, null);
		canvas.rotate(-(new Float(heading)), center.x, center.y);

		canvas.setDrawFilter(remfil);
		
		//super.draw(canvas, mapView, shadow);
	}
}
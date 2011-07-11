package cgeo.geocaching;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class cgDistanceView extends TextView {
	private cgBase base = null;
	private Double cacheLat = null;
	private Double cacheLon = null;

	public cgDistanceView(Context context) {
		super(context);
	}

	public cgDistanceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public cgDistanceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setContent(cgBase baseIn, Double cacheLatIn, Double cacheLonIn) {
		base = baseIn;
		cacheLat = cacheLatIn;
		cacheLon = cacheLonIn;
	}

	public void update(Double latitude, Double longitude) {
		if (cacheLat == null || cacheLon == null) return;
		if (latitude == null || longitude == null) return;
		if (base == null) return;

		setText(base.getHumanDistance(cgBase.getDistance(latitude, longitude, cacheLat, cacheLon)));
	}

	public void setDistance(Double distance) {
		setText("~" + base.getHumanDistance(distance));
	}

	public void clear() {
		setText(null);
	}
}
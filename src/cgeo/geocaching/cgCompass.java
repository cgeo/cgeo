package cgeo.geocaching;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class cgCompass extends View {

	private changeThread watchdog = null;
	private boolean wantStop = false;
	private boolean lock = false;
	private boolean drawing = false;
	private Context context = null;
	private Bitmap compassUnderlay = null;
	private Bitmap compassRose = null;
	private Bitmap compassArrow = null;
	private Bitmap compassOverlay = null;
	private Double azimuth = Double.valueOf(0);
	private Double heading = Double.valueOf(0);
	private Double cacheHeading = Double.valueOf(0);
	private Double northHeading = Double.valueOf(0);
	private PaintFlagsDrawFilter setfil = null;
	private PaintFlagsDrawFilter remfil = null;
	private int compassUnderlayWidth = 0;
	private int compassUnderlayHeight = 0;
	private int compassRoseWidth = 0;
	private int compassRoseHeight = 0;
	private int compassArrowWidth = 0;
	private int compassArrowHeight = 0;
	private int compassOverlayWidth = 0;
	private int compassOverlayHeight = 0;
	private Handler changeHandler = new Handler() {

		@Override
		public void handleMessage(Message message) {
			try {
				invalidate();
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgCompass.changeHandler: " + e.toString());
			}
		}
	};

	public cgCompass(Context contextIn) {
		super(contextIn);
		context = contextIn;
	}

	public cgCompass(Context contextIn, AttributeSet attrs) {
		super(contextIn, attrs);
		context = contextIn;
	}

	@Override
	public void onAttachedToWindow() {
		compassUnderlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_underlay);
		compassRose = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_rose);
		compassArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_arrow);
		compassOverlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_overlay);

		compassUnderlayWidth = compassUnderlay.getWidth();
		compassUnderlayHeight = compassUnderlay.getWidth();
		compassRoseWidth = compassRose.getWidth();
		compassRoseHeight = compassRose.getWidth();
		compassArrowWidth = compassArrow.getWidth();
		compassArrowHeight = compassArrow.getWidth();
		compassOverlayWidth = compassOverlay.getWidth();
		compassOverlayHeight = compassOverlay.getWidth();

		setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
		remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);

		wantStop = false;

		watchdog = new changeThread(changeHandler);
		watchdog.start();
	}

	@Override
	public void onDetachedFromWindow() {
		wantStop = true;

		if (compassUnderlay != null) {
			compassUnderlay.recycle();
		}

		if (compassRose != null) {
			compassRose.recycle();
		}

		if (compassArrow != null) {
			compassArrow.recycle();
		}

		if (compassOverlay != null) {
			compassOverlay.recycle();
		}
	}

	protected void updateNorth(Double northHeadingIn, Double cacheHeadingIn) {
		northHeading = northHeadingIn;
		cacheHeading = cacheHeadingIn;
	}

	private class changeThread extends Thread {

		Handler handler = null;

		public changeThread(Handler handlerIn) {
			handler = handlerIn;
		}

		@Override
		public void run() {
			while (wantStop == false) {
				try {
					sleep(50);
				} catch (Exception e) {
					// nothing
				}

				if (Math.abs(azimuth - northHeading) < 2 && Math.abs(heading - cacheHeading) < 2) {
					continue;
				}

				lock = true;

				Double diff = Double.valueOf(0);
				Double diffAbs = Double.valueOf(0);
				Double tempAzimuth = Double.valueOf(0);
				Double tempHeading = Double.valueOf(0);

				Double actualAzimuth = azimuth;
				Double actualHeading = heading;

				diff = northHeading - actualAzimuth;
				diffAbs = Math.abs(northHeading - actualAzimuth);
				if (diff < 0) {
					diff = diff + 360;
				} else if (diff >= 360) {
					diff = diff - 360;
				}

				if (diff > 0 && diff <= 180) {
					if (diffAbs > 5) {
						tempAzimuth = actualAzimuth + 2;
					} else if (diffAbs > 1) {
						tempAzimuth = actualAzimuth + 1;
					} else {
						tempAzimuth = actualAzimuth;
					}
				} else if (diff > 180 && diff < 360) {
					if (diffAbs > 5) {
						tempAzimuth = actualAzimuth - 2;
					} else if (diffAbs > 1) {
						tempAzimuth = actualAzimuth - 1;
					} else {
						tempAzimuth = actualAzimuth;
					}
				} else {
					tempAzimuth = actualAzimuth;
				}

				diff = cacheHeading - actualHeading;
				diffAbs = Math.abs(cacheHeading - actualHeading);
				if (diff < 0) {
					diff = diff + 360;
				} else if (diff >= 360) {
					diff = diff - 360;
				}

				if (diff > 0 && diff <= 180) {
					if (diffAbs > 5) {
						tempHeading = actualHeading + 2;
					} else if (diffAbs > 1) {
						tempHeading = actualHeading + 1;
					} else {
						tempHeading = actualHeading;
					}
				} else if (diff > 180 && diff < 360) {
					if (diffAbs > 5) {
						tempHeading = actualHeading - 2;
					} else if (diffAbs > 1) {
						tempHeading = actualHeading - 1;
					} else {
						tempHeading = actualHeading;
					}
				} else {
					tempHeading = actualHeading;
				}

				if (tempAzimuth >= 360) {
					tempAzimuth = tempAzimuth - 360;
				} else if (tempAzimuth < 0) {
					tempAzimuth = tempAzimuth + 360;
				}

				if (tempHeading >= 360) {
					tempHeading = tempHeading - 360;
				} else if (tempHeading < 0) {
					tempHeading = tempHeading + 360;
				}

				azimuth = tempAzimuth;
				heading = tempHeading;

				lock = false;

				changeHandler.sendMessage(new Message());
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (lock) {
			return;
		}
		if (drawing) {
			return;
		}

		Double azimuthTemp = azimuth;
		Double azimuthRelative = azimuthTemp - heading;
		if (azimuthRelative < 0) {
			azimuthRelative = azimuthRelative + 360;
		} else if (azimuthRelative >= 360) {
			azimuthRelative = azimuthRelative - 360;
		}

		// compass margins
		int canvasCenterX = (compassRoseWidth / 2) + ((getWidth() - compassRoseWidth) / 2);
		int canvasCenterY = (compassRoseHeight / 2) + ((getHeight() - compassRoseHeight) / 2);

		int marginLeftTemp = 0;
		int marginTopTemp = 0;

		drawing = true;
		super.onDraw(canvas);

		canvas.save();
		canvas.setDrawFilter(setfil);

		marginLeftTemp = (getWidth() - compassUnderlayWidth) / 2;
		marginTopTemp = (getHeight() - compassUnderlayHeight) / 2;

		canvas.drawBitmap(compassUnderlay, marginLeftTemp, marginTopTemp, null);

		marginLeftTemp = (getWidth() - compassRoseWidth) / 2;
		marginTopTemp = (getHeight() - compassRoseHeight) / 2;

		canvas.rotate(-(azimuthTemp.floatValue()), canvasCenterX, canvasCenterY);
		canvas.drawBitmap(compassRose, marginLeftTemp, marginTopTemp, null);
		canvas.rotate(azimuthTemp.floatValue(), canvasCenterX, canvasCenterY);

		marginLeftTemp = (getWidth() - compassArrowWidth) / 2;
		marginTopTemp = (getHeight() - compassArrowHeight) / 2;

		canvas.rotate(-(azimuthRelative.floatValue()), canvasCenterX, canvasCenterY);
		canvas.drawBitmap(compassArrow, marginLeftTemp, marginTopTemp, null);
		canvas.rotate(azimuthRelative.floatValue(), canvasCenterX, canvasCenterY);

		marginLeftTemp = (getWidth() - compassOverlayWidth) / 2;
		marginTopTemp = (getHeight() - compassOverlayHeight) / 2;

		canvas.drawBitmap(compassOverlay, marginLeftTemp, marginTopTemp, null);

		canvas.setDrawFilter(remfil);
		canvas.restore();

		drawing = false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}

	private int measureWidth(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = compassArrow.getWidth() + getPaddingLeft() + getPaddingRight();

			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}

		return result;
	}

	private int measureHeight(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = compassArrow.getHeight() + getPaddingTop() + getPaddingBottom();

			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}

		return result;
	}
}
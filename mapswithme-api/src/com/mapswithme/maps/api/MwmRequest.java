package com.mapswithme.maps.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class MwmRequest
{

  // **
  private List<MWMPoint> mPoints    = new ArrayList<MWMPoint>();
  private PendingIntent  mPendingIntent;
  private String         mTitle;
  private double         mZoomLevel = 1;
  private boolean        mReturnOnBalloonClick;
  private boolean        mPickPoint = false;
  private String         mCustomButtonName = "";
  // **

  public MwmRequest setCustomButtonName(String buttonName)
  {
    mCustomButtonName = buttonName != null ? buttonName : "";
    return this;
  }

  public MwmRequest setTitle(String title)
  {
    mTitle = title;
    return this;
  }

  public MwmRequest setPickPointMode(boolean pickPoint)
  {
    mPickPoint = pickPoint;
    return this;
  }

  public MwmRequest addPoint(MWMPoint point)
  {
    mPoints.add(point);
    return this;
  }

  public MwmRequest addPoint(double lat, double lon, String name, String id)
  {
    return addPoint(new MWMPoint(lat, lon, name, id));
  }

  public MwmRequest setPoints(Collection<MWMPoint> points)
  {
    mPoints = new ArrayList<MWMPoint>(points);
    return this;
  }

  public MwmRequest setReturnOnBalloonClick(boolean doReturn)
  {
    mReturnOnBalloonClick = doReturn;
    return this;
  }

  public MwmRequest setZoomLevel(double zoomLevel)
  {
    mZoomLevel = zoomLevel;
    return this;
  }

  public MwmRequest setPendingIntent(PendingIntent pi)
  {
    mPendingIntent = pi;
    return this;
  }

  public Intent toIntent(Context context)
  {
    final Intent mwmIntent = new Intent(Const.ACTION_MWM_REQUEST);

    // url
    final String mwmUrl = createMwmUrl(context, mTitle, mZoomLevel, mPoints).toString();
    mwmIntent.putExtra(Const.EXTRA_URL, mwmUrl);
    // title
    mwmIntent.putExtra(Const.EXTRA_TITLE, mTitle);
    // more
    mwmIntent.putExtra(Const.EXTRA_RETURN_ON_BALLOON_CLICK, mReturnOnBalloonClick);
    // pick point
    mwmIntent.putExtra(Const.EXTRA_PICK_POINT, mPickPoint);
    // custom button name
    mwmIntent.putExtra(Const.EXTRA_CUSTOM_BUTTON_NAME, mCustomButtonName);

    final boolean hasIntent = mPendingIntent != null;
    mwmIntent.putExtra(Const.EXTRA_HAS_PENDING_INTENT, hasIntent);
    if (hasIntent)
      mwmIntent.putExtra(Const.EXTRA_CALLER_PENDING_INTENT, mPendingIntent);

    addCommonExtras(context, mwmIntent);

    return mwmIntent;
  }

  /**
   * @Hidden
   * This method is internal only.
   * Used for compatibility.
   */
  MwmRequest setPoints(MWMPoint[] points)
  {
    return setPoints(Arrays.asList(points));
  }

  // Below are utilities from MapsWithMeApi because we are not "Feature Envy"

  private static StringBuilder createMwmUrl(Context context, String title, double zoomLevel, List<MWMPoint> points)
  {
    final StringBuilder urlBuilder = new StringBuilder("mapswithme://map?");
    // version
    urlBuilder.append("v=").append(Const.API_VERSION).append("&");
    // back url, always not null
    urlBuilder.append("backurl=").append(getCallbackAction(context)).append("&");
    // title
    appendIfNotNull(urlBuilder, "appname", title);
    // zoom
    appendIfNotNull(urlBuilder, "z", isValidZoomLevel(zoomLevel) ? String.valueOf(zoomLevel) : null);

    // points
    for (final MWMPoint point : points)
    {
      if (point != null)
      {
        urlBuilder.append("ll=").append(String.format(Locale.US, "%f,%f&", point.getLat(), point.getLon()));

        appendIfNotNull(urlBuilder, "n", point.getName());
        appendIfNotNull(urlBuilder, "id", point.getId());
        appendIfNotNull(urlBuilder, "s", point.getStyleForUrl());
      }
    }

    return urlBuilder;
  }

  private static String getCallbackAction(Context context)
  {
    return Const.CALLBACK_PREFIX + context.getPackageName();
  }

  @SuppressLint("NewApi")
  private static Intent addCommonExtras(Context context, Intent intent)
  {
    intent.putExtra(Const.EXTRA_CALLER_APP_INFO, context.getApplicationInfo());
    intent.putExtra(Const.EXTRA_API_VERSION, Const.API_VERSION);

    return intent;
  }

  private static StringBuilder appendIfNotNull(StringBuilder builder, String key, String value)
  {
    if (value != null)
      builder.append(key).append("=").append(Uri.encode(value)).append("&");

    return builder;
  }

  private static boolean isValidZoomLevel(double zoom)
  {
    return zoom >= MapsWithMeApi.ZOOM_MIN && zoom <= MapsWithMeApi.ZOOM_MAX;
  }

}

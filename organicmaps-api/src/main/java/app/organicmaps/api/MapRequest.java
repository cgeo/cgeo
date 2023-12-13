/*
 Copyright (c) 2022, Organic Maps OÜ. All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 Redistributions of source code must retain the above copyright notice, this list
 of conditions and the following disclaimer. Redistributions in binary form must
 reproduce the above copyright notice, this list of conditions and the following
 disclaimer in the documentation and/or other materials provided with the
 distribution. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 OF SUCH DAMAGE.
 */
package app.organicmaps.api;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class MapRequest
{
  private List<Point> mPoints = new ArrayList<>();
  private String mAppName;
  private double mZoomLevel;
  // pick point mode
  private boolean mPickPointMode;

  public @NonNull MapRequest
  setAppName(String appName)
  {
    mAppName = appName;
    return this;
  }

  public @NonNull
  MapRequest addPoint(Point point)
  {
    mPoints.add(point);
    return this;
  }

  public @NonNull
  MapRequest setPoints(Collection<Point> points)
  {
    mPoints = new ArrayList<>(points);
    return this;
  }

  public @NonNull
  MapRequest setZoomLevel(double zoomLevel)
  {
    mZoomLevel = zoomLevel;
    return this;
  }

  public @NonNull MapRequest setPickPointMode(boolean pickPointMode)
  {
    mPickPointMode = pickPointMode;
    return this;
  }

  public @NonNull
  Intent toIntent()
  {
    final StringBuilder builder = new StringBuilder(Const.API_SCHEME);
    builder.append("map?");

    // title
    if (mAppName != null)
      builder.append("appname").append("=").append(Uri.encode(mAppName)).append("&");
    // zoom
    if (mZoomLevel != 0.0)
      builder.append("z").append("=").append(mZoomLevel).append("&");

    // points
    for (final Point point : mPoints)
    {
      if (point != null)
      {
        builder.append("ll=").append(String.format(Locale.US, "%f,%f&", point.getLat(), point.getLon()));
        if (point.getName() != null)
          builder.append("n").append("=").append(Uri.encode(point.getName())).append("&");
        if (point.getId() != null)
          builder.append("id").append("=").append(Uri.encode(point.getId())).append("&");
        if (point.getStyle() != null)
          builder.append("s").append("=").append(Uri.encode(point.getStyle().getName())).append("&");
      }
    }

    final Uri uri = Uri.parse(builder.toString());
    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    if (mPickPointMode)
      intent.putExtra(Const.EXTRA_PICK_POINT, true);
    return intent;
  }
}

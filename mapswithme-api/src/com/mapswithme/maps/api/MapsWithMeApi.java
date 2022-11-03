/******************************************************************************
   Copyright (c) 2013, MapsWithMe GmbH All rights reserved.

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
 ******************************************************************************/
package com.mapswithme.maps.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.Nullable;

@SuppressWarnings("JavaDoc")
public final class MapsWithMeApi {

  /**
   * Most detailed level, buildings and trees are seen.
   */
  public static final double ZOOM_MAX = 19;
  /**
   * Least detailed level, continents are seen.
   */
  public static final double ZOOM_MIN = 1;

  private MapsWithMeApi() {
    // Utility class, not to be instantiated
  }

  public static void showMapsWithMeUrl(final Activity caller, final PendingIntent pendingIntent, final double zoomLevel, final String url) {
    final Uri uri = Uri.parse(url);
    final String[] latlon = uri.getQueryParameter("ll").split(",");
    final double lat = Double.parseDouble(latlon[0]);
    final double lon = Double.parseDouble(latlon[1]);
    final String name = uri.getQueryParameter("n");
    final String id = uri.getQueryParameter("id");

    showPointsOnMap(caller, name, zoomLevel, pendingIntent, new MWMPoint(lat, lon, name, id));
  }

  public static void sendRequest(final Activity caller, final MwmRequest request) {
    final Intent mwmIntent = request.toIntent(caller);

    if (isMapsWithMeInstalled(caller)) {
      // Match activity for intent
      final ActivityInfo aInfo = caller.getPackageManager().resolveActivity(mwmIntent, 0).activityInfo;
      mwmIntent.setClassName(aInfo.packageName, aInfo.name);
      caller.startActivity(mwmIntent);
    } else {
        // replace misleading/outdated maps.me dialog with c:geo warning message, see #13209
        new AlertDialog.Builder(caller)
            .setTitle("maps.me not found")
            .setMessage("This may be due to maps.me not being installed or maps.me being installed in a version > 12.0, which is no longer compatible to their integration API.\n\nSee our FAQ for details.")
            .setNeutralButton("Open FAQ", (dialog, which) -> {
                final Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://faq.cgeo.org#13209"));
                caller.startActivity(viewIntent);
                })
            .setPositiveButton(android.R.string.ok, ((dialog, which) -> { }))
            .show();
        // original code was:
        // (new DownloadMapsWithMeDialog(caller)).show();
    }
  }

  /**
   * Shows single point on the map.
   *
   * @param caller
   * @param lat
   * @param lon
   * @param name
   */
  public static void showPointOnMap(final Activity caller, final double lat, final double lon, final String name) {
    showPointsOnMap(caller, null, (PendingIntent) null, new MWMPoint(lat, lon, name));
  }

  /**
   * Shows single point on the map using specified zoom level in range from
   * {@link MapsWithMeApi#ZOOM_MIN} to {@link MapsWithMeApi#ZOOM_MAX}.
   *
   * @param caller
   * @param lat
   * @param lon
   * @param name
   * @param zoomLevel
   */
  @SuppressWarnings("unused")
  public static void showPointOnMap(final Activity caller, final double lat, final double lon, final String name, final double zoomLevel) {
    showPointsOnMap(caller, null, zoomLevel, null, new MWMPoint(lat, lon, name));
  }

  /**
   * Shows set of points on the map.
   *
   * @param caller
   * @param title
   * @param points
   */
  public static void showPointsOnMap(final Activity caller, final String title, final MWMPoint... points) {
    showPointsOnMap(caller, title, null, points);
  }

  /**
   * Shows set of points on the maps and allows MapsWithMeApplication to send
   * {@link PendingIntent} provided by client application.
   *
   * @param caller
   * @param title
   * @param pendingIntent
   * @param points
   */
  public static void showPointsOnMap(final Activity caller, @Nullable final String title, @Nullable final PendingIntent pendingIntent, final MWMPoint... points) {
    showPointsOnMap(caller, title, -1, pendingIntent, points);
  }

  private static void showPointsOnMap(final Activity caller, @Nullable final String title, final double zoomLevel, @Nullable final PendingIntent pendingIntent, final MWMPoint... points) {
    final MwmRequest request = new MwmRequest()
                                    .setTitle(title)
                                    .setZoomLevel(zoomLevel)
                                    .setPendingIntent(pendingIntent)
                                    .setPoints(points);
    sendRequest(caller, request);
  }

  public static void pickPoint(final Activity caller, final String title, final PendingIntent pi) {
    final MwmRequest request = new MwmRequest()
                                    .setTitle(title)
                                    .setPickPointMode(true)
                                    .setPendingIntent(pi);
    sendRequest(caller, request);
  }

  /**
   * Detects if any version (Lite, Pro) of MapsWithMe, which supports API calls
   * are installed on the device.
   *
   * @param context
   * @return
   */
  public static boolean isMapsWithMeInstalled(final Context context) {
    final Intent intent = new Intent(Const.ACTION_MWM_REQUEST);
    return context.getPackageManager().resolveActivity(intent, 0) != null;
  }
}

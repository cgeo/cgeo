package cgeo.geocaching.mapcommon;

import cgeo.geocaching.R;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.cgeodetail;
import cgeo.geocaching.mapinterfaces.ItemizedOverlayImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.UserOverlayItemImpl;

import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cgUsersOverlay extends ItemizedOverlayBase implements OverlayBase {

    private List<UserOverlayItemImpl> items = new ArrayList<UserOverlayItemImpl>();
    private Context context = null;
    private final Pattern patternGeocode = Pattern.compile("^(GC[A-Z0-9]+)(\\: ?(.+))?$", Pattern.CASE_INSENSITIVE);

    public cgUsersOverlay(ItemizedOverlayImpl ovlImplIn, Context contextIn) {
        super(ovlImplIn);
        populate();

        context = contextIn;
    }

    protected void updateItems(UserOverlayItemImpl item) {
        List<UserOverlayItemImpl> itemsPre = new ArrayList<UserOverlayItemImpl>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    public void updateItems(List<UserOverlayItemImpl> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (UserOverlayItemImpl item : itemsPre) {
            item.setMarker(boundCenter(item.getMarker(0)));
        }

        items.clear();

        if (itemsPre.size() > 0) {
            items = new ArrayList<UserOverlayItemImpl>(itemsPre);
        }

        setLastFocusedItemIndex(-1); // to reset tap during data change
        populate();
    }

    @Override
    public boolean onTap(int index) {
        try {
            if (items.size() <= index) {
                return false;
            }

            final UserOverlayItemImpl item = items.get(index);
            final cgUser user = item.getUser();

            // set action
            String action = null;
            String geocode = null;
            final Matcher matcherGeocode = patternGeocode.matcher(user.action.trim());

            if (user.action.length() == 0 || user.action.equalsIgnoreCase("pending")) {
                action = "Looking around";
            } else if (user.action.equalsIgnoreCase("tweeting")) {
                action = "Tweeting";
            } else if (matcherGeocode.find()) {
                if (matcherGeocode.group(1) != null) {
                    geocode = matcherGeocode.group(1).trim().toUpperCase();
                }
                if (matcherGeocode.group(3) != null) {
                    action = "Heading to " + geocode + " (" + matcherGeocode.group(3).trim() + ")";
                } else {
                    action = "Heading to " + geocode;
                }
            } else {
                action = user.action;
            }

            // set icon
            int icon = -1;
            if (user.client.equalsIgnoreCase("c:geo")) {
                icon = R.drawable.client_cgeo;
            } else if (user.client.equalsIgnoreCase("preCaching")) {
                icon = R.drawable.client_precaching;
            } else if (user.client.equalsIgnoreCase("Handy Geocaching")) {
                icon = R.drawable.client_handygeocaching;
            }

            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            if (icon > -1) {
                dialog.setIcon(icon);
            }
            dialog.setTitle(user.username);
            dialog.setMessage(action);
            dialog.setCancelable(true);
            if (StringUtils.isNotBlank(geocode)) {
                dialog.setPositiveButton(geocode + "?", new cacheDetails(geocode));
            }
            dialog.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            AlertDialog alert = dialog.create();
            alert.show();

            return true;
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgUsersOverlay.onTap: " + e.toString());
        }

        return false;
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        super.draw(canvas, mapView, false);
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {
        super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    @Override
    public UserOverlayItemImpl createItem(int index) {
        try {
            return items.get(index);
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgUsersOverlay.createItem: " + e.toString());
        }

        return null;
    }

    @Override
    public int size() {
        try {
            return items.size();
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgUsersOverlay.size: " + e.toString());
        }

        return 0;
    }

    private class cacheDetails implements DialogInterface.OnClickListener {

        private String geocode = null;

        public cacheDetails(String geocodeIn) {
            geocode = geocodeIn;
        }

        public void onClick(DialogInterface dialog, int id) {
            if (geocode != null) {
                Intent detailIntent = new Intent(context, cgeodetail.class);
                detailIntent.putExtra("geocode", geocode);
                context.startActivity(detailIntent);
            }

            dialog.cancel();
        }
    }
}

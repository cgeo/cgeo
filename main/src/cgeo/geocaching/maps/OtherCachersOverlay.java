package cgeo.geocaching.maps;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

public class OtherCachersOverlay extends AbstractItemizedOverlay {

    private List<OtherCachersOverlayItemImpl> items = new ArrayList<OtherCachersOverlayItemImpl>();
    private Context context = null;

    public OtherCachersOverlay(ItemizedOverlayImpl ovlImplIn, Context contextIn) {
        super(ovlImplIn);
        populate();

        context = contextIn;
    }

    protected void updateItems(OtherCachersOverlayItemImpl item) {
        final List<OtherCachersOverlayItemImpl> itemsPre = new ArrayList<OtherCachersOverlayItemImpl>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    public void updateItems(List<OtherCachersOverlayItemImpl> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (OtherCachersOverlayItemImpl item : itemsPre) {
            item.setMarker(boundCenter(item.getMarker(0)));
        }

        items.clear();

        if (itemsPre.size() > 0) {
            items = new ArrayList<OtherCachersOverlayItemImpl>(itemsPre);
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

            final OtherCachersOverlayItemImpl item = items.get(index);
            final Go4CacheUser user = item.getUser();

            final String geocode = user.getGeocode();
            final int icon = user.getIconId();

            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            if (icon > -1) {
                dialog.setIcon(icon);
            }
            dialog.setTitle(user.getUsername());
            dialog.setMessage(user.getAction());
            dialog.setCancelable(true);
            if (StringUtils.isNotBlank(geocode)) {
                dialog.setPositiveButton(geocode, new cacheDetails(geocode));
            }
            dialog.setNeutralButton(cgeoapplication.getInstance().getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            dialog.create().show();

            return true;
        } catch (Exception e) {
            Log.e("cgUsersOverlay.onTap: " + e.toString());
        }

        return false;
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        super.draw(canvas, mapView, false);
    }

    @Override
    public OtherCachersOverlayItemImpl createItem(int index) {
        try {
            return items.get(index);
        } catch (Exception e) {
            Log.e("cgUsersOverlay.createItem: " + e.toString());
        }

        return null;
    }

    @Override
    public int size() {
        try {
            return items.size();
        } catch (Exception e) {
            Log.e("cgUsersOverlay.size: " + e.toString());
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
                CacheDetailActivity.startActivity(context, geocode);
            }

            dialog.cancel();
        }
    }
}

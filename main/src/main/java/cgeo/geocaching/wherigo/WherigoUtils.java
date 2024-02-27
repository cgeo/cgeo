package cgeo.geocaching.wherigo;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.Container;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import cz.matejcik.openwig.ZonePoint;
import cz.matejcik.openwig.formats.CartridgeFile;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class WherigoUtils {

    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    private WherigoUtils() {
        //no instance
    }

    public static List<Action> getValidActions(final Thing thing) {
        final List<Action> result = new ArrayList<>();
        for (Object aObj : thing.actions) {
            final Action action = (Action) aObj;
            if (action.isEnabled() && action.getActor().visibleToPlayer()) {
                result.add(action);
            }
        }
        return result;
    }

    public static boolean isVisibleToPlayer(final EventTable et) {
        return et != null && et.isVisible() && (!(et instanceof Container) || ((Container) et).visibleToPlayer());
    }

    public static String eventTableToString(final EventTable et, final boolean longVersion) {
        if (et == null) {
            return "null";
        }
        final String sep = longVersion ? "\n" : ",";

        final StringBuilder msg = new StringBuilder();
        if (longVersion) {
            msg.append(et.getClass().getSimpleName() + ": ");
        }
        msg.append(et.name);
        if (longVersion) {
            msg.append(sep + et.description);
        }
        msg.append(sep + "vis:" + et.isVisible());
        if (longVersion) {
            msg.append(sep + "Has Media: " + (et.media != null));
            msg.append(sep + "Located: " + et.isLocated() + " (" + WherigoUtils.GP_CONVERTER.from(et.position) + ")");
        }

        if (et instanceof Container && longVersion) {
            final Container cnt = (Container) et;
            msg.append(sep + "visToPlayer:" + cnt.visibleToPlayer());
        }

        if (et instanceof Thing && longVersion) {
            final List<Action> actions = WherigoUtils.getValidActions((Thing) et);
            msg.append(sep + "Actions (" + actions.size() + "):");
            for (Action act : actions) {
                msg.append(sep + "* " + act.name + "(" + act.text + ", univ=" + act.isUniversal() + ")");
            }
        }
        if (et instanceof Zone) {
            final Zone z = (Zone) et;
            if (longVersion) {
                msg.append(sep + "Zone center:" + WherigoGame.GP_CONVERTER.from(z.bbCenter));
            }
            msg.append(", dist:" + z.distance + "m (");
            switch (z.contain) {
                case Zone.DISTANT:
                    msg.append("distant");
                    break;
                case Zone.PROXIMITY:
                    msg.append("near");
                    break;
                case Zone.INSIDE:
                    msg.append("inside");
                    break;
                default:
                    msg.append("unknown(" + z.contain + ")");
            }
            msg.append(")");
        }
        return msg.toString();
    }

    public static CartridgeFile readCartridge(final Uri uri) throws IOException {
        final FileInputStream fis = (FileInputStream) ContentStorage.get().openForRead(uri);
        return CartridgeFile.read(new WSeekableFile(fis.getChannel()), WherigoSaveFileHandler.get());
    }

    public static CartridgeFile safeReadCartridge(final Uri uri) {
        try {
            return readCartridge(uri);
        } catch (IOException ie) {
            Log.d("Couldn't read Cartridge '" + uri + "'", ie);
            return null;
        }
    }

    public static Bitmap getCartrdigeIcon(final CartridgeFile file) {
        if (file == null) {
            return null;
        }
        try {
            final byte[] iconData = file.getFile(file.iconId);
            return BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap getEventTableIcon(final EventTable et) {
        if (et == null) {
            return null;
        }
        try {
            final byte[] iconData = et.getIcon();
            return BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static void closeCartridgeQuietly(final CartridgeFile file) {
        if (file == null) {
            return;
        }
        try {
            final WSeekableFile seekableFile = (WSeekableFile) FieldUtils.readDeclaredField(file, "source", true);
            seekableFile.close();
        } catch (Exception e) {
            Log.w("WHEERIGO: Couldn't access seekable file inside cartridge", e);
        }
    }


}

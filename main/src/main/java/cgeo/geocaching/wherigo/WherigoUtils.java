package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.SimpleItemListView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.Container;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import cz.matejcik.openwig.ZonePoint;
import cz.matejcik.openwig.formats.CartridgeFile;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class WherigoUtils {

    public static final TextParam TP_OK_BUTTON = TextParam.text("ok").setImage(ImageParam.id(R.drawable.ic_menu_done));
    public static final TextParam TP_CANCEL_BUTTON = TextParam.text("cancel").setImage(ImageParam.id(R.drawable.ic_menu_cancel));


    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    public static class WherigoCartridgeInfo {
        public final ContentStorage.FileInformation fileInfo;
        public final String guid;
        public final CartridgeFile cartridgeFile;
        public final Bitmap icon;

        private WherigoCartridgeInfo(final ContentStorage.FileInformation fileInfo, final String guid, final CartridgeFile cartridgeFile, final Bitmap icon) {
            this.fileInfo = fileInfo;
            this.guid = guid;
            this.cartridgeFile = cartridgeFile;
            this.icon = icon;
        }
    }

    private WherigoUtils() {
        //no instance
    }

    public static List<Action> getActions(final Thing thing) {
        final List<Action> result = new ArrayList<>();
        for (Object aObj : thing.actions) {
            final Action action = (Action) aObj;
            if (Settings.enableFeatureWherigoDebug() || (action.isEnabled() && action.getActor().visibleToPlayer())) {
                result.add(action);
            }
        }
        return result;
    }

    public static String getActionText(final Action action) {
        if (action == null || action.text == null) {
            return "-";
        }
        return action.isEnabled() && action.getActor().visibleToPlayer() ? action.text : action.text + " (disabled)";
    }

    public static void callAction(final Thing thing, final Action action) {
        if (thing == null || action == null || !action.isEnabled() || !action.getActor().visibleToPlayer()) {
            return;
        }

        final String eventName = "On" + action.getName();

        if (action.hasParameter() && action.getActor() != thing) {
            Engine.callEvent(action.getActor(), eventName, thing);
        } else {
            Engine.callEvent(thing, eventName, null);
        }
    }

    public static <T> void setViewActions(final Iterable<T> actions, final SimpleItemListView view, final Function<T, TextParam> displayMapper, final Consumer<T> clickHandler) {
        final SimpleItemListModel<T> model = new SimpleItemListModel<>();
        model
            .setItems(actions)
            .setDisplayMapper(displayMapper, null, (ctx, parent) -> ViewUtils.createButton(ctx, parent, TextParam.text(""), true))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItemPadding(10, 0)
            .addSingleSelectListener(clickHandler);
        view.setModel(model);
        view.setVisibility(View.VISIBLE);
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
            final List<Action> actions = WherigoUtils.getActions((Thing) et);
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

    private static Bitmap getCartrdigeIcon(final CartridgeFile file) {
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

    @Nullable
    public static String getGuid(final ContentStorage.FileInformation fileInfo) {
        if (fileInfo == null || fileInfo.name == null || !fileInfo.name.endsWith(".gwc")) {
            return null;
        }
        final String guid = fileInfo.name.substring(0, fileInfo.name.length() - 4);
        final int idx = guid.indexOf("_");
        return idx <= 0 ? guid : guid.substring(0, idx);
    }

    public static WherigoCartridgeInfo getCartridgeInfo(final ContentStorage.FileInformation file, final boolean loadCartridgeFile, final boolean loadIcon) {
        if (file == null) {
            return null;
        }
        final String guid = getGuid(file);
        final CartridgeFile cf = loadCartridgeFile ? safeReadCartridge(file.uri) : null;
        final Bitmap icon = cf != null && loadIcon ? getCartrdigeIcon(cf) : null;
        closeCartridgeQuietly(cf);
        return new WherigoCartridgeInfo(file, guid, cf, icon);
    }

    public static List<WherigoCartridgeInfo> getAvailableCartridges(final Folder folder, final Predicate<WherigoCartridgeInfo> filter, final boolean loadCartridgeFile, final boolean loadIcon) {
        final List<ContentStorage.FileInformation> candidates = ContentStorage.get().list(folder).stream()
                .filter(fi -> fi.name.endsWith(".gwc")).collect(Collectors.toList());
        final List<WherigoCartridgeInfo> result = new ArrayList<>(candidates.size());
        for (ContentStorage.FileInformation candidate : candidates) {
            final WherigoCartridgeInfo info = getCartridgeInfo(candidate, loadCartridgeFile, loadIcon);
            if (info != null && (filter == null || filter.test(info))) {
                result.add(info);
            }
        }
        return result;
    }

    public static Map<String, Date> getAvailableSaveGames(@NonNull final ContentStorage.FileInformation cartridgeInfo) {
        return WherigoSaveFileHandler.getAvailableSaveFiles(cartridgeInfo.parentFolder, cartridgeInfo.name);
    }



}

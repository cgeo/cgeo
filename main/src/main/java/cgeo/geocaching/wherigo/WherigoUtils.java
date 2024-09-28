package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.SimpleItemListView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
import se.krka.kahlua.vm.LuaTable;

public final class WherigoUtils {

    public static final TextParam TP_OK_BUTTON = TextParam.text("ok").setImage(ImageParam.id(R.drawable.ic_menu_done));
    public static final TextParam TP_CANCEL_BUTTON = TextParam.text("cancel").setImage(ImageParam.id(R.drawable.ic_menu_cancel));


    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    private WherigoUtils() {
        //no instance
    }

    @SuppressWarnings("unchecked")
    public static <T extends EventTable> List<T> getListFromContainer(final LuaTable container, final Class<T> clazz, final Predicate<T> filter) {
        if (container == null) {
            return Collections.emptyList();
        }
        final List<T> result = new ArrayList<>();
        Object key = null;
        while ((key = container.next(key)) != null) {
            final Object o = container.rawget(key);
            if (!clazz.isInstance(o)) {
                continue;
            }
            final T t = (T) o;
            if (filter == null || filter.test(t)) {
                result.add(t);
            }
        }
        return result;
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

    public static Geopoint getZoneCenter(final Zone zone) {
        if (zone == null) {
            return Geopoint.ZERO;
        }
        if (zone.bbCenter != null && zone.bbCenter.latitude != 0d && zone.bbCenter.longitude != 0d) {
            return GP_CONVERTER.from(zone.bbCenter);
        }
        if (zone.points != null && zone.points.length > 0) {
            final List<Geopoint> geopoints = WherigoUtils.GP_CONVERTER.fromList(Arrays.asList(zone.points));
            return new Viewport.ContainingViewportBuilder().add(geopoints).getViewport().getCenter();
        }
        return Geopoint.ZERO;
    }

    public static Viewport getZonesViewport(final Collection<Zone> zones) {
        final Viewport.ContainingViewportBuilder builder = new Viewport.ContainingViewportBuilder();
        for (Zone zone : zones) {
            builder.add(WherigoUtils.GP_CONVERTER.fromList(Arrays.asList(zone.points)));
        }
        return builder.getViewport();
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
            msg.append(", dist:" + Units.getDistanceFromMeters((float) z.distance) + " (");
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

    public static List<WherigoCartridgeInfo> getAvailableCartridges(final Folder folder, final Predicate<WherigoCartridgeInfo> filter, final boolean loadCartridgeFile, final boolean loadIcon) {
        final List<ContentStorage.FileInformation> candidates = ContentStorage.get().list(folder).stream()
                .filter(fi -> fi.name.endsWith(".gwc")).collect(Collectors.toList());
        final List<WherigoCartridgeInfo> result = new ArrayList<>(candidates.size());
        for (ContentStorage.FileInformation candidate : candidates) {
            final WherigoCartridgeInfo info = WherigoCartridgeInfo.get(candidate, loadCartridgeFile, loadIcon);
            if (info != null && (filter == null || filter.test(info))) {
                result.add(info);
            }
        }
        return result;
    }

    public static Map<String, Date> getAvailableSaveGames(@NonNull final ContentStorage.FileInformation cartridgeInfo) {
        return WherigoSaveFileHandler.getAvailableSaveFiles(cartridgeInfo.parentFolder, cartridgeInfo.name);
    }

    public static Comparator<EventTable> getThingsComparator() {
        return CommonUtils.getNullHandlingComparator((t1, t2) -> {
            if (!t1.getClass().equals(t2.getClass())) {
                return t1.getClass().getName().compareTo(t2.getClass().getName());
            }
            if (isVisibleToPlayer(t1) != isVisibleToPlayer(t2)) {
                return isVisibleToPlayer(t1) ? -1 : 1;
            }
            if (t1 instanceof Zone) {
                final double dist1 = ((Zone) t1).distance;
                final double dist2 = ((Zone) t2).distance;
                final boolean dist1Valid = !Double.isNaN(dist1) && !Double.isInfinite(dist1);
                final boolean dist2Valid = !Double.isNaN(dist2) && !Double.isInfinite(dist2);
                if (dist1Valid != dist2Valid) {
                    return dist1Valid ? -1 : 1;
                }
                if (dist1Valid && Math.abs(dist1 - dist2) > 5) { // more than 5 meters
                    return dist1 < dist2 ? -1 : 1;
                }
            }
            final String name1 = t1.name == null ? "-" : t1.name.trim().toLowerCase(Locale.ROOT);
            final String name2 = t2.name == null ? "-" : t2.name.trim().toLowerCase(Locale.ROOT);
            return name1.compareTo(name2);
        }, true);
    }

    public static View createWherigoThingView(final Context ctx, final WherigoThingType type, final EventTable table, final View view) {

        final String name = table.name;
        CharSequence description = WherigoUtils.eventTableToString(table, false);
        if (WherigoUtils.isVisibleToPlayer(table)) {
            description = TextUtils.setSpan(description, new ForegroundColorSpan(Color.BLUE));
        }
        final Bitmap bm = WherigoUtils.getEventTableIcon(table);
        final ImageParam icon = bm == null ? ImageParam.id(type.getIconId()) : ImageParam.drawable(new BitmapDrawable(ctx.getResources(), bm));

        setViewValues(view, TextParam.text(name), TextParam.text(description), icon);
        return view;
    }

    public static View createWherigoThingTypeView(final WherigoThingType type, final View view) {

        final List<EventTable> things = type.getThingsForUserDisplay();
        final String name = type.toUserDisplayableString() + " (" + things.size() + ")";
        final CharSequence description = TextUtils.join(things, i -> {
            final String thingName = i.name;
            return !WherigoUtils.isVisibleToPlayer(i) ? thingName : TextUtils.setSpan(thingName, new ForegroundColorSpan(Color.BLUE));
        }, ", ");

        setViewValues(view, TextParam.text(name), TextParam.text(description), ImageParam.id(type.getIconId()));
        return view;
    }

    public static View getOrCreateItemView(final Context context, final View convertView, final ViewGroup parent) {
        final View view = convertView == null ? LayoutInflater.from(context).inflate(R.layout.cacheslist_item_select, parent, false) : convertView;
        ((TextView) view.findViewById(R.id.text)).setText(new SpannableString(""));
        return view;
    }

    private static void setViewValues(final View view, final TextParam name, final TextParam info, final ImageParam icon) {
        if (name != null) {
            name.applyTo(view.findViewById(R.id.text));
        }
        if (info != null) {
            info.applyTo(view.findViewById(R.id.info));
        }
        if (icon != null) {
            icon.applyTo(view.findViewById(R.id.icon));
        }
    }



}

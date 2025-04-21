package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigolistItemBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.Container;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import cz.matejcik.openwig.ZonePoint;
import cz.matejcik.openwig.formats.CartridgeFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import se.krka.kahlua.vm.LuaTable;

public final class WherigoUtils {

    public static final TextParam TP_OK_BUTTON = TextParam.id(R.string.ok).setAllCaps(true).setImage(ImageParam.id(R.drawable.ic_menu_done));
    public static final TextParam TP_CLOSE_BUTTON = TextParam.id(R.string.close).setAllCaps(true).setImage(ImageParam.id(R.drawable.ic_menu_done));
    public static final TextParam TP_CANCEL_BUTTON = TextParam.id(R.string.cancel).setAllCaps(true).setImage(ImageParam.id(R.drawable.ic_menu_cancel));

    private static final Pattern PATTERN_CARTRIDGE_LINK = Pattern.compile("https?" + Pattern.quote("://") + "(?:www\\.)?" + Pattern.quote("wherigo.com/cartridge/") + "(?:details|download)" + Pattern.quote(".aspx?") + "[Cc][Gg][Uu][Ii][Dd]=([-0-9a-zA-Z]*)");
    private static final String WHERIGO_URL_BASE = "https://www.wherigo.com/cartridge/download.aspx?CGUID=";

    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    private WherigoUtils() {
        //no instance
    }

    @NonNull
    private static Context wrap(@Nullable final Context ctx) {
        return ctx == null ? CgeoApplication.getInstance() : ctx;
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

    public static List<Action> getActions(final Thing thing, final boolean all) {
        final List<Action> result = new ArrayList<>();
        for (Object aObj : thing.actions) {
            final Action action = (Action) aObj;
            if (all || (action.isEnabled() && action.getActor().visibleToPlayer())) {
                result.add(action);
            }
        }
        return result;
    }

    public static String getActionText(final Action action) {
        if (action == null || action.text == null) {
            return "-";
        }
        return (action.isEnabled() && action.getActor().visibleToPlayer()) ? action.text : action.text + " (debug)";
    }

    public static void callAction(final Thing thing, final Action action, final Activity activity) {
        if (thing == null || action == null || !action.isEnabled() || !action.getActor().visibleToPlayer()) {
            return;
        }

        final String eventName = "On" + action.getName();

        if (action.hasParameter()) {
            if (action.getActor() == thing) {
                //choose a target for the action
                final List<EventTable> targets = getActionTargets(action);
                WherigoViewUtils.chooseThing(activity, targets, WherigoThingType.THING.getIconId(), "Choose Target", target -> {
                    WherigoDialogManager.get().display(new WherigoThingDialogProvider(target));
                    Engine.callEvent(action.getActor(), eventName, target);
                });
            } else {
                Engine.callEvent(action.getActor(), eventName, thing);
            }
        } else {
            Engine.callEvent(thing, eventName, null);
        }
    }

    public static List<EventTable> getActionTargets(final Action action) {
        final List<Thing> targets = new ArrayList<>();
        targets.addAll(WherigoGame.get().getItems());
        targets.addAll(WherigoGame.get().getInventory());
        return targets.stream().filter(t -> t.isVisible() && action.isTarget(t)).collect(Collectors.toList());
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

    public static String eventTableDebugInfo(final EventTable et) {
        if (et == null) {
            return "null";
        }

        final StringBuilder msg = new StringBuilder(et.getClass().getSimpleName() + ": " + et.name + "\n");
        msg.append("Description: ").append(et.description);
        msg.append("\n- vis:").append(et.isVisible());
        msg.append("\n- Has Media: ").append(et.media != null);
        msg.append("\n- Located: ").append(et.isLocated()).append(" (").append(WherigoUtils.GP_CONVERTER.from(et.position)).append(")");

        if (et instanceof Container) {
            final Container cnt = (Container) et;
            msg.append("\n- visToPlayer:").append(cnt.visibleToPlayer());
        }

        if (et instanceof Thing) {
            final List<Action> actions = WherigoUtils.getActions((Thing) et, true);
            msg.append("\n- Actions (").append(actions.size()).append("):");
            for (Action act : actions) {
                msg.append("\n  * ").append(act.name).append("(").append(WherigoUtils.getActionText(act)).append(", univ=").append(act.isUniversal()).append(")");
            }
        }
        if (et instanceof Zone) {
            final Zone z = (Zone) et;
            msg.append("\n- Zone center:").append(WherigoGame.GP_CONVERTER.from(z.bbCenter));
            msg.append(", dist:").append(getDisplayableDistanceTo(z)).append(" (");
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
                    msg.append("unknown(").append(z.contain).append(")");
            }
            msg.append(")");
        }
        return msg.toString();
    }

    public static CartridgeFile readCartridge(final Uri uri) throws IOException {
        final FileInputStream fis = (FileInputStream) ContentStorage.get().openForRead(uri);
        return CartridgeFile.read(new WSeekableFile(fis.getChannel()), WherigoSaveFileHandler.get());
    }

    public static Drawable getThingIconAsDrawable(final Context context, final EventTable et) {
        if (et == null) {
            return null;
        }
        final Media iconMedia = et.icon;
        if (iconMedia == null) {
            return null;
        }
        try {
            final byte[] iconData = Engine.mediaFile(iconMedia);
            return getDrawableForImageData(wrap(context), iconData);
        } catch (Exception e) {
            Log.w("WherigoUtils: problem reading icon data from event table " + et, e);
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

    public static Drawable getDrawableForImageData(@Nullable final Context ctx, final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        final Context realCtx = wrap(ctx);
        return new BitmapDrawable(realCtx.getResources(), BitmapFactory.decodeByteArray(data, 0, data.length));
    }

    public static String getDisplayableDistance(final Geopoint from, final Geopoint to) {
        if (from == null || to == null) {
            return "-";
        }
        final String distance = Units.getDistanceFromKilometers(from.distanceTo(to));
        final String direction = Units.getDirectionFromBearing(from.bearingTo(to));
        return distance + " " + direction;
    }

    public static String getDisplayableDistanceTo(final Zone zone) {
        if (zone == null) {
            return "?";
        }
        if (zone.contain == Zone.INSIDE) {
            return LocalizationUtils.getString(R.string.wherigo_zone_inside);
        }
        if (zone.nearestPoint != null) {
            final Geopoint current = new Geopoint(WherigoLocationProvider.get().getLatitude(), WherigoLocationProvider.get().getLongitude());
            return getDisplayableDistance(current, GP_CONVERTER.from(zone.nearestPoint)) + (zone.contain == Zone.PROXIMITY ? " (" + LocalizationUtils.getString(R.string.wherigo_zone_near) + ")" : "");
        }
        final Geopoint center = getZoneCenter(zone);
        final Geopoint current = LocationDataProvider.getInstance().currentGeo().getCoords();
        if (center != Geopoint.ZERO && current != Geopoint.ZERO) {
            return getDisplayableDistance(current, center);
        }
        return "?";
    }

    public static void ensureNoGameRunning(final Activity activity, final Runnable runOnClosedGameOnly) {
        if (!WherigoGame.get().isPlaying()) {
            if (runOnClosedGameOnly != null) {
                runOnClosedGameOnly.run();
            }
            return;
        }

        SimpleDialog.of(activity).setTitle(TextParam.id(R.string.wherigo_confirm_stop_running_game_title))
            .setMessage(TextParam.id(R.string.wherigo_confirm_stop_running_game_message, WherigoGame.get().getCartridgeName())).confirm(() -> {
                if (runOnClosedGameOnly != null) {

                    //ensure that action is performed after game is REALLY stopped! -> add a listener to OpenWIG END notification
                    final int[] listenerId = new int[1];
                    listenerId[0] = WherigoGame.get().addListener(notifyType -> {
                        if (notifyType.equals(WherigoGame.NotifyType.END)) {
                            runOnClosedGameOnly.run();
                            WherigoGame.get().removeListener(listenerId[0]);
                        }
                    });
                }
                WherigoGame.get().stopGame();
            });
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

    @NonNull
    public static List<String> getWherigoGuids(@Nullable final Geocache cache) {
        if (cache == null) {
            return Collections.emptyList();
        }
        return scanWherigoGuids(cache.getShortDescription() + " " + cache.getDescription());
    }

    @Nullable
    public static String getWherigoUrl(@Nullable final String guid) {
        return guid == null ? null : WHERIGO_URL_BASE + guid;
    }

    @NonNull
    public static List<String> scanWherigoGuids(@Nullable final String textToScan) {

        if (textToScan == null) {
            return Collections.emptyList();
        }

        final Matcher matcher = PATTERN_CARTRIDGE_LINK.matcher(textToScan);
        final List<String> cartridgeGuidsList = new ArrayList<>();
        final Set<String> cartridgeGuidsSet = new HashSet<>();
        while (matcher.find()) {
            final String guid = matcher.group(1);
            if (guid != null && !cartridgeGuidsSet.contains(guid)) {
                cartridgeGuidsSet.add(guid);
                cartridgeGuidsList.add(guid);
            }
        }
        return cartridgeGuidsList;
    }

    private static void addDeleteOptions(final Activity activity, final SimpleDialog.ItemSelectModel<WherigoSavegameInfo> model, final String cartridgeName, final Supplier<List<WherigoSavegameInfo>> refresher) {
        model.setItemActionIconMapper(item -> {
                    if (!item.isDeletableByUser()) {
                        return null;
                    }
                    return ImageParam.id(R.drawable.ic_menu_delete);
                })
                .setItemActionListener(item -> {
                    if (!item.isDeletableByUser()) {
                        return;
                    }
                    SimpleDialog.of(activity)
                            .setTitle(TextParam.id(R.string.wherigo_confirm_delete_savegame_slot_title))
                            .setMessage(TextParam.id(R.string.wherigo_confirm_delete_savegame_slot_message,
                                    cartridgeName, item.getUserDisplayableName()))
                            .confirm(() -> {
                                item.delete();
                                model.setItems(refresher.get());
                            });
                });
    }

    public static void loadGame(final Activity activity, final WherigoCartridgeInfo cartridgeInfo) {
        final SimpleDialog.ItemSelectModel<WherigoSavegameInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(WherigoSavegameInfo.getLoadableSavegames(cartridgeInfo.getFileInfo()))
            .setDisplayViewMapper(R.layout.wherigolist_item, (si, group, view) -> {
                final WherigolistItemBinding itemBinding = WherigolistItemBinding.bind(view);
                itemBinding.name.setText(si.getUserDisplayableName());
                itemBinding.description.setText(si.getUserDisplayableSaveDate());
                itemBinding.icon.setImageResource(R.drawable.ic_menu_upload);
            }, (item, itemGroup) -> item == null ? "" : item.getSavefileName())
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        WherigoUtils.addDeleteOptions(activity, model, cartridgeInfo.getName(), () -> WherigoSavegameInfo.getLoadableSavegames(cartridgeInfo.getFileInfo()));

        SimpleDialog.of(activity)
            .setTitle(TextParam.id(R.string.wherigo_choose_new_loadgame))
            .selectSingle(model, s -> WherigoGame.get().loadGame(cartridgeInfo.getFileInfo(), s));
    }

    public static void saveGame(final Activity activity) {
        final WherigoCartridgeInfo cartridgeInfo = WherigoGame.get().getCartridgeInfo();
        if (cartridgeInfo == null) {
            return;
        }

        final SimpleDialog.ItemSelectModel<WherigoSavegameInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(WherigoSavegameInfo.getSavegameSlots(cartridgeInfo.getFileInfo()))
            .setDisplayViewMapper(R.layout.wherigolist_item, (si, group, view) -> {

                final WherigolistItemBinding itemBinding = WherigolistItemBinding.bind(view);
                itemBinding.name.setText(si.getUserDisplayableName());
                itemBinding.description.setText(si.getUserDisplayableSaveDate());
                itemBinding.icon.setImageResource(R.drawable.ic_menu_save);
            }, (item, itemGroup) -> item == null ? "" : item.getSavefileName())
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        WherigoUtils.addDeleteOptions(activity, model, cartridgeInfo.getName(), () -> WherigoSavegameInfo.getSavegameSlots(cartridgeInfo.getFileInfo()));

        SimpleDialog.of(activity)
            .setTitle(TextParam.id(R.string.wherigo_choose_savegame_slot))
            .selectSingle(model, s -> {
                //allow entering/editing a savegame name, warn in case of overwriting an existing game
                CharSequence message = LocalizationUtils.getString(R.string.wherigo_save_game_message,
                        cartridgeInfo.getName(), s.getUserDisplayableNameId());
                if (s.isExistingSavefile()) {
                    final String warningMessage = LocalizationUtils.getString(R.string.wherigo_save_game_warning_overwrite_slot, s.getUserDisplayableSaveDate());
                    message = TextUtils.concat(message, "\n\n", TextUtils.setSpan(warningMessage, new StyleSpan(Typeface.BOLD)));
                }
                String initialValue = s.nameCustom;
                final String geocode = WherigoGame.get().getContextGeocode();
                if (geocode != null && (initialValue == null || !initialValue.contains(geocode))) {
                    initialValue = geocode + (initialValue == null ? "" : " " + initialValue);
                }
                SimpleDialog.of(activity)
                    .setTitle(TextParam.id(R.string.wherigo_save_game_title))
                    .setMessage(TextParam.text(message))
                    .input(new SimpleDialog.InputOptions()
                        .setAllowedChars("[-a-zA-Z0-9 ]")
                        .setLabel(LocalizationUtils.getString(R.string.wherigo_save_game_custom_name_label))
                        .setInitialValue(initialValue), nameCustom -> {
                        if (s.isExistingSavefile()) {
                            s.delete();
                        }
                        final WherigoSavegameInfo newSaveFile = WherigoSavegameInfo.getNewSavefile(s.cartridgeFileInfo, s.nameId, nameCustom);
                        WherigoGame.get().saveGame(newSaveFile);
                    });
        });
    }


    @Nullable
    public static String findGeocacheNameForGeocode(@Nullable final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }
        final Geocache cache = DataStore.loadCache(geocode.trim(), EnumSet.of(LoadFlags.LoadFlag.CACHE_BEFORE, LoadFlags.LoadFlag.DB_MINIMAL));
        if (cache == null || StringUtils.isBlank(cache.getName())) {
            return geocode.trim();
        }
        return geocode.trim() + ": " + cache.getName().trim();
    }

}

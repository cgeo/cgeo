package cgeo.geocaching.wherigo;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.WherigoDialogTitleViewBinding;
import cgeo.geocaching.databinding.WherigoMapQuickinfosBinding;
import cgeo.geocaching.databinding.WherigolistItemBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.ui.BadgeManager;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.SimpleItemListView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.html.HtmlUtils;
import static cgeo.geocaching.wherigo.WherigoUtils.getDisplayableDistance;
import static cgeo.geocaching.wherigo.WherigoUtils.getDrawableForImageData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Zone;
import org.apache.commons.lang3.StringUtils;

public final class WherigoViewUtils {

    private static final List<WherigoThingType> THING_TYPE_LIST = Arrays.asList(WherigoThingType.LOCATION, WherigoThingType.INVENTORY, WherigoThingType.TASK, WherigoThingType.ITEM);
    private static final List<WherigoThingType> THING_TYPE_LIST_DEBUG = Arrays.asList(WherigoThingType.LOCATION, WherigoThingType.INVENTORY, WherigoThingType.TASK, WherigoThingType.ITEM, WherigoThingType.THING);

    private WherigoViewUtils() {
        //no instances of Utility classes
    }

    public static void safeDismissDialog(final Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ex) {
            Log.w("Exception when dismissing dialog", ex);
        }
    }

    public static void ensureRunOnUi(final Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            AndroidRxUtils.runOnUi(r);
        }
    }

    public static AlertDialog createFullscreenDialog(@NonNull final Activity activity, @Nullable final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreen);
        final AlertDialog dialog = builder.create();
        if (!StringUtils.isBlank(title)) {
            final WherigoDialogTitleViewBinding titleBinding = WherigoDialogTitleViewBinding.inflate(LayoutInflater.from(activity));
            titleBinding.dialogTitle.setText(title);
            dialog.setCustomTitle(titleBinding.getRoot());
            titleBinding.dialogBack.setOnClickListener(v -> WherigoViewUtils.safeDismissDialog(dialog));
        }
        return dialog;

    }

    public static <T> void setViewActions(final Iterable<T> actions, final SimpleItemListView view, final int columnCount, final Function<T, TextParam> displayMapper, final Consumer<T> clickHandler) {
        final SimpleItemListModel<T> model = new SimpleItemListModel<>();
        model
            .setItems(actions)
            .setDisplayMapper((item, group) -> displayMapper.apply(item), null, (ctx, parent) -> ViewUtils.createButton(ctx, parent, TextParam.text(""), R.layout.button_wherigo_view))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItemPadding(10, 0)
            .setColumns(columnCount, null)
            .addSingleSelectListener(clickHandler);
        view.setModel(model);
        view.setVisibility(View.VISIBLE);
    }

    public static SimpleItemListModel<WherigoThingType> createThingTypeTable(final Activity activity, final SimpleItemListView target, final Consumer<EventTable> thingSelectAction) {
        //create the model
        final SimpleItemListModel<WherigoThingType> wherigoThingTypeModel = new SimpleItemListModel<WherigoThingType>()
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setDisplayViewMapper(R.layout.wherigolist_item, (type, group, view) -> {
                view.setBackground(null);
                final List<EventTable> things = type.getThingsForUserDisplay();
                final String name = type.toUserDisplayableString() + " (" + things.size() + ")";
                final CharSequence description = TextUtils.join(things, i -> {
                    final String thingName = i.name;
                    return WherigoUtils.isVisibleToPlayer(i) ? thingName : TextUtils.setSpan("(" + thingName + ")", new StyleSpan(Typeface.ITALIC));
                }, ", ");

                final WherigolistItemBinding typeBinding = WherigolistItemBinding.bind(view);
                typeBinding.name.setText(name);
                typeBinding.description.setText(description);
                ImageParam.id(type.getIconId()).applyTo(typeBinding.icon);
            }, (item, itemGroup) -> item == null  ? "" : item.toUserDisplayableString());

        target.setModel(wherigoThingTypeModel);
        wherigoThingTypeModel.addSingleSelectListener(type -> chooseThing(activity, type, thingSelectAction));
        updateThingTypeTable(wherigoThingTypeModel, target);
        return wherigoThingTypeModel;
    }

    public static void updateThingTypeTable(final SimpleItemListModel<WherigoThingType> model, final SimpleItemListView target) {
        model.setItems(WherigoGame.get().isDebugModeForCartridge() ? THING_TYPE_LIST_DEBUG : THING_TYPE_LIST);
        target.setVisibility(WherigoGame.get().isPlaying() ? View.VISIBLE : View.GONE);
    }

    private static void chooseThing(@NonNull final Activity activity, @NonNull final WherigoThingType thingType, final Consumer<EventTable> thingSelectAction) {
        final List<EventTable> things = thingType.getThingsForUserDisplay();
        if (things.isEmpty()) {
            return;
        }
        if (things.size() == 1) {
            thingSelectAction.accept(things.get(0));
            //displayThing(activity, things.get(0), false);
            return;
        }

        final SimpleDialog.ItemSelectModel<EventTable> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(things)
            .setDisplayMapper(item -> {
                CharSequence name = WherigoGame.get().toDisplayText(item.name);
                if (item instanceof Zone) {
                    name = TextUtils.concat(name, " (" + WherigoUtils.getDisplayableDistanceTo((Zone) item) + ")");
                }
                if (!WherigoUtils.isVisibleToPlayer(item)) {
                    name = TextUtils.setSpan("(" + name + ")", new StyleSpan(Typeface.ITALIC));
                }
                return TextParam.text(name);
            })
            .setDisplayIconMapper(item -> {
                final Drawable iconDrawable = WherigoUtils.getThingIconAsDrawable(activity, item);
                return iconDrawable == null ? ImageParam.id(thingType.getIconId()) : ImageParam.drawable(iconDrawable);
            })
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItemPadding(10, 0);

        SimpleDialog.of(activity)
            .setTitle(TextParam.text(thingType.toUserDisplayableString()))
            .selectSingle(model, thingSelectAction::accept);
    }

    public static void displayThing(@Nullable final Activity activity, @NonNull final EventTable thing, final boolean forceDisplay) {
        if (!forceDisplay && thing.hasEvent("OnClick")) {
            Log.iForce("Wherigo: discovered OnClick event on thing: " + thing);
            //this logic was taken over from WhereYouGo. Assumption is that a click event handles triggering of display itself
            Engine.callEvent(thing, "OnClick", null);
        } else if (activity != null) {
            WherigoDialogManager.displayDirect(activity, new WherigoThingDialogProvider(thing));
        } else {
            WherigoDialogManager.get().display(new WherigoThingDialogProvider(thing));
        }
    }

    public static Dialog getQuickViewDialog(final Activity activity) {
        final WherigoMapQuickinfosBinding binding = WherigoMapQuickinfosBinding.inflate(LayoutInflater.from(Dialogs.newContextThemeWrapper(activity)));
        final SimpleItemListModel<WherigoThingType> model = createThingTypeTable(activity, binding.wherigoThingTypeList, thing -> {
            displayThing(activity, thing, false);
        });
        binding.resumeDialog.setOnClickListener(v -> WherigoGame.get().unpauseDialog());
        BadgeManager.get().setBadge(binding.resumeDialog, false, -1);
        binding.cacheContextGotocache.setOnClickListener(v -> {
            CacheDetailActivity.startActivity(activity, WherigoGame.get().getContextGeocode());
        });
        binding.goToWherigo.setOnClickListener(v -> {
            WherigoActivity.start(activity, false);
        });

        final Runnable refreshGui = () -> {
            updateThingTypeTable(model, binding.wherigoThingTypeList);
            binding.resumeDialog.setVisibility(WherigoGame.get().dialogIsPaused() ? View.VISIBLE : View.GONE);
            binding.cacheContextBox.setVisibility(WherigoGame.get().getContextGeocode() != null ? View.VISIBLE : View.GONE);
            binding.cacheContextName.setText(WherigoGame.get().getContextGeocacheName());
        };

        final int wherigoListenerId = WherigoGame.get().addListener(nt -> {
            refreshGui.run();
        });
        refreshGui.run();

        final Dialog dialog = Dialogs.bottomSheetDialogWithActionbar(activity, binding.getRoot(), WherigoGame.get().getCartridgeName());
        dialog.setOnDismissListener(dl -> {
            WherigoGame.get().removeListener(wherigoListenerId);
        });

        return dialog;
    }

    public static void showErrorDialog(final Activity activity) {
        final String lastError = WherigoGame.get().getLastError();
        final String dialogErrorMessage = (lastError == null ? LocalizationUtils.getString(R.string.wherigo_error_game_noerror) :
                LocalizationUtils.getString(R.string.wherigo_error_game_error, lastError));

        final SimpleDialog dialog = SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.wherigo_error_title))
                .setMessage(TextParam.text(dialogErrorMessage).setMarkdown(true))
                .setPositiveButton(TextParam.id(R.string.copy));

        if (lastError != null) {
            dialog
                .setNeutralButton(TextParam.id(R.string.log_clear))
                .setNeutralAction(() -> WherigoGame.get().clearLastError());
        }
        dialog.show(() -> {
            ClipboardUtils.copyToClipboard(lastError);
            ActivityMixin.showToast(activity, R.string.copied_to_clipboard);
        });
        WherigoGame.get().clearLastErrorNotSeen();
    }

    /** adds badge logic to the given view to display current wherigo information as badges */
    public static void addWherigoBadgeNotifications(final View view) {
        if (view == null) {
            return;
        }
        final Runnable refreshRoutine = () -> {
            if (WherigoGame.get().isLastErrorNotSeen() || WherigoGame.get().dialogIsPaused()) {
                BadgeManager.get().setBadge(view, false, -1);
            } else {
                BadgeManager.get().removeBadge(view);
            }
        };

        final int listenerId = WherigoGame.get().addListener(nt -> refreshRoutine.run());
        ViewUtils.addDetachListener(view, v -> WherigoGame.get().removeListener(listenerId));
        refreshRoutine.run();
    }

    public static void executeForOneCartridge(final Activity activity, final List<String> wherigoGuids, final Consumer<String> guidAction) {
        if (wherigoGuids.isEmpty()) {
            return;
        }
        if (wherigoGuids.size() == 1) {
            guidAction.accept(wherigoGuids.get(0));
        } else {
            // prepare some data for display
            final int[] pos = {1};
            final Map<String, WherigoCartridgeInfo> cartridgeInfoMap = new HashMap<>();
            for (String guid : wherigoGuids) {
                cartridgeInfoMap.put(guid, null);
            }
            for (WherigoCartridgeInfo info : WherigoCartridgeInfo.getAvailableCartridges(cartridgeInfoMap::containsKey)) {
                cartridgeInfoMap.put(info.getCGuid(), info);
            }

            final List<Pair<String, Integer>> wherigoGuidPosList = wherigoGuids.stream().map(guid -> new Pair<>(guid, pos[0]++)).collect(Collectors.toList());
            final SimpleDialog.ItemSelectModel<Pair<String, Integer>> model = new SimpleDialog.ItemSelectModel<>();
            model
                .setItems(wherigoGuidPosList)
                .setDisplayMapper((l) -> TextParam.text(LocalizationUtils.getString(R.string.cache_wherigo_cartridge_not_downloaded) + " " + l.second))
                .setDisplayViewMapper(R.layout.wherigolist_item, (p, group, view) -> {
                    final WherigolistItemBinding binding = WherigolistItemBinding.bind(view);
                    final WherigoCartridgeInfo info = cartridgeInfoMap.get(p.first);
                    if (info != null) {
                        WherigoViewUtils.fillCartridgeSelectItem(binding, info);
                    } else {
                        //Cartridge not (yet) downloaded
                        binding.name.setText(LocalizationUtils.getString(R.string.cache_wherigo_cartridge_not_downloaded, "" + p.second));
                        binding.description.setText(p.first);
                        ImageParam.id(R.drawable.ic_menu_wherigo).applyTo(binding.icon);
                    }
                }, (p, group) -> {
                    final WherigoCartridgeInfo info = cartridgeInfoMap.get(p.first);
                    return info != null ? info.getName() : LocalizationUtils.getString(R.string.cache_wherigo_cartridge_not_downloaded, "" + p.second);
                })
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);
            SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.cache_multiple_wherigo_cartridges_title))
                .setMessage(TextParam.id(R.string.cache_multiple_wherigo_cartridges_message))
                .selectSingle(model, s -> guidAction.accept(s.first));
        }
    }

    /** fills an instance of Wherigo-List-Item-layout with info from a given cartridge */
    public static void fillCartridgeSelectItem(final WherigolistItemBinding binding, final WherigoCartridgeInfo info) {
        final String name = info.getCartridgeFile().name;
        final CharSequence description = "v" + info.getCartridgeFile().version + ", " + info.getCartridgeFile().author + ", " +
                getDisplayableDistance(LocationDataProvider.getInstance().currentGeo().getCoords(),
                        new Geopoint(info.getCartridgeFile().latitude, info.getCartridgeFile().longitude));
        final byte[] iconData = info.getIconData();
        final ImageParam icon = iconData == null ? ImageParam.id(R.drawable.ic_menu_wherigo) :
                ImageParam.drawable(getDrawableForImageData(null, iconData));

        binding.name.setText(name);
        binding.description.setText(description);
        icon.applyTo(binding.icon);
    }

    /** replaces found WherigoURLs with link to own wherigo player */
    public static void htmlReplaceWherigoClickAction(final Activity activity, final String geocode, final Spannable spannable) {

        HtmlUtils.replaceUrlClickAction(spannable, (span, spn, start, end) -> !WherigoUtils.scanWherigoGuids(span.getURL()).isEmpty(), span -> {
            final String guid = WherigoUtils.scanWherigoGuids(span.getURL()).get(0);
            WherigoActivity.startForGuid(activity, guid, geocode, false);
        });
    }

}

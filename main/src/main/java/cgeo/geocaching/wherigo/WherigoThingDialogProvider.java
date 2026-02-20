package cgeo.geocaching.wherigo;

import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.unifiedmap.DefaultMap;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.TranslationUtils;
import cgeo.geocaching.wherigo.openwig.Action;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.Thing;
import cgeo.geocaching.wherigo.openwig.Zone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WherigoThingDialogProvider implements IWherigoDialogProvider {

    private static final Set<Dialog> openThingDialogs = Collections.synchronizedSet(new HashSet<>());

    private final EventTable eventTable;

    private enum ThingAction {
        DISPLAY_ON_MAP(TextParam.id(R.string.caches_on_map).setAllCaps(true).setImage(ImageParam.id(R.drawable.ic_menu_mapmode))),
        COMPASS(TextParam.id(R.string.wherigo_zone_navigate_compass).setAllCaps(true).setImage(ImageParam.id(R.drawable.ic_menu_compass))),
        COPY_CENTER(TextParam.id(R.string.wherigo_zone_copy_coordinates).setAllCaps(true).setImage(ImageParam.id(R.drawable.ic_menu_copy))),
        LOCATE_ON_CENTER(TextParam.id(R.string.wherigo_locate_on_center).setAllCaps(true).setImage(ImageParam.id(R.drawable.map_followmylocation_btn))),
        CLOSE(WherigoUtils.TP_CLOSE_BUTTON);

        private final TextParam tp;

        ThingAction(final TextParam tp) {
            this.tp = tp;
        }

        public TextParam getTextParam() {
            return tp;
        }
    }

    public static void closeAllThingDialogs() {
        synchronized (openThingDialogs) {
            for (Dialog d : openThingDialogs) {
                WherigoViewUtils.safeDismissDialog(d);
            }
        }
    }

    public WherigoThingDialogProvider(final EventTable et) {
        this.eventTable = et;
    }

    @Override
    public Dialog createAndShowDialog(final Activity activity, final IWherigoDialogControl control) {
        final WherigoThingDetailsBinding binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = WherigoViewUtils.createFullscreenDialog(activity, "--", binding.getRoot());

        //external translator
        TranslationUtils.registerTranslation(activity, binding.translationExternal, () ->
            TranslationUtils.prepareForTranslation(eventTable.name, eventTable.description));

        refreshGui(activity, control, binding);
        control.setOnGameNotificationListener((d, nt) -> {
            if (nt == WherigoGame.NotifyType.REFRESH) {
                refreshGui(activity, control, binding);
            }
        });
        openThingDialogs.add(dialog);
        control.setOnDismissListener(d -> openThingDialogs.remove(dialog));

        dialog.show();
        return dialog;
    }

    @Override
    public boolean canRefresh(final IWherigoDialogProvider otherDialog) {
        //if a "thing" dialog is open and should be opened again for the same thing
        //-> then just refresh, do not close the old dialog just to open a new one for same thing
        //-> this enables Wherigo Cartridges to have media animations, see #17439
        return otherDialog instanceof WherigoThingDialogProvider &&
            ((WherigoThingDialogProvider) otherDialog).eventTable == this.eventTable;
    }

    private void refreshGui(final Activity activity, final IWherigoDialogControl control, final WherigoThingDetailsBinding binding) {
        TextParam.text(WherigoUtils.eventTableDebugInfo(eventTable)).setMarkdown(true).applyTo(binding.debugInfo, false, true);
        binding.debugBox.setVisibility(WherigoGame.get().isDebugModeForCartridge() ? View.VISIBLE : View.GONE);
        binding.headerInformation.setVisibility(eventTable instanceof Zone ? View.VISIBLE : View.GONE);
        if (eventTable instanceof Zone) {
            binding.headerInformation.setText(WherigoUtils.getDisplayableDistanceTo((Zone) eventTable));
        }
        //media
        binding.media.setMedia((Media) eventTable.table.rawget("Media"));

        //title
        control.setTitle(eventTable.name);
        //description
        WherigoViewUtils.setSelectableTextWithClickableLinks(binding.description, WherigoGame.get().toDisplayText(eventTable.description));

        //actions
        refreshActionList(activity, control, binding);
    }

    private void refreshActionList(final Activity activity, final IWherigoDialogControl control, final WherigoThingDetailsBinding binding) {

        //"actions" will be filled with instance of both "Action" and "ThingAction"
        final List<Object> actions = new ArrayList<>();
        if (eventTable instanceof Thing) {
            actions.addAll(WherigoUtils.getActions((Thing) eventTable, WherigoGame.get().isDebugModeForCartridge()));
        }
        if (eventTable instanceof Zone) {
            actions.add(ThingAction.DISPLAY_ON_MAP);
            actions.add(ThingAction.COMPASS);
            actions.add(ThingAction.COPY_CENTER);
            if (WherigoGame.get().isDebugModeForCartridge()) {
                actions.add(ThingAction.LOCATE_ON_CENTER);
            }
        }
        actions.add(ThingAction.CLOSE);

        WherigoViewUtils.setViewActions(actions, binding.dialogActionlist, 1,
                a -> a instanceof Action ?
                    TextParam.text(WherigoUtils.getUserDisplayableActionText((Action) a)).setImage(ImageParam.id(R.drawable.settings_nut)) :
                    ((ThingAction) a).getTextParam(),
                item -> {
                    if (item instanceof Action) {
                        WherigoUtils.callAction((Thing) eventTable, (Action) item, activity);
                        refreshGui(activity, control, binding);
                        return;
                    }
                    final ThingAction thingAction = (ThingAction) item;
                    switch (thingAction) {
                        case DISPLAY_ON_MAP:
                            control.dismiss();
                            DefaultMap.startActivityWherigoMap(activity, WherigoUtils.getZonesViewport(Collections.singleton((Zone) eventTable), false), eventTable.name, WherigoUtils.getZoneCenter((Zone) eventTable));
                            break;
                        case COMPASS:
                            control.dismiss();
                            CompassActivity.startActivityPoint(activity, WherigoUtils.getNearestPointTo((Zone) eventTable), eventTable.name);
                            break;
                        case COPY_CENTER:
                            ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(WherigoUtils.getZoneCenter((Zone) eventTable).toString()));
                            ViewUtils.showShortToast(activity, R.string.clipboard_copy_ok);
                            break;
                        case LOCATE_ON_CENTER:
                            control.dismiss();
                            final Geopoint center = WherigoUtils.getZoneCenter((Zone) eventTable);
                            WherigoLocationProvider.get().setFixedLocation(center);
                            break;
                        case CLOSE:
                        default:
                            control.dismiss();
                    }
                }
        );
    }

}

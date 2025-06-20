package cgeo.geocaching.wherigo;

import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.ClipboardUtils;
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
import java.util.List;

public class WherigoThingDialogProvider implements IWherigoDialogProvider {

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


    public WherigoThingDialogProvider(final EventTable et) {
        this.eventTable = et;
    }

    @Override
    public Dialog createAndShowDialog(final Activity activity, final IWherigoDialogControl control) {
        final AlertDialog dialog = WherigoViewUtils.createFullscreenDialog(activity, eventTable.name);
        final WherigoThingDetailsBinding binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        dialog.setView(binding.getRoot());

        refreshGui(activity, control, binding);
        control.setOnGameNotificationListener((d, nt) -> refreshGui(activity, control, binding));

        dialog.show();
        return dialog;
    }

    private void refreshGui(final Activity activity, final IWherigoDialogControl control, final WherigoThingDetailsBinding binding) {
        TextParam.text(WherigoUtils.eventTableDebugInfo(eventTable)).setMarkdown(true).applyTo(binding.debugInfo, false, true);
        binding.debugBox.setVisibility(WherigoGame.get().isDebugModeForCartridge() ? View.VISIBLE : View.GONE);
        binding.headerInformation.setVisibility(eventTable instanceof Zone ? View.VISIBLE : View.GONE);
        if (eventTable instanceof Zone) {
            binding.headerInformation.setText(WherigoUtils.getDisplayableDistanceTo((Zone) eventTable));
        }
        //description and media
        binding.media.setMedia((Media) eventTable.table.rawget("Media"));
        ViewUtils.setIfDiffers(binding.description, WherigoGame.get().toDisplayText(eventTable.description));

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
                    TextParam.text(WherigoUtils.getActionText((Action) a)).setImage(ImageParam.id(R.drawable.settings_nut)) :
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
                            DefaultMap.startActivityWherigoMap(activity, WherigoUtils.getZonesViewport(Collections.singleton((Zone) eventTable)), eventTable.name, WherigoUtils.getZoneCenter((Zone) eventTable));
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

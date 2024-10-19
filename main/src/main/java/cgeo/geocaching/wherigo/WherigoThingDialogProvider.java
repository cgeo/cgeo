package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;

public class WherigoThingDialogProvider implements IWherigoDialogProvider {

    private final EventTable eventTable;
    private WherigoThingDetailsBinding binding;

    private WeakReference<Activity> weakActivity;
    private WeakReference<Dialog> weakDialog;

    private enum ThingAction {
        DISPLAY_ON_MAP(TextParam.id(R.string.caches_on_map).setImage(ImageParam.id(R.drawable.ic_menu_mapmode))),
        LOCATE_ON_CENTER(TextParam.id(R.string.wherigo_locate_on_center).setImage(ImageParam.id(R.drawable.map_followmylocation_btn))),
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
    public Dialog createDialog(final Activity activity, final Consumer<Boolean> resultSetter) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreen);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setTitle(eventTable.name);
        dialog.setView(binding.getRoot());
        weakActivity = new WeakReference<>(activity);
        weakDialog = new WeakReference<>(dialog);

        refreshGui();

        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        refreshGui();
    }

    private void refreshGui() {
        TextParam.text(WherigoUtils.eventTableDebugInfo(eventTable)).setMarkdown(true).applyTo(binding.debugInfo);
        binding.debugBox.setVisibility(WherigoGame.get().isDebugModeForCartridge() ? View.VISIBLE : View.GONE);
        binding.headerInformation.setVisibility(eventTable instanceof Zone ? View.VISIBLE : View.GONE);
        if (eventTable instanceof Zone) {
            binding.headerInformation.setText(WherigoUtils.getDisplayableDistanceTo((Zone) eventTable));
        }
        //description and media
        binding.media.setMedia((Media) eventTable.table.rawget("Media"));
        binding.description.setText(WherigoGame.get().toDisplayText(eventTable.description));

        //actions
        refreshActionList();
    }

    private void refreshActionList() {

        //"actions" will be filled with instance of both "Action" and "ThingAction"
        final List<Object> actions = new ArrayList<>();
        if (eventTable instanceof Thing) {
            actions.addAll(WherigoUtils.getActions((Thing) eventTable, false));
        }
        if (eventTable instanceof Zone) {
            actions.add(ThingAction.DISPLAY_ON_MAP);
            if (WherigoGame.get().isDebugModeForCartridge()) {
                actions.add(ThingAction.LOCATE_ON_CENTER);
            }
        }
        actions.add(ThingAction.CLOSE);

        WherigoUtils.setViewActions(actions, binding.dialogActionlist,
                a -> a instanceof Action ?
                        TextParam.text(WherigoUtils.getActionText((Action) a)).setImage(ImageParam.id(R.drawable.settings_nut)) :
                        ((ThingAction) a).getTextParam(),
                item -> {
                    if (item instanceof Action) {
                        WherigoUtils.callAction((Thing) eventTable, (Action) item);
                        refreshGui();
                        return;
                    }
                    final ThingAction thingAction = (ThingAction) item;
                    switch (thingAction) {
                        case DISPLAY_ON_MAP:
                            closeDialog();
                            final Activity activity = weakActivity.get();
                            if (activity != null) {
                                DefaultMap.startActivityViewport(activity, WherigoUtils.getZonesViewport(Collections.singleton((Zone) eventTable)));
                            }
                            break;
                        case LOCATE_ON_CENTER:
                            closeDialog();
                            final Geopoint center = WherigoUtils.getZoneCenter((Zone) eventTable);
                            WherigoLocationProvider.get().setFixedLocation(center);
                            break;
                        case CLOSE:
                        default:
                            closeDialog();
                    }
                }
        );
    }

    private void closeDialog() {
        WherigoDialogManager.dismissDialog(weakDialog == null ? null : weakDialog.get());
    }

}

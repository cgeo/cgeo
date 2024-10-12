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

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;

public class WherigoThingDialogProvider implements IWherigoDialogProvider {

    private final EventTable eventTable;
    private WherigoThingDetailsBinding binding;

    private WeakReference<Activity> weakActivity;

    public WherigoThingDialogProvider(final EventTable et) {
        this.eventTable = et;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreen);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setTitle(eventTable.name);
        dialog.setView(binding.getRoot());
        weakActivity = new WeakReference<>(activity);

        refreshGui();

        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        refreshGui();
    }

    private void refreshGui() {
        TextParam.text("Debug Information:\n\n" + WherigoUtils.eventTableDebugInfo(eventTable)).setMarkdown(true).applyTo(binding.debugInfo);
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

        //get actions
        final List<Object> actions = new ArrayList<>();
        if (eventTable instanceof Thing) {
            actions.addAll(WherigoUtils.getActions((Thing) eventTable, false));
        }
        if (eventTable instanceof Zone) {
            actions.add("Display on Map");
            if (WherigoGame.get().isDebugModeForCartridge()) {
                actions.add("Locate on center");
            }
        }
        actions.add("Close");

        WherigoUtils.setViewActions(actions, binding.dialogActionlist,
                a -> a instanceof Action ?
                        TextParam.text(WherigoUtils.getActionText((Action) a)).setImage(ImageParam.id(R.drawable.settings_nut)) :
                        TextParam.text(a.toString()).setImage(ImageParam.id(R.drawable.ic_menu_done)),
                item -> {
                    if (item instanceof Action) {
                        WherigoUtils.callAction((Thing) eventTable, (Action) item);
                        refreshGui();
                    } else if (item.toString().startsWith("Display")) {
                        WherigoDialogManager.get().clear();
                        final Activity activity = weakActivity.get();
                        if (activity != null) {
                            DefaultMap.startActivityViewport(activity, WherigoUtils.getZonesViewport(Collections.singleton((Zone) eventTable)));
                        }
                    } else if (item.toString().startsWith("Locate")) {
                        WherigoDialogManager.get().clear();
                        final Geopoint center = WherigoUtils.getZoneCenter((Zone) eventTable);
                        WherigoLocationProvider.get().setFixedLocation(center);
                    } else if (item.toString().equals("Close")) {
                        WherigoDialogManager.get().clear();
                    }
                }
        );
    }

}

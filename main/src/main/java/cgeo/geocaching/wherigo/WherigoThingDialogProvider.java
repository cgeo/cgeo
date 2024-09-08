package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;

public class WherigoThingDialogProvider implements IWherigoDialogProvider {

    private final EventTable eventTable;
    private WherigoThingDetailsBinding binding;

    public WherigoThingDialogProvider(final EventTable et) {
        this.eventTable = et;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());
        binding.layoutDetailsTextViewDescription.setText(WherigoUtils.eventTableToString(eventTable, true));

        binding.media.setMedia((Media) eventTable.table.rawget("Media"));

        final List<Object> actions = new ArrayList<>();
        if (eventTable instanceof Thing) {
            actions.addAll(WherigoUtils.getActions((Thing) eventTable));
        }
        if (eventTable instanceof Zone) {
            actions.add("Display on Map");
            actions.add("Locate on center (10s)");
        }
        actions.add("Close");


        WherigoUtils.setViewActions(actions, binding.dialogActionlist,
            a -> a instanceof Action ?
                TextParam.text(WherigoUtils.getActionText((Action) a)).setImage(ImageParam.id(R.drawable.settings_nut)) :
                TextParam.text(a.toString()).setImage(ImageParam.id(R.drawable.ic_menu_done)),
            item -> {
                WherigoDialogManager.get().clear();
                if (item instanceof Action) {
                    WherigoUtils.callAction((Thing) eventTable, (Action) item);
                } else if (item.toString().startsWith("Display")) {
                    DefaultMap.startActivityViewport(activity, WherigoUtils.getZonesViewport(Collections.singleton((Zone) eventTable)));
                } else if (item.toString().startsWith("Locate")) {
                    final Geopoint center = WherigoUtils.getZoneCenter((Zone) eventTable);
                    WherigoLocationProvider.get().setFixedLocation(center);
                    AndroidRxUtils.networkScheduler.createWorker().schedule(() -> WherigoLocationProvider.get().setFixedLocation(null), 10, TimeUnit.SECONDS);
                }
            }
        );

        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        binding.layoutDetailsTextViewDescription.setText(WherigoUtils.eventTableToString(eventTable, true));
    }

}

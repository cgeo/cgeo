package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingListBinding;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Collections;
import java.util.List;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;

public class WherigoThingListDialogProvider implements IWherigoDialogProvider {

    private final WherigoThingType thingType;
    private final SimpleItemListModel<EventTable> model = new SimpleItemListModel<>();

    public WherigoThingListDialogProvider(final WherigoThingType type) {
        this.thingType = type;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        final WherigoThingListBinding binding = WherigoThingListBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());

        model
            .setDisplayViewMapper((item, itemGroup, ctx, view, parent) ->
                WherigoUtils.createWherigoThingView(activity, thingType, item, WherigoUtils.getOrCreateItemView(activity, view, parent)),
                (item, itemGroup) -> item == null || item.name == null ? "" : item.name)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItemPadding(10, 0)
            .addSingleSelectListener(item -> {
                if (item.hasEvent("OnClick")) {
                    Engine.callEvent(item, "OnClick", null);
                } else {
                    WherigoDialogManager.get().display(new WherigoThingDialogProvider(item));
                }
            });

        binding.wherigoThingsList.setModel(model);
        binding.wherigoThingsList.setVisibility(View.VISIBLE);

        WherigoUtils.setViewActions(Collections.singletonList("Close"), binding.dialogActionlist,
            a -> TextParam.text(a).setImage(ImageParam.id(R.drawable.ic_menu_done)),
            i -> WherigoDialogManager.get().clear());

        refreshGui();

        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        refreshGui();
    }

    private void refreshGui() {
        final List<EventTable> items = thingType.getThingsForUserDisplay();
        model.setItems(items);
    }

}

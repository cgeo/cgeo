package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingListBinding;
import cgeo.geocaching.databinding.WherigolistItemBinding;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Collections;
import java.util.List;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Zone;

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
        dialog.setTitle(thingType.toUserDisplayableString());
        dialog.setView(binding.getRoot());

        model
            .setDisplayViewMapper(R.layout.wherigolist_item, (table, group, view) -> {
                final String name = table.name;
                CharSequence description = WherigoGame.get().toDisplayText(table.name);
                if (table instanceof Zone) {
                    description += " (" + WherigoUtils.getDisplayableDistanceTo((Zone) table) + ")";
                }
                if (WherigoUtils.isVisibleToPlayer(table)) {
                    description = TextUtils.setSpan(description, new ForegroundColorSpan(Color.BLUE));
                }
                final Drawable iconDrawable = WherigoUtils.getThingIconAsDrawable(view.getContext(), table);
                final ImageParam icon = iconDrawable == null ? ImageParam.id(thingType.getIconId()) : ImageParam.drawable(iconDrawable);

                final WherigolistItemBinding itemBinding = WherigolistItemBinding.bind(view);
                itemBinding.name.setText(name);
                itemBinding.description.setText(description);
                icon.applyTo(itemBinding.icon);
            }, (item, itemGroup) -> item == null || item.name == null ? "" : item.name)
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

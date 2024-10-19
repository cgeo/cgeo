package cgeo.geocaching.wherigo;

import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.Drawable;

import java.util.List;
import java.util.function.Consumer;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Zone;

public class WherigoThingListDialogProvider implements IWherigoDialogProvider {

    private final WherigoThingType thingType;
    private final SimpleDialog.ItemSelectModel<EventTable> model = new SimpleDialog.ItemSelectModel<>();

    public WherigoThingListDialogProvider(final WherigoThingType type) {
        this.thingType = type;
    }

    @Override
    public Dialog createAndShowDialog(final Activity activity, final Consumer<Boolean> resultSetter) {

        final SimpleDialog dialog = SimpleDialog.of(activity)
            .setTitle(TextParam.text(thingType.toUserDisplayableString()));

        model
            .setDisplayMapper(item -> {
                CharSequence name = WherigoGame.get().toDisplayText(item.name);
                if (item instanceof Zone) {
                    name = TextUtils.concat(name, " (" + WherigoUtils.getDisplayableDistanceTo((Zone) item) + ")");
                }
                return TextParam.text(name);
            })
            .setDisplayIconMapper(item -> {
                final Drawable iconDrawable = WherigoUtils.getThingIconAsDrawable(activity, item);
                return iconDrawable == null ? ImageParam.id(thingType.getIconId()) : ImageParam.drawable(iconDrawable);
            })
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItemPadding(10, 0)
            .addSingleSelectListener(item -> {
                if (item.hasEvent("OnClick")) {
                    Engine.callEvent(item, "OnClick", null);
                } else {
                    WherigoDialogManager.displayDirect(activity, new WherigoThingDialogProvider(item));
                }
            });

        dialog.selectSingle(model, item -> {
            if (item.hasEvent("OnClick")) {
                Engine.callEvent(item, "OnClick", null);
            } else {
                WherigoDialogManager.get().display(new WherigoThingDialogProvider(item));
            }
        });

        refreshGui();

        return dialog.getDialog();
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

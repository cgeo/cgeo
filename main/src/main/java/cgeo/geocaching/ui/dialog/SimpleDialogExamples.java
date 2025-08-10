package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Helper class to test real dialogs */
public class SimpleDialogExamples {

    private enum Example {
        SELECT_SINGLE_DIRECT,
        SEELECT_SINGLE_RADIO,
        SELECT_MULTIPLE
    }

    private static final ImageParam[] IMAGES = new ImageParam[] {
        ImageParam.emoji(EmojiUtils.RED_FLAG), ImageParam.id(R.drawable.ic_menu_myposition), ImageParam.id(CacheType.MULTI.iconId)
    };

    private SimpleDialogExamples() {
        //no instances
    }

    public static void createTestDialog(final Context ctx) {
        final SimpleDialog.ItemSelectModel<Example> model = new SimpleDialog.ItemSelectModel<>();
        model.setItems(Arrays.asList(Example.values()));
        SimpleDialog.ofContext(ctx)
            .setTitle(TextParam.text("SimpleDialog Tests"))
            .setMessage(TextParam.text("Choose your test dialog"))
            .selectSingle(model, test -> executeTest(ctx, test));
    }

    private static void executeTest(final Context ctx, final Example test) {
        final SimpleDialog dialog = SimpleDialog.ofContext(ctx).setTitle(TextParam.text("TEST: " + test));
        SimpleDialog.ItemSelectModel<String> model = null;
        switch (test) {
            case SELECT_SINGLE_DIRECT:
                model = createSimpleDialogModel(ctx, SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);
                dialog.selectSingle(model, item -> display(ctx, "Selected", "Selected: " + item));
                break;
            case SEELECT_SINGLE_RADIO:
                model = createSimpleDialogModel(ctx, SimpleItemListModel.ChoiceMode.SINGLE_RADIO);
                dialog.selectSingle(model, item -> display(ctx, "Selected", "Selected: " + item));
                break;
            case SELECT_MULTIPLE:
            default:
                model = createSimpleDialogModel(ctx, SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX);
                dialog.selectMultiple(model, item -> display(ctx, "Selected", "Selected: " + item));
                break;
        }
    }

    private static SimpleDialog.ItemSelectModel<String> createSimpleDialogModel(final Context ctx, final SimpleItemListModel.ChoiceMode choice) {
        final SimpleDialog.ItemSelectModel<String> model = new SimpleDialog.ItemSelectModel<>();
        model.activateGrouping(SimpleDialogExamples::getGroup)
            .setGroupGroupMapper(SimpleDialogExamples::getGroup)
            .setGroupPruner(gi -> gi.getSize() >= 2);
        model.setItemActionListener(item -> display(ctx, "Action clicked", "Action clicked on: " + item));
        model.setDisplayMapper(item -> {
           final TextParam tp = TextParam.text(item);
           final String id = TextUtils.getMatch(item, Pattern.compile("IMG:([0-9]+)"), null);
           if (id != null) {
               tp.setImage(IMAGES[Integer.parseInt(id)]);
           }
           return tp;
        }).setDisplayIconMapper(item -> {
            final String id = TextUtils.getMatch(item, Pattern.compile("ICON:([0-9]+)"), null);
            if (id != null) {
                return IMAGES[Integer.parseInt(id)];
            }
            return null;
        });
        model.setChoiceMode(choice);

        final List<String> items = new ArrayList<>();
        final List<String> selectedItems = new ArrayList<>();

        //groups
        for (int i = 0; i < 5; i++) {
            items.add("Groups:5:it " + i);
        }
        for (int i = 0; i < 1; i++) {
            items.add("Groups:1:it " + i);
        }

        //preselected
        for (int i = 0; i < 6; i++) {
            final boolean isPre = i % 2 == 0;
            final String item = "PreSel:It " + i + "(PRE: " + isPre + ")";
            items.add(item);
            if (isPre) {
                selectedItems.add(item);
            }
        }

        //images and icons
        for (int i = 0; i < 6; i++) {
            items.add("Images:It " + i + " (IMG:" + (i % IMAGES.length) + ")");
        }
        for (int i = 0; i < 6; i++) {
            items.add("Icons:It " + i + " (ICON:" + (i % IMAGES.length) + ")");
        }

        model.setItems(items);
        model.setSelectedItems(selectedItems);
        return model;
    }

    private static String getGroup(final String item) {
        final int idx = item == null ? -1 : item.lastIndexOf(":");
        return idx < 0 ? null : item.substring(0, idx);
    }

    private static void display(final Context ctx, final String title, final String message) {
        SimpleDialog.ofContext(ctx)
            .setTitle(TextParam.text(title))
            .setMessage(TextParam.text(message)).show();
    }

}

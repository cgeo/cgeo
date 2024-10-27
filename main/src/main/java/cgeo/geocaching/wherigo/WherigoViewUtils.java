package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoDialogTitleViewBinding;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.SimpleItemListView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

public final class WherigoViewUtils {

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
            .setDisplayMapper((item, group) -> displayMapper.apply(item), null, (ctx, parent) -> ViewUtils.createButton(ctx, parent, TextParam.text(""), true))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItemPadding(10, 0)
            .setColumns(columnCount, null)
            .addSingleSelectListener(clickHandler);
        view.setModel(model);
        view.setVisibility(View.VISIBLE);
    }
}

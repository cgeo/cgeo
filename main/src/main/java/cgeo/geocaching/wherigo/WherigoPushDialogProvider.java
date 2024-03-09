package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;

import java.util.Arrays;
import java.util.stream.Collectors;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Media;
import org.apache.commons.lang3.StringUtils;
import se.krka.kahlua.vm.LuaClosure;

public class WherigoPushDialogProvider implements IWherigoDialogProvider {

    private final String[] strings;
    private Media[] media;
    private String s;
    private String s1;
    private LuaClosure luaClosure;

    private WherigoThingDetailsBinding binding;

    WherigoPushDialogProvider(final String[] strings, final Media[] media, final String s, final String s1, final LuaClosure luaClosure) {
        this.strings = strings;
        this.media = media;
        this.s = s;
        this.s1 = s1;
        this.luaClosure = luaClosure;
    }

    @Override
    public Dialog createDialog(final Activity activity) {

        final String msg = Arrays.stream(strings).collect(Collectors.joining("\n"));


        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());
        binding.layoutDetailsTextViewName.setText(s);
        binding.layoutDetailsTextViewDescription.setText(msg);

        if (media != null && media.length > 0) {
            binding.media.setMedia(media[0]);
        }

        WherigoUtils.setViewActions(Arrays.asList(TRUE, FALSE), binding.dialogActionlist, item -> TRUE.equals(item) ?
                TextParam.text(StringUtils.isBlank(s) ? "ok" : s).setImage(ImageParam.id(R.drawable.ic_menu_done)) :
                TextParam.text(StringUtils.isBlank(s1) ? "cancel" : s1).setImage(ImageParam.id(R.drawable.ic_menu_cancel)),
            item -> {
                WherigoDialogManager.get().clear();
                if (luaClosure != null) {
                    Engine.invokeCallback(luaClosure, TRUE.equals(item) ? "Button1" : "Button2");
                }
            }
        );

        return dialog;

    }

}

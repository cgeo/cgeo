package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoMediaViewBinding;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.wherigo.WherigoGame;
import cgeo.geocaching.wherigo.WherigoUtils;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.Media;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

/** Allows live-editing of a formula, optionally associated with a Variable List*/
public class WherigoMediaView extends LinearLayout {

    private WherigoMediaViewBinding binding;

    private int mediaId = -1;
    @Nullable private File currentMediaFile;
    @Nullable private String mediaTitle;
    @Nullable private String mediaDescription;

    public WherigoMediaView(final Context context) {
        super(context);
        init(null, 0, 0);
    }

    public WherigoMediaView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public WherigoMediaView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    public WherigoMediaView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {

        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.wherigo_media_view, this);
        binding = WherigoMediaViewBinding.bind(this);

    }

    /**
     * Sets metadata information for the currently displayed media.
     * This metadata is used when saving the media to a geocache.
     */
    public void setMediaMetadata(@Nullable final String title, @Nullable final String description) {
        this.mediaTitle = title;
        this.mediaDescription = description;
    }

    private void setMediaData(final int mediaId, final String type, final String fileName, final String altText, final Supplier<byte[]> dataSupplier) {
        if (mediaId == this.mediaId) {
            return;
        }

        if (StringUtils.isBlank(altText)) {
            binding.mediaTextView.setVisibility(GONE);
        } else {
            binding.mediaTextView.setVisibility(VISIBLE);
            binding.mediaTextView.setText(Engine.removeHtml(altText));
        }

        if (type == null) {
            return;
        }

        final byte[] data = dataSupplier.get();
        if (data == null) {
            return;
        }

        final File cacheDir = WherigoGame.get().getCacheDirectory();
        final File mediaFile = cgeo.geocaching.utils.FileUtils.getOrCreate(cacheDir, "media-" + fileName, type, data);

        this.mediaId = mediaId;
        this.currentMediaFile = mediaFile;

        binding.mediaImageView.setVisibility(GONE);
        binding.mediaVideoView.setVisibility(GONE);
        binding.mediaGifView.setVisibility(GONE);

        switch (type.toLowerCase(Locale.US)) {
            case "mp4":
                //Video
                binding.mediaVideoView.setVideoURI(Uri.fromFile(mediaFile));
                binding.mediaVideoView.setVisibility(VISIBLE);
                setupLongClickSave(binding.mediaVideoView);
                break;
            case "gif":
                //gif
                binding.mediaGifView.setImageURI(Uri.fromFile(mediaFile));
                binding.mediaGifView.setVisibility(VISIBLE);
                setupLongClickSave(binding.mediaGifView);
                break;
            case "jpeg":
            case "jpg":
            case "png":
            case "bmp":
                binding.mediaImageView.setImageURI(Uri.fromFile(mediaFile));
                binding.mediaImageView.setVisibility(VISIBLE);
                setupLongClickSave(binding.mediaImageView);
                break;
            case "wav":
            case "mp3":
                //Audio - no visual representation, but allow saving via alt text view
                setupLongClickSave(binding.mediaTextView);
                break;
            default:
                //do nothing
                break;
        }
    }

    private void setupLongClickSave(final View view) {
        view.setOnLongClickListener(v -> {
            showSaveContextMenu();
            return true;
        });
    }

    private void showSaveContextMenu() {
        final Activity activity = getActivity();
        if (activity == null || currentMediaFile == null) {
            return;
        }

        final ContextMenuDialog ctxMenu = new ContextMenuDialog(activity);
        ctxMenu.setTitle(LocalizationUtils.getString(R.string.cache_image));
        ctxMenu.addItem(LocalizationUtils.getString(R.string.wherigo_save_media), R.drawable.ic_menu_save, item ->
                WherigoUtils.saveMediaToGeocache(currentMediaFile, mediaTitle, mediaDescription));
        ctxMenu.show();
    }

    @Nullable
    private Activity getActivity() {
        Context ctx = getContext();
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                return (Activity) ctx;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public void setMediaData(final String type, final byte[] data, final String altText) {
        final int mediaId = this.mediaId >= 0 ? -1 : this.mediaId - 1;
        setMediaData(mediaId, type, "id" + mediaId, altText, () -> data);
    }

    public void setMedia(final Media media) {
        if (media == null || media.id == mediaId) {
            return;
        }

        setMediaData(media.id, media.type, media.jarFilename(), media.altText, () -> {
            try {
                return Engine.mediaFile(media);
            } catch (IOException ex) {
                return null;
            }
        });

    }


}

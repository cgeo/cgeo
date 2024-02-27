package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoMediaViewBinding;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.io.File;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Media;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/** Allows live-editing of a formula, optionally associated with a Variable List*/
public class WherigoMediaView extends LinearLayout {

    private WherigoMediaViewBinding binding;

    private int mediaId = -1;

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

    public void setMedia(final Media media) {
        if (media == null || media.id == mediaId) {
            return;
        }

        if (StringUtils.isBlank(media.altText)) {
            binding.mediaTextView.setVisibility(GONE);
        } else {
            binding.mediaTextView.setVisibility(VISIBLE);
            binding.mediaTextView.setText(Engine.removeHtml(media.altText));
        }

        if (media.type == null) {
            return;
        }

        final File cacheDir = LocalStorage.getWherigoCacheDirectory();
        final File mediaFile = new File(cacheDir, media.jarFilename());

        byte[] data = null;
        try {
            data = Engine.mediaFile(media);
            FileUtils.writeByteArrayToFile(mediaFile, data);
        } catch (Exception e) {
            Log.e("Problem extracting media data", e);
            return;
        }

        this.mediaId = media.id;

        binding.mediaImageView.setVisibility(GONE);
        binding.mediaVideoView.setVisibility(GONE);
        binding.mediaGifView.setVisibility(GONE);

        switch (media.type.toLowerCase()) {
            case "mp4":
                //Video
                binding.mediaVideoView.setVideoURI(Uri.fromFile(mediaFile));
                binding.mediaVideoView.setVisibility(VISIBLE);
                break;
            case "gif":
                //gif
                binding.mediaGifView.setImageURI(Uri.fromFile(mediaFile));
                binding.mediaGifView.setVisibility(VISIBLE);
                break;
            case "jpeg":
            case "jpg":
            case "png":
            case "bmp":
                binding.mediaImageView.setImageURI(Uri.fromFile(mediaFile));
                binding.mediaImageView.setVisibility(VISIBLE);
                break;
            default:
                //do nothing
                break;
        }


    }


}

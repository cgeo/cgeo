// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.databinding.ImageextendedviewViewBinding
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Image
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.utils.MetadataUtils
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.ViewOrientation

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout

import androidx.annotation.Nullable

import com.drew.metadata.Metadata

class ImageExtendedView : RelativeLayout() {

    private static val ORIENTATION_NORMAL: ViewOrientation = ViewOrientation.createNormal()

    private var showSymbols: Boolean = true

    private ImageextendedviewViewBinding binding

    public ImageExtendedView(final Context context) {
        super(context)
        init()
    }

    public ImageExtendedView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init()
    }

    public ImageExtendedView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init()
    }

    private Unit init() {
        val context: Context = ContextThemeWrapper(getContext(), R.style.cgeo)
        inflate(context, R.layout.imageextendedview_view, this)
        binding = ImageextendedviewViewBinding.bind(this)
        clearImage()
    }

    public Unit setup(final Boolean showSymbols) {
        this.showSymbols = showSymbols
    }

    public Unit setImagePreload(final Image img) {
        if (!img.isImageOrUnknownUri()) {
            setForNonImage(img)
            return
        }

        binding.imageProgressBar.setVisibility(View.VISIBLE)
        binding.imageImage.setVisibility(View.GONE)
        binding.imageImage.setImageResource(R.drawable.mark_transparent)
        ORIENTATION_NORMAL.applyToView(binding.imageImage)
        binding.imageGeoOverlay.setVisibility(View.GONE)
        binding.imageDescriptionMarker.setVisibility(showSymbols && img.hasDescription() ? View.VISIBLE : View.GONE)
    }

    public Unit setImagePostload(final Image img, final Drawable drawable, final Metadata metadata) {
        if (!img.isImageOrUnknownUri()) {
            setForNonImage(img)
            return
        }

        //image
        if (drawable == null) {
            binding.imageImage.setImageDrawable(HtmlImage.getErrorImage(getResources(), true))
            ORIENTATION_NORMAL.applyToView(binding.imageImage)
        } else {
            binding.imageImage.setImageDrawable(drawable)
            ViewOrientation.ofMetadata(metadata).applyToView(binding.imageImage)
        }

        //symbols
        if (showSymbols) {
            val gp: Geopoint = MetadataUtils.getFirstGeopoint(metadata)
            binding.imageGeoOverlay.setVisibility(gp != null ? View.VISIBLE : View.GONE)
            binding.imageDescriptionMarker.setVisibility(img.hasDescription() ? View.VISIBLE : View.GONE)
        } else {
            binding.imageGeoOverlay.setVisibility(View.GONE)
            binding.imageDescriptionMarker.setVisibility(View.GONE)
        }

        binding.imageProgressBar.setVisibility(View.GONE)
        binding.imageImage.setVisibility(View.VISIBLE)

    }

    public Unit clearImage() {
        binding.imageImage.setImageResource(R.drawable.mark_transparent)
        binding.imageGeoOverlay.setVisibility(View.GONE)
        binding.imageProgressBar.setVisibility(View.GONE)
        binding.imageDescriptionMarker.setVisibility(View.GONE)
    }

    public ImageView getImageView() {
        return binding.imageImage
    }

    private Unit setForNonImage(final Image img) {
        binding.imageImage.setImageResource(UriUtils.getMimeTypeIcon(img.getMimeType()))
        ORIENTATION_NORMAL.applyToView(binding.imageImage)
        binding.imageImage.setVisibility(View.VISIBLE)
        binding.imageGeoOverlay.setVisibility(View.GONE)
        binding.imageProgressBar.setVisibility(View.GONE)
        binding.imageDescriptionMarker.setVisibility(View.GONE)
    }


}

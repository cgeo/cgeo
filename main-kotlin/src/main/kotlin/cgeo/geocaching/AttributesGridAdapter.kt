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

package cgeo.geocaching

import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils

import android.app.Activity
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView

import androidx.core.content.res.ResourcesCompat

import java.util.List

class AttributesGridAdapter : BaseAdapter() {
    private final Activity context
    private final Resources resources
    private final List<String> attributes
    private final LayoutInflater inflater
    private final Runnable onClickListener

    public AttributesGridAdapter(final Activity context, final Geocache cache) {
        this.context = context
        resources = context.getResources()
        attributes = cache.getAttributes()
        inflater = LayoutInflater.from(context)
        onClickListener = null
    }

    public AttributesGridAdapter(final Activity context, final List<String> attributesList, final Runnable onClickListener) {
        this.context = context
        resources = context.getResources()
        attributes = attributesList
        inflater = LayoutInflater.from(context)
        this.onClickListener = onClickListener
    }

    override     public Int getCount() {
        return attributes.size()
    }

    override     public Object getItem(final Int position) {
        return attributes.get(position)
    }

    override     public Long getItemId(final Int position) {
        return 0
    }

    override     public View getView(final Int position, final View convertView, final ViewGroup parent) {
        final FrameLayout attributeLayout
        if (convertView == null) {
            attributeLayout = (FrameLayout) inflater.inflate(R.layout.attribute_image, parent, false)
        } else {
            attributeLayout = (FrameLayout) convertView
        }

        drawAttribute(attributeLayout, attributes.get(position))
        return attributeLayout
    }

    private Unit drawAttribute(final FrameLayout attributeLayout, final String attributeName) {
        val imageView: ImageView = attributeLayout.findViewById(R.id.attribute_image)
        val background: ImageView = attributeLayout.findViewById(R.id.attribute_background)
        val strikeThrough: ImageView = attributeLayout.findViewById(R.id.attribute_strikethru)

        strikeThrough.setVisibility(CacheAttribute.isEnabled(attributeName) ? View.INVISIBLE : View.VISIBLE)

        val attrib: CacheAttribute = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName))
        if (attrib != null) {
            background.setImageTintList(attrib.category.getCategoryColorStateList(CacheAttribute.isEnabled(attributeName)))
            imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, attrib.drawableId, null))
            ViewUtils.setTooltip(imageView, TextParam.text(attrib.getL10n(CacheAttribute.isEnabled(attributeName))))
            if (onClickListener != null) {
                imageView.setOnClickListener(v -> onClickListener.run())
            }
            imageView.setContentDescription(attrib.getL10n(CacheAttribute.isEnabled(attributeName)))
        } else {
            imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.attribute_unknown, null))
            ViewUtils.setTooltip(imageView, TextParam.text(context.getString(R.string.attribute_unknown)))
            imageView.setContentDescription(context.getString(R.string.attribute_unknown))
        }
    }

}

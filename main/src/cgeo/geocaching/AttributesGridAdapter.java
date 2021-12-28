package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import java.util.List;

public class AttributesGridAdapter extends BaseAdapter {
    private final Activity context;
    private final Resources resources;
    private final List<String> attributes;
    private final LayoutInflater inflater;
    private final Runnable onClickListener;

    public AttributesGridAdapter(final Activity context, final Geocache cache) {
        this.context = context;
        resources = context.getResources();
        attributes = cache.getAttributes();
        inflater = LayoutInflater.from(context);
        onClickListener = null;
    }

    public AttributesGridAdapter(final Activity context, final List<String> attributesList, final Runnable onClickListener) {
        this.context = context;
        resources = context.getResources();
        attributes = attributesList;
        inflater = LayoutInflater.from(context);
        this.onClickListener = onClickListener;
    }

    @Override
    public int getCount() {
        return attributes.size();
    }

    @Override
    public Object getItem(final int position) {
        return attributes.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final FrameLayout attributeLayout;
        if (convertView == null) {
            attributeLayout = (FrameLayout) inflater.inflate(R.layout.attribute_image, parent, false);
        } else {
            attributeLayout = (FrameLayout) convertView;
        }

        drawAttribute(attributeLayout, attributes.get(position));
        return attributeLayout;
    }

    private void drawAttribute(final FrameLayout attributeLayout, final String attributeName) {
        final ImageView imageView = attributeLayout.findViewById(R.id.attribute_image);
        final ImageView background = attributeLayout.findViewById(R.id.attribute_background);
        final ImageView strikeThrough = attributeLayout.findViewById(R.id.attribute_strikethru);

        strikeThrough.setVisibility(CacheAttribute.isEnabled(attributeName) ? View.INVISIBLE : View.VISIBLE);

        final CacheAttribute attrib = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName));
        if (attrib != null) {
            background.setImageTintList(attrib.category.getCategoryColorStateList(CacheAttribute.isEnabled(attributeName)));
            imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, attrib.drawableId, null));
            ViewUtils.setTooltip(imageView, TextParam.text(attrib.getL10n(CacheAttribute.isEnabled(attributeName))));
            if (onClickListener != null) {
                imageView.setOnClickListener(v -> onClickListener.run());
            }
            imageView.setContentDescription(attrib.getL10n(CacheAttribute.isEnabled(attributeName)));
        } else {
            imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.attribute_unknown, null));
            ViewUtils.setTooltip(imageView, TextParam.text(context.getString(R.string.attribute_unknown)));
            imageView.setContentDescription(context.getString(R.string.attribute_unknown));
        }
    }

}

package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

    public AttributesGridAdapter(final Activity context, final Geocache cache) {
        this.context = context;
        resources = context.getResources();
        attributes = cache.getAttributes();
        inflater = LayoutInflater.from(context);
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
        final ImageView imageView = (ImageView) attributeLayout.getChildAt(0);

        final boolean strikeThrough = !CacheAttribute.isEnabled(attributeName);
        final CacheAttribute attrib = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName));
        if (attrib != null) {
            imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, attrib.drawableId, null));

            if(attrib.rawName.startsWith("P4NService"))
            {
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setColor(Color.argb(255,0,116,240));
                gradientDrawable.setCornerRadius(6.0f);

                imageView.setBackground(gradientDrawable);


            }
            if(attrib.rawName.startsWith("P4NActivite"))
            {
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setColor(Color.argb(255,240,148,0));
                gradientDrawable.setCornerRadius(6.0f);

                imageView.setBackground(gradientDrawable);


            }
            if (strikeThrough) {
                // generate strike through image with same properties as attribute image
                final ImageView strikeThroughImage = new ImageView(context);
                strikeThroughImage.setLayoutParams(imageView.getLayoutParams());
                strikeThroughImage.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.attribute__strikethru, null));
                attributeLayout.addView(strikeThroughImage);
            }
        } else {
            imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.attribute_unknown, null));
        }
    }

}

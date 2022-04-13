package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ImageviewActivityBinding;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.utils.ImageDataMemoryCache;
import cgeo.geocaching.utils.MetadataUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

public class ImageViewActivity extends AbstractActionBarActivity {

    private ImageviewActivityBinding binding;

    private static final String PARAM_IMAGE_LIST = "param_image_list";
    private static final String PARAM_IMAGE_LIST_POS = "param_image_list_pos";
    private static final String PARAM_IMAGE_CONTEXT_CODE = "param_image_context_code";

    private final ImageDataMemoryCache imageCache = new ImageDataMemoryCache(2);

    private final ArrayList<Image> imageList = new ArrayList<>();
    private int imagePos = 0;
    private String imageContextCode = "shared";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.imageview_activity);
        binding = ImageviewActivityBinding.bind(findViewById(R.id.imageview_activity_viewroot));

        imageList.clear();

        final Uri uri = AndroidBeam.getUri(getIntent());
        if (uri != null) {
            imageList.add(new Image.Builder().setUrl(uri).build());
        }

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final ArrayList<Image> extrasImgList = extras.getParcelableArrayList(PARAM_IMAGE_LIST);
            if (extrasImgList != null) {
                imageList.addAll(extrasImgList);
            }
            imageContextCode = extras.getString(PARAM_IMAGE_CONTEXT_CODE);
            imagePos = extras.getInt(PARAM_IMAGE_LIST_POS, 0);
        }

        // Restore previous state
        if (savedInstanceState != null) {
            final ArrayList<Image> ssImgList = savedInstanceState.getParcelableArrayList(PARAM_IMAGE_LIST);
            if (ssImgList != null) {
                imageList.clear();
                imageList.addAll(ssImgList);
            }
            imageContextCode = savedInstanceState.getString(PARAM_IMAGE_CONTEXT_CODE);
            imagePos = savedInstanceState.getInt(PARAM_IMAGE_LIST_POS, 0);
        }

        imageCache.setCode(imageContextCode);

        binding.previous.setOnClickListener(v -> {
            adjustPos(-1);
            repaintView();
        });
        binding.next.setOnClickListener(v -> {
            adjustPos(1);
            repaintView();
        });

        repaintView();

   }

    private void adjustPos(final int add) {
        imagePos = Math.max(0, Math.min(imageList.size(), imagePos));
        imagePos += add;
        if (imagePos >= imageList.size()) {
            imagePos = 0;
        }
        if (imagePos < 0 && !imageList.isEmpty()) {
            imagePos = imageList.size() - 1;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PARAM_IMAGE_LIST, imageList);
        outState.putString(PARAM_IMAGE_CONTEXT_CODE, imageContextCode);
        outState.putInt(PARAM_IMAGE_LIST_POS, imagePos);
    }

    @SuppressLint("SetTextI18n")
    private void repaintView() {
        adjustPos(0);
        if (imageList.isEmpty() || imageList.get(imagePos) == null) {
            binding.imageviewHeadline.setText("no image");
            binding.imageFull.setVisibility(View.GONE);
            return;
        }
        final Image currentImage = imageList.get(imagePos);
        final String headline = (imagePos + 1) + " / " + imageList.size() + "     " + currentImage.category.getI18n();
        binding.imageviewHeadline.setText(headline);
        binding.imageviewTitle.setText("Title: " + Html.fromHtml(currentImage.title));
        binding.imageviewTitle.setVisibility(StringUtils.isEmpty(currentImage.title) ? View.GONE : View.VISIBLE);
        binding.imageviewDescription.setText("Desc: " + Html.fromHtml(currentImage.getDescription()));
        binding.imageviewDescription.setVisibility(StringUtils.isEmpty(currentImage.getDescription()) ? View.GONE : View.VISIBLE);
        binding.imageviewGeocode.setText("");
        binding.imageviewComment.setText("");

        imageCache.loadImage(currentImage.getUrl(), p -> {
            binding.imageFull.setImageDrawable(p.first);
            binding.imageFull.setVisibility(View.VISIBLE);
            binding.imageviewComment.setText("Comment: " + MetadataUtils.getComment(p.second));
            binding.imageviewComment.setVisibility(StringUtils.isEmpty(MetadataUtils.getComment(p.second)) ? View.GONE : View.VISIBLE);
            binding.imageviewGeocode.setText("Geopoint: " + MetadataUtils.getFirstGeopoint(p.second));
            binding.imageviewGeocode.setVisibility(MetadataUtils.getFirstGeopoint(p.second) == null ? View.GONE : View.VISIBLE);
        }, () -> binding.imageFull.setVisibility(View.GONE));
    }

    public static void openImageView(final Activity activity, final String contextCode, final Collection<Image> images, final int pos) {
        final Intent intent = new Intent(activity, ImageViewActivity.class);
        intent.putExtra(PARAM_IMAGE_CONTEXT_CODE, contextCode);
        intent.putExtra(PARAM_IMAGE_LIST, new ArrayList<>(images));
        intent.putExtra(PARAM_IMAGE_LIST_POS, pos);
        activity.startActivity(intent);
    }

}

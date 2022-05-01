package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.ImageviewActivityBinding;
import cgeo.geocaching.databinding.ImageviewImageBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.utils.ImageDataMemoryCache;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.MetadataUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UriUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.igreenwood.loupe.Loupe;
import org.apache.commons.lang3.StringUtils;

public class ImageViewActivity extends AbstractActionBarActivity {

    private static final String PARAM_IMAGE_LIST = "param_image_list";
    private static final String PARAM_IMAGE_LIST_POS = "param_image_list_pos";
    private static final String PARAM_IMAGE_CONTEXT_CODE = "param_image_context_code";

    private final ImageDataMemoryCache imageCache = new ImageDataMemoryCache(2);
    private ImageAdapter imageAdapter;

    private final ArrayList<Image> imageList = new ArrayList<>();
    private int imagePos = 0;
    private String imageContextCode = "shared";

    private class ImageAdapter extends PagerAdapter {

        private final Context context;

        ImageAdapter(final Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
            final ImageviewImageBinding binding = ImageviewImageBinding.inflate(LayoutInflater.from(context));
            final Image currentImg = imageList.get(position);
            binding.imageOpenFile.setOnClickListener(v ->
                ImageUtils.viewImageInStandardApp(ImageViewActivity.this, currentImg.getUri(), imageContextCode));
            binding.imageOpenBrowser.setOnClickListener(v ->
                ShareUtils.openUrl(ImageViewActivity.this, currentImg.getUrl(), true));
            binding.imageShare.setOnClickListener(v ->
                    ShareUtils.shareImage(ImageViewActivity.this, currentImg.getUri(), imageContextCode, R.string.about_system_info_send_chooser));

            container.addView(binding.getRoot());
            loadImageView(position, binding);
            return binding.getRoot();
        }

        @Override
        public void destroyItem(@NonNull final ViewGroup container, final int position, @NonNull final Object object) {
            container.removeView((View) object);
        }

        @Override
        public void setPrimaryItem(@NonNull final ViewGroup container, final int position, @NonNull final Object object) {
            super.setPrimaryItem(container, position, object);
            imagePos = position;
        }

        @Override
        public int getCount() {
            return Math.max(1, imageList.size());
        }

        @Override
        public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
            return view == object;
        }

        public void clear() {
            //imageViewMap.clear();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.imageview_activity);
        final ImageviewActivityBinding binding = ImageviewActivityBinding.bind(findViewById(R.id.imageview_viewpager));
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
        imagePos = Math.max(0, Math.min(imageList.size() - 1, imagePos));

        imageCache.setCode(imageContextCode);
        setTitle("Images for " + imageContextCode);

        imageAdapter = new ImageAdapter(this);
        binding.imageviewViewpager.setAdapter(imageAdapter);
        binding.imageviewViewpager.setCurrentItem(imagePos);

   }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PARAM_IMAGE_LIST, imageList);
        outState.putString(PARAM_IMAGE_CONTEXT_CODE, imageContextCode);
        outState.putInt(PARAM_IMAGE_LIST_POS, imagePos);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageCache.clear();
        if (imageAdapter != null) {
            imageAdapter.clear();
        }
    }

    @SuppressLint("ClickableViewAccessibility") //this is due to Loupe hack
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
    private void loadImageView(final int imagePos, final ImageviewImageBinding binding) {
        final Image currentImage = imageList.get(imagePos);
        if (currentImage == null) {
            binding.imageviewHeadline.setText("no image");
            binding.imageFull.setVisibility(View.GONE);
            return;
        }
        final String headline = (imagePos + 1) + " / " + imageList.size() +
            (Image.ImageCategory.UNCATEGORIZED == currentImage.category ? "" : "     " + currentImage.category.getI18n());
        binding.imageviewHeadline.setText(headline);

        final List<CharSequence> imageInfos = new ArrayList<>();
        if (!StringUtils.isEmpty(currentImage.title)) {
            imageInfos.add(Html.fromHtml("<b>" + currentImage.title + "</b>"));
        }
        if (!StringUtils.isEmpty(currentImage.getDescription())) {
            imageInfos.add(Html.fromHtml(currentImage.getDescription()));
        }
        if (!StringUtils.isEmpty(currentImage.contextInformation)) {
            imageInfos.add(Html.fromHtml("<i>" + currentImage.contextInformation + "</i>"));
        }
        if (imageInfos.isEmpty()) {
            imageInfos.add(Html.fromHtml("<b>No title</b>"));
        }
        binding.imageviewInformation.setText(TextUtils.join(imageInfos, d -> d, "\n"));
        binding.imageviewInformation.setOnClickListener(v -> {
            binding.imageviewInformation.setMaxLines(
                binding.imageviewInformation.getMaxLines() == 1 ? 1000 : 1);
        });

        binding.imageOpenBrowser.setEnabled(
            !UriUtils.isFileUri(currentImage.getUri()) && !UriUtils.isContentUri(currentImage.getUri()));

        imageCache.loadImage(currentImage.getUrl(), p -> {
            binding.imageFull.setImageDrawable(p.first);
            binding.imageFull.setVisibility(View.VISIBLE);

            final Geopoint gp = MetadataUtils.getFirstGeopoint(p.second);
            final String comment = MetadataUtils.getComment(p.second);
            if (gp != null || !StringUtils.isBlank(comment)) {
                final List<CharSequence> imageInfosNew = new ArrayList<>();
                imageInfosNew.add(binding.imageviewInformation.getText());
                if (gp != null) {
                    final String gpAsHtml = Html.escapeHtml(GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, gp));
                    imageInfosNew.add(Html.fromHtml("<b>Geopoint:</b> <i>" + gpAsHtml + "</i>"));
                }
                if (!StringUtils.isBlank(comment)) {
                    String commentInView = comment;
                    if (comment.length() > 300) {
                        commentInView = comment.substring(0, 280) + "...(" + comment.length() + " chars)";
                    }
                    imageInfosNew.add(Html.fromHtml("<b>Metadata Comment:</b> <i>" + commentInView + "</i>"));
                }
                binding.imageviewInformation.setText(TextUtils.join(imageInfosNew, d -> d, "\n"));
            }

            final Loupe loupe = new Loupe(binding.imageFull, binding.imageviewViewroot);
            loupe.setOnViewTranslateListener(new Loupe.OnViewTranslateListener() {
                @Override
                public void onStart(@NonNull final ImageView imageView) {
                    //empty on purpose
                }

                @Override
                public void onViewTranslate(@NonNull final ImageView imageView, final  float v) {
                    //empty on purpose
                }

                @Override
                public void onDismiss(@NonNull final ImageView imageView) {
                    //close detail view on "fling down"
                    finish();
                }

                @Override
                public void onRestore(@NonNull final ImageView imageView) {
                    //empty on purpose
                }
            });

            //Loupe is unable to detect single clicks (see https://github.com/igreenwood/loupe/issues/25)
            //As a workaround we register a second GestureDetector on top of the one installed by Loupe to detect single taps
            //Workaround START
            final GestureDetector singleTapDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(final MotionEvent e) {
                    //Logic to happen on single tap:
                    inverseFullImageView(binding);
                    return true;
                }
            });
            //Registering an own touch listener overrides the TouchListener registered by Loupe
            binding.imageviewViewroot.setOnTouchListener((v, event) -> {
                //perform singleTap detection
                singleTapDetector.onTouchEvent(event);
                //pass through event to Loupe so it handles all other gestures correctly
                return loupe.onTouch(v, event);
            });
            //Workaround END

        }, () -> binding.imageFull.setVisibility(View.GONE));

    }

    private void inverseFullImageView(final ImageviewImageBinding binding) {
        final boolean gone = binding.imageviewHeadline.getVisibility() != View.GONE;
        binding.imageviewHeadline.setVisibility(gone ? View.GONE : View.VISIBLE);
        binding.imageviewInformation.setVisibility(gone ? View.GONE : View.VISIBLE);
        binding.imageviewActions.setVisibility(gone ? View.GONE : View.VISIBLE);
        ActivityMixin.showHideActionBar(ImageViewActivity.this, !gone);
    }

    public static void openImageView(final Activity activity, final String contextCode, final Collection<Image> images, final int pos) {
        final Intent intent = new Intent(activity, ImageViewActivity.class);
        intent.putExtra(PARAM_IMAGE_CONTEXT_CODE, contextCode);
        intent.putExtra(PARAM_IMAGE_LIST, new ArrayList<>(images));
        intent.putExtra(PARAM_IMAGE_LIST_POS, pos);
        activity.startActivity(intent);
    }

}

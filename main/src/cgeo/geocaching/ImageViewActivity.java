package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ImageviewActivityBinding;
import cgeo.geocaching.databinding.ImageviewImageBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.utils.ImageDataMemoryCache;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MetadataUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SharedElementCallback;
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
import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.igreenwood.loupe.Loupe;
import org.apache.commons.lang3.StringUtils;

public class ImageViewActivity extends AbstractActionBarActivity {

    private static final String TRANSITION_ID = "image_transition";
    private static final String TRANSITION_ID_BACK = "image_transition_back";

    private static final String PARAM_IMAGE_LIST = "param_image_list";
    private static final String PARAM_IMAGE_LIST_POS = "param_image_list_pos";
    private static final String PARAM_IMAGE_CONTEXT_CODE = "param_image_context_code";
    private static final String PARAM_FULLIMAGEVIEW = "param_full_image_view";

    private final ImageDataMemoryCache imageCache = new ImageDataMemoryCache(2);
    private ImageAdapter imageAdapter;

    private final ArrayList<Image> imageList = new ArrayList<>();
    private int imagePos = 0;
    private String imageContextCode = "shared";
    private boolean fullImageView = false;
    private boolean transactionEnterActive = false;

    private class ImageAdapter extends PagerAdapter {

        private static final int ENDLESS_MULTIPLIER = 1000;

        private final Context context;
        private final int realImageSize;
        private final Map<Integer, ImageviewImageBinding> cachedPages = new HashMap<>();
        private int cachedPosition;

        ImageAdapter(final Context context) {
            this.context = context;
            this.realImageSize = Math.max(1, imageList.size());
        }

        public ImageviewImageBinding getCurrentBinding() {
            return cachedPages.get(cachedPosition);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int pos) {
            final int position = pos % realImageSize;
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
            cachedPages.put(pos, binding);
            return binding.getRoot();
        }

        @Override
        public void destroyItem(@NonNull final ViewGroup container, final int position, @NonNull final Object object) {
            container.removeView((View) object);
            cachedPages.remove(position);
        }

        @Override
        public void setPrimaryItem(@NonNull final ViewGroup container, final int position, @NonNull final Object object) {
            super.setPrimaryItem(container, position, object);
            applyFullImageView(ImageviewImageBinding.bind((View) object));

            //strip transition name from all cache objects and set it on current object
            for (ImageviewImageBinding b : cachedPages.values()) {
                b.imageFull.setTransitionName(null);
            }
            cachedPosition = position;
            cachedPages.get(position).imageFull.setTransitionName("test" + position % realImageSize);
            imagePos = position % realImageSize;
        }

        @Override
        public int getCount() {
            return realImageSize * ENDLESS_MULTIPLIER;
        }

        @Override
        public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
            return view == object;
        }

        public void clear() {
            //imageViewMap.clear();
        }

        private void toggleFullImageView() {
            fullImageView = !fullImageView;
            for (ImageviewImageBinding cachedBinding : cachedPages.values()) {
                applyFullImageView(cachedBinding);
            }
        }

    }

    private void applyFullImageView(final ImageviewImageBinding binding) {
        final boolean turnOn = fullImageView;
        binding.imageviewHeadline.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
        binding.imageviewInformation.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
        binding.imageviewActions.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setThemeAndContentView(R.layout.imageview_activity);
        postponeEnterTransition();
        transactionEnterActive = true;
        setEnterSharedElementCallback(new SharedElementCallback() {

            public void onMapSharedElements(final List<String> names, final Map<String, View> sharedElements) {
                Log.i("enter");
                final ImageView imageView = imageAdapter.getCurrentBinding().imageFull;
                sharedElements.clear();
                if (transactionEnterActive) {
                    imageView.setTransitionName(TRANSITION_ID);
                    sharedElements.put(TRANSITION_ID, imageView);
                    return;
                }
                imageView.setTransitionName(TRANSITION_ID_BACK + imagePos);
                names.clear();
                names.add(TRANSITION_ID_BACK + imagePos);
                sharedElements.put(TRANSITION_ID_BACK + imagePos, imageAdapter.getCurrentBinding().imageFull);
            }
        });

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
            fullImageView = extras.getBoolean(PARAM_FULLIMAGEVIEW, false);
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
            fullImageView = savedInstanceState.getBoolean(PARAM_FULLIMAGEVIEW, false);
        }
        imagePos = Math.max(0, Math.min(imageList.size() - 1, imagePos));

        imageCache.setCode(imageContextCode);
        setTitle("Images for " + imageContextCode);

        imageAdapter = new ImageAdapter(this);
        binding.imageviewViewpager.setAdapter(imageAdapter);
        binding.imageviewViewpager.setCurrentItem(imagePos + imageAdapter.getCount() / 2);
        binding.imageviewViewpager.setOffscreenPageLimit(1);

    }

    @Override
    public void onBackPressed() {
        setFinishResult();
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PARAM_IMAGE_LIST, imageList);
        outState.putString(PARAM_IMAGE_CONTEXT_CODE, imageContextCode);
        outState.putInt(PARAM_IMAGE_LIST_POS, imagePos);
        outState.putBoolean(PARAM_FULLIMAGEVIEW, fullImageView);
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
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    // splitting up that method would not help improve readability
    private void loadImageView(final int imagePos, final ImageviewImageBinding binding) {
        final Image currentImage = imageList.get(imagePos);
        if (currentImage == null) {
            binding.imageviewHeadline.setVisibility(View.INVISIBLE);
            binding.imageFull.setVisibility(View.GONE);
            return;
        }
        applyFullImageView(binding);
        binding.imageviewBackbutton.setOnClickListener(v -> {
            setFinishResult();
            finishAfterTransition();
        });
        binding.imageviewPosition.setText((imagePos + 1) + " / " + imageList.size());
        binding.imageviewCategory.setText(currentImage.category.getI18n());

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
            startPostponedEnterTransition();
            transactionEnterActive = false;

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
                public void onViewTranslate(@NonNull final ImageView imageView, final float v) {
                    //empty on purpose
                }

                @Override
                public void onDismiss(@NonNull final ImageView imageView) {
                    //close detail view on "fling down"
                    setFinishResult();
                    finishAfterTransition();
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
                    imageAdapter.toggleFullImageView();
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


    public static void openImageView(final Activity activity, final String contextCode, final Collection<Image> images, final int pos, final Func1<Integer, View> getImageView) {
        final Intent intent = new Intent(activity, ImageViewActivity.class);
        intent.putExtra(PARAM_IMAGE_CONTEXT_CODE, contextCode);
        intent.putExtra(PARAM_IMAGE_LIST, new ArrayList<>(images));
        intent.putExtra(PARAM_IMAGE_LIST_POS, pos);
        if (getImageView == null) {
            activity.startActivity(intent);
        } else {
            activity.setExitSharedElementCallback(new SharedElementCallback() {

                public void onMapSharedElements(final List<String> names, final Map<String, View> sharedElements) {
                    Log.i("exit");
                    if (names == null || names.isEmpty() || !names.get(0).startsWith(TRANSITION_ID_BACK)) {
                        return;
                    }
                    final int pos = Integer.valueOf(names.get(0).substring(TRANSITION_ID_BACK.length()));
                    final View targetView = getImageView.call(pos);
                    targetView.setTransitionName(names.get(0));
                    sharedElements.clear();
                    sharedElements.put(names.get(0), targetView);
                }
            });
            final View posImageView = getImageView.call(pos);
            posImageView.setTransitionName(TRANSITION_ID);
            activity.startActivity(intent,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity, posImageView, posImageView.getTransitionName()).toBundle());
        }
    }

    private void setFinishResult() {
        //pass back selected image index
        final Intent intent = new Intent();
        intent.putExtra(Intents.EXTRA_INDEX, this.imagePos);
        setResult(RESULT_OK, intent);
    }

}

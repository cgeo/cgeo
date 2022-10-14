package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ImageviewActivityBinding;
import cgeo.geocaching.databinding.ImageviewImageBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.ImageDataMemoryCache;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.MetadataUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ImageViewActivity extends AbstractActionBarActivity {

    private static final String TRANSITION_ID_ENTER = "image_enter_transition_id";
    private static final String TRANSITION_ID_EXIT = "image_exit_transition_id_";

    private static final String PARAM_IMAGE_LIST = "param_image_list";
    private static final String PARAM_IMAGEDATA_LIST_SAVE = "param_image_list_save";
    private static final String PARAM_IMAGE_LIST_POS = "param_image_list_pos";
    private static final String PARAM_IMAGE_CONTEXT_CODE = "param_image_context_code";
    private static final String PARAM_FULLIMAGEVIEW = "param_full_image_view";
    private static final String PARAM_SHOWIMAGEINFO = "param_show_image_information";

    private final ImageDataMemoryCache imageCache = new ImageDataMemoryCache(2);
    private ImageAdapter imageAdapter;
    private ImageviewActivityBinding mainBinding;

    private final ArrayList<Image> imageList = new ArrayList<>();
    private int imagePos = 0;
    private int startPagerPos = 0;
    private String imageContextCode = "shared";
    private boolean fullImageView = false;
    private boolean showImageInformation = true;
    private boolean transactionEnterActive = false;

    private class ImageAdapter extends PagerAdapter {

        private static final int ENDLESS_MULTIPLIER = 1000;

        private final Context context;
        private final int realImageSize;
        private final Map<Integer, PageData> cachedPages = new HashMap<>();
        private int cachedPosition;

        class PageData {
            public final ImageviewImageBinding binding;
            public boolean isBrowseable = false;

            PageData(final ImageviewImageBinding binding) {
                this.binding = binding;
            }
        }

        ImageAdapter(final Context context) {
            this.context = context;
            this.realImageSize = Math.max(1, imageList.size());
        }

        public ImageviewImageBinding getCurrentBinding() {
            return cachedPages.get(cachedPosition).binding;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int pagerPos) {
            final int imagePos = pagerPos % realImageSize;
            final ImageviewImageBinding binding = ImageviewImageBinding.inflate(LayoutInflater.from(context));
            container.addView(binding.getRoot());
            final PageData pd = new PageData(binding);

            final Image currentImage = imageList.get(imagePos);
            pd.isBrowseable = currentImage != null && !UriUtils.isFileUri(currentImage.getUri()) && !UriUtils.isContentUri(currentImage.getUri());

            cachedPages.put(pagerPos, pd);
            loadImageView(pagerPos, imagePos, binding);
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
            setFullImageViewOnOff(ImageviewImageBinding.bind((View) object), fullImageView);

            cachedPosition = position;
            imagePos = position % realImageSize;
            mainBinding.imageOpenBrowser.setEnabled(cachedPages.get(cachedPosition).isBrowseable);
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
            cachedPages.clear();
        }

        private void toggleFullImageView() {
            fullImageView = !fullImageView;
            for (Map.Entry<Integer, PageData> cachedData : cachedPages.entrySet()) {
                if (cachedData.getKey() == cachedPosition) {
                    animateFullImageView(cachedData.getValue().binding, fullImageView);
                } else {
                    setFullImageViewOnOff(cachedData.getValue().binding, fullImageView);
                }

            }
        }

        public void toggleShowInformationView() {
            showImageInformation = !showImageInformation;
            for (Map.Entry<Integer, PageData> cachedData : cachedPages.entrySet()) {
                if (cachedData.getKey() == cachedPosition) {
                    animateInfoOutIn(cachedData.getValue().binding, !showImageInformation);
                } else {
                    setInfoShowHide(cachedData.getValue().binding, showImageInformation);
                }
            }
        }

    }

    private void setFullImageViewOnOff(final ImageviewImageBinding binding, final boolean turnOn) {
        binding.imageviewHeadline.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
        binding.imageviewInformation.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
        binding.imageviewActionSpace.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
        mainBinding.imageviewActions.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
        mainBinding.imageviewBackbutton.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE);
    }

    private void animateFullImageView(final ImageviewImageBinding binding, final boolean turnOn) {

        final float end = turnOn ? 0.0f : 1.0f;

        final ObjectAnimator headline = ObjectAnimator.ofFloat(binding.imageviewHeadline, "alpha", end);
        final ObjectAnimator info = ObjectAnimator.ofFloat(binding.imageviewInformation, "alpha", end);
        final ObjectAnimator actionSpace = ObjectAnimator.ofFloat(binding.imageviewActionSpace, "alpha", end);
        final ObjectAnimator actions = ObjectAnimator.ofFloat(mainBinding.imageviewActions, "alpha", end);
        final ObjectAnimator backButton = ObjectAnimator.ofFloat(mainBinding.imageviewBackbutton, "alpha", end);
        final AnimatorSet as = new AnimatorSet();
        as.playTogether(headline, info, actionSpace, actions, backButton);
        as.setDuration(100);
        as.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(final Animator animation) {
                setFullImageViewOnOff(binding, false); //views must be visible, otherwise we don#t see the animation
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                setFullImageViewOnOff(binding, fullImageView);
            }
        });
        as.start();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide(); //do not use normal Action Bar
        setThemeAndContentView(R.layout.imageview_activity);
        enableViewTransitions(this);
        postponeEnterTransition();
        transactionEnterActive = true;

        setEnterSharedElementCallback(new SharedElementCallback() {
            public void onMapSharedElements(final List<String> names, final Map<String, View> sharedElements) {
                if (transactionEnterActive) {
                    return;
                }
                names.clear();
                names.add(TRANSITION_ID_EXIT + imagePos);
                sharedElements.clear();
                sharedElements.put(TRANSITION_ID_EXIT + imagePos, imageAdapter.getCurrentBinding().imageFull);

            }
        });

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
            showImageInformation = extras.getBoolean(PARAM_SHOWIMAGEINFO, true);
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
            showImageInformation = savedInstanceState.getBoolean(PARAM_SHOWIMAGEINFO, true);
        }
        imagePos = Math.max(0, Math.min(imageList.size() - 1, imagePos));

        imageCache.setCode(imageContextCode);

        imageAdapter = new ImageAdapter(this);

        mainBinding = ImageviewActivityBinding.bind(findViewById(R.id.imageview_activityroot));
        mainBinding.imageviewViewpager.setAdapter(imageAdapter);
        startPagerPos = imagePos + imageAdapter.getCount() / 2;
        mainBinding.imageviewViewpager.setCurrentItem(startPagerPos);
        mainBinding.imageviewViewpager.setOffscreenPageLimit(1);

        mainBinding.imageviewBackbutton.setOnClickListener(v -> {
            setFinishResult();
            finishAfterTransition();
        });
        mainBinding.imageOpenFile.setOnClickListener(v -> {
            final Image img = imageList.get(imagePos);

            if (img.isImageOrUnknownUri()) {
                ImageUtils.viewImageInStandardApp(ImageViewActivity.this, img.getUri(), imageContextCode);
            } else {
                ShareUtils.openContentForView(this, img.getUrl());
            }
        });
        mainBinding.imageOpenBrowser.setOnClickListener(v ->
                ShareUtils.openUrl(ImageViewActivity.this, imageList.get(imagePos).getUrl(), true));
        mainBinding.imageShare.setOnClickListener(v ->
                ShareUtils.shareImage(ImageViewActivity.this, imageList.get(imagePos).getUri(), imageContextCode, R.string.about_system_info_send_chooser));


    }

    @Override
    public void onBackPressed() {
        setFinishResult();
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PARAM_IMAGEDATA_LIST_SAVE, imageList);
        outState.putString(PARAM_IMAGE_CONTEXT_CODE, imageContextCode);
        outState.putInt(PARAM_IMAGE_LIST_POS, imagePos);
        outState.putBoolean(PARAM_FULLIMAGEVIEW, fullImageView);
        outState.putBoolean(PARAM_SHOWIMAGEINFO, showImageInformation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageCache.clear();
        if (imageAdapter != null) {
            imageAdapter.clear();
        }
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    @SuppressLint("SetTextI18n")
    private void loadImageView(final int pagerPos, final int loadImagePos, final ImageviewImageBinding binding) {
        final Image currentImage = imageList.get(loadImagePos);
        if (currentImage == null) {
            binding.imageviewHeadline.setVisibility(View.INVISIBLE);
            binding.imageFull.setVisibility(View.GONE);
            return;
        }
        setFullImageViewOnOff(binding, fullImageView);
        binding.imageviewPosition.setText((loadImagePos + 1) + " / " + imageList.size());
        binding.imageviewCategory.setText(currentImage.category.getI18n());

        final List<CharSequence> imageInfos = new ArrayList<>();
        if (!StringUtils.isEmpty(currentImage.title)) {
            imageInfos.add(Html.fromHtml("<b>" + currentImage.title + (currentImage.isImageOrUnknownUri() ? "" : " (" + currentImage.getMimeFileExtension() + ")") + "</b>"));
        }
        if (!currentImage.isImageOrUnknownUri()) {
            imageInfos.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_mimetype) + ":</b> " + currentImage.getMimeType()));
        }
        if (!StringUtils.isEmpty(currentImage.getDescription())) {
            imageInfos.add(Html.fromHtml(currentImage.getDescription()));
        }
        if (!StringUtils.isEmpty(currentImage.contextInformation)) {
            imageInfos.add(Html.fromHtml("<i>" + currentImage.contextInformation + "</i>"));
        }
        if (imageInfos.isEmpty()) {
            imageInfos.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_notitle) + "</b>"));
        }
        binding.imageviewInformationText.setText(TextUtils.join(imageInfos, d -> d, "\n"));
        binding.imageviewInformation.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setInfoShowHide(binding, showImageInformation));
        binding.imageviewInformation.setOnClickListener(v -> imageAdapter.toggleShowInformationView());
        setInfoShowHide(binding, showImageInformation);

        if (!currentImage.isImageOrUnknownUri()) {
            binding.imageFull.setImageResource(UriUtils.getMimeTypeIcon(currentImage.getMimeType()));
            showImage(pagerPos, binding);
        } else {
            imageCache.loadImage(currentImage.getUrl(), p -> {

                binding.imageFull.setImageDrawable(p.first);

                //enhance description with metadata
                final Geopoint gp = MetadataUtils.getFirstGeopoint(p.second);
                final String comment = MetadataUtils.getComment(p.second);
                if (gp != null || !StringUtils.isBlank(comment)) {
                    final List<CharSequence> imageInfosNew = new ArrayList<>();
                    imageInfosNew.add(binding.imageviewInformationText.getText());
                    if (gp != null) {
                        final String gpAsHtml = Html.escapeHtml(GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, gp));
                        imageInfosNew.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_metadata_geopoint) + ":</b> <i>" + gpAsHtml + "</i>"));
                    }
                    if (!StringUtils.isBlank(comment)) {
                        String commentInView = comment;
                        if (comment.length() > 300) {
                            commentInView = comment.substring(0, 280) + "...(" + comment.length() + " chars)";
                        }
                        imageInfosNew.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_metadata_comment) + ":</b> <i>" + commentInView + "</i>"));
                    }
                    binding.imageviewInformationText.setText(TextUtils.join(imageInfosNew, d -> d, "\n"));
                    setInfoShowHide(binding, showImageInformation);
                }

                showImage(pagerPos, binding);

            }, () -> binding.imageFull.setVisibility(View.GONE));
        }

    }

    private void showImage(final int pagerPos, final ImageviewImageBinding binding) {
        binding.imageFull.setVisibility(View.VISIBLE);
        ImageUtils.createZoomableImageView(this, binding.imageFull, binding.imageviewViewroot, () -> {
            setFinishResult();
            finishAfterTransition();
        }, () -> imageAdapter.toggleFullImageView());

        //trigger enter transition if this is start
        if (pagerPos == startPagerPos) {
            binding.imageFull.setTransitionName(TRANSITION_ID_ENTER);
            //trigger transition for NEXT GUI cycle. If triggered immediately, ViewPager is not ready
            binding.imageFull.post(() -> {
                startPostponedEnterTransition();
                transactionEnterActive = false;
            });
        }
    }

    private static void setInfoShowHide(final ImageviewImageBinding binding, final boolean show) {
        final int infoheight = binding.imageviewInformation.getHeight();
        binding.imageviewInformation.setTranslationY(show ? 0 : infoheight == 0 ? 0 : infoheight - ViewUtils.dpToPixel(24));
        binding.imageviewInformationIconLess.setAlpha(show ? 1.0f : 0.0f);
        binding.imageviewInformationIconMore.setAlpha(show ? 0.0f : 1.0f);
    }

    private static void animateInfoOutIn(final ImageviewImageBinding binding, final boolean out) {
        setInfoShowHide(binding, out);
        final int infoheight = binding.imageviewInformation.getHeight();
        final ObjectAnimator textAnim = ObjectAnimator.ofFloat(binding.imageviewInformation, "translationY", out ? infoheight - ViewUtils.dpToPixel(24) : 0);
        final ObjectAnimator lessAnim = ObjectAnimator.ofFloat(binding.imageviewInformationIconLess, "alpha", out ? 0.0f : 1.0f);
        final ObjectAnimator moreAnim = ObjectAnimator.ofFloat(binding.imageviewInformationIconMore, "alpha", out ? 1.0f : 0.0f);
        final AnimatorSet as = new AnimatorSet();
        as.playTogether(textAnim, lessAnim, moreAnim);
        as.setDuration(200);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                setInfoShowHide(binding, !out);
            }
        });
        as.start();
    }

    public static void openImageView(final Activity activity, final String contextCode, final Image image, final View imageView) {
        openImageView(activity, contextCode, Collections.singleton(image), 0, p -> imageView);
    }

    public static void openImageView(final Activity activity, final String contextCode, final Collection<Image> images, final int pos, final Func1<Integer, View> getImageView) {
        final Intent intent = new Intent(activity, ImageViewActivity.class);
        intent.putExtra(PARAM_IMAGE_CONTEXT_CODE, contextCode);
        intent.putExtra(PARAM_IMAGE_LIST, new ArrayList<>(images));
        intent.putExtra(PARAM_IMAGE_LIST_POS, pos);
        if (getImageView == null || getImageView.call(pos) == null) {
            activity.startActivity(intent);
        } else {
            registerCallerActivity(activity, getImageView);
            final View posImageView = getImageView.call(pos);
            posImageView.setTransitionName(TRANSITION_ID_ENTER);
            activity.startActivity(intent,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity, posImageView, posImageView.getTransitionName()).toBundle());
        }
    }

    public static void registerCallerActivity(final Activity activity, final Func1<Integer, View> getImageView) {

        activity.setExitSharedElementCallback(new SharedElementCallback() {

            public void onMapSharedElements(final List<String> names, final Map<String, View> sharedElements) {
                if (names.size() == 1 && names.get(0) != null && names.get(0).startsWith(TRANSITION_ID_EXIT)) {
                    final int pos = Integer.valueOf(names.get(0).substring(TRANSITION_ID_EXIT.length()));
                    final View v = getImageView.call(pos);
                    sharedElements.clear();
                    sharedElements.put(names.get(0), v);
                }
            }
        });
    }

    /** sets properties for activity to enable smooth image transation between master/list and detail activity */
    public static void enableViewTransitions(final Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        final Window w = activity.getWindow();
        w.setSharedElementsUseOverlay(true);
        final Transition trans = TransitionInflater.from(activity).inflateTransition(R.transition.imageview_smooth_transition);
        w.setSharedElementEnterTransition(trans);
        w.setSharedElementExitTransition(trans);
    }

    private void setFinishResult() {
        //pass back selected image index
        final Intent intent = new Intent();
        intent.putExtra(Intents.EXTRA_INDEX, this.imagePos);
        setResult(RESULT_OK, intent);
    }

}

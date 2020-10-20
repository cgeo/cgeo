package cgeo.geocaching.ui;

import cgeo.geocaching.ImageSelectActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Fragment displays and maintains an image list where
 * user can select/delete/edit/sort images using various options
 *
 * Activities using this fragment have to ensure to call {@link #onParentActivityResult(int, int, Intent)}
 * in their {@link android.app.Activity#onActivityResult(int, int, Intent)} method since
 * this fragment starts and works with results of intents.
 */
public class ImageListFragment extends Fragment {

    private Unbinder unbinder;

    @BindView(R.id.image_add_multi)
    protected View imageAddMultiButton;
    @BindView(R.id.image_add_camera)
    protected View imageAddCameraButton;

    private ImageListAdapter imageList;
    private ImageActivityHelper imageHelper;

    //following info is used to restrict image selections and for display
    private CharSequence contextCode;
    private Long maxImageUploadSize;
    private boolean captionMandatory;

    private static final int SELECT_IMAGE = 101;

    private final Set<String> lastSavedStateImagePaths = new HashSet<>();


    /**
     * call once to initialize values for image retrieval
     */
    public void init(final CharSequence contextCode, final Long maxImageUploadSize, final boolean captionMandatory) {
        this.contextCode = contextCode;
        this.maxImageUploadSize = maxImageUploadSize;
        this.captionMandatory = captionMandatory;
    }

    /**
     * get title for an image in the list as displayed
     */
    public String getImageTitle(final Image image, final int position) {
        if (!StringUtils.isBlank(image.getTitle())) {
            return image.getTitle();
        }
        return getString(R.string.log_image_titleprefix) + " " + (position + 1);
    }

    /**
     * make sure to call this method in the activites {@link android.app.Activity#onActivityResult(int, int, Intent)} method.
     *
     * When this method returns true, this means that the result has been consumed. False otherwise.
     */
    public boolean onParentActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (imageHelper.onActivityResult(requestCode, resultCode, data)) {
            return true;
        }

        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                final int imageIndex = data.getIntExtra(Intents.EXTRA_INDEX, -1);
                final boolean indexIsValid = imageIndex >= 0 && imageIndex < imageList.getItemCount();
                final boolean deleteFlag = data.getBooleanExtra(Intents.EXTRA_DELETE_FLAG, false);
                final Image image = data.getParcelableExtra(Intents.EXTRA_IMAGE);
                if (deleteFlag && indexIsValid) {
                    imageList.removeItem(imageIndex);
                } else if (image != null && indexIsValid) {
                    imageList.updateItem(image, imageIndex);
                } else if (image != null) {
                    imageList.addItem(image);
                }
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed, advise user
                ActivityMixin.showToast(getActivity(), getString(R.string.err_select_logimage_failed));
            }
            return true;

        }
        return false;
    }

    /** sets the list of images to display in this fragment */
    public void setImages(final List<Image> images) {
        imageList.submitList(images);
    }

    /** clears the list of images to display in this fragment */
    public void clearImages() {
        imageList.clearList();
    }

    /** gets the list of images to display in this fragment */
    public List<Image> getImages() {
        return imageList.getCurrentList();
    }
    /** initializes image persistence state to this list. Call after loading list e.g. from database */
    public void setImagePersistentState(final Collection<Image> images) {
        adjustImagePersistentStateTo(images, false);
    }

    /** adjusts image persistent state. Call after storing list e.g. to database.
     * Note: a call to this method physically DELETED images from device which were in previous state call but not in this one! */
    public void adjustImagePersistentState(final Collection<Image> images) {
        adjustImagePersistentStateTo(images, true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.imagelist_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        imageHelper = new ImageActivityHelper(this.getActivity(), 3000);
        imageList = new ImageListAdapter(view.findViewById(R.id.image_list));

        this.imageAddMultiButton.setOnClickListener(v ->
            imageHelper.getMultipleImagesFromStorage(getFastImageAutoScale(), true, false, imgs -> {
                imageList.addItems(imgs);
            }));
        this.imageAddCameraButton.setOnClickListener(v ->
            imageHelper.getImageFromCamera(getFastImageAutoScale(), true, false, img -> {
                imageList.addItem(img);
            }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }


    protected static class ImageViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.image_thumbnail)
        protected ImageView imageThumbnail;
        @BindView(R.id.image_title)
        protected TextView imageTitle;
        @BindView(R.id.image_description)
        protected TextView imageDescription;
        @BindView(R.id.image_info)
        protected TextView imageInfo;
        @BindView(R.id.image_delete)
        protected View imageDeleteButton;
        @BindView(R.id.image_drag)
        protected View imageDrag;

        public ImageViewHolder(final View rowView) {
            super(rowView);
        }
    }


    private final class ImageListAdapter extends ManagedListAdapter<Image, ImageViewHolder> {

        private ImageListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                .setNotifyOnPositionChange(true)
                .setSupportDragDrop(true));
        }

        private void fillViewHolder(final ImageViewHolder holder, final Image image, final int position) {
            if (image == null || holder.imageThumbnail == null) {
                return;
            }
            ImageActivityHelper.displayImageAsync(image, holder.imageThumbnail);
            holder.imageTitle.setText(getImageTitle(image, position));
            holder.imageInfo.setText(getImageInfo(image));
            holder.imageDescription.setText(image.getDescription());
            holder.imageDescription.setVisibility(!StringUtils.isBlank(image.getDescription()) ? View.VISIBLE : View.GONE);
        }

        private String getImageInfo(final Image image) {
            final File imgFile = image.getFile();
            if (imgFile == null) {
                return "---";
            }
            if (!imgFile.exists()) {
                return ImageUtils.getRelativePathToOutputImageDir(imgFile);
            }
            int width = 0;
            int height = 0;
            final ImmutablePair<Integer, Integer> widthHeight = ImageUtils.getImageSize(imgFile);
            if (widthHeight != null) {
                width = widthHeight.getLeft();
                height = widthHeight.getRight();
            }
            return getString(R.string.log_image_info, width, height, imgFile.length() / 1024, ImageUtils.getRelativePathToOutputImageDir(imgFile));
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagelist_item, parent, false);
            final ImageViewHolder viewHolder = new ImageViewHolder(view);
            viewHolder.itemView.setOnClickListener(view1 -> addOrEditImage(viewHolder.getAdapterPosition()));
            viewHolder.imageDeleteButton.setOnClickListener(v -> removeItem(viewHolder.getAdapterPosition()));
            registerStartDrag(viewHolder, viewHolder.imageDrag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final ImageViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position), position);
        }

    }

    /** trigger the start of the detailed "add image" dialog */
    public void startAddImageDialog() {
        addOrEditImage(-1);
    }

    /** internally start the detail image edit dialog */
    private void addOrEditImage(final int imageIndex) {
        final Intent selectImageIntent = new Intent(this.getActivity(), ImageSelectActivity.class);
        if (imageIndex >= 0 && imageIndex < imageList.getItemCount()) {
            selectImageIntent.putExtra(Intents.EXTRA_IMAGE, imageList.getItem(imageIndex));
        }
        selectImageIntent.putExtra(Intents.EXTRA_INDEX, imageIndex);
        selectImageIntent.putExtra(Intents.EXTRA_GEOCODE, contextCode);
        selectImageIntent.putExtra(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE, maxImageUploadSize);
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE_CAPTION_MANDATORY, captionMandatory);

        getActivity().startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    private int getFastImageAutoScale() {
        int scale = Settings.getLogImageScale();
        if (scale <= 0) {
            scale = 1024;
        }
        return scale;
    }

    private void adjustImagePersistentStateTo(final Collection<Image> images, final boolean deleteOld) {
        Log.d("Adjust persistent state from  " + lastSavedStateImagePaths + " to " + images + " (deleteOld=" + deleteOld + ")");
        if (deleteOld) {
            //delete all images on device which are in old state but not in new
            final Set<String> existingPaths = CollectionStream.of(images).map(img -> img.getPath()).toSet();
            for (final String oldPath : lastSavedStateImagePaths) {
                if (!existingPaths.contains(oldPath)) {
                    final boolean result = new File(oldPath).delete();
                    Log.d("Deleting image " + oldPath + " (result: " + result + ")");
                }
            }
        }
        //refill image state
        lastSavedStateImagePaths.clear();
        for (final Image img : images) {
            if (!img.getPath().isEmpty()) {
                lastSavedStateImagePaths.add(img.getPath());
            }
        }
    }

}

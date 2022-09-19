package cgeo.geocaching.ui;

import cgeo.geocaching.ImageSelectActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.ImagelistFragmentBinding;
import cgeo.geocaching.databinding.ImagelistItemBinding;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.ImageUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Fragment displays and maintains an image list where
 * user can select/delete/edit/sort images using various options
 *
 * Activities using this fragment have to ensure to call {@link #onParentActivityResult(int, int, Intent)}
 * in their {@link android.app.Activity#onActivityResult(int, int, Intent)} method since
 * this fragment starts and works with results of intents.
 */
public class ImageListFragment extends Fragment {
    private ImagelistFragmentBinding binding;

    private ImageListAdapter imageList;
    private ImageActivityHelper imageHelper;

    //following info is used to restrict image selections and for display
    private String geocode;
    private Long maxImageUploadSize;
    private boolean captionMandatory;

    private static final int SELECT_IMAGE = 101;

    private static final String SAVED_STATE_IMAGELIST = "cgeo.geocaching.saved_state_imagelist";
    private static final String SAVED_STATE_IMAGEHELPER = "cgeo.geocaching.saved_state_imagehelper";


    /**
     * call once to initialize values for image retrieval
     */
    public void init(final String contextCode, final Long maxImageUploadSize, final boolean captionMandatory) {
        this.geocode = contextCode;
        this.maxImageUploadSize = maxImageUploadSize;
        this.captionMandatory = captionMandatory;
    }

    /**
     * get title for an image in the list as displayed
     */
    public String getImageTitle(final Image image, final int position) {
        if (StringUtils.isNotBlank(image.getTitle())) {
            return image.getTitle();
        }
        if (imageList.getItemCount() == 1) {
            return getString(R.string.log_image_titleprefix); // number is unnecessary if only one image is posted
        }
        return getString(R.string.log_image_titleprefix) + " " + (position + 1);
    }

    private void rebuildImageTitles() {
        // The title of the first image will vary depending on whether there is more than one image present.
        // Therefore we always update the first element if something does change.
        if (imageList.getItemCount() > 0) {
            imageList.notifyItemChanged(0);
        }
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

    /**
     * sets the list of images to display in this fragment
     */
    public void setImages(final List<Image> images) {
        imageList.setItems(images);
    }

    /**
     * clears the list of images to display in this fragment
     */
    public void clearImages() {
        imageList.clearList();
    }

    /**
     * gets the list of images to display in this fragment
     */
    public List<Image> getImages() {
        return imageList.getItems();
    }

    /**
     * adjusts image persistent state to what is actually in the list
     */
    public void adjustImagePersistentState() {
        ImageUtils.deleteOfflineLogImagesFor(geocode, getImages());
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.imagelist_fragment, container, false);
        binding = ImagelistFragmentBinding.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedState) {

        imageHelper = new ImageActivityHelper(this.getActivity(), (r, imgs, uk) -> {
            final List<Image> imagesToAdd = CollectionStream.of(imgs).map(img -> ImageUtils.toLocalLogImage(geocode, img).buildUpon().setTargetScale(getFastImageAutoScale()).build()).toList();
            imageList.addItems(imagesToAdd);
        });
        imageList = new ImageListAdapter(view.findViewById(R.id.image_list));

        this.binding.imageAddMulti.setOnClickListener(v ->
                imageHelper.getMultipleImagesFromStorage(geocode, false, null));
        this.binding.imageAddCamera.setOnClickListener(v ->
                imageHelper.getImageFromCamera(geocode, false, null));

        if (savedState != null) {
            imageList.setItems(savedState.getParcelableArrayList(SAVED_STATE_IMAGELIST));
            imageHelper.setState(savedState.getBundle(SAVED_STATE_IMAGEHELPER));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_STATE_IMAGELIST, new ArrayList<>(imageList.getItems()));
        outState.putBundle(SAVED_STATE_IMAGEHELPER, imageHelper.getState());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    protected static class ImageViewHolder extends AbstractRecyclerViewHolder {
        private final ImagelistItemBinding binding;

        public ImageViewHolder(final View rowView) {
            super(rowView);
            binding = ImagelistItemBinding.bind(rowView);
        }
    }


    private final class ImageListAdapter extends ManagedListAdapter<Image, ImageViewHolder> {

        private ImageListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                    .setNotifyOnPositionChange(true)
                    .setSupportDragDrop(true));

            registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(final int positionStart, final int itemCount) {
                    rebuildImageTitles();
                }

                @Override
                public void onItemRangeRemoved(final int positionStart, final int itemCount) {
                    rebuildImageTitles();
                }
            });
        }

        private void fillViewHolder(final ImageViewHolder holder, final Image image, final int position) {
            if (image == null || holder.binding.imageThumbnail == null) {
                return;
            }
            ImageActivityHelper.displayImageAsync(image, holder.binding.imageThumbnail);
            holder.binding.imageTitle.setText(getImageTitle(image, position));
            holder.binding.imageInfo.setText(getImageInfo(image));
            holder.binding.imageDescription.setText(image.getDescription());
            holder.binding.imageDescription.setVisibility(StringUtils.isNotBlank(image.getDescription()) ? View.VISIBLE : View.GONE);
        }

        private String getImageInfo(final Image image) {

            final ContentStorage.FileInformation imageFileInfo = ImageUtils.getImageFileInfos(image);
            int width = 0;
            int height = 0;
            int scaledWidth = width;
            int scaledHeight = height;
            final ImmutablePair<Integer, Integer> widthHeight = ImageUtils.getImageSize(image.getUri());
            if (widthHeight != null) {
                width = widthHeight.getLeft();
                height = widthHeight.getRight();
                final ImmutableTriple<Integer, Integer, Boolean> scaledImageSizes = ImageUtils.calculateScaledImageSizes(width, height, image.targetScale, image.targetScale);
                scaledWidth = scaledImageSizes.left;
                scaledHeight = scaledImageSizes.middle;
            }
            final String isScaled = getString(width != scaledWidth || height != scaledHeight ? R.string.log_image_info_scaled : R.string.log_image_info_notscaled);

            final long fileSize = imageFileInfo == null ? 0 : imageFileInfo.size;
            //A rough estimation for the size of the compressed image:
            // * tenth the size due to compress
            // * linear scale by pixel size
            // * round to full KB
            final long roughCompressedSize = width * height == 0 ? 0 :
                    ((fileSize * (scaledHeight * scaledWidth) / 10 / (width * height)) / 1024) * 1024;

            return getString(R.string.log_image_info2, isScaled, scaledWidth, scaledHeight, Formatter.formatBytes(roughCompressedSize));
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagelist_item, parent, false);
            final ImageViewHolder viewHolder = new ImageViewHolder(view);
            viewHolder.itemView.setOnClickListener(view1 -> addOrEditImage(viewHolder.getAdapterPosition()));
            viewHolder.binding.imageDelete.setOnClickListener(v -> removeItem(viewHolder.getAdapterPosition()));
            registerStartDrag(viewHolder, viewHolder.binding.imageDrag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final ImageViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position), position);
        }

    }

    /**
     * trigger the start of the detailed "add image" dialog
     */
    public void startAddImageDialog() {
        addOrEditImage(-1);
    }

    /**
     * internally start the detail image edit dialog
     */
    private void addOrEditImage(final int imageIndex) {
        final Intent selectImageIntent = new Intent(this.getActivity(), ImageSelectActivity.class);
        if (imageIndex >= 0 && imageIndex < imageList.getItemCount()) {
            selectImageIntent.putExtra(Intents.EXTRA_IMAGE, imageList.getItem(imageIndex));
        }
        selectImageIntent.putExtra(Intents.EXTRA_INDEX, imageIndex);
        selectImageIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        selectImageIntent.putExtra(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE, maxImageUploadSize);
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE_CAPTION_MANDATORY, captionMandatory);

        getActivity().startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    private int getFastImageAutoScale() {
        return Settings.getLogImageScale();
    }
}

package cgeo.geocaching.ui;

import cgeo.geocaching.ImageSelectActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.ImagelistFragmentBinding;
import cgeo.geocaching.databinding.ImagelistItemBinding;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.net.Uri;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private ImagelistFragmentBinding binding;

    private ImageListAdapter imageList;
    private ImageActivityHelper imageHelper;

    //following info is used to restrict image selections and for display
    private CharSequence contextCode;
    private Long maxImageUploadSize;
    private boolean captionMandatory;

    private static final int SELECT_IMAGE = 101;

    private final Set<Uri> lastSavedStateImagePaths = new HashSet<>();


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
        binding = ImagelistFragmentBinding.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        imageHelper = new ImageActivityHelper(this.getActivity(), 3000);
        imageList = new ImageListAdapter(view.findViewById(R.id.image_list));

        this.binding.imageAddMulti.setOnClickListener(v ->
            imageHelper.getMultipleImagesFromStorage(getFastImageAutoScale(), false, imgs -> {
                imageList.addItems(imgs);
            }));
        this.binding.imageAddCamera.setOnClickListener(v ->
            imageHelper.getImageFromCamera(getFastImageAutoScale(), false, img -> {
                imageList.addItem(img);
            }));
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
        }

        private void fillViewHolder(final ImageViewHolder holder, final Image image, final int position) {
            if (image == null || holder.binding.imageThumbnail == null) {
                return;
            }
            ImageActivityHelper.displayImageAsync(image, holder.binding.imageThumbnail);
            holder.binding.imageTitle.setText(getImageTitle(image, position));
            holder.binding.imageInfo.setText(getImageInfo(image));
            holder.binding.imageDescription.setText(image.getDescription());
            holder.binding.imageDescription.setVisibility(!StringUtils.isBlank(image.getDescription()) ? View.VISIBLE : View.GONE);
        }

        private String getImageInfo(final Image image) {

            final ImmutablePair<String, Long> imageFileInfo = ImageUtils.getImageFileInfos(image);
            int width = 0;
            int height = 0;
            final ImmutablePair<Integer, Integer> widthHeight = ImageUtils.getImageSize(image.getUri());
            if (widthHeight != null) {
                width = widthHeight.getLeft();
                height = widthHeight.getRight();
            }
            return getString(R.string.log_image_info, width, height, imageFileInfo.right / 1024, imageFileInfo.left);
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
            final Set<Uri> existingPaths = CollectionStream.of(images).filter(img -> img.getUri() != null).map(img -> img.getUri()).toSet();
            for (final Uri oldPath : lastSavedStateImagePaths) {
                if (!existingPaths.contains(oldPath)) {
                    final boolean result = ImageUtils.deleteImage(oldPath);
                    Log.d("Deleting image " + oldPath + " (result: " + result + ")");
                }
            }
        }
        //refill image state
        lastSavedStateImagePaths.clear();
        for (final Image img : images) {
            if (img.getUri() != null) {
                lastSavedStateImagePaths.add(img.getUri());
            }
        }
    }

}

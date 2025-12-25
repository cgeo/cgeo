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

package cgeo.geocaching.ui

import cgeo.geocaching.ImageEditActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.databinding.ImagelistFragmentBinding
import cgeo.geocaching.databinding.ImagelistItemBinding
import cgeo.geocaching.log.LogUtils
import cgeo.geocaching.models.Image
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.Folder
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.ImageLoader
import cgeo.geocaching.utils.ImageUtils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.ImmutableTriple

/**
 * Fragment displays and maintains an image list where
 * user can select/delete/edit/sort images using various options
 * <br>
 * Activities using this fragment have to ensure to call {@link #onParentActivityResult(Int, Int, Intent)}
 * in their {@link android.app.Activity#onActivityResult(Int, Int, Intent)} method since
 * this fragment starts and works with results of intents.
 */
class ImageListFragment : Fragment() {
    private ImagelistFragmentBinding binding

    private ImageListAdapter imageList
    private ImageActivityHelper imageHelper

    //following info is used to restrict image selections and for display
    private String geocode
    private Long maxImageUploadSize
    private Uri ownImageFolderUri

    private val imageCache: ImageLoader = ImageLoader()

    private static val SELECT_IMAGE: Int = 101

    private static val SAVED_STATE_IMAGELIST: String = "cgeo.geocaching.saved_state_imagelist"
    private static val SAVED_STATE_IMAGEHELPER: String = "cgeo.geocaching.saved_state_imagehelper"

    /**
     * call once to initialize values for image retrieval
     */
    public Unit init(final String contextCode, final Long maxImageUploadSize) {
        this.geocode = contextCode
        this.maxImageUploadSize = maxImageUploadSize

        if (geocode != null) {
            val ownImageFolder: Folder = ImageUtils.getSpoilerImageFolder(geocode)
            val hasOwnImages: Boolean = ownImageFolder != null && !ContentStorage.get().list(ownImageFolder).isEmpty()
            this.ownImageFolderUri = hasOwnImages ? ownImageFolder.getUri() : null
            binding.imageAddOwn.setVisibility(hasOwnImages ? View.VISIBLE : View.GONE)
        }
    }

    /**
     * get title for an image in the list as displayed
     */
    public String getImageTitle(final Image image, final Int position) {
        return LogUtils.getLogImageTitle(image, position, imageList.getItemCount())
    }

    private Unit rebuildImageTitles() {
        // The title of the first image will vary depending on whether there is more than one image present.
        // Therefore we always update the first element if something does change.
        if (imageList.getItemCount() > 0) {
            imageList.notifyItemChanged(0)
        }
    }

    /**
     * make sure to call this method in the activites {@link android.app.Activity#onActivityResult(Int, Int, Intent)} method.
     * <br>
     * When this method returns true, this means that the result has been consumed. False otherwise.
     */
    public Boolean onParentActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        if (imageHelper.onActivityResult(requestCode, resultCode, data)) {
            return true
        }

        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                val imageIndex: Int = data.getIntExtra(Intents.EXTRA_INDEX, -1)
                val indexIsValid: Boolean = imageIndex >= 0 && imageIndex < imageList.getItemCount()
                val image: Image = data.getParcelableExtra(Intents.EXTRA_IMAGE)
                if (image != null && indexIsValid) {
                    imageList.updateItem(image, imageIndex)
                } else if (image != null) {
                    imageList.addItem(image)
                }
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed, advise user
                ActivityMixin.showToast(getActivity(), getString(R.string.err_select_logimage_failed))
            }
            return true

        }
        return false
    }

    /**
     * sets the list of images to display in this fragment
     */
    public Unit setImages(final List<Image> images) {
        imageList.setItems(images)
    }

    /**
     * clears the list of images to display in this fragment
     */
    public Unit clearImages() {
        imageList.clearList()
    }

    /**
     * gets the list of images to display in this fragment
     */
    public List<Image> getImages() {
        return imageList.getItems()
    }

    /**
     * adjusts image persistent state to what is actually in the list
     */
    public Unit adjustImagePersistentState() {
        ImageUtils.deleteOfflineLogImagesFor(geocode, getImages())
    }

    override     public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.imagelist_fragment, container, false)
        binding = ImagelistFragmentBinding.bind(view)
        return view
    }

    override     public Unit onViewCreated(final View view, final Bundle savedState) {

        imageHelper = ImageActivityHelper(this.getActivity(), (r, imgs, uk) -> {
            val imagesToAdd: List<Image> = CollectionStream.of(imgs).map(img -> ImageUtils.toLocalLogImage(geocode, img).buildUpon().setTargetScale(getFastImageAutoScale()).build()).toList()
            imageList.addItems(imagesToAdd)
        })
        imageList = ImageListAdapter(view.findViewById(R.id.image_list))

        this.binding.imageAddMulti.setOnClickListener(v ->
                imageHelper.getMultipleImagesFromStorage(geocode, false, null, null))
        this.binding.imageAddCamera.setOnClickListener(v ->
                imageHelper.getImageFromCamera(geocode, false, null))
        this.binding.imageAddOwn.setOnClickListener(v ->
                imageHelper.getMultipleImagesFromStorage(geocode, false, null, ownImageFolderUri))

        if (savedState != null) {
            imageList.setItems(savedState.getParcelableArrayList(SAVED_STATE_IMAGELIST))
            imageHelper.setState(savedState.getBundle(SAVED_STATE_IMAGEHELPER))
        }
    }

    override     public Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SAVED_STATE_IMAGELIST, ArrayList<>(imageList.getItems()))
        outState.putBundle(SAVED_STATE_IMAGEHELPER, imageHelper.getState())
    }

    /** @noinspection EmptyMethod*/
    override     public Unit onDestroyView() {
        super.onDestroyView()
    }

    override     public Unit onDestroy() {
        super.onDestroy()
        imageCache.clear()
    }


    protected static class ImageViewHolder : AbstractRecyclerViewHolder() {
        private final ImagelistItemBinding binding
        private Image entry

        public ImageViewHolder(final View rowView) {
            super(rowView)
            binding = ImagelistItemBinding.bind(rowView)
        }
    }


    private class ImageListAdapter : ManagedListAdapter()<Image, ImageViewHolder> {

        private ImageListAdapter(final RecyclerView recyclerView) {
            super(ManagedListAdapter.Config(recyclerView)
                    .setNotifyOnPositionChange(true)
                    .setSupportDragDrop(true))

            registerAdapterDataObserver(RecyclerView.AdapterDataObserver() {
                override                 public Unit onItemRangeInserted(final Int positionStart, final Int itemCount) {
                    rebuildImageTitles()
                }

                override                 public Unit onItemRangeRemoved(final Int positionStart, final Int itemCount) {
                    rebuildImageTitles()
                }
            })
        }

        private Unit fillViewHolder(final ImageViewHolder holder, final Image image, final Int position) {
            if (image == null || holder.binding.imageThumbnail == null) {
                return
            }
            holder.entry = image

            holder.binding.imageInfo.setText("...")

            holder.binding.imageTitle.setText(getImageTitle(image, position))
            holder.binding.imageDescription.setText(image.getDescription())
            holder.binding.imageDescription.setVisibility(StringUtils.isNotBlank(image.getDescription()) ? View.VISIBLE : View.GONE)

            holder.binding.imageThumbnail.setImagePreload(image)
            if (image.isImageOrUnknownUri()) {
                imageCache.loadImage(image.getUrl(), imageData -> {
                    if (holder.entry == null || !holder.entry.getUrl() == (image.getUrl())) {
                        //holder holds a different image meanwhile, skip showing the image
                        return
                    }
                    holder.binding.imageThumbnail.setImagePostload(image, imageData.bitmapDrawable, imageData.metadata)
                    holder.binding.imageInfo.setText(getImageInfo(imageData.localUri, image.targetScale))
                })
            }
        }

        private String getImageInfo(final Uri localImageUri, final Int targetScale) {

            Int width = 0
            Int height = 0
            Int scaledWidth = width
            Int scaledHeight = height
            val widthHeight: ImmutablePair<Integer, Integer> = localImageUri == null ? null : ImageUtils.getImageSize(localImageUri)
            if (widthHeight != null) {
                width = widthHeight.getLeft()
                height = widthHeight.getRight()
                val scaledImageSizes: ImmutableTriple<Integer, Integer, Boolean> = ImageUtils.calculateScaledImageSizes(width, height, targetScale, targetScale)
                scaledWidth = scaledImageSizes.left
                scaledHeight = scaledImageSizes.middle
            }
            val isScaled: String = getString(width != scaledWidth || height != scaledHeight ? R.string.log_image_info_scaled : R.string.log_image_info_notscaled)

            final ContentStorage.FileInformation imageFileInfo = localImageUri == null ? null : ContentStorage.get().getFileInfo(localImageUri)
            val fileSize: Long = imageFileInfo == null ? 0 : imageFileInfo.size
            //A rough estimation for the size of the compressed image:
            // * tenth the size due to compress
            // * linear scale by pixel size
            // * round to full KB
            val roughCompressedSize: Long = width * height == 0 ? 0 :
                ((fileSize * ((Long) scaledHeight * scaledWidth) / 10 / ((Long) width * height)) / 1024) * 1024

            return getString(R.string.log_image_info2, isScaled, scaledWidth, scaledHeight, Formatter.formatBytes(roughCompressedSize))
        }

        override         public ImageViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagelist_item, parent, false)
            val viewHolder: ImageViewHolder = ImageViewHolder(view)
            viewHolder.itemView.setOnClickListener(view1 -> addOrEditImage(viewHolder.getAdapterPosition()))
            viewHolder.binding.imageDelete.setOnClickListener(v -> removeItem(viewHolder.getAdapterPosition()))
            registerStartDrag(viewHolder, viewHolder.binding.imageDrag)
            return viewHolder
        }

        override         public Unit onBindViewHolder(final ImageViewHolder holder, final Int position) {
            fillViewHolder(holder, getItem(position), position)
        }

    }

    /**
     * internally start the detail image edit dialog
     */
    private Unit addOrEditImage(final Int imageIndex) {
        val selectImageIntent: Intent = Intent(this.getActivity(), ImageEditActivity.class)
        if (imageIndex >= 0 && imageIndex < imageList.getItemCount()) {
            selectImageIntent.putExtra(Intents.EXTRA_IMAGE, imageList.getItem(imageIndex))
        }
        selectImageIntent.putExtra(Intents.EXTRA_INDEX, imageIndex)
        selectImageIntent.putExtra(Intents.EXTRA_GEOCODE, geocode)
        selectImageIntent.putExtra(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE, maxImageUploadSize)

        requireActivity().startActivityForResult(selectImageIntent, SELECT_IMAGE)
    }

    private Int getFastImageAutoScale() {
        return Settings.getLogImageScale()
    }
}

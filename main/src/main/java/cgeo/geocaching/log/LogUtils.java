package cgeo.geocaching.log;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.ComponentActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

@TargetApi(24)
public final class LogUtils {

    private LogUtils() {
        //no instance
    }

    /** get title for a log image in the list */
    public static String getLogImageTitle(final Image image, final int imagePos, final int imageCount) {
        if (StringUtils.isNotBlank(image.getTitle())) {
            return image.getTitle();
        }
        final String imageTitlePrafix = Settings.getLogImageCaptionDefaultPraefix();
        if (imageCount <= 1) {
            return imageTitlePrafix; // number is unnecessary if only one image is posted
        }
        return imageTitlePrafix + " " + (imagePos + 1);
    }

    public static boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsDeleteLog(logEntry);
    }

    public static boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsEditLog(logEntry);
    }

    public static void startEditLog(final Context activity, final Geocache cache, final LogEntry entry) {
        LogCacheActivity.startForEdit(activity, cache.getGeocode(), entry);
    }

    public static void editLog(final ComponentActivity activity, final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry, final Consumer<LogResult> postExecute) {
        if (!canEditLog(cache, newEntry)) {
            ActivityMixin.showToast(activity, "Can't edit log");
            return;
        }

        WorkerTask.<Void, String, LogResult>of(activity, () -> (input, progress, isCancelled) -> editLogTaskLogic(cache, oldEntry, newEntry, progress))
            .addFeature(ProgressDialogFeature.of(activity).setTitle("Editing").setInitialMessage("Editing log"))
            .addResultListener(result -> {
                SimpleDialog.ofContext(activity).setTitle(TextParam.text("Edit result: " + result.isOk()))
                    .setMessage(TextParam.text("EDIT RESULT: " + result)).show(() -> {
                        if (postExecute != null) {
                            postExecute.accept(result);
                        }
                    });
            })
            .start();
    }


    public static void deleteLog(final ComponentActivity activity, final Geocache cache, final LogEntry entry) {
        if (!canDeleteLog(cache, entry)) {
            ActivityMixin.showToast(activity, "Can't delete log");
            return;
        }
        SimpleDialog.ofContext(activity)
            .setTitle(TextParam.text("Delete log"))
            .setMessage(TextParam.text("Really delete log '" + entry.log + "' of cache '" + cache.getGeocode() + "'?"))
            .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
            .confirm(() -> {
                WorkerTask.ofSimple(activity, () -> deleteLogTaskLogic(cache, entry))
                    .addFeature(ProgressDialogFeature.of(activity).setTitle("Deleting").setInitialMessage("Deleting log"))
                    .addResultListener(result -> {
                        SimpleDialog.ofContext(activity).setTitle(TextParam.text("DELETE RESULT: " + result.isOk())).show();
                    })
                    .start();
            });
    }

    @WorkerThread
    private static LogResult editLogTaskLogic(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry, final Consumer<String> progress) {
        final ILoggingManager loggingManager = cache.getLoggingManager();
        //Upload changed log entry itself
        progress.accept("1: send changed log");
        final LogResult result = cache.getLoggingManager().editLog(newEntry);
        if (!result.isOk()) {
            return result;
        }

        //Upload images
        final ImmutableTriple<ImageResult, List<Image>, Runnable> imgResult = updateImages(loggingManager, newEntry.serviceLogId, oldEntry.logImages, newEntry.logImages, progress);
        if (!imgResult.left.isOk()) {
            return LogResult.error(imgResult.left.getStatusCode(), imgResult.left.getPostServerMessage(), null);
        }

        //adapt logs in database
        final LogEntry adaptedNewLogEntry = newEntry.buildUpon().setLogImages(imgResult.middle).build();
        changeLogsInDatabase(cache, logs -> {
            final ListIterator<LogEntry> it = logs.listIterator();
            while (it.hasNext()) {
                final LogEntry log = it.next();
                if (Objects.requireNonNull(adaptedNewLogEntry.serviceLogId).equals(log.serviceLogId)) {
                    it.set(adaptedNewLogEntry.buildUpon().setAuthor(log.author).build());
                    break;
                }
            }
        });
        //adapt commons in database
        adaptCommonsForNewLog(cache, loggingManager, oldEntry, adaptedNewLogEntry);
        DataStore.saveChangedCache(cache);

        //Cleanup
        imgResult.right.run();

        cache.notifyChange();

        return result;

    }

    private static void adaptCommonsForNewLog(final Geocache cache, final ILoggingManager loggingManager, @Nullable final LogEntry oldLogEntry, final LogEntry newLogEntry) {

        final IConnector cacheConnector = loggingManager.getConnector();

        //update "found" counter
        final boolean oldIsFound = oldLogEntry != null && oldLogEntry.logType.isFoundLog();
        final boolean newIsFound = newLogEntry.logType.isFoundLog();
        if (cacheConnector instanceof ILogin && oldIsFound != newIsFound) {
            ((ILogin) cacheConnector).increaseCachesFound(newIsFound ? 1 : -1);
        }

        //update cache state
        for (LogEntry log : cache.getLogs()) {
            if (log.logType == LogType.TEMP_DISABLE_LISTING) {
                cache.setDisabled(true);
                break;
            } else if (log.logType == LogType.ENABLE_LISTING) {
                cache.setDisabled(false);
                break;
            }
        }

        //update own found state
        if (newLogEntry.date >= cache.getVisitedDate()) {
            if (newLogEntry.logType.isFoundLog()) {
                cache.setFound(true);
                cache.setVisitedDate(newLogEntry.date);
            } else if (newLogEntry.logType == LogType.DIDNT_FIND_IT) {
                cache.setDNF(true);
                cache.setVisitedDate(newLogEntry.date);
            }
        }
    }

    @WorkerThread
    private static LogResult deleteLogTaskLogic(final Geocache cache, final LogEntry logEntry) {
        final LogResult result = cache.getLoggingManager().deleteLog(logEntry);
        if (!result.isOk()) {
            return result;
        }
        changeLogsInDatabase(cache, logs -> {
            final Iterator<LogEntry> it = logs.listIterator();
            while (it.hasNext()) {
                final LogEntry log = it.next();
                if (Objects.requireNonNull(logEntry.serviceLogId).equals(log.serviceLogId)) {
                    it.remove();
                    break;
                }
            }
        });
        cache.notifyChange();
        return result;
    }

    @WorkerThread
    private static void changeLogsInDatabase(final Geocache cache, final Consumer<List<LogEntry>> logChanger) {
        final List<LogEntry> logs = new ArrayList<>(cache.getLogs());
        logChanger.accept(logs);
        DataStore.saveLogs(cache.getGeocode(), logs, true);
    }

    @WorkerThread
    private static ImageResult uploadNewImage(final ILoggingManager loggingManager, final String logId, final Image image, final int pos, final int imageCount) {

        //uploader can only deal with files, not with content Uris. Thus scale/compress into a temporary file
        final File imageFileForUpload = ImageUtils.scaleAndCompressImageToTemporaryFile(image.getUri(), image.targetScale, 75);
        if (imageFileForUpload == null) {
            return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "Failed to process: " + image.getUrl(), null);
        }

        final Image imgToSend = image.buildUpon()
            .setUrl(Uri.fromFile(imageFileForUpload))
            .setTitle(getLogImageTitle(image, pos, imageCount))
            .build();
        final ImageResult result = loggingManager.createLogImage(logId, imgToSend);

        //delete temp file for upload
        if (!imageFileForUpload.delete()) {
            Log.i("Temporary image not deleted: " + imageFileForUpload);
        }

        return result;
    }

    @WorkerThread
    private static ImmutableTriple<ImageResult, List<Image>, Runnable> updateImages(final ILoggingManager loggingManager, final String logId, final List<Image> oldImages, final List<Image> newImages, final Consumer<String> progress) {

        final Map<String, Image> oldImageMap = oldImages.stream()
            .filter(img -> img.serviceImageId != null)
            .collect(Collectors.toMap(img -> img.serviceImageId, img -> img, (i1, i2) -> i1, HashMap::new));

        ImageResult result = ImageResult.ok("");
        final List<Image> adaptedImageList = new ArrayList<>();
        final List<Uri> localImageUrisToDelete = new ArrayList<>();

        int imgPos = 0;
        int imgCount = newImages.size();
        for (Image newImg : newImages) {
            final Image oldImg = oldImageMap.get(newImg.serviceImageId);
            final String newTitle = getLogImageTitle(newImg, imgPos, imgCount);

            if (newImg.serviceImageId == null || oldImg == null) {
                //New Image -> upload
                progress.accept("Image " + (imgPos + 1) + "/" + imgCount + ": upload new image");
                result = uploadNewImage(loggingManager, logId, newImg, imgPos, imgCount);
                adaptedImageList.add(newImg.buildUpon()
                    .setServiceImageId(result.getServiceImageId())
                    .setUrl(result.getImageUri())
                    .setTitle(newTitle)
                    .build());

                if (result.isOk()) {
                    localImageUrisToDelete.add(newImg.getUri());
                }

            } else if (loggingManager.supportsEditLogImages() && !imagePropertiesEqual(oldImg, newImg, newTitle)) {
                //Existing image with changed properties -> edit
                progress.accept("Image " + (imgPos + 1) + "/" + imgCount + ": edit properties");
                result = loggingManager.editLogImage(logId, newImg.serviceImageId, getLogImageTitle(newImg, imgPos, imgCount), newImg.getDescription());
                oldImageMap.remove(newImg.serviceImageId);
                adaptedImageList.add(newImg.buildUpon()
                    .setTitle(newTitle)
                    .build());
            }
            if (!result.isOk()) {
                return new ImmutableTriple<>(result, Collections.emptyList(), () -> { });
            }
            imgPos++;
        }

        if (loggingManager.supportsDeleteLogImages()) {
            imgPos = 0;
            imgCount = oldImageMap.size();
            for (String oldImgIdToRemove : oldImageMap.keySet()) {
                progress.accept("Old Image " + (imgPos + 1) + "/" + imgCount + ": delete");
                //remove image from log
                result = loggingManager.deleteLogImage(logId, oldImgIdToRemove);
                if (!result.isOk()) {
                    return new ImmutableTriple<>(result, Collections.emptyList(), () -> { });
                }
            }
        }
        return new ImmutableTriple<>(result, adaptedImageList, () -> {
            for (Uri uri : localImageUrisToDelete) {
                ImageUtils.deleteImage(uri);
            }
        });
    }

    private static boolean imagePropertiesEqual(final Image oldImg, final Image newImg, final String newTitle) {
        return Objects.equals(oldImg.title, newTitle) && Objects.equals(oldImg.getDescription(), newImg.getDescription());
    }


}

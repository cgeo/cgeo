package cgeo.geocaching.service;

import cgeo.geocaching.Intents;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Image;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;

import androidx.test.core.app.ApplicationProvider;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogPostingServiceTest {

    @Test
    public void editRetryIntentKeepsOriginalAndEditedEntries() {
        final Image originalImageKept = new Image.Builder()
                .setServiceImageId("kept-id")
                .setUrl(Uri.parse("https://example.invalid/kept.jpg"))
                .setTitle("old title")
                .build();
        final Image originalImageRemoved = new Image.Builder()
                .setServiceImageId("removed-id")
                .setUrl(Uri.parse("https://example.invalid/removed.jpg"))
                .build();
        final Image editedImageKept = originalImageKept.buildUpon().setTitle("new title").build();
        final Image editedImageAdded = new Image.Builder()
                .setUrl(Uri.parse("file:///tmp/new-image.jpg"))
                .setTitle("new image")
                .build();

        final LogEntry originalEntry = new LogEntry.Builder()
                .setServiceLogId("GL123")
                .setLogType(LogType.NOTE)
                .setLog("original text")
                .setLogImages(Arrays.asList(originalImageKept, originalImageRemoved))
                .build();
        final OfflineLogEntry editedEntry = new OfflineLogEntry.Builder()
                .setServiceLogId("GL123")
                .setLogType(LogType.NOTE)
                .setLog("edited text")
                .setFavorite(true)
                .setRating(4.5f)
                .setPassword("edit password")
                .setImageScale(1234)
                .setImageTitlePraefix("retry image")
                .setLogImages(Arrays.asList(editedImageKept, editedImageAdded))
                .build();

        final Context context = ApplicationProvider.getApplicationContext();
        final Intent sourceIntent = LogPostingService.buildRetryActivityIntent(context, "GC123", originalEntry, editedEntry);
        final Parcel parcel = Parcel.obtain();
        final Intent intent;
        try {
            sourceIntent.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            intent = Intent.CREATOR.createFromParcel(parcel);
            intent.setExtrasClassLoader(OfflineLogEntry.class.getClassLoader());
        } finally {
            parcel.recycle();
        }
        final LogEntry restoredOriginal = intent.getParcelableExtra(Intents.EXTRA_LOGENTRY);
        final OfflineLogEntry restoredEdit = intent.getParcelableExtra(Intents.EXTRA_LOGENTRY_EDITED);

        assertThat(intent.getStringExtra(Intents.EXTRA_GEOCODE)).isEqualTo("GC123");
        assertThat(restoredOriginal.log).isEqualTo("original text");
        assertThat(restoredOriginal.serviceLogId).isEqualTo("GL123");
        assertThat(restoredOriginal.logImages).extracting(image -> image.serviceImageId)
                .containsExactly("kept-id", "removed-id");
        assertThat(restoredOriginal.logImages).extracting(image -> image.getUri().toString())
                .containsExactly("https://example.invalid/kept.jpg", "https://example.invalid/removed.jpg");
        assertThat(restoredEdit).isInstanceOf(OfflineLogEntry.class);
        assertThat(restoredEdit.log).isEqualTo("edited text");
        assertThat(restoredEdit.serviceLogId).isEqualTo("GL123");
        assertThat(restoredEdit.favorite).isTrue();
        assertThat(restoredEdit.rating).isEqualTo(4.5f);
        assertThat(restoredEdit.password).isEqualTo("edit password");
        assertThat(restoredEdit.imageScale).isEqualTo(1234);
        assertThat(restoredEdit.imageTitlePraefix).isEqualTo("retry image");
        assertThat(restoredEdit.logImages).extracting(image -> image.serviceImageId)
                .containsExactly("kept-id", null);
        assertThat(restoredEdit.logImages).extracting(image -> image.getUri().toString())
                .containsExactly("https://example.invalid/kept.jpg", "file:///tmp/new-image.jpg");
        assertThat(restoredEdit.logImages).extracting(Image::getTitle)
                .containsExactly("new title", "new image");
    }

    @Test
    public void createRetryIntentRemainsCompatibleWithoutEditEntries() {
        final Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = LogPostingService.buildRetryActivityIntent(context, "GC123", null, null);

        assertThat(intent.getStringExtra(Intents.EXTRA_GEOCODE)).isEqualTo("GC123");
        assertThat(intent.hasExtra(Intents.EXTRA_LOGENTRY)).isFalse();
        assertThat(intent.hasExtra(Intents.EXTRA_LOGENTRY_EDITED)).isFalse();
    }

    @Test
    public void legacyEditIntentRemainsCompatibleWithoutEditedEntry() {
        final Context context = ApplicationProvider.getApplicationContext();
        final LogEntry originalEntry = new LogEntry.Builder()
                .setServiceLogId("GL123")
                .setLogType(LogType.NOTE)
                .setLog("original text")
                .setLogImages(Collections.emptyList())
                .build();

        final Intent intent = LogPostingService.buildRetryActivityIntent(context, "GC123", originalEntry, null);

        assertThat(intent.getParcelableExtra(Intents.EXTRA_LOGENTRY)).isEqualTo(originalEntry);
        assertThat(intent.hasExtra(Intents.EXTRA_LOGENTRY_EDITED)).isFalse();
    }
}

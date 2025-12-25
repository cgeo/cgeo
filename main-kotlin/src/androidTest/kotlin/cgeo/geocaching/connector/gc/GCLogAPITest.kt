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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.R
import cgeo.geocaching.connector.ImageResult
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.connector.StatusResult
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.log.TrackableLogEntry
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.test.NotForIntegrationTests
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.UriUtils

import android.net.Uri

import java.io.File
import java.util.Collections
import java.util.Date
import java.util.List
import java.util.function.Supplier

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GCLogAPITest {

    private static val TEST_GEOCODE: String = "GC8H4YX"; // Am Hart
    private static val TEST_TRAVELBUG: String = "TB7YFQV"; // The shirt, the duck and the sea

    @Test
    @NotForIntegrationTests
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") //Asserts are in submethods
    public Unit cacheLoggingLifecycleTest() {
        GCLogin.getInstance().login()
        val logId: String = executeCacheTest("createCacheLog", null, this::createCacheLog)
        executeCacheTest("editCacheLog", logId, () -> editCacheLog(logId))
        val logImageId: String = executeCacheTest("addImage", logId, () -> addImage(logId))
        executeCacheTest("editImage", logId, () -> editImage(logId, logImageId))
        executeCacheTest("editLogPreserveImage", logId, () -> editCacheLogPreserveImage(logId, logImageId))
        executeCacheTest("deleteImage", logId, () -> deleteImage(logId, logImageId))
        executeCacheTest("deleteLog", logId, () -> deleteCacheLog(logId))
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") //Asserts are in submethods
    @NotForIntegrationTests
    public Unit trackableLoggingLifecycleTest() {
        GCLogin.getInstance().login()

        val logTextMarker: String = "MARKER-" + Date().getTime() + ":"

        val logId: String = executeTrackableTest("createTrackableLog", null, () -> createTrackableLog(logTextMarker))
        executeTrackableTest("deleteTrackableLog", logId, () -> deleteTrackableLog(logId, logTextMarker))
    }

    private String createCacheLog() {
        val logTime: Long = Date().getTime()
        val logEntry: OfflineLogEntry = OfflineLogEntry.Builder()
            .setLog("This is a test log (should be deleted automatically)")
            .setDate(logTime)
            .setLogType(LogType.NOTE)
            .build()
        val result: LogResult = GCLogAPI.createLog(TEST_GEOCODE, logEntry, Collections.emptyMap())
        assertStatusOk(result)
        assertThat(result.getServiceLogId()).startsWith("GL")
        val check: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, result.getServiceLogId())
        assertThat(check).isNotNull()
        assertThat(check.log).isEqualTo("This is a test log (should be deleted automatically)")

        return result.getServiceLogId()
    }

    private String editCacheLog(final String logId) {
        val newLogTime: Long = Date().getTime()
        val newLogEntry: LogEntry = LogEntry.Builder()
            .setServiceLogId(logId)
            .setLog("This is a test log (should be deleted automatically)")
            .setDate(newLogTime)
            .setLogType(LogType.NOTE)
            .build()
        val result: LogResult = GCLogAPI.editLog(TEST_GEOCODE, newLogEntry)
        assertStatusOk(result)
        assertThat(result.getServiceLogId()).startsWith("GL")
        val check: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, result.getServiceLogId())
        assertThat(check).isNotNull()
        assertThat(check.log).isEqualTo("This is a test log (should be deleted automatically)")
        return logId
    }

    private String deleteCacheLog(final String logId) {
        val result: LogResult = GCLogAPI.deleteLog(logId, null)
        assertStatusOk(result)
        assertThat(result.getServiceLogId()).startsWith("GL")
        assertThat(result.getServiceLogId()).isEqualTo(logId)
        val check2: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, result.getServiceLogId())
        assertThat(check2).isNull()
        return logId
    }

    private String createTrackableLog(final String logTextMarker) {
        val tLog: TrackableLogEntry = TrackableLogEntry.of(TEST_TRAVELBUG, null, TrackableBrand.TRAVELBUG)
        tLog.setAction(LogTypeTrackable.NOTE)
        val result: LogResult = GCLogAPI.createLogTrackable(tLog, Date(), logTextMarker + "This is a test log (will be deleted automatically)")

        assertStatusOk(result)
        assertThat(result.getServiceLogId()).startsWith("TL")
        val check: LogEntry = retrieveTrackableLogEntry(TEST_TRAVELBUG, logTextMarker)
        assertThat(check).isNotNull()
        assertThat(check.log).isEqualTo(logTextMarker + "This is a test log (will be deleted automatically)")

        return result.getServiceLogId()
    }

    private String deleteTrackableLog(final String logId, final String logTextMarker) {
        val result: LogResult = GCLogAPI.deleteLogTrackable(logId)
        assertStatusOk(result)
        assertThat(result.getServiceLogId()).startsWith("TL")
        assertThat(result.getServiceLogId()).isEqualTo(logId)
        val check2: LogEntry = retrieveTrackableLogEntry(TEST_TRAVELBUG, logTextMarker)
        assertThat(check2).isNull()
        return logId
    }

    private String addImage(final String logId) {
        val logImage: Image = Image.Builder()
            .setTitle("logImageTitle")
            .setDescription("logImageDescription")
            .setUrl(getLogFileUri()).build()

        val result: ImageResult = GCLogAPI.addLogImage(logId, logImage)
        assertStatusOk(result)

        val check: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, logId)
        assertThat(check.logImages).hasSize(1)
        assertThat(check.logImages.get(0).getTitle()).isEqualTo("logImageTitle")
        assertThat(check.logImages.get(0).getDescription()).isEqualTo("logImageDescription")
        assertThat(check.logImages.get(0).serviceImageId).isNotNull()
        assertThat(check.logImages.get(0).serviceImageId).startsWith(result.getServiceImageId())

        return result.getServiceImageId()
    }

    private String editImage(final String logId, final String logImageId) {
        val result: ImageResult = GCLogAPI.editLogImageData(logId, logImageId, "Image Name", "Image Description")
        assertStatusOk(result)
        assertThat(result.getServiceImageId()).isEqualTo(logImageId)

        val check: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, logId)
        assertThat(check.logImages).hasSize(1)
        assertThat(check.logImages.get(0).getTitle()).isEqualTo("Image Name")
        assertThat(check.logImages.get(0).getDescription()).isEqualTo("Image Description")
        assertThat(check.logImages.get(0).serviceImageId).startsWith(logImageId)

        return logImageId
    }

    private String editCacheLogPreserveImage(final String logId, final String logImageId) {

        //Construct an edited log entry. This test is to ensure that images are preserved on log edit.
        val newLogEntry: LogEntry = LogEntry.Builder()
            .setServiceLogId(logId)
            .setLog("This is a third test log (should be deleted automatically)")
            .setDate(Date().getTime())
            .setLogType(LogType.NOTE)
            .setLogImages(Collections.singletonList(Image.Builder().setServiceImageId(logImageId).build()))
            .build()
        val result: LogResult = GCLogAPI.editLog(TEST_GEOCODE, newLogEntry)
        assertStatusOk(result)
        assertThat(result.getServiceLogId()).isEqualTo(logId)
        val check: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, result.getServiceLogId())
        assertThat(check).isNotNull()
        assertThat(check.log).isEqualTo("This is a third test log (should be deleted automatically)")
        assertThat(check.logImages).hasSize(1)
        assertThat(check.logImages.get(0).serviceImageId).startsWith(logImageId)
        return logId
    }

    private String deleteImage(final String logId, final String logImageId) {
        val result: ImageResult = GCLogAPI.deleteLogImage(logId, logImageId)
        assertStatusOk(result)
        assertThat(result.getServiceImageId()).isEqualTo(logImageId)

        val check: LogEntry = retrieveCacheLogEntry(TEST_GEOCODE, logId)
        assertThat(check.logImages).isEmpty()

        return logImageId
    }

    private Uri getLogFileUri() {
        val uri: Uri = UriUtils.resourceToUri(R.drawable.cgeo_notification); //PNG resource
        val file: File = ImageUtils.scaleAndCompressImageToTemporaryFile(uri, -1, -1)
        file.deleteOnExit()
        return Uri.fromFile(file)
    }

    private Unit assertStatusOk(final StatusResult result) {
        //assert in a way which will lead to a meaningful error message on CI server in case of failure
        //-> postservermessage should be included
        assertThat(result.getStatusCode() + ":" + result.getPostServerMessage()).startsWith(StatusCode.NO_ERROR.toString())
    }

    private <T> T executeCacheTest(final String testId, final String logId, final Supplier<T> test) {
        return executeTest(testId, logId, false, test)
    }

    private <T> T executeTrackableTest(final String testId, final String logId, final Supplier<T> test) {
        return executeTest(testId, logId, true, test)
    }

    private <T> T executeTest(final String testId, final String logId, final Boolean isTrackable, final Supplier<T> test) {
        try {
            return test.get()
        } catch (Exception | AssertionError ae) {
            //try to clean up
            if (logId != null) {
                try {
                    if (isTrackable) {
                        GCLogAPI.deleteLogTrackable(logId)
                    } else {
                        GCLogAPI.deleteLog(logId, null)
                    }
                } catch (Exception ex) {
                    Log.w("Cleanup of test failed", ex)
                }
            }
            throw AssertionError("FAILED for '" + testId + "'(logId = " + logId + "): " + ae, ae)
        }
    }

    private LogEntry retrieveCacheLogEntry(final String geocode, final String logId) {
        val list: List<LogEntry> = GCParser.loadLogs(geocode, GCParser.Logs.OWN, 30)
        for (LogEntry le : list) {
            if (logId == (le.serviceLogId)) {
                return le
            }
        }
        return null
    }

    private LogEntry retrieveTrackableLogEntry(final String geocode, final String logTextMarker) {
        val trackable: Trackable = GCParser.searchTrackable(geocode, null, null)
        if (trackable == null) {
            return null
        }
        for (LogEntry le : trackable.getLogs()) {
            if (le.log != null && le.log.contains(logTextMarker)) {
                return le
            }
        }
        return null
    }
}

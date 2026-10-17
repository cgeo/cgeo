package cgeo.geocaching.log;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogUtilsTest {

    @Test
    public void trimForPublishingRemovesLeadingAndTrailingWhitespace() {
        // #6631: the offline draft is kept verbatim, but publishing trims outer whitespace
        assertThat(LogUtils.trimForPublishing("\n\n\nmy signature")).isEqualTo("my signature");
        assertThat(LogUtils.trimForPublishing("text\n\n")).isEqualTo("text");
    }

    @Test
    public void trimForPublishingKeepsInnerNewlines() {
        assertThat(LogUtils.trimForPublishing("my text\n\n\nmy signature")).isEqualTo("my text\n\n\nmy signature");
    }

    @Test
    public void createLogDuplicateRemovalKeepsExistingLogsWithoutRemoteId() {
        final LogEntry withoutRemoteId = new LogEntry.Builder().setLogType(LogType.NOTE).setLog("local").build();
        final LogEntry existingMain = new LogEntry.Builder().setLogType(LogType.NOTE)
                .setServiceLogId("main-id").setLog("old main").build();
        final List<LogEntry> logs = new ArrayList<>(List.of(withoutRemoteId, existingMain));

        LogUtils.removeCreateLogDuplicates(logs, "main-id", null);

        assertThat(logs).containsExactly(withoutRemoteId);
    }

    @Test
    public void endedPostUiDecisionResetsOnlyMatchingPage() {
        final OfflineLogEntry a = new OfflineLogEntry.Builder().setLogType(LogType.NOTE).setDate(1L).setLog("A").build();
        final OfflineLogEntry b = a.buildUponOfflineLogEntry().setLog("B").build();
        final CreateLogPostState state = CreateLogPostState.create("GC123", a, null, false, null);

        final LogCacheActivity.EndedPostUiDecision notFoundA =
                LogCacheActivity.endedPostUiDecision(state, state.postId, null, a);
        final LogCacheActivity.EndedPostUiDecision notFoundB =
                LogCacheActivity.endedPostUiDecision(state, state.postId, null, b);
        final LogCacheActivity.EndedPostUiDecision removeFailed =
                LogCacheActivity.endedPostUiDecision(state, state.postId, state, a);

        final LogCacheActivity.EndedPostUiDecision completedA =
                LogCacheActivity.endedPostUiDecision(state, state.postId, state, a);
        final LogCacheActivity.EndedPostUiDecision abandonedB =
                LogCacheActivity.endedPostUiDecision(state, state.postId, state, b);

        assertThat(notFoundA.clearTaskReference).isTrue();
        assertThat(notFoundA.resetPage).isTrue();
        assertThat(notFoundB.clearTaskReference).isTrue();
        assertThat(notFoundB.resetPage).isFalse();
        assertThat(removeFailed.clearTaskReference).isTrue();
        assertThat(removeFailed.resetPage).isTrue();
        assertThat(completedA.clearTaskReference).isTrue();
        assertThat(completedA.resetPage).isTrue();
        assertThat(abandonedB.clearTaskReference).isTrue();
        assertThat(abandonedB.resetPage).isFalse();
    }

    @Test
    public void persistentOfflineListDetectionMatchesCacheSemantics() {
        assertThat(LogCacheActivity.hasPersistentOfflineList(null)).isFalse();
        assertThat(LogCacheActivity.hasPersistentOfflineList(Set.of())).isFalse();
        assertThat(LogCacheActivity.hasPersistentOfflineList(Set.of(0))).isFalse();
        assertThat(LogCacheActivity.hasPersistentOfflineList(Set.of(42))).isTrue();
        assertThat(LogCacheActivity.hasPersistentOfflineList(Set.of(0, 42))).isTrue();
    }

    @Test
    public void storedDraftComparisonRejectsMissingImagesAndTrackableActions() {
        final OfflineLogEntry expected = new OfflineLogEntry.Builder().setLogType(LogType.NOTE).setDate(1L)
                .setLog("draft").addInventoryAction("TB1", LogTypeTrackable.VISITED)
                .addLogImage(new Image.Builder().setUrl(Uri.parse("file:///one.jpg")).build()).build();
        final OfflineLogEntry missingImage = new OfflineLogEntry.Builder().setLogType(LogType.NOTE).setDate(1L)
                .setLog("draft").addInventoryAction("TB1", LogTypeTrackable.VISITED).build();
        final OfflineLogEntry missingTrackable = expected.buildUponOfflineLogEntry().clearInventoryActions().build();

        assertThat(LogCacheActivity.entryMatchesStoredDraft(expected, expected)).isTrue();
        assertThat(LogCacheActivity.entryMatchesStoredDraft(expected, missingImage)).isFalse();
        assertThat(LogCacheActivity.entryMatchesStoredDraft(expected, missingTrackable)).isFalse();
    }

    @Test
    public void localCacheStateComparisonIncludesVisitedDateAndMaintenanceAttributes() {
        final Geocache expected = new Geocache();
        expected.setGeocode("GC123");
        expected.setVisitedDate(123L);
        expected.getAttributes().add("firstaid_yes");
        final Geocache staleDate = new Geocache();
        staleDate.setGeocode("GC123");
        staleDate.setVisitedDate(0L);
        staleDate.getAttributes().add("firstaid_yes");
        final Geocache staleAttributes = new Geocache();
        staleAttributes.setGeocode("GC123");
        staleAttributes.setVisitedDate(123L);

        assertThat(LogUtils.localCacheStateMatches(expected, staleDate)).isFalse();
        assertThat(LogUtils.localCacheStateMatches(expected, staleAttributes)).isFalse();
        assertThat(LogUtils.localCacheStateMatches(expected, expected)).isTrue();
    }

    @Test
    public void imageCleanupDeletesCompletedImageAfterMatchingDraftWasCleared() {
        final Uri uri = Uri.parse("file:///done.jpg");
        final OfflineLogEntry posted = new OfflineLogEntry.Builder().setLogType(LogType.NOTE).setDate(1L)
                .setLog("posted").addLogImage(new Image.Builder().setUrl(uri).build()).build();
        final CreateLogPostState completed = CreateLogPostState.create("GC123", posted, null, false, null);
        completed.images.get(0).status = CreateLogPostState.StepStatus.CONFIRMED;
        completed.images.get(0).remoteUri = "https://example.invalid/done.jpg";

        assertThat(LogUtils.findLocalImagesSafeToDelete(completed, null, List.of())).containsExactly(uri);
    }

    @Test
    public void imageCleanupKeepsUriUsedByChangedDraft() {
        final Uri sharedUri = Uri.parse("file:///shared.jpg");
        final OfflineLogEntry posted = new OfflineLogEntry.Builder().setLogType(LogType.NOTE).setDate(1L)
                .setLog("posted").addLogImage(new Image.Builder().setUrl(sharedUri).build()).build();
        final CreateLogPostState completed = CreateLogPostState.create("GC123", posted, null, false, null);
        completed.images.get(0).status = CreateLogPostState.StepStatus.CONFIRMED;
        completed.images.get(0).remoteUri = "https://example.invalid/shared.jpg";
        final OfflineLogEntry changedDraft = posted.buildUponOfflineLogEntry().setLog("changed").build();

        assertThat(LogUtils.findLocalImagesSafeToDelete(completed, changedDraft, List.of())).isEmpty();
    }

    @Test
    public void trimForPublishingHandlesNull() {
        assertThat(LogUtils.trimForPublishing(null)).isEqualTo("");
    }
}

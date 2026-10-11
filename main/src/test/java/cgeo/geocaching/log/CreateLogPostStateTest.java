package cgeo.geocaching.log;

import cgeo.geocaching.models.Image;

import android.net.Uri;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateLogPostStateTest {

    private static OfflineLogEntry createEntry() {
        return new OfflineLogEntry.Builder()
                .setLogType(LogType.FOUND_IT)
                .setDate(123456789L)
                .setLog(" test log ")
                .setReportProblem(ReportProblemType.OTHER)
                .setFavorite(true)
                .setRating(4f)
                .addLogImage(new Image.Builder()
                        .setUrl(Uri.parse("file:///first.jpg"))
                        .setTitle("first")
                        .setDescription("description")
                        .setTargetScale(800)
                        .build())
                .addLogImage(new Image.Builder()
                        .setUrl(Uri.parse("file:///second.jpg"))
                        .setTitle("second")
                        .setTargetScale(1600)
                        .build())
                .build();
    }

    @Test
    public void jsonRoundTripKeepsSnapshotAndProgress() {
        final CreateLogPostState state = CreateLogPostState.create("GC123", createEntry(), null, true, "problem");
        state.mainStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.mainServiceLogId = "main-id";
        state.remoteCompletedAt = 123456790L;
        state.problemStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.problemServiceLogId = "problem-id";
        state.images.get(0).status = CreateLogPostState.StepStatus.CONFIRMED;
        state.images.get(0).serviceImageId = "image-id";
        state.images.get(0).remoteUri = "https://example.invalid/image";
        state.images.get(1).status = CreateLogPostState.StepStatus.PENDING;

        final CreateLogPostState restored = CreateLogPostState.fromJson(state.toJson());

        assertThat(restored).isNotNull();
        assertThat(restored.mainServiceLogId).isEqualTo("main-id");
        assertThat(restored.remoteCompletedAt).isEqualTo(123456790L);
        assertThat(restored.problemServiceLogId).isEqualTo("problem-id");
        assertThat(restored.images).hasSize(2);
        assertThat(restored.images.get(0).uploadTitle).isEqualTo("first");
        assertThat(restored.getConfirmedImages()).hasSize(1);
        assertThat(restored.getConfirmedImages().get(0).getTitle()).isEqualTo("first");
        assertThat(restored.toOfflineLogEntry().log).isEqualTo(" test log ");
        assertThat(restored.isRemoteComplete()).isFalse();
    }

    @Test
    public void inFlightRemoteStepIsUncertain() {
        final CreateLogPostState state = CreateLogPostState.create("GC123", createEntry(), null, true, "problem");
        state.mainStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.mainServiceLogId = "main-id";
        state.problemStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.problemServiceLogId = "problem-id";
        state.images.get(0).status = CreateLogPostState.StepStatus.CONFIRMED;
        state.images.get(0).remoteUri = "https://example.invalid/first";
        state.images.get(1).status = CreateLogPostState.StepStatus.IN_FLIGHT;

        assertThat(state.hasUncertainStep()).isTrue();

        state.images.get(1).status = CreateLogPostState.StepStatus.PENDING;
        state.localLogsStatus = CreateLogPostState.StepStatus.IN_FLIGHT;
        assertThat(state.hasUncertainStep()).isFalse();
    }

    @Test
    public void confirmedStepsSurviveSerializationAndRemoteCompletion() {
        final CreateLogPostState state = CreateLogPostState.create("GC123", createEntry(), null, true, "problem");
        state.mainStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.mainServiceLogId = "main-id";
        state.remoteCompletedAt = 123456790L;
        state.problemStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.problemServiceLogId = "problem-id";
        int index = 0;
        for (CreateLogPostState.ImageState image : state.images) {
            image.status = CreateLogPostState.StepStatus.CONFIRMED;
            image.serviceImageId = "image-" + index;
            image.remoteUri = "https://example.invalid/" + index;
            index++;
        }

        final CreateLogPostState restored = CreateLogPostState.fromJson(state.toJson());

        assertThat(restored).isNotNull();
        assertThat(restored.remoteCompletedAt).isEqualTo(123456790L);
        assertThat(restored.isRemoteComplete()).isTrue();
        assertThat(restored.images).allMatch(image -> image.status == CreateLogPostState.StepStatus.CONFIRMED);
    }

    @Test
    public void tamperedSnapshotIsRejected() {
        final CreateLogPostState state = CreateLogPostState.create("GC123", createEntry(), null, false, null);
        final String tampered = state.toJson().replace("test log", "changed log");

        assertThat(CreateLogPostState.fromJson(tampered)).isNull();
    }

    @Test
    public void missingOrUnknownProgressStateIsRejected() {
        final CreateLogPostState state = CreateLogPostState.create("GC123", createEntry(), null, false, null);
        final String json = state.toJson();

        assertThat(CreateLogPostState.fromJson(json.replace("\"mainStatus\":\"PENDING\",", ""))).isNull();
        assertThat(CreateLogPostState.fromJson(json.replace("\"mainStatus\":\"PENDING\"",
                "\"mainStatus\":\"INVALID\""))).isNull();
        assertThat(CreateLogPostState.fromJson(json.replace("\"completionStatus\":\"ACTIVE\"",
                "\"completionStatus\":\"INVALID\""))).isNull();
    }

    @Test
    public void matchingIgnoresProgressButRejectsChangedDraft() {
        final OfflineLogEntry entry = createEntry();
        final CreateLogPostState state = CreateLogPostState.create("GC123", entry, null, true, "problem");
        state.mainStatus = CreateLogPostState.StepStatus.CONFIRMED;
        state.mainServiceLogId = "main-id";

        assertThat(state.matches(entry, null, true, "problem")).isTrue();
        assertThat(state.matches(entry.buildUponOfflineLogEntry().setLog("changed").build(), null, true, "problem")).isFalse();
    }
}

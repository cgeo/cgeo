package dev.davidv.bergamot;

public class DetectionResult {
    public final String language;
    public final boolean isReliable;
    public final int confidence;

    public DetectionResult(final String language, final boolean isReliable, final int confidence) {
        this.language = language;
        this.isReliable = isReliable;
        this.confidence = confidence;
    }
}

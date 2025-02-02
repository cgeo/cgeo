package cgeo.geocaching.files;

import androidx.annotation.NonNull;

import java.util.Collection;

public interface IGPXMultiParser {
    void onParsingDone(@NonNull Collection<Object> result);
}

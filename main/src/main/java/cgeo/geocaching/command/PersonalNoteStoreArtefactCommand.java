package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.models.CacheArtefactParser;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;


/**
 * Command that extracts artefact information from detailed geocaches and stores it into the caches'
 * personal note field in a parseable form.
 *
 * <p>Behavior:
 * - Operates only on caches that are detailed and contain an artefact to store.
 * - For each affected cache the parsed artefact (waypoints and variables) is inserted into the
 * existing personal note text in a parseable way.
 * - Modified personal notes are persisted via DataStore.setCachePersonalNote().
 *
 * <p>Undo:
 * - The original personal notes of the affected caches are cached and restored when undoCommand() is invoked.
 *
 * <p>Usage:
 * - Construct with an Activity context, a collection of Geocache objects and an optional Runnable
 * callback executed when the command finishes.
 * The Runnable can be used to update the UI or trigger further actions.
 */
public class PersonalNoteStoreArtefactCommand extends AbstractCachesCommand {

    @NonNull
    private final Map<String, String> personalNotes = new HashMap<>();
    private int updatedCount = 0;

    @Nullable
    private final Runnable onFinished;

    public PersonalNoteStoreArtefactCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches, @Nullable final Runnable onFinished) {
        super(context, caches.stream().filter(cache -> cache.isDetailed() && CacheArtefactParser.getArtefactToStore(cache) != null).toList(), R.string.command_personal_note_store_artefact_progress);
        this.onFinished = onFinished;
    }

    @Override
    protected void doCommand() {
        updatedCount = 0;
        final Collection<Geocache> caches = getCaches();
        final HashMap<String, String> newPersonalNotes = new HashMap<>(caches.size());
        for (final Geocache cache : caches) {
            final String oldPersonalNote = cache.getPersonalNote();
            final CacheArtefactParser.CacheArtefactParserArtefact artefactToStore = CacheArtefactParser.getArtefactToStore(cache);
            if (artefactToStore != null) {
                final String newNote = CacheArtefactParser.putParseableWaypointsInText(oldPersonalNote,
                        artefactToStore.waypoints, artefactToStore.variables);
                final String updatedPersonalNote = StringUtils.trimToEmpty(newNote);
                if (!Strings.CI.equals(StringUtils.trimToEmpty(oldPersonalNote), updatedPersonalNote)) {
                    newPersonalNotes.put(cache.getGeocode(), updatedPersonalNote);
                    personalNotes.put(cache.getGeocode(), StringUtils.defaultIfEmpty(oldPersonalNote, StringUtils.EMPTY));
                    updatedCount++;
                }
            }
        }
        DataStore.setCachePersonalNote(caches, newPersonalNotes);
    }


    @Override
    protected void undoCommand() {
        DataStore.setCachePersonalNote(getCaches(), personalNotes);
    }


    @Override
    protected void onFinished() {
        if (onFinished != null) {
            onFinished.run();
        }
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return LocalizationUtils.getString(R.string.command_personal_note_store_artefact_result, updatedCount);
    }
}

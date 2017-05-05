package cgeo.geocaching.command;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import java.util.HashSet;
import java.util.Set;

/**
 * Removes caches from the current list which are already stored on other lists.
 */
public abstract class RemoveNotUniqueCommand extends AbstractCommand {

  private final SearchResult searchResult;
  private final int listId;

  @NonNull
  private final Set<Geocache> removedCaches = new HashSet<>();

  protected RemoveNotUniqueCommand(@NonNull final Activity context, final int listId, SearchResult searchResult) {
    super(context);
    this.listId = listId;
    this.searchResult = searchResult;
  }

  @Override
  protected void doCommand() {
    final Set<Geocache> caches = DataStore.loadCaches(searchResult.getGeocodes(), LoadFlags.LOAD_CACHE_OR_DB);

    for (final Geocache geocache : caches) {
      if (geocache.getLists().size() > 1) {
        // stored on more than this list
        removedCaches.add(geocache); // remember deleted geocaches
      }
    }
    DataStore.removeFromList(removedCaches, listId); // remove from this one
  }

  @Override
  protected void undoCommand() {
    DataStore.addToList(removedCaches, listId);
  }

  @Override
  @Nullable
  protected String getResultMessage() {
    return getContext().getString(R.string.command_remove_not_unique_result);
  }
}

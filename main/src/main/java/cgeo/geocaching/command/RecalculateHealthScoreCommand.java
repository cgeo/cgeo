package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CacheUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class RecalculateHealthScoreCommand extends AbstractCommand {

    @NonNull
    private final Collection<Geocache> caches;
    private int recalculatedCount = 0;

    public RecalculateHealthScoreCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches) {
        super(context, R.string.command_recalculate_health_score_progress);
        this.caches = caches;
    }

    @Override
    protected boolean supportsUndo() {
        return false;
    }

    @Override
    protected void doCommand() {
        recalculatedCount = 0;
        for (final Geocache cache : caches) {
            if (!cache.isDetailed()) {
                continue;
            }
            final List<LogEntry> logs = DataStore.loadLogsForHealthScore(cache.getGeocode());
            final Integer healthScore = CacheUtils.calculateHealthScore(logs, cache.getDetailedUpdate());
            cache.setHealthScore(healthScore);
            DataStore.saveHealthScore(cache.getGeocode(), healthScore);
            recalculatedCount++;
        }
    }

    @Override
    protected void onFinished() {
        // nothing to do
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return LocalizationUtils.getString(R.string.command_recalculate_health_score_result, recalculatedCount);
    }
}

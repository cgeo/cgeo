package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import androidx.annotation.NonNull;

import java.util.Collection;

import org.apache.commons.lang3.tuple.ImmutablePair;

/** capability to provide the needed difficulty-terrain-combinations to fill the 81-er-matrix */
public interface IDifficultyTerrainMatrixNeededCapability extends IConnector {

    @NonNull
    Collection<ImmutablePair<Float, Float>> getNeededDifficultyTerrainCombisFor81Matrix();
}

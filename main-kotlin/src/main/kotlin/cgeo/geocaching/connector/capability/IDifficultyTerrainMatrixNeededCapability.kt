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

package cgeo.geocaching.connector.capability

import cgeo.geocaching.connector.IConnector

import androidx.annotation.NonNull

import java.util.Collection

import org.apache.commons.lang3.tuple.ImmutablePair

/** capability to provide the needed difficulty-terrain-combinations to fill the 81-er-matrix */
interface IDifficultyTerrainMatrixNeededCapability : IConnector() {

    Collection<ImmutablePair<Float, Float>> getNeededDifficultyTerrainCombisFor81Matrix()
}

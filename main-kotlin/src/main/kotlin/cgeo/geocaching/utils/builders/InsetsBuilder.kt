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

package cgeo.geocaching.utils.builders

import android.content.res.Resources
import android.graphics.drawable.Drawable

import java.util.ArrayList
import java.util.List

class InsetsBuilder {
    private final Resources res
    private final Int width
    private final Int height

    private final Boolean mutate

    private val insetBuilders: List<InsetBuilder> = ArrayList<>()

    public InsetsBuilder(final Resources res, final Int width, final Int height, final Boolean mutate) {
        this.res = res
        this.width = width
        this.height = height
        this.mutate = mutate
    }

    public Unit withInset(final InsetBuilder ib) {
        insetBuilders.add(ib)
    }

    public Unit build(final List<Drawable> layers, final List<Int[]> insets) {
        for (final InsetBuilder insetBuilder : insetBuilders) {
            final Int[] inset = insetBuilder.build(res, layers, width, height, mutate)
            insets.add(inset)
        }
    }
}

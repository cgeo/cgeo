package cgeo.geocaching.utils.builders;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class InsetsBuilder {
    private final Resources res;
    private final int width;
    private final int height;

    private final boolean mutate;

    private final List<InsetBuilder> insetBuilders = new ArrayList<>();

    public InsetsBuilder(final Resources res, final int width, final int height, final boolean mutate) {
        this.res = res;
        this.width = width;
        this.height = height;
        this.mutate = mutate;
    }

    public void withInset(final InsetBuilder ib) {
        insetBuilders.add(ib);
    }

    public void build(final List<Drawable> layers, final List<int[]> insets) {
        for (final InsetBuilder insetBuilder : insetBuilders) {
            final int[] inset = insetBuilder.build(res, layers, width, height, mutate);
            insets.add(inset);
        }
    }
}

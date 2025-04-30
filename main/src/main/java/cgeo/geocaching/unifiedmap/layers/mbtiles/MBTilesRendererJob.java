package cgeo.geocaching.unifiedmap.layers.mbtiles;

/* based upon work of (c) 2023 Christian Pesch, https://github.com/cpesch */

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

public class MBTilesRendererJob extends Job {
    private final MBTilesRenderer renderer;
    private final int hashCodeValue;

    public MBTilesRendererJob(final Tile tile, final MBTilesRenderer renderer, final boolean isTransparent) {
        super(tile, isTransparent);
        this.renderer = renderer;
        this.hashCodeValue = calculateHashCode();
    }

    public MBTilesRenderer getDatabaseRenderer() {
        return renderer;
    }

    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!super.equals(obj)) {
            return false;
        } else if (!(obj instanceof MBTilesRendererJob)) {
            return false;
        }
        final MBTilesRendererJob other = (MBTilesRendererJob) obj;
        return this.getDatabaseRenderer().equals(other.getDatabaseRenderer());
    }

    public int hashCode() {
        return this.hashCodeValue;
    }

    private int calculateHashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getDatabaseRenderer().hashCode();
        return result;
    }
}

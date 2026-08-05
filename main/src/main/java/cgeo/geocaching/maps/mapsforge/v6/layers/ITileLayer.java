package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.map.layer.Layer;

public interface ITileLayer {

    Layer getTileLayer();

    boolean hasThemes();

    void onResume();

    void onPause();

    int getFixedTileSize();

    byte getZoomLevelMin();

    byte getZoomLevelMax();

}

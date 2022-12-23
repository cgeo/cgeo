package cgeo.geocaching.maps.mapsforge.v6.legend;

import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

interface ThemeLegend {

    RenderThemeLegend.LegendCategory[] loadLegend(RenderThemeHelper.RenderThemeType rtt, ArrayList<RenderThemeLegend.LegendEntry> entries);

    @StringRes
    int getInfoUrl();
}

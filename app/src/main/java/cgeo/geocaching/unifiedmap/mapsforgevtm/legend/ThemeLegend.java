package cgeo.geocaching.unifiedmap.mapsforgevtm.legend;

import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

interface ThemeLegend {

    RenderThemeLegend.LegendCategory[] loadLegend(MapsforgeThemeHelper.RenderThemeType rtt, ArrayList<RenderThemeLegend.LegendEntry> entries);

    @StringRes
    int getInfoUrl();
}

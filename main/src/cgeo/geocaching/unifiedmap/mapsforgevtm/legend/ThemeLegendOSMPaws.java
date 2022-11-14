package cgeo.geocaching.unifiedmap.mapsforgevtm.legend;

import cgeo.geocaching.R;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

public class ThemeLegendOSMPaws  implements ThemeLegend {

    @Override
    // method is long, but trivial; splitting it into submethods would not improve readability
    @SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.NPathComplexity"})
    public RenderThemeLegend.LegendCategory[] loadLegend(final MapsforgeThemeHelper.RenderThemeType rtt, final ArrayList<RenderThemeLegend.LegendEntry> entries) {
        final String p = rtt.relPath + "/patterns";
        final String s = rtt.relPath + "/symbols";
        final ArrayList<RenderThemeLegend.LegendCategory> cats = new ArrayList<>();

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(11, 12, R.string.rtl_category_areas, 5, 4));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "bare_rock", R.string.rtl_rock));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "marsh", R.string.rtl_marsh));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "nature_reserve", R.string.rtl_nature_reserve));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "orchard", R.string.rtl_orchard));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "scree", R.string.rtl_scree));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "scrub", R.string.rtl_scrub));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "vineyard", R.string.rtl_vineyard));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "wood_coniferous", R.string.rtl_wood_coniferous));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "wood_deciduous", R.string.rtl_wood_deciduous));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "wood_mixed", R.string.rtl_wood_mixed));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(41, 42, R.string.rtl_category_poi_symbols, 7, 7));

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "adit", R.string.rtl_adit));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "cave_entrance", R.string.rtl_cave_entrance));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "cliff", R.string.rtl_cliff));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "peak", R.string.rtl_peak));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "rock", R.string.rtl_rock));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "stone", R.string.rtl_stone));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "alpine_hut", R.string.rtl_alpine_hut));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "shelter", R.string.rtl_shelter));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "wilderness_hut", R.string.rtl_wilderness_hut));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "observation_tower", R.string.rtl_tower_observation));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "archaeological_site", R.string.rtl_archaeological));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "hunting_stand", R.string.rtl_hunting_stand));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "castle_fortress", R.string.rtl_castle));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "castle_ruin", R.string.rtl_castle));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "ruin", R.string.rtl_ruin));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "artwork", R.string.rtl_artwork));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "memorial", R.string.rtl_memorial));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "monument", R.string.rtl_monument));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "wayside_cross", R.string.rtl_wayside_cross));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "wayside_shrine", R.string.rtl_wayside_shrine));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "museum", R.string.rtl_museum));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "theatre", R.string.rtl_theatre));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "bench", R.string.rtl_bench));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "board", R.string.rtl_info_board));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "information_office", R.string.rtl_info_office));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "guidepost", R.string.rtl_guidepost));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "viewpoint", R.string.rtl_viewpoint));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "drinking_water", R.string.rtl_drinking_water));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "spring", R.string.rtl_spring));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "camping", R.string.rtl_camp_site));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "caravan", R.string.rtl_caravan_site));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "picnic_site", R.string.rtl_picnic_site));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "playground", R.string.rtl_playground));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "zoo", R.string.rtl_zoo));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "swimming", R.string.rtl_swimming_area));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "tennis", R.string.rtl_tennis));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "fountain", R.string.rtl_fountain));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "waterfall", R.string.rtl_waterfall));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "atm", R.string.rtl_atm));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "telephone", R.string.rtl_telephone));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "fuel", R.string.rtl_fuel));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "toilets", R.string.rtl_toilets));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "cable_car", R.string.rtl_cable_car));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "chair_lift", R.string.rtl_chair_lift));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "gondola", R.string.rtl_gondola));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "parking_bicycle", R.string.rtl_bicycle_parking));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "parking_car", R.string.rtl_parking));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "airport", R.string.rtl_airport));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "heliport", R.string.rtl_helipad));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "bakery", R.string.rtl_bakery));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "bank", R.string.rtl_bank));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "bicycle", R.string.rtl_bicycle_shop));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "rental_bicycle", R.string.rtl_bicycle_rental));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "butcher", R.string.rtl_butcher));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "cafe", R.string.rtl_cafe));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "pub", R.string.rtl_pub));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "restaurant", R.string.rtl_restaurant));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "chemist", R.string.rtl_chemist));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "cinema", R.string.rtl_cinema));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "convenience", R.string.rtl_convenience));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "fast_food", R.string.rtl_fastfood));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "fire_station", R.string.rtl_firebrigade));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "library", R.string.rtl_library));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "pharmacy", R.string.rtl_pharmacy));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "post_office", R.string.rtl_postoffice));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "supermarket", R.string.rtl_supermarket));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "christian", R.string.rtl_pow_christian));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "jewish", R.string.rtl_pow_jewish));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "muslim", R.string.rtl_pow_muslim));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "gate", R.string.rtl_gate));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "lift_gate", R.string.rtl_lift_gate));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "power_wind", R.string.rtl_power_wind));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "windmill", R.string.rtl_windmill));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "recycling", R.string.rtl_recycling));

        // ---------------------------------------------------------------

        final RenderThemeLegend.LegendCategory[] categories = new RenderThemeLegend.LegendCategory[cats.size()];
        int i = 0;
        for (RenderThemeLegend.LegendCategory cat : cats) {
            categories[i++] = cat;
        }
        return categories;
    }

    @Override
    public @StringRes
    int getInfoUrl() {
        return R.string.maptheme_legend_osmpaws;
    }
}

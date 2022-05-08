package cgeo.geocaching.maps.mapsforge.v6.legend;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

class ThemeLegendElevate implements ThemeLegend {

    // method is long, but trivial; splitting it into submethods would not improve readability
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public RenderThemeLegend.LegendCategory[] loadLegend(final RenderThemeHelper.RenderThemeType rtt, final ArrayList<RenderThemeLegend.LegendEntry> entries) {
        final String p = "ele-res";
        final ArrayList<RenderThemeLegend.LegendCategory> cats = new ArrayList<>();

        // ---------------------------------------------------------------

        /* area images from theme
        cats.add(new LegendCategory(1, 2, "Test", 5, 4));

        entries.add(new LegendEntry(1, p, "p_fell"));
        entries.add(new LegendEntry(1, p, "p_heath"));
        entries.add(new LegendEntry(1, p, "p_marsh"));
        entries.add(new LegendEntry(1, p, "p_rock"));
        entries.add(new LegendEntry(1, p, "p_scree"));
        entries.add(new LegendEntry(1, p, "p_scrub"));
        entries.add(new LegendEntry(1, p, "p_wood-mixed"));
        entries.add(new LegendEntry(1, p, "p_wood-coniferous"));
        entries.add(new LegendEntry(1, p, "p_wood-deciduous"));
        entries.add(new LegendEntry(1, p, "p_glacier"));

        entries.add(new LegendEntry(1, p, "p_orchard"));
        entries.add(new LegendEntry(1, p, "p_vineyard"));

        entries.add(new LegendEntry(1, p, "p_military"));

        entries.add(new LegendEntry(1, p, "p_nature-reserve"));


        entries.add(new LegendEntry(1, p, "p_aboriginal_lands"));
        entries.add(new LegendEntry(1, p, "p_swimming-outdoor"));
        entries.add(new LegendEntry(1, p, "p_cemetry"));
        entries.add(new LegendEntry(1, p, "p_attraction"));
        entries.add(new LegendEntry(1, p, "p_pedestrian"));
        entries.add(new LegendEntry(1, p, "p_access-no"));
        entries.add(new LegendEntry(1, p, "p_access-private"));
        entries.add(new LegendEntry(1, p, "p_access-destination"));
        */

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(11, 12, R.string.rtl_category_areas, 5, 4));

        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_fell, R.string.rtl_fell_meadow));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_heath, R.string.rtl_heath));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_marsh, R.string.rtl_marsh));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_rock, R.string.rtl_rock));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_scree, R.string.rtl_scree));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_scrub, R.string.rtl_scrub));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_wood_mixed, R.string.rtl_wood_mixed));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_wood_coniferous, R.string.rtl_wood_coniferous));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_wood_deciduous, R.string.rtl_wood_deciduous));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_glacier, R.string.rtl_glacier));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_grass, R.string.rtl_grass));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_farmland, R.string.rtl_farmland));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_orchard, R.string.rtl_orchard));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_vineyard, R.string.rtl_vineyard));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_water, R.string.rtl_water));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_intermittent, R.string.rtl_water_intermittent));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.elevate_p_military, R.string.rtl_military));

        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_pa, R.string.rtl_nature_reserve));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_pa_strict, R.string.rtl_nature_reserve_strict));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_cliff, R.string.rtl_cliff));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_ridge, R.string.rtl_ridge));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_dyke, R.string.rtl_dyke));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_embankment, R.string.rtl_embankment));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_spring, R.string.rtl_spring));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.elevate_powerline, R.string.rtl_powerline));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(21, 22, R.string.rtl_category_roads_paths, 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_track1, R.string.rtl_paved_path));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_track2, R.string.rtl_paved_path_gravel));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_track3, R.string.rtl_unpaved_path));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_track4, R.string.rtl_unpaved_path_partly_grown));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_track5, R.string.rtl_unpaved_path_grown));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_track_o, R.string.rtl_surface_unknown));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_path_o, R.string.rtl_unpaved_footpath));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_footway, R.string.rtl_paved_footpath));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_bridleway, R.string.rtl_bridleway));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_acc_dest, R.string.rtl_access_destination));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_acc_priv, R.string.rtl_access_private));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.elevate_acc_no, R.string.rtl_access_no));

        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_cycleway, R.string.rtl_cycleway));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_cycleway_steep, R.string.rtl_cycleway_steep));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_cycleway_paved, R.string.rtl_cycleway_paved));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_surface_smooth_paved, R.string.rtl_paved_smooth));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_surface_rough_paved, R.string.rtl_paved_rough));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_surface_compacted, R.string.rtl_surface_compacted));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_surface_gravel, R.string.rtl_surface_gravel));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.elevate_surface_raw, R.string.rtl_surface_raw));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(31, 32, R.string.rtl_category_walking_cycling_climbing, 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_path_t1, R.string.rtl_hiking_T1));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_path_t2, R.string.rtl_hiking_T2));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_path_t3, R.string.rtl_hiking_T3));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_path_t4, R.string.rtl_hiking_T4));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_path_t5, R.string.rtl_hiking_T5));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_path_t6, R.string.rtl_hiking_T6));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtb0, R.string.rtl_mtb_S0));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtb1, R.string.rtl_mtb_S1));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtb2, R.string.rtl_mtb_S2));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtb3, R.string.rtl_mtb_S3));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtb4, R.string.rtl_mtb_S4));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtb5, R.string.rtl_mtb_S56));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtbu0, R.string.rtl_uphill15));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtbu1, R.string.rtl_uphill20));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtbu2, R.string.rtl_uphill25));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtbu3, R.string.rtl_uphill30));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtbu4, R.string.rtl_uphill40));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.elevate_mtbu5, R.string.rtl_uphill_too_steep));

        entries.add(new RenderThemeLegend.LegendEntry(32, R.drawable.elevate_s_rope, R.string.rtl_security_rope));
        entries.add(new RenderThemeLegend.LegendEntry(32, R.drawable.elevate_s_rungs, R.string.rtl_security_rungs));
        entries.add(new RenderThemeLegend.LegendEntry(32, R.drawable.elevate_s_ladder, R.string.rtl_security_ladder));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(41, 42, R.string.rtl_category_poi_symbols, 7, 7));

        // walking (black)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bench", R.string.rtl_bench));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cairn", R.string.rtl_cairn));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_drinking_water", R.string.rtl_drinking_water));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_guidepost", R.string.rtl_guidepost));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_info_board", R.string.rtl_info_board));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_info_office", R.string.rtl_info_office));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_info_terminal", R.string.rtl_info_terminal));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_peak", R.string.rtl_peak));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_peak_cross", R.string.rtl_peak_cross));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_picnic_site", R.string.rtl_picnic_site));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_playground", R.string.rtl_playground));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_playground_private", R.string.rtl_playground_private));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_recycling", R.string.rtl_recycling));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter", R.string.rtl_shelter));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter_picnic", R.string.rtl_shelter_picnic));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter_rock", R.string.rtl_shelter_rock));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_telephone", R.string.rtl_telephone));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_toilets", R.string.rtl_toilets));
        // entries.add(new LegendEntry(41, p, "s_toilets_fee", R.string.rtl_toilets_fee)); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_volcano", R.string.rtl_volcano));

        // attractions (ocher)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_adit_disused", R.string.rtl_adit_disused));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_archaeological", R.string.rtl_archaeological));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_artwork", R.string.rtl_artwork));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_attraction", R.string.rtl_attraction));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_boundary_stone", R.string.rtl_boundary_stone));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bunker_disused", R.string.rtl_bunker_disused));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_castle", R.string.rtl_castle));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cinema", R.string.rtl_cinema));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cross", R.string.rtl_cross));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_fountain", R.string.rtl_fountain));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_library", R.string.rtl_library));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial", R.string.rtl_memorial));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_plaque", R.string.rtl_memorial_plaque));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_statue", R.string.rtl_memorial_statue));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_stolperstein", R.string.rtl_memorial_stolperstein));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_stone", R.string.rtl_memorial_stone));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_war", R.string.rtl_memorial_war));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_monument", R.string.rtl_monument));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_museum", R.string.rtl_museum));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ruins", R.string.rtl_ruins));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_stone_historic", R.string.rtl_stone_historic));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_theatre", R.string.rtl_theatre));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_water_well", R.string.rtl_water_well));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wayside_cross", R.string.rtl_wayside_cross));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wayside_shrine", R.string.rtl_wayside_shrine));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_zoo", R.string.rtl_zoo));

        // recreation (light green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_beach_resort", R.string.rtl_beach_resort));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cliff_diving", R.string.rtl_cliff_diving));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_climbing", R.string.rtl_climbing));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_climbing_adventure", R.string.rtl_climbing_adventure));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_free_flying", R.string.rtl_free_flying));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_golf", R.string.rtl_golf));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gym", R.string.rtl_gym));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_miniature_golf", R.string.rtl_miniature_golf));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_roller_skating", R.string.rtl_roller_skating));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shooting", R.string.rtl_shooting));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_skateboard", R.string.rtl_skateboard));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_slipway", R.string.rtl_slipway));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_soccer", R.string.rtl_soccer));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_stadium", R.string.rtl_stadium));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_swimming_area", R.string.rtl_swimming_area));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_swimming_outdoor", R.string.rtl_swimming_outdoor));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_tennis", R.string.rtl_tennis));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_via_ferrata", R.string.rtl_via_ferrata));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_water_park", R.string.rtl_water_park));

        // transport (turquoise)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_aerialway_station", R.string.rtl_aerialway_station));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_airport", R.string.rtl_airport));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cable_car", R.string.rtl_cable_car));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_car_shop", R.string.rtl_car_shop));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_chair_lift", R.string.rtl_chair_lift));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_drag_lift", R.string.rtl_drag_lift));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gondola", R.string.rtl_gondola));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_goods_lift", R.string.rtl_goods_lift));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_helipad", R.string.rtl_helipad));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_parking", R.string.rtl_parking));
        // entries.add(new LegendEntry(41, p, "s_parking_fee", R.string.rtl_parking_fee)); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_parking_private", R.string.rtl_parking_private));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_rail_funicular", R.string.rtl_rail_funicular));

        // accommodation (light blue)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_alpine_hut", R.string.rtl_alpine_hut));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_alpine_hut_private", R.string.rtl_alpine_hut_private));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_alpine_hut_winter", R.string.rtl_alpine_hut_winter));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_apartment", R.string.rtl_apartment));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_camp_site", R.string.rtl_camp_site));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_caravan_site", R.string.rtl_caravan_site));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_chalet", R.string.rtl_chalet));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hostel", R.string.rtl_hostel));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hotel", R.string.rtl_hotel));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter_basic_hut", R.string.rtl_shelter_basic_hut));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wilderness_hut", R.string.rtl_wilderness_hut));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wilderness_hut_private", R.string.rtl_wilderness_hut_private));

        // food / beverage (brown)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bar", R.string.rtl_bar));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_biergarten", R.string.rtl_biergarten));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cafe", R.string.rtl_cafe));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_fastfood", R.string.rtl_fastfood));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ice_cream", R.string.rtl_ice_cream));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_pub", R.string.rtl_pub));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_restaurant", R.string.rtl_restaurant));

        // amenities (lila)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_atm", R.string.rtl_atm));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bakery", R.string.rtl_bakery));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bank", R.string.rtl_bank));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_beverages", R.string.rtl_beverages));
        // entries.add(new LegendEntry(41, p, "s_bicycle_charging_station", R.string.rtl_bicycle_charging_station)); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bicycle_rental", R.string.rtl_bicycle_rental));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bicycle_shop", R.string.rtl_bicycle_shop));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_books", R.string.rtl_books));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_butcher", R.string.rtl_butcher));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_chemist", R.string.rtl_chemist));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_convenience", R.string.rtl_convenience));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_doityourself", R.string.rtl_doityourself));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_farm_shop", R.string.rtl_farm_shop));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_laundry", R.string.rtl_laundry));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_mall", R.string.rtl_mall));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_marketplace", R.string.rtl_marketplace));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_organic", R.string.rtl_organic));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_postbox", R.string.rtl_postbox));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_postoffice", R.string.rtl_postoffice));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_sports_shop", R.string.rtl_sports_shop));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_supermarket", R.string.rtl_supermarket));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_travel_agency", R.string.rtl_travel_agency));

        // emergency (lila)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_defibrillator", R.string.rtl_defibrillator));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_doctors", R.string.rtl_doctors));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_embassy", R.string.rtl_embassy));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_emergency_phone", R.string.rtl_emergency_phone));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_firebrigade", R.string.rtl_firebrigade));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_emergency_access_point", R.string.rtl_emergency_access_point));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hospital", R.string.rtl_hospital));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_pharmacy", R.string.rtl_pharmacy));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_police", R.string.rtl_police));

        // energy (dark green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_adit", R.string.rtl_adit));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_petroleum_well", R.string.rtl_petroleum_well));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_coal", R.string.rtl_power_coal));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_gas", R.string.rtl_power_gas));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_nuclear", R.string.rtl_power_nuclear));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_water", R.string.rtl_power_water));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_wind", R.string.rtl_power_wind));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wastewater_plant", R.string.rtl_wastewater_plant));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_water_works", R.string.rtl_water_works));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_watermill", R.string.rtl_watermill));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_weir", R.string.rtl_weir));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_windmill", R.string.rtl_windmill));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_windpump", R.string.rtl_windpump));

        // other (dark green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bunker", R.string.rtl_bunker));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_farmyard", R.string.rtl_farmyard));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hunting_stand", R.string.rtl_hunting_stand));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_kindergarten", R.string.rtl_kindergarten));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_lighthouse", R.string.rtl_lighthouse));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_school", R.string.rtl_school));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_tower_communication", R.string.rtl_tower_communication));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_tower_observation", R.string.rtl_tower_observation));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_townhall", R.string.rtl_townhall));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_university", R.string.rtl_university));

        // worship (dark green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_buddhist", R.string.rtl_buddhist));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_church", R.string.rtl_church));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hindu", R.string.rtl_hindu));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_mosque", R.string.rtl_mosque));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_place_of_worship", R.string.rtl_place_of_worship));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shinto", R.string.rtl_shinto));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_synagogue", R.string.rtl_synagogue));

        // obstacles (grey)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_block", R.string.rtl_block));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bollard", R.string.rtl_bollard));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_border_control", R.string.rtl_border_control));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bridge_movable", R.string.rtl_bridge_movable));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cave_entrance", R.string.rtl_cave_entrance));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cliff", R.string.rtl_cliff));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cycle_barrier", R.string.rtl_cycle_barrier));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_elevator", R.string.rtl_elevator));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ford", R.string.rtl_ford));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ford_small", R.string.rtl_ford_small));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gate", R.string.rtl_gate));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gate_private", R.string.rtl_gate_private));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_geyser", R.string.rtl_geyser));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hot_spring", R.string.rtl_hot_spring));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_kissing_gate", R.string.rtl_kissing_gate));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ladder", R.string.rtl_ladder));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_lift_gate", R.string.rtl_lift_gate));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_log", R.string.rtl_log));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway", R.string.rtl_oneway));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway_cycle", R.string.rtl_oneway_cycle));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway_cycle_rev", R.string.rtl_oneway_cycle_rev));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway_rev", R.string.rtl_oneway_rev));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_peak_small", R.string.rtl_peak_small));
        // entries.add(new LegendEntry(41, p, "s_railway_crossing", R.string.rtl_railway_crossing)); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_rapids", R.string.rtl_rapids));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ridge", R.string.rtl_ridge));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_rungs", R.string.rtl_rungs));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_0", R.string.rtl_saddle_0));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_45", R.string.rtl_saddle_45));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_90", R.string.rtl_saddle_90));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_135", R.string.rtl_saddle_135));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_safety_rope", R.string.rtl_safety_rope));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_stone", R.string.rtl_stone));
        // entries.add(new LegendEntry(41, p, "s_tile", R.string.rtl_tile)); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_traffic_signal", R.string.rtl_traffic_signal));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_turnstile", R.string.rtl_turnstile));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_viewpoint", R.string.rtl_viewpoint));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_waterfall", R.string.rtl_waterfall));

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
        return R.string.maptheme_legend_elevate;
    }

}

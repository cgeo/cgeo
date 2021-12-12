package cgeo.geocaching.maps.mapsforge.v6.legend;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

class ThemeLegendElevate implements ThemeLegend {

    @SuppressWarnings("PMD.ExcessiveMethodLength") // method is long, but trivial; splitting it into submethods would not improve readability
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

        cats.add(new RenderThemeLegend.LegendCategory(11, 12, "Areas", 5, 4));

        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_fell, "fell / meadow"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_heath, "heath"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_marsh, "marsh"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_rock, "rock"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_scree, "scree"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_scrub, "scrub"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_wood_mixed, "wood (mixed)"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_wood_coniferous, "wood (coniferous)"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_wood_deciduous, "wood (deciduous)"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_glacier, "glacier"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_grass, "grass"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_farmland, "farmland"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_orchard, "orchard"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_vineyard, "vineyard"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_water, "water"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.intermittent, "water (intermittent)"));
        entries.add(new RenderThemeLegend.LegendEntry(11, R.drawable.p_military, "military"));

        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.pa, "nature reserve"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.pa_strict, "nature reserve (strict)"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.cliff, "cliff"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.ridge, "ridge"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.dyke, "dyke"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.embankment, "embankment"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.spring, "spring"));
        entries.add(new RenderThemeLegend.LegendEntry(12, R.drawable.powerline, "powerline"));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(21, 22, "Roads & Paths", 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.track1, "paved path"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.track2, "paved path (gravel)"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.track3, "unpaved path"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.track4, "unpaved path, partly grown"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.track5, "unpaved path, grown"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.track_o, "surface unknown"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.path_o, "unpaved footpath"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.footway, "paved footpath"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.bridleway, "bridleway"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.acc_dest, "access (destination)"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.acc_priv, "access (private)"));
        entries.add(new RenderThemeLegend.LegendEntry(21, R.drawable.acc_no, "no access"));

        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.cycleway, "cycleway"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.cycleway_steep, "cycleway (steep)"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.cycleway_paved, "cycleway paved"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.surface_smooth_paved, "paved (smooth)"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.surface_rough_paved, "paved (rough)"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.surface_compacted, "surface compacted"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.surface_gravel, "surface gravel"));
        entries.add(new RenderThemeLegend.LegendEntry(22, R.drawable.surface_raw, "surface raw"));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(31, 32, "Walking, Cycling, Climbing", 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.path_t1, "hiking T1"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.path_t2, "hiking T2"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.path_t3, "hiking T3"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.path_t4, "hiking T4"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.path_t5, "hiking T5"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.path_t6, "hiking T6"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtb0, "MTB S0"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtb1, "MTB S1"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtb2, "MTB S2"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtb3, "MTB S3"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtb4, "MTB S4"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtb5, "MTB S5/6"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtbu0, "uphill -15%"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtbu1, "uphill -20%"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtbu2, "uphill -25%"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtbu3, "uphill -30%"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtbu4, "uphill -40%"));
        entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.mtbu5, "uphill too steep"));

        entries.add(new RenderThemeLegend.LegendEntry(32, R.drawable.s_rope, "Security: rope"));
        entries.add(new RenderThemeLegend.LegendEntry(32, R.drawable.s_rungs, "Security: rungs"));
        entries.add(new RenderThemeLegend.LegendEntry(32, R.drawable.s_ladder, "Security: ladder"));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(41, 42, "POI / Symbols", 7, 7));

        // walking (black)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bench"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cairn"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_drinking_water"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_guidepost"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_info_board"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_info_office"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_info_terminal"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_peak"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_peak_cross"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_picnic_site"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_playground"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_playground_private"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_recycling"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter_picnic"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter_rock"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_telephone"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_toilets"));
        // entries.add(new LegendEntry(41, p, "s_toilets_fee")); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_volcano"));

        // attractions (ocher)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_adit_disused"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_archaeological"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_artwork"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_attraction"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_boundary_stone"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bunker_disused"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_castle"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cinema"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cross"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_fountain"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_library"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_plaque"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_statue"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_stolperstein"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_stone"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_memorial_war"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_monument"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_museum"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ruins"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_stone_historic"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_theatre"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_water_well"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wayside_cross"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wayside_shrine"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_zoo"));

        // recreation (light green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_beach_resort"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cliff_diving"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_climbing"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_climbing_adventure"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_free_flying"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_golf"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gym"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_miniature_golf"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_roller_skating"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shooting"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_skateboard"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_slipway"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_soccer"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_stadium"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_swimming_area"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_swimming_outdoor"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_tennis"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_via_ferrata"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_water_park"));

        // transport (turquoise)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_aerialway_station"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_airport"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cable_car"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_car_shop"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_chair_lift"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_drag_lift"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gondola"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_goods_lift"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_helipad"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_parking"));
        // entries.add(new LegendEntry(41, p, "s_parking_fee")); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_parking_private"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_rail_funicular"));

        // accommodation (light blue)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_alpine_hut"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_alpine_hut_private"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_alpine_hut_winter"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_apartment"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_camp_site"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_caravan_site"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_chalet"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hostel"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hotel"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shelter_basic_hut"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wilderness_hut"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wilderness_hut_private"));

        // food / beverage (brown)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bar"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_biergarten"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cafe"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_fastfood"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ice_cream"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_pub"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_restaurant"));

        // amenities (lila)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_atm"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bakery"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bank"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_beverages"));
        // entries.add(new LegendEntry(41, p, "s_bicycle_charging_station")); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bicycle_rental"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bicycle_shop"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_books"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_butcher"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_chemist"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_convenience"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_doityourself"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_farm_shop"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_laundry"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_mall"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_marketplace"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_organic"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_postbox"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_postoffice"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_sports_shop"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_supermarket"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_travel_agency"));

        // emergency (lila)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_defibrillator"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_doctors"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_embassy"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_emergency_phone"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_firebrigade"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_emergency_access_point"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hospital"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_pharmacy"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_police"));

        // energy (dark green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_adit"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_petroleum_well"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_coal"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_gas"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_nuclear"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_water"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_power_wind"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_wastewater_plant"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_water_works"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_watermill"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_weir"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_windmill"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_windpump"));

        // other (dark green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bunker"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_farmyard"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hunting_stand"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_kindergarten"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_lighthouse"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_school"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_tower_communication"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_tower_observation"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_townhall"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_university"));

        // worship (dark green)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_buddhist"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_church"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hindu"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_mosque"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_place_of_worship"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_shinto"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_synagogue"));

        // obstacles (grey)
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_block"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bollard"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_border_control"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_bridge_movable"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cave_entrance"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cliff"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_cycle_barrier"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_elevator"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ford"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ford_small"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gate"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_gate_private"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_geyser"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_hot_spring"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_kissing_gate"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ladder"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_lift_gate"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_log"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway_cycle"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway_cycle_rev"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_oneway_rev"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_peak_small"));
        // entries.add(new LegendEntry(41, p, "s_railway_crossing")); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_rapids"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_ridge"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_rungs"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_0"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_45"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_90"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_saddle_135"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_safety_rope"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_stone"));
        // entries.add(new LegendEntry(41, p, "s_tile")); // NPE: InputStream.markSupported() on null object reference
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_traffic_signal"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_turnstile"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_viewpoint"));
        entries.add(new RenderThemeLegend.LegendEntry(41, p, "s_waterfall"));


        // ---------------------------------------------------------------

        final RenderThemeLegend.LegendCategory[] categories = new RenderThemeLegend.LegendCategory[cats.size()];
        int i = 0;
        for (RenderThemeLegend.LegendCategory cat : cats) {
            categories[i++] = cat;
        }
        return categories;
    }

    @Override
    public @StringRes int getInfoUrl() {
        return R.string.maptheme_legend_elevate;
    }

}

package cgeo.geocaching.unifiedmap.mapsforgevtm.legend;

import cgeo.geocaching.R;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

class ThemeLegendFreizeitkarte implements ThemeLegend {

    boolean isBasic = true;

    @Override
    // method is long, but trivial; splitting it into submethods would not improve readability
    @SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.NPathComplexity"})
    public RenderThemeLegend.LegendCategory[] loadLegend(final MapsforgeThemeHelper.RenderThemeType rtt, final ArrayList<RenderThemeLegend.LegendEntry> entries) {
        isBasic = rtt == MapsforgeThemeHelper.RenderThemeType.RTT_FZK_BASE;
        final String p = rtt.relPath + "/patterns";
        final String s = rtt.relPath + "/symbols";
        final ArrayList<RenderThemeLegend.LegendCategory> cats = new ArrayList<>();

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(11, 12, R.string.rtl_category_areas, 5, 4));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "alm", R.string.rtl_alp));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "feuchtgebiet", R.string.rtl_wetland));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "feuchtwiese", R.string.rtl_marsh));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "moorgebiet", R.string.rtl_moor));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "gebuesch", R.string.rtl_coppice));
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "laubwald-einfach", R.string.rtl_wood_deciduous));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_laubwald-einfach", R.string.rtl_wood_deciduous));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "mischwald-einfach", R.string.rtl_wood_mixed));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_mischwald-einfach", R.string.rtl_wood_mixed));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "nadelwald-einfach", R.string.rtl_wood_coniferous));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_nadelwald-einfach", R.string.rtl_wood_coniferous));
        } else {
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "laubwald", R.string.rtl_wood_deciduous));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_laubwald", R.string.rtl_wood_deciduous));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "mischwald", R.string.rtl_wood_mixed));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_mischwald", R.string.rtl_wood_mixed));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "nadelwald", R.string.rtl_wood_coniferous));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_nadelwald", R.string.rtl_wood_coniferous));
        }
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "naturschutzgebiet", R.string.rtl_nature_reserve));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "totalreservat", R.string.rtl_nature_reserve_strict));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "plantage", R.string.rtl_plantation));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "schilf", R.string.rtl_reed));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "schlick", R.string.rtl_mud));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "sumpfgebiet", R.string.rtl_marsh));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "watt", R.string.rtl_mudflat));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "weinberg", R.string.rtl_vineyard));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof", R.string.rtl_cemetery));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof-christlich", R.string.rtl_cemetery_christian));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof-juedisch", R.string.rtl_cemetery_jewish));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof-muslimisch", R.string.rtl_cemetery_muslim));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "fussgaengerzone", R.string.rtl_pedestrian_area));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "handel", R.string.rtl_trading));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "parken-eingeschraenkt", R.string.rtl_parking_restricted));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "parken-fahrrad", R.string.rtl_bicycle_parking));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "gefaengnis", R.string.rtl_prison));
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "gehege", R.string.rtl_corral));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "reitplatz", R.string.rtl_riding_range));
        }
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "schrebergaerten", R.string.rtl_allotment));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "spielplatz", R.string.rtl_playground));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "rueckhaltebecken", R.string.rtl_retention_reservoir));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "deponie", R.string.rtl_dump));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "driving-range", R.string.rtl_driving_range));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "militaer", R.string.rtl_military));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "ruine", R.string.rtl_ruin));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "saline", R.string.rtl_saline));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "fels", R.string.rtl_rock));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "geroell", R.string.rtl_scree));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "riff", R.string.rtl_reef));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "gletscher", R.string.rtl_glacier));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "strand", R.string.rtl_beach));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "strand-kies", R.string.rtl_beach_gravel));

        // @todo outdoor themes additionally support baugebiet, dog_park, gefahrenzone, kraftwerk, parken-privat

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(21, 22, R.string.rtl_category_roads_paths, 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_motorway : R.drawable.fzk_outdoor_line_motorway, R.string.rtl_freeway));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_trunk : R.drawable.fzk_outdoor_line_trunk, R.string.rtl_freeway_similar));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_primary : R.drawable.fzk_outdoor_line_primary, R.string.rtl_interstate));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_secondary : R.drawable.fzk_outdoor_line_secondary, R.string.rtl_secondary_road));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_tertiary : R.drawable.fzk_outdoor_line_tertiary, R.string.rtl_district_road));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_unclassified : R.drawable.fzk_outdoor_line_unclassified, R.string.rtl_side_street));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_residential : R.drawable.fzk_outdoor_line_residential, R.string.rtl_residential_road));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_service : R.drawable.fzk_outdoor_line_service, R.string.rtl_access_road));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_living_street : R.drawable.fzk_outdoor_line_living_street, R.string.rtl_traffic_calmed_area));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_pedestrian : R.drawable.fzk_outdoor_line_pedestrian, R.string.rtl_pedestrian_road));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_access_no : R.drawable.fzk_outdoor_line_access_no, R.string.rtl_access_no));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_access_destination : R.drawable.fzk_outdoor_line_access_destination, R.string.rtl_access_destination));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_access_agricultural : R.drawable.fzk_outdoor_line_access_agricultural, R.string.rtl_access_agricultural));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_tunnel : R.drawable.fzk_outdoor_line_tunnel, R.string.rtl_tunnel));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_bridge : R.drawable.fzk_outdoor_line_bridge, R.string.rtl_bridge));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_rail : R.drawable.fzk_outdoor_line_rail, R.string.rtl_railway));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(31, 32, R.string.rtl_category_walking_cycling_climbing, 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_foot : R.drawable.fzk_outdoor_line_path_foot, R.string.rtl_path_foot));
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.fzk_line_path_bicycle, R.string.rtl_path_bicycle));
        }
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_bicycle_foot : R.drawable.fzk_outdoor_line_path_bicycle_foot, R.string.rtl_path_combined_bicycle_foot));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_bridleway : R.drawable.fzk_outdoor_line_bridleway, R.string.rtl_bridleway));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path : R.drawable.fzk_outdoor_line_path, R.string.rtl_path_narrow));
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.fzk_outdoor_line_path_visibility, R.string.rtl_path_badly_visible));
        }
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_track_1 : R.drawable.fzk_outdoor_line_track_1, R.string.rtl_path_quality1));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_track_230 : R.drawable.fzk_outdoor_line_track_230, R.string.rtl_path_quality23));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_track_45 : R.drawable.fzk_outdoor_line_track_45, R.string.rtl_path_quality45));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_t1 : R.drawable.fzk_outdoor_line_path_t1, R.string.rtl_hiking_T1));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_t2 : R.drawable.fzk_outdoor_line_path_t2, R.string.rtl_hiking_T23));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_t4 : R.drawable.fzk_outdoor_line_path_t4, R.string.rtl_hiking_T45));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_via_ferrata : R.drawable.fzk_outdoor_line_via_ferrata, R.string.rtl_via_ferrata));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(41, 42, R.string.rtl_category_poi_symbols, 7, 7));

        // amenities: transport
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bicycle_parking", R.string.rtl_bicycle_parking));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bicycle_rental", R.string.rtl_bicycle_rental));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_car_rental", R.string.rtl_car_rental));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_car_sharing", R.string.rtl_car_sharing));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_car_wash", R.string.rtl_car_wash));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fuel", R.string.rtl_fuel));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_ferry_terminal", R.string.rtl_ferry_terminal));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "parken" : "amenity_parking", R.string.rtl_parking));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "parken-eingeschraenkt" : "amenity_parking_private", isBasic ? R.string.rtl_parking_restricted : R.string.rtl_parking_private));

        // amenities: food & drink
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bar", R.string.rtl_bar));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bbq", R.string.rtl_bbq));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_biergarten", R.string.rtl_biergarten));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_cafe", R.string.rtl_cafe));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pub", R.string.rtl_pub));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_restaurant", R.string.rtl_restaurant));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_ice_cream", R.string.rtl_ice_cream));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fastfood", R.string.rtl_fastfood));

        // amenities: emergency
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pharmacy", R.string.rtl_pharmacy));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_hospital", R.string.rtl_hospital));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_clinic", R.string.rtl_clinic));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_police", R.string.rtl_police));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fire_station", R.string.rtl_firebrigade));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "emergency_fire_hydrant", R.string.rtl_emergency_fire_hydrant));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "emergency_suction_point", R.string.rtl_emergency_suction_point));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "emergency_access_point", R.string.rtl_emergency_access_point));

        // amenities: education
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_college", R.string.rtl_college));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_kindergarten", R.string.rtl_kindergarten));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_school", R.string.rtl_school));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_university", R.string.rtl_university));
        }

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_arts_centre", R.string.rtl_arts_centre));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_library", R.string.rtl_library));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_theatre", R.string.rtl_theatre));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_cinema", R.string.rtl_cinema));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_casino", R.string.rtl_casino));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_nightclub", R.string.rtl_nightclub));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_community_centre", R.string.rtl_community_centre));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_social_facility", R.string.rtl_social_facility));

        // amenities
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "geldautomat" : "amenity_atm", R.string.rtl_atm));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bank", R.string.rtl_bank));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_post_office", R.string.rtl_postoffice));

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_townhall", R.string.rtl_townhall));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_courthouse", R.string.rtl_courthouse));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_embassy", R.string.rtl_embassy));

        // amenities: places of worship
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "kirche", R.string.rtl_pow));
        } else {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pow", R.string.rtl_pow));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pow_christian", R.string.rtl_pow_christian));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pow_jewish", R.string.rtl_pow_jewish));
        }
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "moschee" : "amenity_pow_muslim", R.string.rtl_pow_muslim));

        // amenities: other
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_drinking_water", R.string.rtl_drinking_water));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fountain", R.string.rtl_fountain));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_water_point", R.string.rtl_water_point));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_toilets", R.string.rtl_toilets));

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_recycling", R.string.rtl_recycling));
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bench", R.string.rtl_bench));
        }
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_hunting_stand", R.string.rtl_hunting_stand));
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_shelter", R.string.rtl_shelter));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_shelter_basic_hut", R.string.rtl_shelter_basic_hut));
        }

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
        return isBasic ? R.string.maptheme_legend_freizeitkarte_base : R.string.maptheme_legend_freizeitkarte_outdoor;
    }
}

package cgeo.geocaching.maps.mapsforge.v6.legend;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;

import androidx.annotation.StringRes;

import java.util.ArrayList;

class ThemeLegendFreizeitkarte implements ThemeLegend {

    boolean isBasic = true;

    @Override
    @SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.NPathComplexity"}) // method is long, but trivial; splitting it into submethods would not improve readability
    public RenderThemeLegend.LegendCategory[] loadLegend(final RenderThemeHelper.RenderThemeType rtt, final ArrayList<RenderThemeLegend.LegendEntry> entries) {
        isBasic = rtt == RenderThemeHelper.RenderThemeType.RTT_FZK_BASE;
        final String p = rtt.relPath + "/patterns";
        final String s = rtt.relPath + "/symbols";
        final ArrayList<RenderThemeLegend.LegendCategory> cats = new ArrayList<>();

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(11, 12, R.string.rtl_category_areas, 5, 4));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "alm"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "feuchtgebiet"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "feuchtwiese"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "moorgebiet"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "gebuesch"));
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "laubwald-einfach"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_laubwald-einfach"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "mischwald-einfach"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_mischwald-einfach"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "nadelwald-einfach"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_nadelwald-einfach"));
        } else {
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "laubwald"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_laubwald"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "mischwald"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_mischwald"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "nadelwald"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "natur_nadelwald"));
        }
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "naturschutzgebiet"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "totalreservat"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "plantage"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "schilf"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "schlick"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "sumpfgebiet"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "watt"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "weinberg"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof-christlich"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof-juedisch"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "friedhof-muslimisch"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "fussgaengerzone"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "handel"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "parken-eingeschraenkt"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "parken-fahrrad"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "gefaengnis"));
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "gehege"));
            entries.add(new RenderThemeLegend.LegendEntry(11, p, "reitplatz"));
        }
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "schrebergaerten"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "spielplatz"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "rueckhaltebecken"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "deponie"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "driving-range"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "militaer"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "ruine"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "saline"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "fels"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "geroell"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "riff"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "gletscher"));

        entries.add(new RenderThemeLegend.LegendEntry(11, p, "strand"));
        entries.add(new RenderThemeLegend.LegendEntry(11, p, "strand-kies"));

        // @todo outdoor themes additionally support baugebiet, dog_park, gefahrenzone, kraftwerk, parken-privat

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(21, 22, R.string.rtl_category_roads_paths, 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_motorway : R.drawable.fzk_outdoor_line_motorway, "freeway"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_trunk : R.drawable.fzk_outdoor_line_trunk, "similar to freeway"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_primary : R.drawable.fzk_outdoor_line_primary, "interstate / A road"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_secondary : R.drawable.fzk_outdoor_line_secondary, "secondary road"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_tertiary : R.drawable.fzk_outdoor_line_tertiary, "district / tertiary road"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_unclassified : R.drawable.fzk_outdoor_line_unclassified, "side street"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_residential : R.drawable.fzk_outdoor_line_residential, "residential road"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_service : R.drawable.fzk_outdoor_line_service, "access road"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_living_street : R.drawable.fzk_outdoor_line_living_street, "traffic-calmed area"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_pedestrian : R.drawable.fzk_outdoor_line_pedestrian, "pedestrian road"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_access_no : R.drawable.fzk_outdoor_line_access_no, "usage is not allowed"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_access_destination : R.drawable.fzk_outdoor_line_access_destination, "restricted for motor traffic"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_access_agricultural : R.drawable.fzk_outdoor_line_access_agricultural, "restricted to agricultural / forest traffic"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_tunnel : R.drawable.fzk_outdoor_line_tunnel, "tunnel"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_bridge : R.drawable.fzk_outdoor_line_bridge, "bridge"));
        entries.add(new RenderThemeLegend.LegendEntry(21, isBasic ? R.drawable.fzk_line_rail : R.drawable.fzk_outdoor_line_rail, "railway"));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(31, 32, R.string.rtl_category_walking_cycling_climbing, 4, 6));

        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_foot : R.drawable.fzk_outdoor_line_path_foot, "general foot path"));
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.fzk_line_path_bicycle, "general bicycle path"));
        }
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_bicycle_foot : R.drawable.fzk_outdoor_line_path_bicycle_foot, "combined bicycle and foot path"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_bridleway : R.drawable.fzk_outdoor_line_bridleway, "bridleway"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path : R.drawable.fzk_outdoor_line_path, "general way or path, not wide enough for cars"));
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(31, R.drawable.fzk_outdoor_line_path_visibility, "badly visible path"));
        }
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_track_1 : R.drawable.fzk_outdoor_line_track_1, "quality 1 (paved: asphalt, ...)"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_track_230 : R.drawable.fzk_outdoor_line_track_230, "quality 2/3 (paved: gravel, repaired, ...)"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_track_45 : R.drawable.fzk_outdoor_line_track_45, "quality 4/5 (unpaved)"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_t1 : R.drawable.fzk_outdoor_line_path_t1, "hiking T1"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_t2 : R.drawable.fzk_outdoor_line_path_t2, "hiking T2/T3"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_path_t4 : R.drawable.fzk_outdoor_line_path_t4, "hiking T4/T5"));
        entries.add(new RenderThemeLegend.LegendEntry(31, isBasic ? R.drawable.fzk_line_via_ferrata : R.drawable.fzk_outdoor_line_via_ferrata, "via ferrata"));

        // ---------------------------------------------------------------

        cats.add(new RenderThemeLegend.LegendCategory(41, 42, R.string.rtl_category_poi_symbols, 7, 7));

        // amenities: transport
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bicycle_parking"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bicycle_rental"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_car_rental"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_car_sharing"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_car_wash"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fuel"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_ferry_terminal"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "parken" : "amenity_parking"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "parken-eingeschraenkt" : "amenity_parking_private"));

        // amenities: food & drink
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bar"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bbq"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_biergarten"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_cafe"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pub"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_restaurant"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_ice_cream"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fastfood"));

        // amenities: emergency
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pharmacy"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_hospital"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_clinic"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_police"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fire_station"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "emergency_fire_hydrant"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "emergency_suction_point"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "emergency_access_point"));

        // amenities: education
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_college"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_kindergarten"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_school"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_university"));
        }

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_arts_centre"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_library"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_theatre"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_cinema"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_casino"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_nightclub"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_community_centre"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_social_facility"));

        // amenities
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "geldautomat" : "amenity_atm"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bank"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_post_office"));

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_townhall"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_courthouse"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_embassy"));

        // amenities: places of worship
        if (isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "kirche"));
        } else {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pow"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pow_christian"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_pow_jewish"));
        }
        entries.add(new RenderThemeLegend.LegendEntry(41, s, isBasic ? "moschee" : "amenity_pow_muslim"));

        // amenities: other
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_drinking_water"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_fountain"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_water_point"));
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_toilets"));

        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_recycling"));
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_bench"));
        }
        entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_hunting_stand"));
        if (!isBasic) {
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_shelter"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_shelter_basic_hut"));
            entries.add(new RenderThemeLegend.LegendEntry(41, s, "amenity_shelter_lean_to"));
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
    public @StringRes int getInfoUrl() {
        return isBasic ? R.string.maptheme_legend_freizeitkarte_base : R.string.maptheme_legend_freizeitkarte_outdoor;
    }
}

// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.core

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.location.GeopointParser
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.BooleanUtils

class DistanceGeocacheFilter : NumberRangeGeocacheFilter()<Float> {

    private static val CONFIG_KEY_COORD: String = "coord"
    private static val CONFIG_KEY_USE_CURRENT_POS: String = "use_current_pos"

    private Geopoint coordinate
    private Boolean useCurrentPosition

    public DistanceGeocacheFilter() {
        super(Float::valueOf, f -> f)
    }

    /**
     * Gets fixed-value coordinate set to this filter, may be null
     */
    public Geopoint getCoordinate() {
        return coordinate
    }

    public Unit setCoordinate(final Geopoint coordinate) {
        this.coordinate = coordinate
    }

    public Boolean isUseCurrentPosition() {
        return useCurrentPosition
    }

    public Unit setUseCurrentPosition(final Boolean useCurrentPosition) {
        this.useCurrentPosition = useCurrentPosition
    }

    override     protected Float getValue(final Geocache cache) {
        val gp: Geopoint = (useCurrentPosition || coordinate == null) ?
                LocationDataProvider.getInstance().currentGeo().getCoords() : coordinate

        return gp.distanceTo(cache.getCoords())
    }

    /**
     * Returns the coordinate which will effectively be used for calculation (either fixed-value or current position)
     */
    public Geopoint getEffectiveCoordinate() {
        return (useCurrentPosition || coordinate == null) ?
                LocationDataProvider.getInstance().currentGeo().getCoords() : coordinate
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        super.setConfig(config)
        useCurrentPosition = config.getFirstValue(CONFIG_KEY_USE_CURRENT_POS, false, BooleanUtils::toBoolean)
        coordinate = config.getFirstValue(CONFIG_KEY_COORD, null, s -> GeopointParser.parse(s, null))
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = super.getConfig()
        config.putList(CONFIG_KEY_USE_CURRENT_POS, Boolean.toString(useCurrentPosition))
        config.putList(CONFIG_KEY_COORD, coordinate == null ? "-" : GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, coordinate))
        return config
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = super.getJsonConfig()
        JsonUtils.setBoolean(node, CONFIG_KEY_USE_CURRENT_POS, useCurrentPosition)
        JsonUtils.setText(node, CONFIG_KEY_COORD, coordinate == null ? null : GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, coordinate))
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode config) {
        super.setJsonConfig(config)
        useCurrentPosition = JsonUtils.getBoolean(config, CONFIG_KEY_USE_CURRENT_POS, false)
        val coordText: String = JsonUtils.getText(config, CONFIG_KEY_COORD, null)
        coordinate = coordText == null ? null : GeopointParser.parse(coordText, null)
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        val gp: Geopoint = (useCurrentPosition || coordinate == null) ?
                LocationDataProvider.getInstance().currentGeo().getCoords() : coordinate
        val sql: String = DataStore.getSqlDistanceSquare(
                sqlBuilder.getMainTableId() + ".latitude", sqlBuilder.getMainTableId() + ".longitude", gp)

        addRangeToSqlBuilder(sqlBuilder, sql, v -> v * v)
    }

    override     public Boolean isFiltering() {
        return super.isFiltering() || !useCurrentPosition
    }

    override     protected String getUserDisplayableConfig() {
        return GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT, getEffectiveCoordinate()) + "(" + super.getUserDisplayableConfig() + ")"
    }


}

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

import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.functions.Action1

import java.util.List

/**
 * Base implementation for common (non-logical) geocache filters
 */
abstract class BaseGeocacheFilter : IGeocacheFilter {

    private GeocacheFilterType type

    public Unit setType(final GeocacheFilterType type) {
        this.type = type
    }

    public GeocacheFilterType getType() {
        return type
    }

    public String getId() {
        return this.type == null ? "" : this.type.getTypeId()
    }

    override     public Unit addChild(final IGeocacheFilter child) {
        //common filters have no children
    }

    override     public List<IGeocacheFilter> getChildren() {
        //common filters have no children
        return null
    }

    override     public String toUserDisplayableString(final Int level) {
        if (!isFiltering()) {
            return null
        }
        val sb: StringBuilder = StringBuilder(getType().getUserDisplayableName())
        if (level <= 2) {
            val userDisplayValue: String = getUserDisplayableConfig()
            if (userDisplayValue != null) {
                sb.append(": ").append(userDisplayValue)
            }
        }
        return sb.toString()
    }

    /**
     * To be overwrite potentially by subclasses wishing to provide user-displayable filter config information
     */
    protected String getUserDisplayableConfig() {
        return null
    }

    /**
     * Helper method to read Boolean config values as flags
     */
    protected static Boolean checkBooleanFlag(final String expectedFlag, final String value) {
        return TextUtils.isEqualIgnoreCaseAndSpecialChars(value, expectedFlag)
    }

    /**
     * Helper method to read enum class config values
     */
    protected static <E : Enum()<E>> Unit checkEnumValue(final Class<E> enumClass, final String value, final Action1<E> executeIfFound) {
        val enumValue: E = TextUtils.getEnumIgnoreCaseAndSpecialChars(enumClass, value, null)
        if (enumValue != null) {
            executeIfFound.call(enumValue)
        }
    }


}

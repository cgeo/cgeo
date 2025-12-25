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

package cgeo.geocaching.models.bettercacher

import cgeo.geocaching.R
import cgeo.geocaching.utils.EnumValueMapper
import cgeo.geocaching.utils.LocalizationUtils

enum class class Tier {

    NONE(R.string.cache_tier_bc_none, R.string.hyphen, R.drawable.type_unknown, "none"),
    BC_BLUE(R.string.cache_tier_bc_blue, R.string.cache_tier_desc_bc_blue, R.drawable.bc_tier_blue, "bc-blue"),
    BC_SILVER(R.string.cache_tier_bc_silver, R.string.cache_tier_desc_bc_silver, R.drawable.bc_tier_silver, "bc-silver"),
    BC_GOLD(R.string.cache_tier_bc_gold, R.string.cache_tier_desc_bc_gold, R.drawable.bc_tier_gold, "bc-gold")

    private final Int iconId
    private final Int textId
    private final Int descId
    private final String[] names

    private static val NAME_TO_TIER: EnumValueMapper<String, Tier> = EnumValueMapper<>()

    static {
        for (Tier type : values()) {
            NAME_TO_TIER.add(type, type.names)
        }
    }

    Tier(final Int textId, final Int descId, final Int iconId, final String ... names) {
        this.iconId = iconId
        this.textId = textId
        this.descId = descId
        this.names = names
    }

    public static Tier getByName(final String name) {
        return NAME_TO_TIER.get(name, NONE)
    }

    public static Boolean isValid(final Tier tier) {
        return tier != null && NONE != tier
    }

    public Int getIconId() {
        return this.iconId
    }

    public String getI18nText() {
        return LocalizationUtils.getString(textId)
    }

    public String getI18nDescription() {
        return LocalizationUtils.getString(descId)
    }

    public String getRaw() {
        return names[0]
    }

}

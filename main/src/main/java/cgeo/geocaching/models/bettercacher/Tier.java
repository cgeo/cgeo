package cgeo.geocaching.models.bettercacher;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

public enum Tier {

    NONE(R.string.cache_tier_bc_none, R.string.hyphen, R.drawable.type_unknown, "none"),
    BC_BLUE(R.string.cache_tier_bc_blue, R.string.cache_tier_desc_bc_blue, R.drawable.bc_tier_blue, "bc-blue"),
    BC_SILVER(R.string.cache_tier_bc_silver, R.string.cache_tier_desc_bc_silver, R.drawable.bc_tier_silver, "bc-silver"),
    BC_GOLD(R.string.cache_tier_bc_gold, R.string.cache_tier_desc_bc_gold, R.drawable.bc_tier_gold, "bc-gold");

    private final int iconId;
    private final int textId;
    private final int descId;
    private final String[] names;

    private static final EnumValueMapper<String, Tier> NAME_TO_TIER = new EnumValueMapper<>();

    static {
        for (Tier type : values()) {
            NAME_TO_TIER.add(type, type.names);
        }
    }

    Tier(final int textId, final int descId, final int iconId, final String ... names) {
        this.iconId = iconId;
        this.textId = textId;
        this.descId = descId;
        this.names = names;
    }

    public static Tier getByName(final String name) {
        return NAME_TO_TIER.get(name, NONE);
    }

    public static boolean isValid(final Tier tier) {
        return tier != null && NONE != tier;
    }

    public int getIconId() {
        return this.iconId;
    }

    public String getI18nText() {
        return LocalizationUtils.getString(textId);
    }

    public String getI18nDescription() {
        return LocalizationUtils.getString(descId);
    }

    public String getRaw() {
        return names[0];
    }

}

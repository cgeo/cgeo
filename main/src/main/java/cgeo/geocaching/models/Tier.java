package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

public enum Tier {

    NONE(R.string.cache_tier_none, R.drawable.type_unknown, "none"),
    BC_BLUE(R.string.cache_tier_bc_blue, R.drawable.type_unknown, "bc-blue"),
    BC_SILVER(R.string.cache_tier_bc_silver, R.drawable.type_unknown, "bc-silver"),
    BC_GOLD(R.string.cache_tier_bc_gold, R.drawable.type_unknown, "bc-gold");

    public final int iconId;
    public final int textId;
    private final String[] names;

    private static final EnumValueMapper<String, Tier> NAME_TO_TIER = new EnumValueMapper<>();

    static {
        for (Tier type : values()) {
            NAME_TO_TIER.add(type, type.names);
        }
    }

    Tier(final int textId, final int iconId, final String ... names) {
        this.iconId = iconId;
        this.textId = textId;
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

    public String getRaw() {
        return names[0];
    }

}

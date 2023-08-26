package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.Objects;

public class Tier {

    public enum Type {
        UNKNOWN(R.string.cache_tier_unknown, R.drawable.type_unknown, "unknown"),
        BC_BLUE(R.string.cache_tier_bc_blue, R.drawable.type_unknown, "bc-blue"),
        BC_SILVER(R.string.cache_tier_bc_silver, R.drawable.type_unknown, "bc-silver"),
        BC_GOLD(R.string.cache_tier_bc_gold, R.drawable.type_unknown, "bc-gold");

        public final int iconId;
        public final int textId;
        private final String[] names;

        private static final EnumValueMapper<String, Type> NAME_TO_TYPE = new EnumValueMapper<>();

        static {
            for (Type type : values()) {
                NAME_TO_TYPE.add(type, type.names);
            }
        }

        Type(final int textId, final int iconId, final String ... names) {
            this.iconId = iconId;
            this.textId = textId;
            this.names = names;
        }

        public static Type getByName(final String name) {
            return NAME_TO_TYPE.get(name, UNKNOWN);
        }
    }

    private final Type type;
    private final String raw;

    private Tier(final String name) {
        this.raw = name;
        this.type = Type.getByName(name);
    }

    public static Tier of(final String name) {
        if (name == null) {
            return null;
        }
        return new Tier(name);
    }

    public int getIconId() {
        return this.type.iconId;
    }

    public String getI18nText() {
        final StringBuilder sb = new StringBuilder();
        sb.append(LocalizationUtils.getString(type.textId));
        if (this.type == Type.UNKNOWN) {
            sb.append("(").append(raw).append(")");
        }
        return sb.toString();
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Tier tier = (Tier) o;
        return raw.equals(tier.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }
}

package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.Objects;

public class Category {

    public enum Type {
        UNKNOWN(R.string.cache_cat_unknown, R.drawable.type_unknown, "unknown"),
        BC_MYSTERY(R.string.cache_cat_bc_mystery, R.drawable.type_unknown, "bc-mystery"),
        BC_GADGET(R.string.cache_cat_bc_gadget, R.drawable.type_unknown, "bc-gadget"),
        BC_NATURE(R.string.cache_cat_bc_nature, R.drawable.type_unknown, "bc-nature"),
        BC_HIKING(R.string.cache_cat_bc_hiking, R.drawable.type_unknown, "bc-hiking"),
        BC_LOCATION(R.string.cache_cat_bc_location, R.drawable.type_unknown, "bc-location"),
        BC_OTHER(R.string.cache_cat_bc_other, R.drawable.type_unknown, "bc-other"),
        BC_RECOMMENDATION(R.string.cache_cat_bc_recommended, R.drawable.type_unknown, "bc-recommendation");

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
    private final String rawCategory;

    public Category(final String name) {
        this.rawCategory = name;
        this.type = Type.getByName(name);
    }

    public Category(final Type catType) {
        this.type = catType;
        this.rawCategory = this.type.names[0];
    }

    public Type getType() {
        return type;
    }

    public int getIconId() {
        return this.type.iconId;
    }

    public String getI18nText() {
        final StringBuilder sb = new StringBuilder();
        sb.append(LocalizationUtils.getString(type.textId));
        if (this.type == Type.UNKNOWN) {
            sb.append("(").append(rawCategory).append(")");
        }
        return sb.toString();
    }

    public String getRaw() {
        return rawCategory;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Category category = (Category) o;
        return rawCategory.equals(category.rawCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawCategory);
    }
}

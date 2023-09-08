package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.ArrayList;
import java.util.List;

public enum Category {

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

    private static final EnumValueMapper<String, Category> NAME_TO_CATEGORY = new EnumValueMapper<>();

    static {
        for (Category type : values()) {
            NAME_TO_CATEGORY.add(type, type.names);
        }
    }

    Category(final int textId, final int iconId, final String ... names) {
        this.iconId = iconId;
        this.textId = textId;
        this.names = names;
    }

    public static Category getByName(final String name) {
        return NAME_TO_CATEGORY.get(name, UNKNOWN);
    }

    public static boolean isValid(final Category cat) {
        return cat != null && UNKNOWN != cat;
    }

    public static List<Category> getAllCategoriesExceptUnknown() {
        final List<Category> list = new ArrayList<>();
        for (Category cat : Category.values()) {
            if (UNKNOWN != cat) {
                list.add(cat);
            }
        }
        return list;
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

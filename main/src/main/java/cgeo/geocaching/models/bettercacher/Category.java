package cgeo.geocaching.models.bettercacher;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.ArrayList;
import java.util.List;

public enum Category {
    UNKNOWN(R.string.cache_cat_bc_unknown, R.string.hyphen, R.drawable.type_unknown, "unknown"),
    BC_MYSTERY(R.string.cache_cat_bc_mystery, R.string.cache_cat_desc_bc_mystery, R.drawable.bc_category_mystery, "bc-mystery"),
    BC_GADGET(R.string.cache_cat_bc_gadget, R.string.cache_cat_desc_bc_gadget, R.drawable.bc_category_gadget, "bc-gadget"),
    BC_NATURE(R.string.cache_cat_bc_nature, R.string.cache_cat_desc_bc_nature, R.drawable.bc_category_nature, "bc-nature"),
    BC_HIKING(R.string.cache_cat_bc_hiking, R.string.cache_cat_desc_bc_hiking, R.drawable.bc_category_hiking, "bc-hiking"),
    BC_LOCATION(R.string.cache_cat_bc_location, R.string.cache_cat_desc_bc_location, R.drawable.bc_category_location, "bc-location"),
    BC_OTHER(R.string.cache_cat_bc_other, R.string.cache_cat_desc_bc_other, R.drawable.bc_category_other, "bc-other"),
    BC_RECOMMENDATION(R.string.cache_cat_bc_recommended, R.string.cache_cat_desc_bc_recommended, R.drawable.bc_category_recommendation, "bc-recommendation");

    private final int iconId;
    private final int textId;
    private final int descId;
    private final String[] names;

    private static final EnumValueMapper<String, Category> NAME_TO_CATEGORY = new EnumValueMapper<>();

    static {
        for (Category type : values()) {
            NAME_TO_CATEGORY.add(type, type.names);
        }
    }

    Category(final int textId, final int descId, final int iconId, final String ... names) {
        this.iconId = iconId;
        this.textId = textId;
        this.descId = descId;
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

    public String getI18nDescription() {
        return LocalizationUtils.getString(descId);
    }

    public String getRaw() {
        return names[0];
    }

}

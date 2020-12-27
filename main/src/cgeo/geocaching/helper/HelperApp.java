package cgeo.geocaching.helper;

import androidx.annotation.StringRes;

final class HelperApp {
    final int titleId;
    final int descriptionId;
    final int iconId;
    final int packageNameResId;

    HelperApp(final int title, final int description, final int icon, @StringRes final int packageNameResId) {
        this.titleId = title;
        this.descriptionId = description;
        this.iconId = icon;
        this.packageNameResId = packageNameResId;
    }

}

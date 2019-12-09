package cgeo.geocaching.helper;

import androidx.annotation.NonNull;

final class HelperApp {
    final int titleId;
    final int descriptionId;
    final int iconId;
    @NonNull
    final String packageName;

    HelperApp(final int title, final int description, final int icon, @NonNull final String packageName) {
        this.titleId = title;
        this.descriptionId = description;
        this.iconId = icon;
        this.packageName = packageName;
    }

}

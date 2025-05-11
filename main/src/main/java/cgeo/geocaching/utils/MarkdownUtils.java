package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.SettingsActivity;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.LinkResolverDef;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import org.apache.commons.lang3.StringUtils;

public class MarkdownUtils {

    private MarkdownUtils() {
        // utility class
    }

    public static Markwon create(final Context context) {
        return Markwon.builder(context)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureConfiguration(@NonNull final MarkwonConfiguration.Builder builder) {
                        builder.linkResolver((view, link) -> {
                            final Uri uri = Uri.parse(link);
                            if (uri != null) {
                                // filter links to c:geo-internal settings
                                if (StringUtils.equals(uri.getScheme(), context.getString(R.string.settings_scheme))) {
                                    SettingsActivity.openForSettingsLink(uri, context);
                                } else {
                                    new LinkResolverDef().resolve(view, link);
                                }
                            }
                        });
                    }
                })
                .build();
    }

}

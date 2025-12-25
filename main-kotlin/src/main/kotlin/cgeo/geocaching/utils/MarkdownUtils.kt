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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.settings.SettingsActivity

import android.content.Context
import android.net.Uri

import androidx.annotation.NonNull

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolverDef
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import org.apache.commons.lang3.StringUtils

class MarkdownUtils {

    private MarkdownUtils() {
        // utility class
    }

    public static Markwon create(final Context context) {
        return Markwon.builder(context)
                .usePlugin(AbstractMarkwonPlugin() {
                    override                     public Unit configureConfiguration(final MarkwonConfiguration.Builder builder) {
                        builder.linkResolver((view, link) -> {
                            val uri: Uri = Uri.parse(link)
                            if (uri != null) {
                                // filter links to c:geo-internal settings
                                if (StringUtils == (uri.getScheme(), context.getString(R.string.settings_scheme))) {
                                    SettingsActivity.openForSettingsLink(uri, context)
                                } else {
                                    LinkResolverDef().resolve(view, link)
                                }
                            }
                        })
                    }
                })
                .build()
    }

}

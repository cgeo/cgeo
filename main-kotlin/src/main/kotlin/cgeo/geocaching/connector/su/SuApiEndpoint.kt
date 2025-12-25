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

package cgeo.geocaching.connector.su

import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel

import androidx.annotation.NonNull


enum class SuApiEndpoint {
    CACHE("/api/geocache.php", OAuthLevel.Level1),
    CACHE_LIST("/api/list.php", OAuthLevel.Level1),
    CACHE_LIST_CENTER("/api/list_center.php", OAuthLevel.Level1),
    CACHE_LIST_OWNER("/api/list_owner.php", OAuthLevel.Level1),
    CACHE_LIST_KEYWORD("/api/list_keyword.php", OAuthLevel.Level1),
    NOTE("/api/note.php", OAuthLevel.Level1),
    MARK("/api/mark.php", OAuthLevel.Level1),
    IGNORE("/api/ignore.php", OAuthLevel.Level1),
    RECOMMENDATION("/api/recommendation.php", OAuthLevel.Level1),
    VALUE("/api/value.php", OAuthLevel.Level1),
    POST_IMAGE("/api/photo.php", OAuthLevel.Level1),
    USER("/api/profile.php", OAuthLevel.Level1),
    PERSONAL_NOTE("/api/personal_note.php", OAuthLevel.Level1)

    final String methodName
    final OAuthLevel level

    SuApiEndpoint(final String methodName, final OAuthLevel level) {
        this.methodName = methodName
        this.level = level
    }

}

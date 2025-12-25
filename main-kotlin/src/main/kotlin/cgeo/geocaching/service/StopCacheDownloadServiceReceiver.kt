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

package cgeo.geocaching.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopCacheDownloadServiceReceiver : BroadcastReceiver() {
    override     public Unit onReceive(final Context context, final Intent intent) {
        CacheDownloaderService.requestStopService()
    }
}

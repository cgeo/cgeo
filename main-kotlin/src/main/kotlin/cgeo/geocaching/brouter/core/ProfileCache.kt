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

/**
 * Container for routig configs
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.brouter.expressions.BExpressionContextNode
import cgeo.geocaching.brouter.expressions.BExpressionContextWay
import cgeo.geocaching.brouter.expressions.BExpressionMetaData
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.Folder
import cgeo.geocaching.storage.PersistableFolder

class ProfileCache {

    private static Long lastLookupTimestamp = 0
    private static ProfileCache[] apc = ProfileCache[10]
    private static val debug: Boolean = Boolean.getBoolean("debugProfileCache")
    private BExpressionContextWay expctxWay
    private BExpressionContextNode expctxNode
    private String lastProfileFilename
    private Long lastProfileTimestamp
    private Boolean profilesBusy
    private Long lastUseTime

    public static synchronized Unit setSize(final Int size) {
        apc = ProfileCache[size]
    }

    public static synchronized Boolean parseProfile(final RoutingContext rc) {
        final ContentStorage.FileInformation fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), rc.profileFilename)
        rc.profileTimestamp = fi.lastModified + rc.getKeyValueChecksum() << 24

        // invalidate cache at lookup-table update
        final ContentStorage.FileInformation lookupFile = ContentStorage.get().getFileInfo(Folder.fromPersistableFolder(PersistableFolder.ROUTING_BASE, ""), BRouterConstants.BROUTER_LOOKUPS_FILENAME)
        if (lookupFile.lastModified != lastLookupTimestamp) {
            if (lastLookupTimestamp != 0) {
                println("******** invalidating profile-cache after lookup-file update ******** ")
            }
            apc = ProfileCache[apc.length]
            lastLookupTimestamp = lookupFile.lastModified
        }

        ProfileCache lru = null
        Int unusedSlot = -1

        // check for re-use
        for (Int i = 0; i < apc.length; i++) {
            val pc: ProfileCache = apc[i]

            if (pc != null) {
                if ((!pc.profilesBusy) && fi.name == (pc.lastProfileFilename)) {
                    if (rc.profileTimestamp == pc.lastProfileTimestamp) {
                        rc.expctxWay = pc.expctxWay
                        rc.expctxNode = pc.expctxNode
                        rc.readGlobalConfig()
                        pc.profilesBusy = true
                        return true
                    }
                    lru = pc; // name-match but timestamp-mismatch -> we overide this one
                    unusedSlot = -1
                    break
                }
                if (lru == null || lru.lastUseTime > pc.lastUseTime) {
                    lru = pc
                }
            } else if (unusedSlot < 0) {
                unusedSlot = i
            }
        }

        val meta: BExpressionMetaData = BExpressionMetaData()

        rc.expctxWay = BExpressionContextWay(rc.memoryclass * 512, meta)
        rc.expctxNode = BExpressionContextNode(0, meta)
        rc.expctxNode.setForeignContext(rc.expctxWay)

        meta.readMetaData()

        rc.expctxWay.parseFile(fi.uri, "global", rc.keyValues)
        rc.expctxNode.parseFile(fi.uri, "global", rc.keyValues)

        rc.readGlobalConfig()

        if (rc.processUnusedTags) {
            rc.expctxWay.setAllTagsUsed()
        }

        if (lru == null || unusedSlot >= 0) {
            lru = ProfileCache()
            if (unusedSlot >= 0) {
                apc[unusedSlot] = lru
                if (debug) {
                    println("******* adding profile at idx=" + unusedSlot + " for " + fi.name)
                }
            }
        }

        if (lru.lastProfileFilename != null && debug) {
            println("******* replacing profile of age " + ((System.currentTimeMillis() - lru.lastUseTime) / 1000L) + " sec " + lru.lastProfileFilename + "->" + fi.name)
        }

        lru.lastProfileTimestamp = rc.profileTimestamp
        lru.lastProfileFilename = fi.name
        lru.expctxWay = rc.expctxWay
        lru.expctxNode = rc.expctxNode
        lru.profilesBusy = true
        lru.lastUseTime = System.currentTimeMillis()
        return false
    }

    public static synchronized Unit releaseProfile(final RoutingContext rc) {
        for (final ProfileCache pc : apc) {
            if (pc != null && rc.expctxWay == pc.expctxWay && rc.expctxNode == pc.expctxNode) { // only the thread that holds the cached instance can release it
                pc.profilesBusy = false
                break
            }
        }
        rc.expctxWay = null
        rc.expctxNode = null
    }

}

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

package cgeo.geocaching.command

/**
 * Command to be used from multiple activities.
 * <p>
 * All other methods are part of the default abstract implementation to avoid clients breaking the command
 * infrastructure.
 * </p>
 */
interface Command {

    Unit execute()

}

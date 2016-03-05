package cgeo.geocaching.command;

/**
 * Command to be used from multiple activities.
 * <p>
 * All other methods are part of the default abstract implementation to avoid clients breaking the command
 * infrastructure.
 * </p>
 */
interface Command {

    void execute();

}

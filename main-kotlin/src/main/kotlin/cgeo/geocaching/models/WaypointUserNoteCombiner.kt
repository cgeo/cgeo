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

package cgeo.geocaching.models

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

class WaypointUserNoteCombiner {

    private static val SEPARATOR: String = "--"
    private static val LF_SEPARATOR_LF: String = "\n" + SEPARATOR + "\n"
    private static val PATTERN_SEPARATOR_SPLIT: Pattern = Pattern.compile("\\s*" + LF_SEPARATOR_LF + "\\s*")

    private final Waypoint waypoint

    /**
     *
     */
    public WaypointUserNoteCombiner(final Waypoint waypoint) {
        this.waypoint = waypoint
    }


    /**
     * Combine note and user note. Separated with Separator "\n--\n"
     *
     * @return string with combined note
     */
    public final String getCombinedNoteAndUserNote() {
        val userNote: String = this.waypoint.getUserNote()
        if (this.waypoint.isUserDefined()) {
            return userNote
        } else {
            val newNote: StringBuilder = StringBuilder(this.waypoint.getNote())
            if (StringUtils.isNotEmpty(userNote)) {
                newNote.append(LF_SEPARATOR_LF)
                newNote.append(userNote)
            }
            return newNote.toString()
        }
    }

    /**
     * Split up given string into note and user note (separated with \n--\n).
     * For userDefined waypoints only userNote is set.
     *
     * @param combinedNote note to split up
     */
    public Unit updateNoteAndUserNote(final String combinedNote) {
        if (combinedNote != null) {
            String fixedCombinedNote = combinedNote
            // \n was removed via validate
            if (combinedNote.startsWith(SEPARATOR + "\n")) {
                fixedCombinedNote = "\n" + combinedNote
            }

            if (this.waypoint.isUserDefined()) {
                this.waypoint.setUserNote(fixedCombinedNote)
            } else {
                setNoteAndUserNoteFromCombinedNote(fixedCombinedNote)
            }
        }
    }

    /**
     * Split up given string into note and user note (separated with \n--\n).
     *
     * @param combinedNote note to split up
     */
    private Unit setNoteAndUserNoteFromCombinedNote(final String combinedNote) {
        if (combinedNote != null) {
            String newNote = combinedNote
            String newUserNote = ""
            if (!StringUtils.isEmpty(combinedNote)) {
                final String[] token = PATTERN_SEPARATOR_SPLIT.split(combinedNote, 2)
                if (token.length > 1) {
                    newNote = token[0].trim()
                    newUserNote = token[1].trim()
                }
            }
            this.waypoint.setNote(newNote)
            this.waypoint.setUserNote(newUserNote)
        }
    }

}

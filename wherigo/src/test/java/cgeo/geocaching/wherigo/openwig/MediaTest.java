package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link Media}'s id assignment and its "Resources"/"AltText" Lua setters, which are
 * pure bookkeeping with no dependency on {@link Engine}. serialize()/deserialize() are excluded:
 * they delegate to {@code Engine.instance.savegame}, which isn't available in a unit test.
 */
public class MediaTest {

    @After
    public void resetMediaIdCounter() {
        Media.reset();
    }

    private static LuaTable resourceEntry(final String type) {
        final LuaTable entry = new LuaTableImpl();
        entry.rawset("Type", type);
        return entry;
    }

    /** Media#setItem looks up "Resources" entries with Double keys (new Double(i)); LuaTable's
     * rawset(Object, Object) would otherwise autobox a plain int literal to Integer, which
     * wouldn't match, silently leaving the list looking empty (len() == 0). */
    private static void putResource(final LuaTable resources, final int index, final LuaTable entry) {
        resources.rawset((double) index, entry);
    }

    @Test
    public void idsAreAssignedSequentially() {
        Media.reset();
        final Media first = new Media();
        final Media second = new Media();

        assertThat(first.id).isEqualTo(1);
        assertThat(second.id).isEqualTo(2);
    }

    @Test
    public void resetRestartsIdsFromOne() {
        new Media();
        new Media();

        Media.reset();
        final Media afterReset = new Media();

        assertThat(afterReset.id).isEqualTo(1);
    }

    @Test
    public void altTextIsSetThroughRawset() {
        final Media media = new Media();

        media.rawset("AltText", "a torch");

        assertThat(media.altText).isEqualTo("a torch");
    }

    @Test
    public void resourcesPicksLastNonFdlTypeLowercased() {
        final Media media = new Media();
        final LuaTable resources = new LuaTableImpl();
        putResource(resources, 1, resourceEntry("PNG"));
        putResource(resources, 2, resourceEntry("fdl"));
        putResource(resources, 3, resourceEntry("WAV"));

        media.rawset("Resources", resources);

        assertThat(media.type).isEqualTo("wav");
    }

    @Test
    public void resourcesIgnoresTrailingFdlEntry() {
        final Media media = new Media();
        final LuaTable resources = new LuaTableImpl();
        putResource(resources, 1, resourceEntry("JPG"));
        putResource(resources, 2, resourceEntry("fdl"));

        media.rawset("Resources", resources);

        assertThat(media.type).isEqualTo("jpg");
    }

    @Test
    public void jarFilenameCombinesIdAndType() {
        final Media media = new Media();
        final LuaTable resources = new LuaTableImpl();
        putResource(resources, 1, resourceEntry("wav"));

        media.rawset("Resources", resources);

        assertThat(media.jarFilename()).isEqualTo(media.id + ".wav");
    }

    @Test
    public void jarFilenameHasTrailingDotWhenTypeIsUnknown() {
        final Media media = new Media();

        assertThat(media.jarFilename()).isEqualTo(media.id + ".");
    }

    @Test
    public void toStringCombinesIdAndName() {
        final Media media = new Media();

        media.rawset("Name", "torch");

        assertThat(media.toString()).isEqualTo(media.id + ":torch");
    }
}

package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileNameCreator;

import android.net.Uri;

import org.apache.commons.lang3.tuple.ImmutablePair;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PublicLocalStorageTest extends CGeoTestCase {

    //a first small test to see how CI handles it
    public static void testSimpleCreateDelete() {
        cleanup();

        final FolderLocation testFolder = getTestFolder();
        cleanup();

        final Uri uri = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("cgeo-test.txt"));
        final FolderLocation subfolder = FolderLocation.fromFolderLocation(testFolder, "eins");
        final FolderLocation subsubfolder = FolderLocation.fromFolderLocation(subfolder, "zwei");
        final Uri uri2 = PublicLocalStorage.get().create(subsubfolder, FileNameCreator.forName("cgeo-test-sub.txt"));

        assertThat(PublicLocalStorage.get().delete(uri)).isTrue();
        assertThat(PublicLocalStorage.get().delete(uri2)).isTrue();

        //cleanup
        cleanup();
    }

    public static void testCopyAll() {
        cleanup();
        final FolderLocation sourceFolder = FolderLocation.fromFolderLocation(getTestFolder(), "source");
        final FolderLocation targetFolder = FolderLocation.fromFolderLocation(getTestFolder(), "target");

        //create something to copy in source Folder
        final FolderLocation fOne = FolderLocation.fromFolderLocation(sourceFolder, "eins");
        final FolderLocation fTwo = FolderLocation.fromFolderLocation(sourceFolder, "zwei");
        final FolderLocation fThree = FolderLocation.fromFolderLocation(sourceFolder, "drei");
        final FolderLocation fTwoSub = FolderLocation.fromFolderLocation(fTwo, "sub");

        final FolderLocation[] sourceFolders = new FolderLocation[] {fOne, fTwo, fThree, fTwoSub};

        for (int i = 0; i < 20 ; i++) {
            PublicLocalStorage.get().create(sourceFolders[i % sourceFolders.length], FileNameCreator.forName("testfile" + i + ".txt"));
        }
        assertThat(PublicLocalStorage.get().getFileCounts(sourceFolder)).isEqualTo(new ImmutablePair<>(20, 4));
        assertThat(PublicLocalStorage.get().getFileCounts(targetFolder)).isEqualTo(new ImmutablePair<>(0, 0));

        //copy
        PublicLocalStorage.get().copyAll(sourceFolder, targetFolder, false);
        //after copy, source should be unchanged
        assertThat(PublicLocalStorage.get().getFileCounts(sourceFolder)).isEqualTo(new ImmutablePair<>(20, 4));
        //after copy, target should be identical to source
        assertThat(PublicLocalStorage.get().getFileCounts(targetFolder)).isEqualTo(new ImmutablePair<>(20, 4));


        //move
        PublicLocalStorage.get().copyAll(sourceFolder, targetFolder, true);

        //after move, source should be empty
        assertThat(PublicLocalStorage.get().getFileCounts(sourceFolder)).isEqualTo(new ImmutablePair<>(0, 0));
        //we expect now the DOUBLE amount of files in target, since PublicLocalStorage never overwrites existing files in create, always created new ones!
        assertThat(PublicLocalStorage.get().getFileCounts(targetFolder)).isEqualTo(new ImmutablePair<>(40, 4));

        cleanup();

    }

    private static FolderLocation getTestFolder() {
        return FolderLocation.fromFolderLocation(FolderLocation.CGEO_PRIVATE_FILES, "unittest");
    }

    private static void cleanup() {
        final FolderLocation folder = getTestFolder();
        PublicLocalStorage.get().deleteAll(folder);
    }
}

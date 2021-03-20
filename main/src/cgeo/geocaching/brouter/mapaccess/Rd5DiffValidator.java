/**
 * Manage rd5 diff-file creation
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.File;

final public class Rd5DiffValidator {
    public static void main(String[] args) throws Exception {
        validateDiffs(new File(args[0]), new File(args[1]));
    }

    /**
     * Validate diffs for all DF5 files
     */
    public static void validateDiffs(File oldDir, File newDir) throws Exception {
        File oldDiffDir = new File(oldDir, "diff");
        File newDiffDir = new File(newDir, "diff");

        File[] filesNew = newDir.listFiles();

        for (File fn : filesNew) {
            String name = fn.getName();
            if (!name.endsWith(".rd5")) {
                continue;
            }
            if (fn.length() < 1024 * 1024) {
                continue; // expecting no diff for small files
            }
            String basename = name.substring(0, name.length() - 4);
            File fo = new File(oldDir, name);
            if (!fo.isFile()) {
                continue;
            }

            // calculate MD5 of old file
            String md5 = Rd5DiffManager.getMD5(fo);

            String md5New = Rd5DiffManager.getMD5(fn);

            System.out.println("name=" + name + " md5=" + md5);

            File specificNewDiffs = new File(newDiffDir, basename);

            String diffFileName = md5 + ".df5";
            File diffFile = new File(specificNewDiffs, diffFileName);

            File fcmp = new File(oldDir, name + "_tmp");

            // merge old file and diff
            Rd5DiffTool.recoverFromDelta(fo, diffFile, fcmp, new Rd5DiffTool());
            String md5Cmp = Rd5DiffManager.getMD5(fcmp);

            if (!md5Cmp.equals(md5New)) {
                throw new RuntimeException("**************** md5 mismatch!! *****************");
            }
        }
    }

}

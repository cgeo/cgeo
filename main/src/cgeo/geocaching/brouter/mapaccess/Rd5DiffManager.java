/**
 * Manage rd5 diff-file creation
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

final public class Rd5DiffManager {
    public static void main(String[] args) throws Exception {
        calcDiffs(new File(args[0]), new File(args[1]));
    }

    /**
     * Compute diffs for all RD5 files
     */
    public static void calcDiffs(File oldDir, File newDir) throws Exception {
        File oldDiffDir = new File(oldDir, "diff");
        File newDiffDir = new File(newDir, "diff");

        File[] filesNew = newDir.listFiles();

        for (File fn : filesNew) {
            String name = fn.getName();
            if (!name.endsWith(".rd5")) {
                continue;
            }
            if (fn.length() < 1024 * 1024) {
                continue; // exclude very small files from diffing
            }
            String basename = name.substring(0, name.length() - 4);
            File fo = new File(oldDir, name);
            if (!fo.isFile()) {
                continue;
            }

            // calculate MD5 of old file
            String md5 = getMD5(fo);

            String md5New = getMD5(fn);

            System.out.println("name=" + name + " md5=" + md5);

            File specificNewDiffs = new File(newDiffDir, basename);
            specificNewDiffs.mkdirs();

            String diffFileName = md5 + ".df5";
            File diffFile = new File(specificNewDiffs, diffFileName);

            String dummyDiffFileName = md5New + ".df5";
            File dummyDiffFile = new File(specificNewDiffs, dummyDiffFileName);
            dummyDiffFile.createNewFile();

            // calc the new diff
            Rd5DiffTool.diff2files(fo, fn, diffFile);

            // ... and add that to old diff files
            File specificOldDiffs = new File(oldDiffDir, basename);
            if (specificOldDiffs.isDirectory()) {
                File[] oldDiffs = specificOldDiffs.listFiles();
                for (File od : oldDiffs) {
                    if (!od.getName().endsWith(".df5")) {
                        continue;
                    }
                    if (System.currentTimeMillis() - od.lastModified() > 9 * 86400000L) {
                        continue; // limit diff history to 9 days
                    }

                    File updatedDiff = new File(specificNewDiffs, od.getName());
                    if (!updatedDiff.exists()) {
                        Rd5DiffTool.addDeltas(od, diffFile, updatedDiff);
                        updatedDiff.setLastModified(od.lastModified());
                    }
                }
            }
        }
    }

    public static String getMD5(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        DigestInputStream dis = new DigestInputStream(bis, md);
        byte[] buf = new byte[8192];
        for (; ; ) {
            int len = dis.read(buf);
            if (len <= 0) {
                break;
            }
        }
        dis.close();
        byte[] bytes = md.digest();

        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xff;
            sb.append(hexChar(v >>> 4)).append(hexChar(v & 0xf));
        }
        return sb.toString();
    }

    private static char hexChar(int v) {
        return (char) (v > 9 ? 'a' + (v - 10) : '0' + v);
    }
}

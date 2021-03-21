/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Locale;

import cgeo.geocaching.utils.LocalizationUtils;
import menion.android.whereyougo.MainApplication;
import cgeo.geocaching.R;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class FileSystem {

    private static final String TAG = "FileSystem";
    private static final String CARD_ROOT = "{CARD_ROOT}";
    private static final String[] EXTERNAL_DIRECTORIES = new String[]{CARD_ROOT + "external_sd",
            CARD_ROOT + "_externalsd", CARD_ROOT + "sd", CARD_ROOT + "emmc", // CM7 + SGS
            CARD_ROOT + "ext_sd", "/Removable/MicroSD", // Asus Transformer
            "/mnt/emms", // CM7 + SGS2
            "/mnt/external1" // Xoom
    };
    public static String ROOT = null;
    public static String CACHE = "cache/";
    public static String CACHE_AUDIO = CACHE + "audio/";

    public static boolean createRoot(String appDirName) {
        if (ROOT != null && new File(ROOT).exists())
            return true;

        try {
            // Android native
            // Logger.i(TAG, "createRoot(), " + android.os.Environment.getExternalStorageState() + ", " +
            // Environment.getExternalStorageDirectory());

            String cardRoot = getExternalStorageDir();
            if (cardRoot == null)
                return false;

            // test if exist external mounted sdcard
            String externalCardRoot = null;
            for (String cardTestDir : EXTERNAL_DIRECTORIES) {
                if (cardTestDir.contains(CARD_ROOT))
                    cardTestDir = cardTestDir.replace(CARD_ROOT, cardRoot);

                if (new File(cardTestDir).exists()) {
                    externalCardRoot = cardTestDir + "/";
                    break;
                }
            }

            File appOnCard = new File(cardRoot + appDirName);
            if (externalCardRoot == null) {
                return setRootDirectory(cardRoot, appOnCard.getAbsolutePath());
            } else {
                File appOnExternalCard = new File(externalCardRoot + appDirName);
                // test whether root already exist
                if (appOnExternalCard.exists()) {
                    return setRootDirectory(externalCardRoot, appOnExternalCard.getAbsolutePath());
                } else if (appOnCard.exists()) {
                    return setRootDirectory(cardRoot, appOnCard.getAbsolutePath());
                } else {
                    return setRootDirectory(externalCardRoot, appOnExternalCard.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            Logger.e(TAG, "createRoot(), ex: " + ex.toString());
        }
        return false;
    }

    public static String getExternalStorageDir() {
        String cardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (cardRoot == null)
            return null;

        if (!cardRoot.endsWith("/"))
            cardRoot += "/";
        return cardRoot;
    }

    public static File[] getFiles(String folder, final String filter) {
        FileFilter fileFilter = pathname -> pathname.getName().toLowerCase(Locale.getDefault()).endsWith(filter);
        return getFiles2(folder, fileFilter);
    }

    public static File[] getFiles2(String folder, FileFilter filter) {
        try {
            File file = new File(folder);
            if (!file.exists()) {
                return new File[0];
            }

            return file.listFiles(filter);
        } catch (Exception e) {
            Logger.e(TAG, "getFiles2(), folder: " + folder);
            return new File[0];
        }
    }

    public static String getRoot() {
        if (ROOT == null) {
            createRoot(LocalizationUtils.getString(R.string.app_name));
        }
        return ROOT;
    }

    /**
     * Checks folders in given filePath and creates them if necessary
     *
     * @param fileName file name
     */
    public static void checkFolders(String fileName) {
        try {
            (new File(fileName)).getParentFile().mkdirs();
        } catch (Exception e) {
            Logger.e(TAG, "checkFolders(" + fileName + "), ex: " + e.toString());
        }
    }

    /**
     * Writes binary data into file
     *
     * @param fileName file name (absolute)
     * @param data     binary data
     */
    public static synchronized void saveBytes(String fileName, byte[] data) {
        try {
            if (data.length == 0)
                return;
            new FileSystemDataWritter(fileName, data, -1);
        } catch (Exception e) {
            Logger.e(TAG, "saveBytes(" + fileName + "), e: " + e.toString());
        }
    }

    public static boolean setRootDirectory(String appRoot) {
        return setRootDirectory(null, appRoot);
    }

    private static boolean setRootDirectory(String cardRoot, String appRoot) {
        if (appRoot == null || appRoot.equals(""))
            return false;
        if (!appRoot.endsWith("/"))
            appRoot += "/";

        // create root directory
        File rootAppDir = new File(appRoot);
        if (!rootAppDir.exists()) {
            if (!rootAppDir.mkdir())
                return false;
        }
        FileSystem.ROOT = appRoot;
        return true;
    }

    public static File findFile(String prefix, String extension) {
        File[] files = FileSystem.getFiles(FileSystem.ROOT, extension);
        if (files == null) return null;
        for (File file : files) {
            if (file.getName().startsWith(prefix)) {
                return file;
            }
        }
        return null;
    }

    public static File findFile(String prefix) {
        return findFile(prefix, "gwc");
    }

    public static boolean backupFile(File file) {
        try {
            if (file.length() > 0) {
                File backupFile = new File(file.getAbsolutePath() + ".bak");
                FileSystem.copyFile(file, backupFile);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void copyFile(File source, File dest) throws IOException {
        if (source.equals(dest)) {
            return;
        }
        if (!dest.exists()) {
            dest.createNewFile();
        }
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            if (sourceChannel != null) {
                try {
                    sourceChannel.close();
                } catch (IOException e) {
                }
            }
            if (destChannel != null) {
                try {
                    destChannel.close();
                } catch (IOException e) {
                }
            }
        }
        dest.setLastModified(source.lastModified());
    }
}

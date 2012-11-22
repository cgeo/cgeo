package cgeo.geocaching.network;

import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.utils.ImageHelper;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.Html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

public class HtmlImage implements Html.ImageGetter {

    private static final String[] BLOCKED = new String[] {
            "gccounter.de",
            "gccounter.com",
            "cachercounter/?",
            "gccounter/imgcount.php",
            "flagcounter.com",
            "compteur-blog.net",
            "counter.digits.com",
            "andyhoppe",
            "besucherzaehler-homepage.de",
            "hitwebcounter.com",
            "kostenloser-counter.eu",
            "trendcounter.com"
    };

    final private String geocode;
    /**
     * on error: return large error image, if <code>true</code>, otherwise empty 1x1 image
     */
    final private boolean returnErrorImage;
    final private int listId;
    final private boolean onlySave;
    final private BitmapFactory.Options bfOptions;
    final private int maxWidth;
    final private int maxHeight;
    final private Resources resources;

    public HtmlImage(final String geocode, final boolean returnErrorImage, final int listId, final boolean onlySave) {
        this.geocode = geocode;
        this.returnErrorImage = returnErrorImage;
        this.listId = listId;
        this.onlySave = onlySave;

        bfOptions = new BitmapFactory.Options();
        bfOptions.inTempStorage = new byte[16 * 1024];

        Point displaySize = Compatibility.getDisplaySize();
        this.maxWidth = displaySize.x - 25;
        this.maxHeight = displaySize.y - 25;
        this.resources = cgeoapplication.getInstance().getResources();
    }

    @Override
    public BitmapDrawable getDrawable(final String url) {
        // Reject empty and counter images URL
        if (StringUtils.isBlank(url) || isCounter(url)) {
            return new BitmapDrawable(resources, getTransparent1x1Image());
        }

        final boolean shared = url.contains("/images/icons/icon_");
        final String pseudoGeocode = shared ? "shared" : geocode;

        Bitmap imagePre = loadImageFromStorage(url, pseudoGeocode, shared);

        // Download image and save it to the cache
        if (imagePre == null) {
            final String absoluteURL = makeAbsoluteURL(url);

            if (absoluteURL != null) {
                try {
                    final File file = LocalStorage.getStorageFile(pseudoGeocode, url, true, true);
                    final HttpResponse httpResponse = Network.getRequest(absoluteURL, null, file);
                    if (httpResponse != null) {
                        final int statusCode = httpResponse.getStatusLine().getStatusCode();
                        if (statusCode == 200) {
                            LocalStorage.saveEntityToFile(httpResponse, file);
                        } else if (statusCode == 304) {
                            if (!file.setLastModified(System.currentTimeMillis())) {
                                makeFreshCopy(file);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("HtmlImage.getDrawable (downloading from web)", e);
                }
            }
        }

        if (onlySave) {
            return null;
        }

        // now load the newly downloaded image
        if (imagePre == null) {
            imagePre = loadImageFromStorage(url, pseudoGeocode, shared);
        }

        // get image and return
        if (imagePre == null) {
            Log.d("HtmlImage.getDrawable: Failed to obtain image");

            if (returnErrorImage) {
                imagePre = BitmapFactory.decodeResource(resources, R.drawable.image_not_loaded);
            } else {
                imagePre = getTransparent1x1Image();
            }
        }

        return imagePre != null ? ImageHelper.scaleBitmapToFitDisplay(imagePre) : null;
    }

    /**
     * Make a fresh copy of the file to reset its timestamp. On some storage, it is impossible
     * to modify the modified time after the fact, in which case a brand new file must be
     * created if we want to be able to use the time as validity hint.
     *
     * See Android issue 1699.
     *
     * @param file the file to refresh
     */
    private static void makeFreshCopy(final File file) {
        final File tempFile = new File(file.getParentFile(), file.getName() + "-temp");
        file.renameTo(tempFile);
        LocalStorage.copy(tempFile, file);
        tempFile.delete();
    }

    private Bitmap getTransparent1x1Image() {
        return BitmapFactory.decodeResource(resources, R.drawable.image_no_placement);
    }

    private Bitmap loadImageFromStorage(final String url, final String pseudoGeocode, final boolean forceKeep) {
        try {
            final File file = LocalStorage.getStorageFile(pseudoGeocode, url, true, false);
            final Bitmap image = loadCachedImage(file, forceKeep);
            if (image != null) {
                return image;
            }
            final File fileSec = LocalStorage.getStorageSecFile(pseudoGeocode, url, true);
            return loadCachedImage(fileSec, forceKeep);
        } catch (Exception e) {
            Log.w("HtmlImage.getDrawable (reading cache): " + e.toString());
        }
        return null;
    }

    private String makeAbsoluteURL(final String url) {
        // Check if uri is absolute or not, if not attach the connector hostname
        // FIXME: that should also include the scheme
        if (Uri.parse(url).isAbsolute()) {
            return url;
        }

        final String host = ConnectorFactory.getConnector(geocode).getHost();
        if (StringUtils.isNotEmpty(host)) {
            final StringBuilder builder = new StringBuilder("http://");
            builder.append(host);
            if (!StringUtils.startsWith(url, "/")) {
                // FIXME: explain why the result URL would be valid if the path does not start with
                // a '/', or signal an error.
                builder.append('/');
            }
            builder.append(url);
            return builder.toString();
        }

        return null;
    }

    private Bitmap loadCachedImage(final File file, final boolean forceKeep) {
        if (file.exists()) {
            if (listId >= StoredList.STANDARD_LIST_ID || file.lastModified() > (new Date().getTime() - (24 * 60 * 60 * 1000)) || forceKeep) {
                setSampleSize(file);
                return BitmapFactory.decodeFile(file.getPath(), bfOptions);
            }
        }
        return null;
    }

    private void setSampleSize(final File file) {
        //Decode image size only
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            BitmapFactory.decodeStream(fis, null, options);
        } catch (FileNotFoundException e) {
            Log.e("HtmlImage.setSampleSize", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        int scale = 1;
        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            scale = Math.max(options.outHeight / maxHeight, options.outWidth / maxWidth);
        }
        bfOptions.inSampleSize = scale;
    }

    private static boolean isCounter(final String url) {
        for (String entry : BLOCKED) {
            if (StringUtils.containsIgnoreCase(url, entry)) {
                return true;
            }
        }
        return false;
    }
}

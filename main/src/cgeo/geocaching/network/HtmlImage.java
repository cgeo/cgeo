package cgeo.geocaching.network;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.Html;
import android.view.Display;
import android.view.WindowManager;

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
            "andyhoppe"
    };
    final private Context context;
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

    public HtmlImage(final Context contextIn, final String geocode, final boolean returnErrorImage, final int listId, final boolean onlySave) {
        this.context = contextIn;
        this.geocode = geocode;
        this.returnErrorImage = returnErrorImage;
        this.listId = listId;
        this.onlySave = onlySave;

        bfOptions = new BitmapFactory.Options();
        bfOptions.inTempStorage = new byte[16 * 1024];

        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        this.maxWidth = display.getWidth() - 25;
        this.maxHeight = display.getHeight() - 25;
    }

    @Override
    public BitmapDrawable getDrawable(final String url) {
        // Reject empty and counter images URL
        if (StringUtils.isBlank(url) || isCounter(url)) {
            return new BitmapDrawable(getTransparent1x1Image());
        }

        Bitmap imagePre = onlySave ? null : loadImageFromStorage(url);

        // Download image and save it to the cache
        if (imagePre == null || onlySave) {
            final String absoluteURL = makeAbsoluteURL(url);

            if (absoluteURL != null) {
                try {
                    final File file = LocalStorage.getStorageFile(geocode, url, true, true);
                    final HttpResponse httpResponse = Network.request(absoluteURL, null, false, file);
                    if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
                        LocalStorage.saveEntityToFile(httpResponse, file);
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "HtmlImage.getDrawable (downloading from web)", e);
                }
            }
        }

        if (onlySave) {
            return null;
        }

        // now load the newly downloaded image
        if (imagePre == null) {
            imagePre = loadImageFromStorage(url);
        }

        // get image and return
        if (imagePre == null) {
            Log.d(Settings.tag, "HtmlImage.getDrawable: Failed to obtain image");

            if (returnErrorImage) {
                imagePre = BitmapFactory.decodeResource(context.getResources(), R.drawable.image_not_loaded);
            } else {
                imagePre = getTransparent1x1Image();
            }
        }

        final int imgWidth = imagePre.getWidth();
        final int imgHeight = imagePre.getHeight();

        int width;
        int height;

        if (imgWidth > maxWidth || imgHeight > maxHeight) {
            final double ratio = Math.min((double) maxHeight / (double) imgHeight, (double) maxWidth / (double) imgWidth);
            width = (int) Math.ceil(imgWidth * ratio);
            height = (int) Math.ceil(imgHeight * ratio);

            try {
                imagePre = Bitmap.createScaledBitmap(imagePre, width, height, true);
            } catch (Exception e) {
                Log.d(Settings.tag, "HtmlImage.getDrawable: Failed to scale image");
                return null;
            }
        } else {
            width = imgWidth;
            height = imgHeight;
        }

        final BitmapDrawable image = new BitmapDrawable(imagePre);
        image.setBounds(new Rect(0, 0, width, height));

        return image;
    }

    private Bitmap getTransparent1x1Image() {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.image_no_placement);
    }

    private Bitmap loadImageFromStorage(final String url) {
        try {
            final File file = LocalStorage.getStorageFile(geocode, url, true, false);
            final Bitmap image = loadCachedImage(file);
            if (image != null) {
                return image;
            }
            final File fileSec = LocalStorage.getStorageSecFile(geocode, url, true);
            return loadCachedImage(fileSec);
        } catch (Exception e) {
            Log.w(Settings.tag, "HtmlImage.getDrawable (reading cache): " + e.toString());
        }
        return null;
    }

    private final String makeAbsoluteURL(final String url) {
        try {
            // Check if uri is absolute or not, if not attach the connector hostname
            // FIXME: that should also include the scheme
            if (Uri.parse(url).isAbsolute()) {
                return url;
            } else {
                final String host = ConnectorFactory.getConnector(geocode).getHost();
                if (StringUtils.isNotEmpty(host)) {
                    StringBuilder builder = new StringBuilder("http://");
                    builder.append(host);
                    if (!StringUtils.startsWith(url, "/")) {
                        builder.append('/');
                    }
                    builder.append(url);
                    return builder.toString();
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "HtmlImage.makeAbsoluteURL (parse URL)", e);
        }
        return null;
    }

    private Bitmap loadCachedImage(final File file) {
        if (file.exists()) {
            if (listId >= StoredList.STANDARD_LIST_ID || file.lastModified() > (new Date().getTime() - (24 * 60 * 60 * 1000))) {
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
            Log.e(Settings.tag, "HtmlImage.setSampleSize", e);
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

package cgeo.geocaching.network;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.files.LocalStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BufferedHttpEntity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.util.Date;

public class HtmlImage implements Html.ImageGetter {

    private static final String[] BLOCKED = new String[] {
            "gccounter.de",
            "gccounter.com",
            "cachercounter/?",
            "gccounter/imgcount.php",
            "flagcounter.com",
            "compteur-blog.net",
            "counter.digits.com"
    };
    final private Context context;
    final private String geocode;
    /**
     * on error: return large error image, if <code>true</code>, otherwise empty 1x1 image
     */
    final private boolean returnErrorImage;
    final private int reason;
    final private boolean onlySave;
    final private boolean save;
    final private BitmapFactory.Options bfOptions;
    final private int maxWidth;
    final private int maxHeight;

    public HtmlImage(final Context context, final String geocode, final boolean returnErrorImage, final int reason, final boolean onlySave) {
        this(context, geocode, returnErrorImage, reason, onlySave, true);
    }

    public HtmlImage(final Context contextIn, final String geocode, final boolean returnErrorImage, final int reason, final boolean onlySave, final boolean save) {
        this.context = contextIn;
        this.geocode = geocode;
        this.returnErrorImage = returnErrorImage;
        this.reason = reason;
        this.onlySave = onlySave;
        this.save = save;

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
            return null;
        }

        Bitmap imagePre = null;

        // Load image from cache
        if (!onlySave) {
            imagePre = loadImageFromStorage(url);
        }

        // Download image and save it to the cache
        if (imagePre == null || onlySave) {
            final String absoluteURL = makeAbsoluteURL(url);
            BufferedHttpEntity bufferedEntity = null;

            if (absoluteURL != null) {
                try {
                    final HttpResponse httpResponse = cgBase.request(absoluteURL, null, false);
                    if (httpResponse != null) {
                        bufferedEntity = new BufferedHttpEntity(httpResponse.getEntity());
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "HtmlImage.getDrawable (downloading from web)", e);
                }
            }

            if (save) {
                final File file = LocalStorage.getStorageFile(geocode, url, true);
                LocalStorage.saveEntityToFile(bufferedEntity, file);
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
                imagePre = BitmapFactory.decodeResource(context.getResources(), R.drawable.image_no_placement);
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

    private Bitmap loadImageFromStorage(final String url) {
        try {
            final File file = LocalStorage.getStorageFile(geocode, url, true);
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
                    return "http://" + host + url;
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "HtmlImage.makeAbsoluteURL (parse URL)", e);
        }
        return null;
    }

    private Bitmap loadCachedImage(final File file) {
        if (file.exists()) {
            if (reason > 0 || file.lastModified() > (new Date().getTime() - (24 * 60 * 60 * 1000))) {
                setSampleSize(file.length());
                return BitmapFactory.decodeFile(file.getPath(), bfOptions);
            }
        }
        return null;
    }

    private void setSampleSize(final long imageSize) {
        // large images will be downscaled on input to save memory
        if (imageSize > (6 * 1024 * 1024)) {
            bfOptions.inSampleSize = 48;
        } else if (imageSize > (4 * 1024 * 1024)) {
            bfOptions.inSampleSize = 16;
        } else if (imageSize > (2 * 1024 * 1024)) {
            bfOptions.inSampleSize = 10;
        } else if (imageSize > (1 * 1024 * 1024)) {
            bfOptions.inSampleSize = 6;
        } else if (imageSize > (0.5 * 1024 * 1024)) {
            bfOptions.inSampleSize = 2;
        }
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

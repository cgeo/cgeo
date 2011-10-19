package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.files.LocalStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BufferedHttpEntity;

import android.app.Activity;
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
import java.io.InputStream;
import java.util.Date;

public class cgHtmlImg implements Html.ImageGetter {

    private static final String[] BLOCKED = new String[] {
            "gccounter.de",
            "gccounter.com",
            "cachercounter/?",
            "gccounter/imgcount.php",
            "flagcounter.com",
            "compteur-blog.net"
    };
    final private Context context;
    final private String geocode;
    final private boolean placement;
    final private int reason;
    final private boolean onlySave;
    final private boolean save;
    final private BitmapFactory.Options bfOptions;
    final private Display display;
    final private int maxWidth;
    final private int maxHeight;

    public cgHtmlImg(Activity activityIn, String geocodeIn, boolean placementIn, int reasonIn, boolean onlySaveIn) {
        this(activityIn, geocodeIn, placementIn, reasonIn, onlySaveIn, true);
    }

    public cgHtmlImg(Context contextIn, String geocodeIn, boolean placementIn, int reasonIn, boolean onlySaveIn, boolean saveIn) {
        context = contextIn;
        geocode = geocodeIn;
        placement = placementIn;
        reason = reasonIn;
        onlySave = onlySaveIn;
        save = saveIn;

        bfOptions = new BitmapFactory.Options();
        bfOptions.inTempStorage = new byte[16 * 1024];

        display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        maxWidth = display.getWidth() - 25;
        maxHeight = display.getHeight() - 25;
    }

    @Override
    public BitmapDrawable getDrawable(final String url) {
        // Reject empty and counter images URL
        if (StringUtils.isBlank(url) || isCounter(url)) {
            return null;
        }

        final File file = LocalStorage.getStorageFile(geocode, url, true);
        final File fileSec = LocalStorage.getStorageSecFile(geocode, url, true);

        Bitmap imagePre = null;

        // Load image from cache
        if (!onlySave) {
            try {
                imagePre = loadCachedImage(file);
                if (imagePre == null) {
                    imagePre = loadCachedImage(fileSec);
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "cgHtmlImg.getDrawable (reading cache): " + e.toString());
            }
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
                        setSampleSize(bufferedEntity.getContentLength());
                        final InputStream is = bufferedEntity.getContent();
                        try {
                            imagePre = BitmapFactory.decodeStream(is, null, bfOptions);
                        } finally {
                            is.close();
                        }
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgHtmlImg.getDrawable (downloading from web)", e);
                }
            }

            if (save) {
                LocalStorage.saveEntityToFile(bufferedEntity, file);
            }
        }

        if (onlySave) {
            return null;
        }

        // get image and return
        if (imagePre == null) {
            Log.d(Settings.tag, "cgHtmlImg.getDrawable: Failed to obtain image");

            if (placement) {
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
            double ratio;
            if ((maxWidth / imgWidth) > (maxHeight / imgHeight)) {
                ratio = (double) maxHeight / (double) imgHeight;
            } else {
                ratio = (double) maxWidth / (double) imgWidth;
            }

            width = (int) Math.ceil(imgWidth * ratio);
            height = (int) Math.ceil(imgHeight * ratio);

            try {
                imagePre = Bitmap.createScaledBitmap(imagePre, width, height, true);
            } catch (Exception e) {
                Log.d(Settings.tag, "cgHtmlImg.getDrawable: Failed to scale image");
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

    private final String makeAbsoluteURL(final String url) {
        try {
            // Check if uri is absolute or not, if not attach the connector hostname
            // FIXME: that should also include the scheme
            if (Uri.parse(url).isAbsolute()) {
                return url;
            } else {
                return "http://" + ConnectorFactory.getConnector(geocode).getHost() + url;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgHtmlImg.makeAbsoluteURL (parse URL)", e);
            return null;
        }
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

package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class cgHtmlImg implements Html.ImageGetter {

    private static final String[] BLOCKED = new String[] {
            "gccounter.de",
            "gccounter.com",
            "cachercounter/?",
            "gccounter/imgcount.php"
    };
    final private Activity activity;
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

    public cgHtmlImg(Activity activityIn, String geocodeIn, boolean placementIn, int reasonIn, boolean onlySaveIn, boolean saveIn) {
        activity = activityIn;
        geocode = geocodeIn;
        placement = placementIn;
        reason = reasonIn;
        onlySave = onlySaveIn;
        save = saveIn;

        bfOptions = new BitmapFactory.Options();
        bfOptions.inTempStorage = new byte[16 * 1024];

        display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        maxWidth = display.getWidth() - 25;
        maxHeight = display.getHeight() - 25;
    }

    @Override
    public BitmapDrawable getDrawable(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        if (isCounter(url)) {
            return null;
        }

        String urlExt = StringUtils.substringAfterLast(url, ".");
        if (urlExt.length() > 0) {
            urlExt = "." + urlExt;
        }
        if (urlExt.length() > 5) {
            urlExt = "";
        }

        String dirName;
        String fileName;
        String fileNameSec;

        if (StringUtils.isNotBlank(geocode)) {
            dirName = Settings.getStorage() + geocode + "/";
            fileName = Settings.getStorage() + geocode + "/" + CryptUtils.md5(url) + urlExt;
            fileNameSec = Settings.getStorageSec() + geocode + "/" + CryptUtils.md5(url) + urlExt;
        } else {
            dirName = Settings.getStorage() + "_others/";
            fileName = Settings.getStorage() + "_others/" + CryptUtils.md5(url) + urlExt;
            fileNameSec = Settings.getStorageSec() + "_others/" + CryptUtils.md5(url) + urlExt;
        }

        File dir = null;
        dir = new File(Settings.getStorage());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dir = null;

        Bitmap imagePre = null;
        // load image from cache
        if (!onlySave) {
            try {
                imagePre = loadCachedImage(fileName);
                if (null == imagePre) {
                    imagePre = loadCachedImage(fileNameSec);
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "cgHtmlImg.getDrawable (reading cache): " + e.toString());
            }
        }

        // download image and save it to the cache
        if ((null == imagePre && 0 == reason) || onlySave) {
            Uri uri = null;
            BufferedHttpEntity bufferedEntity = null;

            try {
                // check if uri is absolute or not, if not attach geocaching.com hostname and scheme
                uri = Uri.parse(url);

                if (!uri.isAbsolute()) {
                    final IConnector connector = ConnectorFactory.getConnector(geocode);
                    url = "http://" + connector.getHost() + url;
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgHtmlImg.getDrawable (parse URL): " + e.toString());
            }

            if (null != uri) {
                for (int i = 0; i < 2; i++) {
                    if (i > 0) {
                        Log.w(Settings.tag, "cgHtmlImg.getDrawable: Failed to download data, retrying. Attempt #" + (i + 1));
                    }

                    try {
                        final HttpGet getMethod = new HttpGet(url);
                        final HttpResponse httpResponse = cgBase.doRequest(getMethod);
                        if (null != httpResponse) {
                            final HttpEntity entity = httpResponse.getEntity();
                            bufferedEntity = new BufferedHttpEntity(entity);

                            setSampleSize(bufferedEntity.getContentLength());

                            final InputStream is = bufferedEntity.getContent();
                            try {
                                imagePre = BitmapFactory.decodeStream(is, null, bfOptions);
                            } finally {
                                is.close();
                            }
                        }

                        if (null != imagePre) {
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(Settings.tag, "cgHtmlImg.getDrawable (downloading from web)", e);
                    }
                }
            }

            if (save) {
                try {
                    // save to memory/SD cache
                    if (null != bufferedEntity) {
                        final InputStream is = bufferedEntity.getContent();
                        try {
                            final FileOutputStream fos = new FileOutputStream(fileName);
                            try {
                                final byte[] buffer = new byte[4096];
                                int l;
                                while ((l = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, l);
                                }
                                fos.flush();
                            } finally {
                                fos.close();
                            }
                        } finally {
                            is.close();
                        }
                    }
                } catch (IOException e) {
                    Log.e(Settings.tag, "cgHtmlImg.getDrawable (saving to cache)", e);
                }
            }
        }

        if (onlySave) {
            return null;
        }

        // get image and return
        if (null == imagePre) {
            Log.d(Settings.tag, "cgHtmlImg.getDrawable: Failed to obtain image");

            if (placement) {
                imagePre = BitmapFactory.decodeResource(activity.getResources(), R.drawable.image_not_loaded);
            } else {
                imagePre = BitmapFactory.decodeResource(activity.getResources(), R.drawable.image_no_placement);
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

    private Bitmap loadCachedImage(final String fileName) {
        final File file = new File(fileName);
        if (file.exists()) {
            if (reason > 0 || file.lastModified() > (new Date().getTime() - (24 * 60 * 60 * 1000))) {
                setSampleSize(file.length());
                return BitmapFactory.decodeFile(fileName, bfOptions);
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

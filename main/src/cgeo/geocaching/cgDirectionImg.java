package cgeo.geocaching;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class cgDirectionImg {

    public static void getDrawable(String geocode, String code) {
        String dirName;
        String fileName;

        if (StringUtils.isBlank(geocode) || StringUtils.isBlank(code)) {
            return;
        }

        if (StringUtils.isNotBlank(geocode)) {
            dirName = cgSettings.getStorage() + geocode + "/";
            fileName = cgSettings.getStorage() + geocode + "/direction.png";
        } else {
            return;
        }

        File dir = null;
        dir = new File(cgSettings.getStorage());
        if (dir.exists() == false) {
            dir.mkdirs();
        }
        dir = new File(dirName);
        if (dir.exists() == false) {
            dir.mkdirs();
        }
        dir = null;

        HttpClient client = null;
        HttpGet getMethod = null;
        HttpResponse httpResponse = null;
        HttpEntity entity = null;

        boolean ok = false;

        for (int i = 0; i < 3; i++) {
            if (i > 0)
                Log.w(cgSettings.tag, "cgDirectionImg.getDrawable: Failed to download data, retrying. Attempt #" + (i + 1));

            try {
                client = new DefaultHttpClient();
                getMethod = new HttpGet("http://www.geocaching.com/ImgGen/seek/CacheDir.ashx?k=" + code);
                httpResponse = client.execute(getMethod);
                entity = httpResponse.getEntity();
                final BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(entity);

                Log.i(cgSettings.tag, "[" + entity.getContentLength() + "B] Downloading direction image " + code);

                InputStream is = (InputStream) bufferedEntity.getContent();
                FileOutputStream fos = new FileOutputStream(fileName);

                try {
                    byte[] buffer = new byte[4096];
                    int l;
                    while ((l = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, l);
                    }
                    ok = true;
                    fos.flush();
                } catch (IOException e) {
                    Log.e(cgSettings.tag, "cgDirectionImg.getDrawable (saving to cache): " + e.toString());
                } finally {
                    is.close();
                    fos.close();
                }

                if (ok) {
                    break;
                }
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgDirectionImg.getDrawable (downloading from web): " + e.toString());
            }
        }
    }
}

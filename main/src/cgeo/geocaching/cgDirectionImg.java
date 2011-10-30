package cgeo.geocaching;

import cgeo.geocaching.files.LocalStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import java.io.File;

public class cgDirectionImg {

    public static void getDrawable(String geocode, String code) {
        if (StringUtils.isBlank(geocode) || StringUtils.isBlank(code)) {
            return;
        }

        final HttpResponse httpResponse =
                    cgBase.request("http://www.geocaching.com/ImgGen/seek/CacheDir.ashx", new Parameters("k", code), false);
        if (httpResponse != null) {
            LocalStorage.saveEntityToFile(httpResponse.getEntity(), getDirectionFile(geocode));
        }
    }

    public static File getDirectionFile(final String geocode) {
        return LocalStorage.getStorageFile(geocode, "direction.png", false);
    }

}
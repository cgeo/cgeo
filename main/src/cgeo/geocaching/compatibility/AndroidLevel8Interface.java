package cgeo.geocaching.compatibility;

import android.app.Activity;

import java.io.File;

public interface AndroidLevel8Interface {
    public int getRotation(final Activity activity);
    public void dataChanged(final String name);

    public int getRotationOffset(final Activity activity);

    public File getExternalPictureDir();
}
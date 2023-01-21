package cgeo.geocaching.files;

import android.content.Context;

public interface IFileSelectionView {

    Context getContext();

    String getCurrentFile();

    void setCurrentFile(String name);

    void close();

}

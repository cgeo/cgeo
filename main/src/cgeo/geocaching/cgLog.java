package cgeo.geocaching;

import java.util.List;

public class cgLog {
    public int id = 0;
    public int type = 4; // note
    public String author = "";
    public String log = "";
    public long date = 0;
    public int found = -1;
    public List<cgImage> logImages = null;
    public String cacheName = ""; // used for trackables
    public String cacheGuid = ""; // used for trackables
}

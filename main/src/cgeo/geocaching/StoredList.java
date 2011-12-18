package cgeo.geocaching;


public class StoredList {
    public static final int STANDARD_LIST_ID = 1;

    public final int id;
    public final String title;
    public final int count;

    public StoredList(int id, String title, int count) {
        this.id = id;
        this.title = title;
        this.count = count;
    }

    public String getTitleAndCount() {
        return title + " [" + count + "]";
    }
}

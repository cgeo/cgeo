package cgeo.geocaching;


public class StoredList {
    public static final int TEMPORARY_LIST_ID = 0;
    public static final int STANDARD_LIST_ID = 1;

    public final int id;
    public final String title;
    private final int count; // this value is only valid as long as the list is not changed by other database operations

    public StoredList(int id, String title, int count) {
        this.id = id;
        this.title = title;
        this.count = count;
    }

    public String getTitleAndCount() {
        return title + " [" + count + "]";
    }
}

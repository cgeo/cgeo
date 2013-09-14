package cgeo.geocaching.list;

abstract class AbstractList {

    public final int id;
    public final String title;

    public AbstractList(final int id, final String title) {
        this.id = id;
        this.title = title;
    }

    public abstract String getTitleAndCount();

}

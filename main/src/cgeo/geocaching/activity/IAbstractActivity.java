package cgeo.geocaching.activity;


public interface IAbstractActivity {

    public void showToast(String text);

    public void showShortToast(String text);

    public void invalidateOptionsMenuCompatible();
}

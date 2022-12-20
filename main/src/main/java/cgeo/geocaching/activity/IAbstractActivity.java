package cgeo.geocaching.activity;


public interface IAbstractActivity {

    void showToast(String text);

    void showShortToast(String text);

    void invalidateOptionsMenuCompatible();

}

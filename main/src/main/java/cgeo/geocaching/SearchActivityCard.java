package cgeo.geocaching;

public enum SearchActivityCard {
    GCCODE(R.drawable.search_identifier, R.string.search_geo, R.id.card_gccode, R.id.searchg_geocode),
    FILTER(R.drawable.ic_menu_filter, R.string.search_filter_button, R.id.card_filter, R.id.searchg_filter),
    COORDINATES(R.drawable.ic_menu_mylocation, R.string.search_coordinates, R.id.card_coords, R.id.searchg_coordinates),
    ADDRESS( R.drawable.ic_menu_home, R.string.search_address, R.id.card_addr, R.id.searchg_address),
    KEYWORD(R.drawable.search_keyword, R.string.search_kw, R.id.card_kw, R.id.searchg_keyword),
    OWNER(R.drawable.ic_menu_owned, R.string.search_hbu, R.id.card_owner, R.id.searchg_owner),
    FINDER(R.drawable.ic_menu_emoticons, R.string.search_finder, R.id.card_finder, R.id.searchg_finder),
    TRACKABLE(R.drawable.trackable_all, R.string.search_tb, R.id.card_tb, R.id.searchg_trackable);

    private int icon;
    private int title;
    private int searchFieldId;
    private int id;

    SearchActivityCard(final int icon, final int title, final int id, final int callback) {
        this.icon = icon;
        this.title = title;
        this.id = id;
        this.searchFieldId = callback;
    }

    public int getIcon() {
        return icon;
    }
    public int getId() {
        return id;
    }

    public int getTitle() {
        return title;
    }

    public int getSearchFieldId() {
        return searchFieldId;
    }
}

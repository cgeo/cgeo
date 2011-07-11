package cgeo.geocaching;

public class cgList {
	public boolean def = false;
	public int id = 0;
	public String title = null;
	public Long updated = null;
	public Double latitude = null;
	public Double longitude = null;
	
	public cgList(boolean defIn) {
		def = defIn;
	}
	
	public cgList(boolean defIn, int idIn, String titleIn) {
		def = defIn;
		id = idIn;
		title = titleIn;
	}
}

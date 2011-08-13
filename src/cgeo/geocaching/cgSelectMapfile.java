package cgeo.geocaching;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

public class cgSelectMapfile extends cgFileList<cgMapfileListAdapter> {

	String mapFile;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mapFile = getSettings().getMapFile();
	}

	public void close() {
		
		Intent intent = new Intent();
		intent.putExtra("mapfile", mapFile);
		
		setResult(RESULT_OK, intent);
		
		finish();
	}

	@Override
	protected cgMapfileListAdapter getAdapter(ArrayList<File> files) {
		return new cgMapfileListAdapter(this, files);
	}

	@Override
	protected String[] getBaseFolders() {
		String base = Environment.getExternalStorageDirectory().toString();
		return new String[]{base + "/mfmaps", base + "/Locus/mapsVector"};
	}

	@Override
	protected String getFileExtension() {
		return "map";
	}
	
	@Override
	protected void setTitle() {
		setTitle(res.getString(R.string.map_file_select_title));
	}
	
	public String getCurrentMapfile() {
		return mapFile;
	}
	
	public void setMapfile(String newMapfile) {
		mapFile = newMapfile;
	}

}

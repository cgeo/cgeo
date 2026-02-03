package saveanalyzer;

import cz.matejcik.openwig.*;
import cz.matejcik.openwig.platform.FileHandle;

import java.util.HashMap;

public class AnalyzingSavegame extends cz.matejcik.openwig.formats.Savegame {

	public AnalyzingSavegame (FileHandle fc) {
		super(fc);
		debug = true;

		translation.put("aa", Timer.class);
		translation.put("ad", Thing.class);
		translation.put("ak", Cartridge.class);
		translation.put("an", Zone.class);
		translation.put("as", Distance.class);
		translation.put("ar", EventTable.class); // ZInput
		translation.put("bd", Player.class);
		translation.put("bg", Task.class);
		translation.put("c", Media.class);
		translation.put("e", Action.class);
		translation.put("h", ZonePoint.class);
	}

	@Override
	protected void debug (String s) {
		System.out.print(s);
	}

	private HashMap<String,Class> translation = new HashMap<String, Class>();

	@Override
	protected Class classForName (String s) throws ClassNotFoundException {
		try {
			if (translation.containsKey(s)) return translation.get(s);
			else return Class.forName(s);
		} catch (ClassNotFoundException e) {
			return EventTable.class;
		}
	}

	@Override
	protected boolean versionOk (String version) {
		System.out.println("version : " + version);
		return true;
	}
}

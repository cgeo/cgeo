package saveanalyzer;

import cz.matejcik.openwig.Timer;
import cz.matejcik.openwig.j2se.J2SEFileHandle;
import java.io.*;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTableImpl;
import util.BackgroundRunner;

public class Main {

	private static class CartridgeFile extends cz.matejcik.openwig.formats.CartridgeFile {
		public CartridgeFile (AnalyzingSavegame s) {
			savegame = s;
		}
	}

	private static class Engine extends cz.matejcik.openwig.Engine {
		public Engine (AnalyzingSavegame s) throws IOException {
			super(new CartridgeFile(s), null);
			instance = this;
			state = new LuaState();
			eventRunner = new BackgroundRunner(true);
		}
		
		public void stopRunner () {
			eventRunner.kill();
		}
	}

	public static void main (String[] args) {
		Engine e = null;
		try {
			e = new Engine(new AnalyzingSavegame(new J2SEFileHandle(new File(args[0]))));
			LuaTableImpl t = new LuaTableImpl();
			e.savegame.restore(t);
			System.out.println("restore proceeded");
		} catch (Exception f) {
			System.out.println("failed:");
			f.printStackTrace();
		} finally {
			Timer.kill();
			e.stopRunner();
		}
	}
}

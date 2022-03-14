package johnidis.maverick;

import com.biotools.TheC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.util.Preferences;

import poker.MaverickGameInfo;

public class Pokibot extends com.biotools.poker.N.P implements Agent {
	
	private static final boolean DEBUG = false;
	
	private static final String 			PREFERENCES_FILE 			= "data/bots/Pokibot.pd";
	private static final Capabilities		CAPABILITIES				= // TODO: fix min players
			new Capabilities (3, Holdem.MAX_PLAYERS, Capabilities.FIXED_LIMIT + Capabilities.PA_MODELLING_PERS);
	
	private final Holdem holdemSession;

	
	static {
		initFile (com.biotools.poker.E.u(), "default_limit.opp");
		initFile (com.biotools.poker.E.u(), "default_nolimit.opp");
	}
	
	protected static void copyFile (File source, File dest) throws IOException {
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();
			destChannel = new FileOutputStream(dest).getChannel();
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		} finally {
			sourceChannel.close();
			destChannel.close();
		}
	}
	
	protected static void initFile(String dirname, String filename) {
		File path = new File(dirname);
		path.mkdirs();
		File file = new File (path, filename);
		if (! file.exists())
			try {
				copyFile (new File("data", filename), file);
			} catch (IOException e) {
				System.out.println ("Failed to initialize file "+filename+" to "+dirname+", please copy file manually and restart.");
				System.exit(-11);
			}
	}

	
	public Pokibot (Holdem holdem) {
		holdemSession = holdem;
		com.biotools.A.d.A (0);
		init (new Preferences (PREFERENCES_FILE));
	}
	

	/**
	 * Debug output to console
	 * @param paramString the debug text
	 */
	@Override
	public void J(String paramString) {
		if (holdemSession != null)
			holdemSession.println(paramString);
	}
	
	protected MaverickGameInfo getGameInfo () {
		return (MaverickGameInfo) k;
	}
	
	@Override
	public String getId() {
		return "Pokibot";
	}

	@Override
	public Capabilities capabilities() {
		return CAPABILITIES;
	}
	
	@Override
	public void setEconomyMode(boolean econ) {
		getGameInfo().C(econ);
		getGameInfo().A(econ);
	}

	@Override
	public boolean isActionInProgress() {
		return true;	// action is always available upon explicit request
	}
	
	@Override
	public synchronized Action getAction() {							// pre-play
		long time;
		if (DEBUG)
			time = System.nanoTime();
		Action action = super.getAction();							// A = amount due to call, B = amount to raise on top of A
		if (DEBUG)
			System.out.println("Pokibot used "+((System.nanoTime()-time)/1000)+" μs");
		return getGameInfo().fromDeltaToTotalAmounts(action);		// A = amount due to call, B = amount to call/raise to
	}

	@Override
	public void playerUpdate(int seat) {
		;
	}
	
	public Object getLastActionProbabilities () {
		return h();
	}

}

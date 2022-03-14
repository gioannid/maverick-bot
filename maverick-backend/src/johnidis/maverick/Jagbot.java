package johnidis.maverick;

import poker.MaverickGameInfo;

import com.biotools.meerkat.Action;

public class Jagbot extends com.biotools.poker.N.U implements Agent {
	
	private static final boolean DEBUG = false;
	
	private static final String 			PREFERENCES_FILE 			= "data/bots/JagBot.pd";
	private static final Capabilities		CAPABILITIES				= 
			new Capabilities (2, Holdem.MAX_PLAYERS, Capabilities.FIXED_LIMIT + Capabilities.PA_MODELLING_TRANS);
	
	private final Holdem holdemSession;

	
	public Jagbot (Holdem holdem) {
		holdemSession = holdem;
		com.biotools.A.d.A (0);
		init (new com.biotools.meerkat.util.Preferences (PREFERENCES_FILE));
	}
	
	
	/**
	 * Debug output to console
	 * @param paramString the debug text
	 */
	@Override
	public void T(String paramString) {
		if (holdemSession != null)
			holdemSession.println(paramString);
	}

	public MaverickGameInfo getGameInfo () {
		return (MaverickGameInfo) ł;
	}
	
	@Override
	public String getId() {
		return "Jagbot";
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
	public void playerUpdate(int seat) {
		;
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
			System.out.println("Jagbot used "+((System.nanoTime()-time)/1000)+" μs");
		return getGameInfo().fromDeltaToTotalAmounts(action);		// A = amount due to call, B = amount to call/raise to
	}
	
}

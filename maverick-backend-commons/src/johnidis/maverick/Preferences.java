package johnidis.maverick;

public enum Preferences {

	MODEL_PATH,
	MODEL_TRAIN,
	MODEL_READONLY,
	MODEL_WINDOW,
	MODEL_ULTRAAGGR,
	MODEL_AGINGEPOCH,
	MODEL_GENERIC,
	MODEL_FULL,
	
/*	DT_MINSAMPLESACTION,
	DT_MINSAMPLESHAND,*/
	
	SIM_HANDTIME, 
	SIM_STOPLOSS,
	SIM_FEARFACTOR,

//	MCTS_DEBUG,
	
	TEST_UNIFORMBUCKETS,
	TEST_ACTIONDELAY,
	
	UI_ECHOHOPPER,
	UI_ECHOMESSAGES,
	UI_INACTIVETIMER;
	
	public final static String PREFERENCE_SET				= "1Yy";

	public static boolean ready = false;
	
	private String value = null;
	
	public static Preferences add (String pref) {
		if (pref == null)
			return null;
		if ((pref.charAt(0) == '"') && (pref.charAt(pref.length() - 1) == '"'))
			pref = pref.substring(1, pref.length() - 2);
		int eq = pref.indexOf('=');
		String prefname, prefvalue;
		if (eq == -1) {
			prefname = pref.toUpperCase();
			prefvalue = PREFERENCE_SET.substring(0, 1);
		} else {
			prefname = pref.substring(0, eq).toUpperCase();
			prefvalue = pref.substring(eq + 1);
		}
		Preferences p;
		try {
			p = Preferences.valueOf(prefname);
		} catch (IllegalArgumentException e) {
			return null;
		}
		p.set(prefvalue);
		return p;
	}


	public void set (String v) {
		value = v;
	}
	
	public String get () {
		if (! ready)
			throw new RuntimeException ("Preferences not yet ready");
		return value;
	}
	
	public boolean set() {
		if (! ready)
			throw new RuntimeException ("Preferences not yet ready");
		return (value != null);
	}
	
	public Long getValue () throws NumberFormatException {
		if (! ready)
			throw new RuntimeException ("Preferences not yet ready");
		if (value != null)
			return Long.valueOf(value);
		else
			return null;
	}
	
	public boolean isOn () {
		if (! ready)
			throw new RuntimeException ("Preferences not yet ready");
		if (value == null)
			return false;
		else
			return (PREFERENCE_SET.contains(value));
	}

}

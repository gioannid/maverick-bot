package johnidis.maverick;

public interface Agent extends com.biotools.meerkat.Player {
	
	public static class Capabilities {
		
		public final static int FIXED_LIMIT 					= 1 << 0;
		public final static int POT_LIMIT 						= 1 << 1;
		public final static int NO_LIMIT 						= 1 << 2;
		public final static int TOURNAMENT 						= 1 << 3;
		public final static int BEHAVIORAL_MODELLING			= 1 << 10;
		public final static int EXPERT_MODELLING				= 1 << 11;
		public final static int PA_MODELLING_TRANS				= 1 << 12;
		public final static int PA_MODELLING_PERS				= 1 << 13;

		public final int minPlayers;
		public final int maxPlayers;
		private final int flags;
		
		public Capabilities (int minPlayers, int maxPlayers, int flags) {
			this.minPlayers = minPlayers;
			this.maxPlayers = maxPlayers;
			this.flags = flags;
		}
		
		public boolean supported (int what) {
			return (flags & what) > 0;
		}
		
	}
	
	public String getId ();
	public Capabilities capabilities ();
	public void setEconomyMode (boolean econ);
	public void playerUpdate (int seat);
	public boolean isActionInProgress ();

}

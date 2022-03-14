/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.helper;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class PlayersDB implements Iterable<PlayerStats> {
	String rootfolder = Consts.rootFolder;
	RandomAccessFile f = null;
	final private String pattern;

	// Used for building playerIndex
	Hashtable<Integer,PlayerStats> playerInfos = new Hashtable<>();
	
	
	private class PlayersDBIterator implements Iterator<PlayerStats> {
		
		private PlayerStats playerStats;
		private int playerId = 0;
		private final int maxPlayers = getNumberOfPlayers();
		
		private PlayersDBIterator () {
			getNextPlayer();
		}
				
		private void getNextPlayer() {
			do {
				playerStats = getPlayerInfo(playerId++);
			} while ((playerStats.hands == 0) && (playerId < maxPlayers));
		}

		@Override
		public boolean hasNext() {
			return (playerStats.hands > 0);
		}

		@Override
		public PlayerStats next() {
			PlayerStats ps = playerStats;
			getNextPlayer();
			return ps;
		}
		
	}
	
	
	public PlayersDB (String pattern) {
		this.pattern = pattern;
	}
	
	public PlayersDB () {
		this.pattern = null;
	}

	public void init(String root) {
		rootfolder = root;
		File file = new File (root, 
				((pattern == null) ? "pokerai.org.players" :
				pattern.replace("\\", "").replace('|',  '_').replace('*',  '_').replace('?',  '_').replace('+', '_')) 
				+ ".index");
		try {
			boolean toinit = (! file.exists());
			f = new RandomAccessFile(file, "rw");
			this.rootfolder = root;
			if (toinit) initialize();
		} catch (Exception e) { e.printStackTrace(); }
	}

	public void close() { try { f.close(); } catch (Exception e) { e.printStackTrace(); }}
	public int getNumberOfPlayers() { try { return (int) (f.length() / PlayerStats.REC_LENGTH); } catch (Exception e) { e.printStackTrace(); } return 0; }

	public void savePlayerInfo(int playerId, PlayerStats pinfo) {
		try {
			byte[] data = pinfo.save();
			if (data.length != PlayerStats.REC_LENGTH) {
				System.out.println("Players REC_LENGTH is different that data.length (" + data.length + ")");
				System.exit(1);
			}
			f.seek(playerId * PlayerStats.REC_LENGTH);
			f.write(data);
		} catch (Exception e) { e.printStackTrace(); }
	}

	// doesn't work for some reason!
	public void savePlayerIfDoesntExist(int playerId, PlayerStats pl) {
		try {
			if (f.length() <= playerId * PlayerStats.REC_LENGTH) savePlayerInfo(playerId, pl);
		} catch (Exception e) { e.printStackTrace(); }
	}

	public PlayerStats getPlayerInfo(int playerId) {
		PlayerStats pl = new PlayerStats();
		//savePlayerIfDoesntExist(playerId, pl);
		try {
			f.seek(playerId * PlayerStats.REC_LENGTH);
			byte[] data = new byte[PlayerStats.REC_LENGTH];
			f.read(data);
			pl.load(playerId, data);
		} catch (Exception e) { e.printStackTrace(); }
		return pl;
	}

	/*
   -------  Add player information  -------
	 */

	long totalHands = 0;
	private void initialize() {
		System.out.println("Initializing players index (this is done only the first time, then persisted), please wait ...");
		long time = System.currentTimeMillis();
		File dir = new File(rootfolder);
		String[] all = dir.list(new FilenameFilter () {

			@Override
			public boolean accept(File dir, String name) {
				return (pattern == null) ? name.endsWith(".hhex") : name.matches(pattern);
			}
			
		});

		for (String filename : all) {
			System.out.println ("Scanning file "+filename);
			scan(rootfolder, filename);
		}
		flushAll();
		double time2 = (System.currentTimeMillis() - time) / 60000.0;
		System.out.println("A total of " + totalHands + " hands has been indexed, for " + GeneralHelper.ds(time2) + " min.");
	}

	private void scan(String rootfolder, String fullName) {
		HandManagerNIO hm = new HandManagerNIO();
		hm.init(rootfolder, fullName);
		hm.reset();
		while (hm.hasMoreHands()) {
			PokerHand hand = hm.nextPokerHand();
			try {
				addPlayerStats(hand);
			} catch (Exception e) {
				System.out.println("ERROR: Uncaught exception while building player index!");
				e.printStackTrace();
			}
		}
		hm.closedb();
	}

	private PlayerStats getPlayerInfoInMemory(int playerId) {
		PlayerStats p = (PlayerStats) playerInfos.get(playerId);
		if (p == null) {
			p = getPlayerInfo(playerId);
			playerInfos.put(playerId, p);
		}
		return p;
	}

	private void flushAll() {
		Enumeration<Integer> keys = playerInfos.keys();
		Enumeration<PlayerStats> elem = playerInfos.elements();
		while (keys.hasMoreElements()) {
			savePlayerInfo(((Integer)keys.nextElement()).intValue(), (PlayerStats)elem.nextElement());
		}
	}

	private void addPlayerStats(PokerHand h) {
		int[] playerIds = h.getPlayerIDs();
		int[] stacks = h.getStacks();
		totalHands++;
		if (totalHands % 10000000 == 0) System.out.println(totalHands + " hands indexed.");
		Action[] preflop = h.aactionsPreflop();
		boolean[] vpip = new boolean[h.getNumberOfSeats()];
		boolean[] pfr = new boolean[h.getNumberOfSeats()];
		if (preflop != null)
			for (Action action : preflop) {
				if (Consts.isVPIPAction(action)) {
					vpip[action.getPlayerSeatId()] = true;
				}
				if (Consts.isAggressiveAction(action)) {
					pfr[action.getPlayerSeatId()] = true;
				}
			}
		for (byte seat = 0; seat < h.getNumberOfSeats(); seat++) 
			if (stacks[seat] > 0) {
				PlayerStats p = getPlayerInfoInMemory(playerIds[seat]);   // this is used for in memory parsing
				p.hands++;
				p.totalWinnings += h.getMoneyMade(seat);
				if (vpip[seat])
					p.countVPIP++;
				if (pfr[seat])
					p.countPFR++;
			}
	}

	@Override
	public Iterator<PlayerStats> iterator() {
		return new PlayersDBIterator();
	}

}


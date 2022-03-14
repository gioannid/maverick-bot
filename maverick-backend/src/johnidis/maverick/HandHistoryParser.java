package johnidis.maverick;

import java.io.*;
import java.util.*;
import java.util.regex.MatchResult;

import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.utils.AbortException;

import ca.ualberta.cs.poker.free.dynamics.Card;

public class HandHistoryParser {
	
	static private final boolean DEBUG = false;
	
	static private final String MANDATORY_FIELD = "-1";

	static private enum Parameters {
		
		PATTERN_HANDNUMBER (MANDATORY_FIELD),
		PATTERN_HAND (MANDATORY_FIELD),

		INDEX_HANDNUMBER (MANDATORY_FIELD),
		INDEX_PLAYER (MANDATORY_FIELD),
		INDEX_CARD_1 (MANDATORY_FIELD), 
		INDEX_CARD_2 (MANDATORY_FIELD),
		FILE_MONITOR_PERIOD ("10000");
		
		private String token = null;
		private int value = -1;
		private boolean set = false;
		
		private static void populate (Parameters parameter, String token) {
			if (token.charAt(0) == '"') {
				if (token.charAt(token.length() - 1) == '"')
					parameter.token = token.substring(1, token.length() - 1);
				else
					throw new IllegalArgumentException ("No matching quotes in token "+ token);
			} else {
				parameter.token = token;
				parameter.value = Integer.valueOf(token);
			}
			parameter.set = true;
			if (DEBUG)
				System.out.println (parameter.toString() + " token=" + parameter.token + " value=" + parameter.value);
		}
		
		public static void populate (String filename) throws FileNotFoundException {
			Scanner scanner = new Scanner(new File(filename));
			scanner.useDelimiter("=|"+Iface.LINE_SEPARATOR);
			try {
				while (scanner.hasNextLine()) {
					String p = scanner.next().trim().toUpperCase();
					if (p.startsWith("#"))
						scanner.nextLine();
					else
						populate (Parameters.valueOf(p), scanner.nextLine().substring(1).trim());
				}
			} finally {
				scanner.close();
			}
			for (Parameters p : Parameters.values()) {
				if (! p.set)
					throw new IllegalArgumentException ("Mandatory parameter not specified: " + p.toString());
			}
		}

		
		private Parameters (String def) {
			if (def != MANDATORY_FIELD)
				populate (this, def);
		}

		public String getToken () {
			return token;
		}
		
		public int getValue () {
			return value;
		}
		
	}
	
	
	static public final HandHistoryParser instance = new HandHistoryParser ();
	
	private File path;
	private volatile boolean active = false;
	private long lastFileTimestamp = System.currentTimeMillis();
	private Timer timer;
	private Map<File,Long> handHistoryLastSize = new HashMap<File,Long>();
	
	private class FileMonitor extends TimerTask {
		
		@Override
		public void run() {
			File[] files = path.listFiles (new FileFilter() {
				public boolean accept (File file) {
					return (file.isFile()) &&
							((DEBUG && (! handHistoryLastSize.containsKey(file)))
							|| (file.lastModified() > lastFileTimestamp));
				}
			});
			lastFileTimestamp = System.currentTimeMillis();
			for (File file : files) {
				System.out.println("Parsing hand history file: "+file);
				Long lastSize = handHistoryLastSize.get(file);
				FileInputStream stream = null;
				Scanner scanner = null;
				try {
					stream = new FileInputStream (file);
					if (lastSize != null)
						stream.skip (lastSize);
					scanner = new Scanner (new BufferedReader (new InputStreamReader (stream)));
					long handnumber = -1;
					while (scanner.hasNextLine()) {
						if (scanner.findInLine (Parameters.PATTERN_HANDNUMBER.getToken()) != null) {
							MatchResult matches = scanner.match();
							handnumber = Long.valueOf(matches.group(Parameters.INDEX_HANDNUMBER.getValue()));
							if (DEBUG)
								System.out.println ("Hand "+handnumber+":");
						} else if (scanner.findInLine (Parameters.PATTERN_HAND.getToken()) != null) {
							MatchResult matches = scanner.match();
							String player = matches.group(Parameters.INDEX_PLAYER.getValue()).replace(" ", "");
							Card card1 = Card.get(matches.group(Parameters.INDEX_CARD_1.getValue()));
							Card card2 = Card.get(matches.group(Parameters.INDEX_CARD_2.getValue()));
							if (DEBUG)
								System.out.println ("  "+player+" had ["+ card1+" "+card2+"]");
							if ((card1 != null) && (card2 != null)) {
								synchronized (BotServer.allInstances) {
									for (BotServer bot : BotServer.allInstances)
										if ((bot != null) && (bot.client != null)) {
											GameAdapter adapter = bot.holdemSession.getCachedMuckedHand (handnumber, player, true);
											if (adapter != null) {
												System.out.println ("Game "+handnumber+": "+player+" had ["+ card1+" "+card2+"], updating model");
												adapter.hole[adapter.seatToAct] = new Card[] {card1, card2};
												adapter.update(null);
												if (DEBUG) {
													adapter.report = true;
													bot.holdemSession.println(adapter.toString());
												}
												try {
//													Holdem.histogramHands.addPoint(player, adapter, GameAdapter.ACTION_NA);
													Holdem.modeller.addData (player, Holdem.BEHAVIORAL_MODEL_HAND, adapter, GameAdapter.ACTION_NA);
												} catch (AbortException e) {
													bot.holdemSession.println(e.toString());
												}
												break;
											}
										}
								}
							} else
								System.err.println("Warning: Error in parsing.");
						}
						scanner.nextLine();
					}
					handHistoryLastSize.put(file, file.length());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (scanner != null)
						scanner.close();
					if (stream != null)
						try {
							stream.close();
						} catch (IOException e) {
							;
						}
				}
			}
		}
		
	}
	

	private HandHistoryParser () {
		// public instantiation not permitted
	}

	public boolean init (String filename) {
		try {
			Parameters.populate(filename);
		} catch (Exception e) {
			System.err.println (e.getMessage());
			return false;
		}
		System.out.println ("Initialized hand history format from file "+filename);
		return true;
	}

	public boolean setListeningPath (String p) {
		path = new File(p);
		FileMonitor monitor = new FileMonitor ();
		timer = new Timer(true);
		timer.schedule (monitor, 10000, Parameters.FILE_MONITOR_PERIOD.getValue());
		active = path.exists();
		if (active)
			System.out.println ("Monitoring hand history files in "+path);
		return active;
	}
	
	public boolean isActive () {
		return active;
	}
	
}

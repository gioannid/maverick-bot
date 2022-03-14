package johnidis.maverick;

import java.io.*;
import java.util.*;

public class Raketable {
	
	static public final Raketable instance = new Raketable ();
	
	public int size = 0;
	
	static private final int EMPTY = -1;
	static private final boolean DEBUG = false;
	
	private List<Integer> game = new Vector<Integer>();
	private List<Integer> bigBlindsFrom = new Vector<Integer>();
	private List<Integer> bigBlindsTo = new Vector<Integer>();
	private List<Integer> playersFrom = new Vector<Integer>();
	private List<Integer> playersTo = new Vector<Integer>();
	private List<Integer> rakeNominator = new Vector<Integer>();
	private List<Integer> rakeDenominator = new Vector<Integer>();
	private List<Integer> maxRake = new Vector<Integer>();
	private int cacheGameType = EMPTY;
	private int cacheBigBlind = EMPTY;
	private int cachePlayers = EMPTY;
	private int cacheRakeNominator = EMPTY;
	private int cacheRakeDenominator = EMPTY;
	private int cacheMaxRake = EMPTY;
	
	private Raketable () {
		// public instantiation not permitted
	}

	private static void fillRange (String[] range, List<Integer> from, List<Integer> to) {
    	from.add(Integer.valueOf(range[0]));
    	if (range.length == 2)
    		to.add(Integer.valueOf(range[1]));
    	else
    		to.add(Integer.valueOf(range[0]));

	}
	
	public void populate (String filename) {
        Scanner scanner;
		try {
			scanner = new Scanner(new File(filename));
			scanner.useDelimiter(";|"+Iface.LINE_SEPARATOR);
	        while (scanner.hasNextLine()) {
                try {
					String t = scanner.next().trim().toUpperCase();
					if (! t.startsWith("#")) {
						if (t.equals("FL"))
							game.add(Agent.Capabilities.FIXED_LIMIT);
						else if (t.equals("PL"))
							game.add(Agent.Capabilities.POT_LIMIT);
						else if (t.equals("NL"))
							game.add(Agent.Capabilities.NO_LIMIT);
						else if (t.equals("any"))
							game.add(Agent.Capabilities.FIXED_LIMIT + Agent.Capabilities.POT_LIMIT + Agent.Capabilities.NO_LIMIT);
						else
							throw new NoSuchElementException (t + ": game type not supported");
						fillRange (scanner.next().trim().split("-"), bigBlindsFrom, bigBlindsTo);
						fillRange (scanner.next().trim().split("-"), playersFrom, playersTo);
						fillRange (scanner.next().trim().split("/"), rakeNominator, rakeDenominator);
						maxRake.add (Integer.valueOf(scanner.next()));
						size++;
					} else
						scanner.nextLine();
				} catch (NoSuchElementException e) {
					System.out.println ("Error parsing line: " + e);
				}
	        }
		} catch (Exception e) {
			size = 0;
		}
	}
	
	public int getRake (int gameType, int bigBlind, int players, int pot) {
		int rn = EMPTY, rd = EMPTY, mr = EMPTY, r = 0;
		
		if (size == 0)
			return 0;
		if ((cacheRakeNominator != EMPTY) && (cacheGameType == gameType) && 
				(cacheBigBlind == bigBlind) && (cachePlayers == players)) {
			rn = cacheRakeNominator;
			rd = cacheRakeDenominator;
			mr = cacheMaxRake;
		} else {
			for (r = 0; r < size; r++) {
				if ((game.get(r) & gameType) == gameType) {
					if ((bigBlindsFrom.get(r) <= bigBlind) && (bigBlindsTo.get(r) >= bigBlind)) {
						if ((playersFrom.get(r) <= players) && (playersTo.get(r) >= players)) {
							rn = rakeNominator.get(r);
							rd = rakeDenominator.get(r);
							mr = maxRake.get(r);
							break;
						}
					
					}
				}
			}
			cacheGameType = gameType;
			cacheBigBlind = bigBlind;
			cachePlayers = players;
			cacheRakeNominator = rn;
			cacheRakeDenominator = rd;
			cacheMaxRake = mr;
		}
		if (r < size) {
			int rake = rn * pot / rd;
			if (DEBUG)
				System.out.println ("Rake calculated for pot="+pot+", = "+(rake < mr ? rake : mr));
			return (rake < mr ? rake : mr);
		} else {
			if (DEBUG)
				System.out.println ("Rake calculated for pot="+pot+", = zero");
			return 0;
		}
	}

}

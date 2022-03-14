package com.biotools.poker.D;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import johnidis.maverick.modelling.PaPlayer;
import johnidis.maverick.modelling.adapters.BoardCache;
import johnidis.maverick.modelling.modellers.Modeller;
import johnidis.maverick.modelling.models.Model;
import johnidis.utils.AbortException;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.GameObserver;
import com.biotools.meerkat.Hand;
import com.biotools.meerkat.HandEvaluator;
import com.biotools.meerkat.util.NChoose2IntTable;

/**
 * Game observer for modelling opponents, used by Poker Academy bots
 */
public class G extends Modeller<PaPlayer,Void,Void> implements GameObserver {
	
	protected static final boolean DEBUG = false;
	
	private static final String TRANSIENT = "$transient";
	
	protected static WeakHashMap<GameInfo,G> instances = new WeakHashMap<GameInfo,G>();
	private static BoardCache<NChoose2IntTable> cacheRanks = new BoardCache<>(1000);
	
	private GameInfo gameInfo = null;
	private final boolean persistent;
	private boolean relayEvents = true;
	private boolean lastGameIsNoLimit = false;
	private BitSet boardId = null;
	private Hand board = null;

	/**
	 * Retrieve a GameObserver for the given GameInfo.
	 * @param gInfo the GameInfo to model players in
	 * @return the appropriate GameObserver object
	 */
	public static G A(GameInfo gInfo) {
		synchronized (instances) {
			G instance = instances.get(gInfo);
			if (instance == null)
				throw new RuntimeException ("G.A(): G not found for "+gInfo);
			return instance;
		}
	}
	
	/**
	 * Destructor for the GameObserver of the given GameInfo.
	 * Not mandatory for non-null GameInfo, since GameObserver will be disposed as soon as
	 * corresponding GameInfo is garbage collected.
	 * @param gInfo the GameInfo to model players in
	 */
	public static void B(GameInfo gInfo) {
		synchronized (instances) {
			G instance = instances.remove(gInfo);
			if (instance == null)
				throw new RuntimeException ("G.B(): modeller not found for "+gInfo);
			if (DEBUG)
				System.out.println("PA: Removed modeller for "+gInfo);
		}
	}

	protected static void setPlayerName (B player, String name) {
		try { 
			Field playerName = B.class.getDeclaredField("Θ‰");
			playerName.setAccessible(true);
			playerName.set(player, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	protected G (boolean persist) {		// direct instantiation not permitted
		persistent = persist;
	}

	
	private void reset() {
		boardId = null;
		board = null;
	}

	/**
	 * Returns a player model
	 * @param paramInt the seat of the player to return
	 * @return the player model
	 */
	public B Q(int paramInt)
	{
		if (gameInfo != null) {
			String key = gameInfo.getPlayerName(paramInt);
			if (! persistent)
				key += TRANSIENT;
			return open(key);
		}
		else
			return null;
	}
	
	public B newPlayer (String name) {
		B player = open (name);
		player.gameStartEvent(gameInfo);
		return player;
	}

	@Override
	public void actionEvent(int arg0, Action arg1) {
	    if (relayEvents)
	        Q(arg0).actionEvent(arg0, arg1);
	}

	@Override
	public void gameOverEvent() {
		if (relayEvents) {
			if (gameInfo == null)
				return;
			for (int i = 0; i < gameInfo.getNumSeats(); i++)
				if (gameInfo.inGame(i))
					Q(i).gameOverEvent();
		}
		if (DEBUG)
			System.out.println(toString());
		reset();
		gameInfo = null;
	}

	@Override
	public void gameStartEvent(GameInfo paramGameInfo) {
		gameInfo = paramGameInfo;
		lastGameIsNoLimit = gameInfo.isNoLimit();
		reset ();
		for (int i = 0; i < paramGameInfo.getNumSeats(); i++)
			if (paramGameInfo.inGame(i)) {
				newPlayer (paramGameInfo.getPlayerName(i));
			}
	}

	@Override
	public void stageEvent(int arg0) {
	    reset();
	    if (relayEvents)
	      for (int i = 0; i < gameInfo.getNumSeats(); i++)
	        if (gameInfo.inGame(i))
	          Q(i).stageEvent(arg0);
	}

	@Override
	public void dealHoleCardsEvent() {
		;
	}

	@Override
	public void gameStateChanged() {
		;
	}

	@Override
	public void showdownEvent(int arg0, Card arg1, Card arg2) {
		;
	}

	@Override
	public void winEvent(int arg0, double arg1, String arg2) {
		;
	}

	/**
	 * Persist players info
	 */
	public void ΕΉ()
	{
		persist();
	}
	
	private Hand getBoard() {
		if (board == null)
			board = gameInfo.getBoard();
		return board;
	}

	/**
	 * Get ranks of game board
	 */
	public NChoose2IntTable Ε·() {
		synchronized (cacheRanks) {
			if (boardId == null)
				boardId = BoardCache.key(getBoard());
			NChoose2IntTable ranks = cacheRanks.get(boardId);
			if (ranks == null) {
				long time;
				if (DEBUG)
					time = System.nanoTime();
				ranks = HandEvaluator.getRanks(getBoard());
				if (DEBUG)
					System.out.println("G.Ε·() used "+((System.nanoTime()-time)/1000)+" ΞΌs");
				cacheRanks.put(boardId, ranks);
			}
			return ranks;
		}
	}

	/**
	 * Retrieves the ranks of a hand by name
	 * @param paramHand
	 * @param paramString
	 */
	public NChoose2IntTable A(Hand paramHand, String paramString) {
		throw new RuntimeException ("A(Hand, String) not implemented");
	}

	/**
	 * Sets the relaying of events to players
	 * @param paramBoolean
	 */
	public void D(boolean paramBoolean) {
		relayEvents = paramBoolean;
	}

	@Override
	protected void doPersist(PaPlayer modeller) {
		if (! persistent)
			return;
		modeller.Ε¬();
	}

	@Override
	public String canonicalKey(PaPlayer modeller) {
		return modeller.key;
	}

	@Override
	public PaPlayer instantiate(String key, String player) {
		String suffix = (lastGameIsNoLimit ? "_N" : "_L") + ".opp";
		if (! persistent) {
			key += TRANSIENT;
		}
		ModellerFilenameFilter filter = new ModellerFilenameFilter (key, suffix);
		if (filter.exactMatch(pathToModels)) {
			if (DEBUG)
				System.out.println ("PA: Loading player model: "+key);
		} else {
			File[] foundModels = new File(pathToModels).listFiles(filter);
			if (foundModels.length == 0) {
				if (DEBUG)
					System.out.println ("PA: New player model: "+key);
			} else {
				key = foundModels[0].getName().replace(suffix, "");
				if (DEBUG)
					System.out.println ("PA: Loading player model under canonical name: "+key);
			}
		}
		return new PaPlayer (player, key, suffix, this);
	}
	
	@Override
	public String genericName() {
		return null;
	}

	@Override
	public String toString() {
		return super.toString() + ", " + instances.size() + " games in progress";
	}

	@Override
	protected double doAddData(String player, Model model, Void gameSnapshot, char action) throws AbortException {
		throw new RuntimeException ("G.addData() not supported");
	}

	@Override
	protected Void doEstimate(String player, Model model, Void gameSnapshot, boolean genericModel) throws AbortException {
		throw new RuntimeException ("G.estimate() not supported");
	}

	@Override
	protected void doModelling(String player, boolean active) {
		throw new RuntimeException ("G.setModelling() not supported");
	}

	@Override
	public double addData(String player, Model<Void, Void> model, Void gameSnapshot, char action) throws AbortException {
		return doAddData(player, model, gameSnapshot, action);
	}

	@Override
	public Void estimate(String player, Model<Void, Void> model, Void gameSnapshot, boolean genericModel) throws AbortException {
		return doEstimate(player, model, gameSnapshot, genericModel);
	}

	@Override
	public void setModelling(String player, boolean active) {
		doModelling(player, active);
	}

	@Override
	public PaPlayer open(String player) {
		return doOpen(player);
	}

}
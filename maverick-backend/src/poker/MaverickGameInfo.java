package poker;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.PlayerInfo;

import johnidis.maverick.Holdem;
import johnidis.maverick.Preferences;

public class MaverickGameInfo extends com.biotools.poker.Q.D {
	
	private static final long serialVersionUID = 5595872867442481689L;
	
	private static boolean ultraAggressiveness = Preferences.MODEL_ULTRAAGGR.isOn();

	private static final AtomicLong id = new AtomicLong(0);
	
	private long handNumber;
	
	static {
		System.out.println("Using ultra-aggressiveness: "+ultraAggressiveness);
	}
	
	static protected PlayerInfo[] getPlayers (com.biotools.poker.Q.D playerInfo) {
		try {
			Field playersField = com.biotools.poker.Q.D.class.getDeclaredField("V");
			playersField.setAccessible(true);
			return (com.biotools.poker.Q.F[]) playersField.get(playerInfo);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	protected PlayerInfo[] getPlayers () {
		return getPlayers ((com.biotools.poker.Q.D) this);
	}
	
	protected void setPlayers (MaverickPlayerInfo[] players) {
		try {
			Field playersField = com.biotools.poker.Q.D.class.getDeclaredField("V");
			playersField.setAccessible(true);
			playersField.set((com.biotools.poker.Q.D) this, players);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void advanceStage () {
		MaverickPlayerInfo player;
		for (int p = 0; p < Holdem.MAX_PLAYERS; p++)
			if ((player = getPlayer(p)) != null)
				player.logAdvanceStage();
	}
	
	/**
	 * Duplicates a GameInfo structure to this object
	 * @param paramD the GameInfo to copy information from
	 */
	@Override
	public void A(com.biotools.poker.Q.D paramD) {
		try {
			super.A(paramD);
		} catch (NullPointerException e) {
			;
		}
		PlayerInfo[] thisPlayers = getPlayers();
		PlayerInfo[] paramPlayers = getPlayers(paramD);
	    for (int i = 0; i < thisPlayers.length; i++)
	    	if (paramPlayers[i] != null)
	    		thisPlayers[i] = new MaverickPlayerInfo((MaverickPlayerInfo) paramPlayers[i], this);
	}

	/**
	 * Adds a new player to the game.
	 * @param paramString the name of the new player
	 * @param paramInt the seat of the player
	 * @return whether player was added successfully to game
	 */
	@Override
	public synchronized boolean A(String paramString, int paramInt) {
		if (super.A(paramString, paramInt)) {
			PlayerInfo[] thisPlayers = getPlayers();
		    thisPlayers[paramInt] = new MaverickPlayerInfo(paramString, this);
		    ((MaverickPlayerInfo) thisPlayers[paramInt]).F(true);
		    return true;
		} else
			return false;
	}


	/** 
	 * Get the current size of the bet.
	 * @return the size of the bet for the current stage.
	 */
	public double getBetSize() {
		return c();
	}

	public void startNewGame(long hn) {
		handNumber = hn;
		MaverickPlayerInfo player;
		for (int p = 0; p < Holdem.MAX_PLAYERS; p++)
			if ((player = getPlayer(p)) != null)
				player.startNewGame();
		A (id.incrementAndGet());
	}

	/**
	 * Obtain the total amount players must have in the pot in this round, to stay in
	 * @return the total bet amount
 	 */
	public double getBetAmount() {
		MaverickPlayerInfo player = getPlayer (getCurrentPlayerSeat ());
		return getStakes() + player.getAmountInPotThisRound() - player.getAmountInPot();
	}

	public void setButton(int i) {
		L(i);
	}

	/**
	 * Sets the big blind, the small blind (=bigblind/2) and the big bet (= bigblind*2)
	 * @param bigblind the big blind
 	 */
	public void setBigBlind(int bigblind) {
		A(bigblind/2, bigblind, bigblind*2);
	}

	/**
	 * Sets the small blind only
	 * @param amnt the small blind
 	 */
	public void setSmallBlind(int amnt) {
		A(amnt, getBigBlindSize() , N());
	}

	/**
	 * Adds a new player to the game.
	 * @param name the name of the new player
	 * @param file the file name of the player's persistent info
	 * @return the seat where the player was added, or -1 in case of failure
	 */
	public int addPlayer(String name, String file) {
		int p;
		for (p = 0; p < Holdem.MAX_PLAYERS; p++)
			if (G(p) == null) {
				A (name, p);
				break;
			}
		if (p == Holdem.MAX_PLAYERS)
			return -1;
		return p;
	}

	public void setCurrentPlayerPosition(int seat) {
		H (seat);
	}

	/**
	 * Advance the current player to the next active player in the game.
	 * @return returns the position of the new current player 
	 */
	public int advanceCurrentPlayer() {
		return C();
	}

	/**
	 * Report the end of the game and log player and game history.
	 * Should only be called once at the end of a game
	 */
	public void gameOver() {
		D();
	}
	
	/** 
	 * Fold the current player
	 * @return true if game is over.
	 */
	public boolean fold() {
		getPlayer(getCurrentPlayerSeat()).logFold();
		return B();
	}

	public void call() {
		getPlayer(getCurrentPlayerSeat()).logCall();
		S();
	}

	/**
	 * Raise the current player
	 * @return true if player bet, false if he raised
	 */
	public boolean raise() {
		if (ultraAggressiveness)
			E (getBetSize()+getBetAmount());
		else
			E (getBetSize());
		if (getNumRaises() == 1)
			getPlayer(getCurrentPlayerSeat()).logBet();
		else
			getPlayer(getCurrentPlayerSeat()).logRaise();
		return (getNumRaises() == 1);
	}
	
	/**
	 * Proceed to stage FLOP
	 * @param c1 table card 1
	 * @param c2 table card 2
	 * @param c3 table card 3 
	 */
	public void flop(Card c1, Card c2, Card c3) {
		advanceStage();
		A(c1, c2, c3);
	}	

	/**
	 * Proceed to stage TURN
	 * @param c table card 4
	 */
	public void turn(Card c) {
		advanceStage();
		B(c);
	}	

	/**
	 * Proceed to stage RIVER
	 * @param c table card 4
	 */
	public void river(Card c) {
		advanceStage();
		A(c);
	}

	public void bigBlind() {
		getPlayer(getCurrentPlayerSeat()).logBigBlind();
		A();
	}

	public void smallBlind() {
		getPlayer(getCurrentPlayerSeat()).logSmallBlind();
		I();
	}

	@Override
	public MaverickPlayerInfo getPlayer(int paramInt) {
		return (MaverickPlayerInfo) super.getPlayer(paramInt);
	}

	@Override
	public MaverickPlayerInfo getPlayer(String paramString) {
		return (MaverickPlayerInfo) super.getPlayer(paramString);
	}
	
	public double getBigBetSize () {
		return N();
	}

	public synchronized void addWinner (String paramString) {
		if ((Z() == null) || (! Z().contains(paramString)))
			D(paramString);
	}

	public synchronized String getWinners () {
		return Z();
	}
	
	public String getBettingSequence () {
		return d();
	}
	
	public long getHandNumber () {
		return handNumber;
	}
	
	public Action fromDeltaToTotalAmounts (Action action) {
		if (action.isCheckOrCall())
			action = new Action (action.getType(), 
					action.getToCall(), 
					getBetAmount());
		if (action.isBetOrRaise())
			action = new Action (action.getType(), 
					action.getToCall(), 
					getBetAmount() + getBetSize());
		return action;
	}

	public Action fromTotalToDeltaAmounts (Action action) {
		if (action.isCheckOrCall())						
			action = new Action (action.getType(), 
					action.getToCall(),
					0); 
		if (action.isBetOrRaise()) {
			int lastInPot = (int) (action.getAmount() - getBetSize());
			if ((getStage() == Holdem.PREFLOP) && (lastInPot == 0)) {
				if (getCurrentPlayerSeat() == getSmallBlindSeat())
					lastInPot = (int) getSmallBlindSize();
				else if (getCurrentPlayerSeat() == getBigBlindSeat())
					lastInPot = (int) getBigBlindSize();
			}
			action = new Action (action.getType(), 
					action.getToCall(),
					action.getAmount() - lastInPot);
		}
		return action;
	}

	@Override
	public String toString() {
		return "[" + getGameID() + "]:" + d() + ":" + getBoard();
	}
	
}

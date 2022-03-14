package simulation.real;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.biotools.meerkat.GameInfo;

import johnidis.maverick.Agent;
import johnidis.maverick.Holdem;
import johnidis.maverick.Raketable;
import johnidis.maverick.modelling.adapters.ActionGameAdapter;
import johnidis.maverick.modelling.data.MLData;
import johnidis.utils.AbortException;
import ca.ualberta.cs.poker.free.dynamics.Card;
import ca.ualberta.cs.poker.free.dynamics.RingDynamics;
import data.Action;
import data.CardGenerator;
import data.GameState;
import ringclient.ClientRingDynamics;
import simulation.datastructure.DecisionArc;
import simulation.datastructure.LeafNode;
import simulation.interfaces.INode;
import simulation.real.datastructure.PlayerNode;
import simulation.real.datastructure.PlayerState;

public abstract class SimulationGame {

	static public final boolean 					DEBUG 					= false;
	// =VALUE(MID(J1;FIND("[";J1)+1;LEN(J1)-FIND("[";J1)-1))
	static public final String 						DEBUG_SIM_THREAD 		= "raise";
	static public final int 						DEBUG_ROUND 			= -2;
	
	public static AtomicLong nodeId = new AtomicLong (0);

	public PlayerState[] playerStates;
	public int actualSeat;
	public int bettingRound;
	public int round;
	public int activePlayers;
	public int ownSeat;
	public int maxInPot;
	public CardGenerator cardGenerator;
	public Card[] board;
	public RingDynamics crd;
	public String debugDeck;
	public StringBuilder debugRound[];
	public StringBuilder debugWinners;
	public String debugValue;
	public String debugHole[];

	public int[] showdowns;
	public long[] sumShowdownRanks;
	public int[] nextAction;

	protected final Holdem holdemSession;

    static public void debug (String text, boolean newline) {
    	String n = Thread.currentThread().getName();
    	if (n.equals(DEBUG_SIM_THREAD+"1")) {
    		if (newline)
    			System.out.println(text);
    		else
    			System.out.print(text);
    	}
    }
    
    
    protected SimulationGame() {
		holdemSession = null;
    }
    
	protected SimulationGame(ClientRingDynamics crd, Holdem holdem) {
		this.crd = crd;
		this.cardGenerator = new CardGenerator();
		Card [] board = crd.getBoard(false);
		if (board != null) {
			// copy board
			this.board = new Card[board.length];
			for (int i = 0; i < board.length; i++)
				this.board[i] = board[i];
			// remove board cards
			for (Card c : board) {
				if (c != null) {
					cardGenerator.removeCard(c);
				}
			}
		}
		activePlayers = crd.numActivePlayers;
		this.actualSeat = crd.seatTaken;
		this.ownSeat = this.actualSeat;
		bettingRound = crd.roundBets;
		round = crd.roundIndex + 1;
		maxInPot = crd.getMaxInPot();
		for (Card c : crd.hole[crd.seatTaken]) {
			cardGenerator.removeCard(c);
		}
		holdemSession = holdem;
		this.playerStates = new PlayerState[crd.numPlayers];
		for (int i = 0; i < crd.numPlayers; i++) {
			this.playerStates[i] = new PlayerState(crd, i);
		}
		showdowns = new int[crd.numPlayers];
		sumShowdownRanks = new long[crd.numPlayers];
		nextAction = new int[crd.numPlayers];
	}

	protected SimulationGame(SimulationGame game, Holdem holdem) {
		this.cardGenerator = new CardGenerator(game.cardGenerator);
		this.crd = game.crd;
		activePlayers = game.activePlayers;
		this.actualSeat = game.actualSeat;
		this.bettingRound = game.bettingRound;
		this.round = game.round;
		this.ownSeat = game.ownSeat;
		this.maxInPot = game.maxInPot;
		if (DEBUG) {
			debugDeck = cardGenerator.toString();
			if ((DEBUG_ROUND == crd.handNumber) || (DEBUG_ROUND == -1))
				debug("<Game deck=\""+debugDeck+"\">", true);
			debugRound = new StringBuilder[4];
			debugHole = new String[crd.numPlayers];
		}
		if (game.board != null) {
			// clone board
			this.board = new Card[game.board.length];
			for (int i = 0; i < game.board.length; i++)
				this.board[i] = game.board[i];
		}
		holdemSession = holdem;
		this.playerStates = new PlayerState[crd.numPlayers];
		for (int i = 0; i < crd.numPlayers; i++) {
			this.playerStates[i] = new PlayerState(game.playerStates[i]);
			if (DEBUG)
				debugHole[i] = playerStates[i].getName();
		}
		showdowns = new int[crd.numPlayers];
		sumShowdownRanks = new long[crd.numPlayers];
		nextAction = new int[crd.numPlayers];
		if (DEBUG)
			if ((DEBUG_ROUND == crd.handNumber) || (DEBUG_ROUND == -1))
				debug(" <NewRound round="+round+">", true);
	}
    

	protected void debugRound(String string) {
		if (round < GameState.SHOWDOWN.ordinal()) {
			if (debugRound[round-1] == null)
				debugRound[round-1] = new StringBuilder ();
			debugRound[round-1].append(string);
		}
	}
	
	public int getAmountToCall (int seat) {
		return maxInPot - playerStates[seat].getAmountIn();
	}
	
	public int getTotalPotSize() {
		int sum = 0;
		for (PlayerState player : playerStates)
			sum += player.getAmountIn();
		return sum;
	}

	protected int getBetSize() {
		if (round < GameState.TURN.ordinal())
			return crd.info.bigBlindSize;
		else
			return crd.info.bigBetSize;
	}

	protected int getNextSeatCanAct() {
		for (int i = 0; i < crd.numPlayers; i++) {
			PlayerState p = playerStates[i];
			if (p.canAct(maxInPot))
				return i;
		}
		return 0;
	}
	
	protected boolean getNumPlayerToAct() {
		for (PlayerState player : playerStates) {
			if (player.canAct(maxInPot))
				return true;
		}
		return false;
	}
	
	protected PlayerState getNextPlayerCanAct() {
		for (int i = 0; i < crd.numPlayers; i++) {
			PlayerState player = playerStates[(i + actualSeat) % crd.numPlayers];
			if (player.canAct(maxInPot))
				return player;
		}
		return null;
	}

	protected void generateFlop() {
		this.board = new Card[5];
		for (int i = 0; i < 3; i++)
			this.board[i] = cardGenerator.getNextAndRemoveCard();
	}

	protected void generateTurnRiver() {
		for (int i = 0; i < this.board.length; i++) {
			if (board[i] == null) {
				board[i] = cardGenerator.getNextAndRemoveCard();
				break;
			}
		}
	}

	protected void generateNextRound() {
		if ((round == GameState.FLOP.ordinal()) && (board[0] == null))
			generateFlop();
		else if (((round == GameState.TURN.ordinal()) || ((round == GameState.RIVER.ordinal())))
				&& (board[round] == null))
			generateTurnRiver();
	}

	protected void startNewRound() {
		round++;
		generateNextRound ();
		bettingRound = 0;
		for (PlayerState p : playerStates) {
			if (p.isActive())
				p.setCanRaiseNextRound(true);
		}
		actualSeat = getNextSeatCanAct();
	}

	protected INode getNextNodeStart() throws AbortException {
		PlayerState player;
		// Game ends
		if (activePlayers <= 1)
			return actualLeafNode();
		// new Round starts
		if (! getNumPlayerToAct()) {
			round++;
			if (round == GameState.SHOWDOWN.ordinal()) {
				LeafNode showdownNode = actualLeafNode();
				showdownNode.setValue(LeafNode.VALUE_PENDING);
				return showdownNode;
			}
			bettingRound = 0;
			for (PlayerState p : playerStates) {
				if (p.isActive())
					p.setCanRaiseNextRound(true);
			}
			actualSeat = getNextSeatCanAct();
		}
		player = getNextPlayerCanAct();
		actualSeat = player.getSeat();
		return new PlayerNode(player, nodeId.getAndIncrement());
	}

	protected INode getNextNode() throws AbortException {
		PlayerState player;
		// Game ends
		if (activePlayers <= 1)
			return actualLeafNode();
		// new Round starts
		if (! getNumPlayerToAct()) {
			startNewRound();
		}
		player = getNextPlayerCanAct();
		if (round < GameState.SHOWDOWN.ordinal()) {
			actualSeat = player.getSeat();
			return new PlayerNode(player, nodeId.getAndIncrement());
		} else
			return actualLeafNode();
	}
	
	protected void raiseAction() {
		bettingRound++;
		maxInPot += getBetSize();
		PlayerState player = playerStates[actualSeat];
		player.setAmountIn(maxInPot);
		for (PlayerState p : playerStates) {
			if (p.isActive())
				p.setCanRaiseNextRound(true);
		}
		player.setCanRaiseNextRound(false);
	}

	protected void callAction() {
		PlayerState player = playerStates[actualSeat];
		player.setAmountIn(maxInPot);
		player.setCanRaiseNextRound(false);
	}

	protected void foldAction() {
		if (actualSeat == ownSeat) {
			for (PlayerState p : playerStates) {
				p.setActive(false);
			}
			activePlayers = 0;
		} else {
			playerStates[actualSeat].setActive(false);
			activePlayers--;
		}
	}
	
	public Action getDecision (int i) {
		Action a;
		if (i == Action.FOLD.toInt()) {
			foldAction();
			a = Action.FOLD;
		} else if (i == Action.RAISE.toInt()) {
			if (bettingRound == 4) {
				callAction();
				a = Action.CALL;
			} else {
				raiseAction();
				a = Action.RAISE;
			}
		} else {
			callAction();
			a = Action.CALL;
		}
		return a;
	}
	
	public LeafNode actualLeafNode() throws AbortException {
		LeafNode leaf = new LeafNode(nodeId.getAndIncrement());
		int[] value = new int[crd.numPlayers];
		if (!playerStates[ownSeat].isActive()) {
			value[ownSeat] = -playerStates[ownSeat].getAmountIn();
			leaf.setValue(value[ownSeat]);
			if (DEBUG) {
				debugWinners = new StringBuilder("{").append(-value[ownSeat]).append("} ");
				if ((DEBUG_ROUND == crd.handNumber) || (DEBUG_ROUND == -1)) {
					debug(" Won: ", false);
					debug(debugWinners.toString(), false);
				}
			}
			return leaf;
		}
		
		int pot = 0;
		boolean[] active = new boolean[crd.numPlayers];
		for (int i = 0; i < crd.numPlayers; i++) {
			PlayerState player = playerStates[i];
			if (player.isActive()) {
				active[i] = true;
			}
			pot += player.getAmountIn();
			value[i] = -player.getAmountIn();
		}

		if (activePlayers == 1) {
			value[ownSeat] = pot - playerStates[ownSeat].getAmountIn() - 
					Raketable.instance.getRake(Agent.Capabilities.FIXED_LIMIT, crd.info.bigBlindSize, crd.numPlayers, pot);
			leaf.setValue(value[ownSeat]);
			if (DEBUG) {
				debugWinners = new StringBuilder(crd.botNames[ownSeat]).append("{").append(value[ownSeat]).append("} ");
				if ((DEBUG_ROUND == crd.handNumber) || (DEBUG_ROUND == -1)) {
					debug(" Won: ", false);
					debug(debugWinners.toString(), false);
				}
			}
			return leaf;
		}
		determineWinner(active, pot, value);
		if (value[ownSeat] > 0) {
			value[ownSeat] -= (playerStates[ownSeat].getAmountIn() +
					Raketable.instance.getRake(Agent.Capabilities.FIXED_LIMIT, crd.info.bigBlindSize, crd.numPlayers, pot));
		}
		leaf.setValue(value[ownSeat]);
		if (DEBUG) {
			debugWinners = new StringBuilder ();
			for (int i = 0; i < crd.numPlayers; i++)
				if (value[i] > 0)
					debugWinners.append(crd.botNames[i]).append("(").append(value[i]).append(") ");
			if ((DEBUG_ROUND == crd.handNumber) || (DEBUG_ROUND == -1)) {
				debug(" Won: ", false);
				debug(debugWinners.toString(), false);
			}
		}
		return leaf;
	}

	public DecisionArc getDecision(PlayerNode pNode) throws AbortException {
		Action act = getDecision ();
		if (nextAction[actualSeat] == 0)
			nextAction[actualSeat] = act.ordinal() + 1;
		DecisionArc arc = new DecisionArc(pNode, act);
		INode nextNode = getNextNode();
		arc.setChild(nextNode);
		nextNode.initParent(arc);
		pNode.addArc(arc);
		return arc;
	}

	public DecisionArc getDecision(PlayerNode pNode, int i) throws AbortException {
		Action a = getDecision (i);
		DecisionArc arc = new DecisionArc(pNode, a);
		INode nextNode = getNextNodeStart();
		arc.setChild(nextNode);
		nextNode.initParent(arc);
		pNode.addArc(arc);
		return arc;
	}
	
	protected Action getDecision() throws AbortException {
		Action act = Action.FOLD;
		if (activePlayers > 1) {
			double fold, raise;
			if (round < GameState.SHOWDOWN.ordinal()) {
				MLData estimation = estimateDecisionProbabilities();
				if (estimation != null) {
					fold = ActionGameAdapter.IdealData.FOLD.getEstimation(estimation);
					raise = ActionGameAdapter.IdealData.RAISE.getEstimation(estimation);
				} else {
					throw new AbortException("Action estimation not available for "+crd.botNames[actualSeat]+
							" during "+GameState.values()[round]);
				}
			} else {
				fold = 0;
				raise = 0.5;
			}
			double ran = Math.random();
			if (ran < fold) {
				if (playerStates[actualSeat].getAmountIn() == maxInPot) {
					callAction();
					act = Action.CALL;
				}
				else {
					foldAction();
					act = Action.FOLD;
				}
			} else if (ran < raise + fold) {
				if (bettingRound == 4) {
					callAction();
					act = Action.CALL;
				} else {
					raiseAction();
					act = Action.RAISE;
				}
			} else {
				callAction();
				act = Action.CALL;
			}
			if (DEBUG) {
				debugRound(new StringBuilder(crd.botNames[actualSeat]).append('[').
						append(act.toChar()).append(':').append((int) (fold*100)).append(',').
						append((int) (raise*100)).append("] ").toString());
				if ((DEBUG_ROUND == crd.handNumber) || (DEBUG_ROUND == -1))
					debug(new StringBuilder(" seat: ").append(actualSeat).append(", Fold: ").
							append(fold).append(", Raise: ").append(raise).
							append(" ... ").append(act.toString()).toString(), true);
			}
		}
		return act;
	}
	
	private boolean[] getPotWinners(boolean[] playersIn) throws AbortException {
		boolean[] potWinners = new boolean[crd.numPlayers];
		int firstIndex = 0;
		while (playersIn[firstIndex] == false) {
			firstIndex++;
		}
		int hr0 = estimatedHandRank (firstIndex);
		if (hr0 == LeafNode.VALUE_PENDING)
			return null;
		potWinners[firstIndex] = true;
		for (int i = firstIndex + 1; i < crd.numPlayers; i++) {
			if (playersIn[i]) {
				int hr1 = estimatedHandRank (i);
				if (hr1 == hr0) {
					// tie
					potWinners[i] = true;
				} else if (hr1 > hr0) {
					// new winner
					potWinners[i] = true;
					for (int j = 0; j < i; j++) {
						potWinners[j] = false;
					}
					hr0 = hr1;
				}
			}
		}
		return potWinners;
	}

	protected void determineWinner(boolean[] playersIn, int potSize, int[] value) throws AbortException {
		boolean[] potWinners = getPotWinners (playersIn);
		if (potWinners == null)
			return;
		int numWinners = 0;
		for (int i = 0; i < crd.numPlayers; i++) {
			if (potWinners[i]) {
				numWinners++;
			}
		}
		// Amount given to every winner.
		int baseAmountWon = potSize / numWinners;
		int remainder = potSize - (baseAmountWon * numWinners);
		for (int i = 0; i < crd.numPlayers; i++) {
			if (potWinners[i]) {
				value[i] = baseAmountWon;
				if (remainder > 0) {
					value[i]++;
					remainder--;
				}
			}
		}
	}

	@Override
	public String toString() {
		return Arrays.deepToString(board);
	}

	public abstract void setDebug (boolean debug);
	protected abstract MLData estimateDecisionProbabilities () throws AbortException;
	protected abstract int estimatedHandRank (int player) throws AbortException;
	public abstract SimulationGame newInstance(SimulationGame game, Holdem holdemSession) throws AbortException;
	public abstract SimulationGame newInstance(ClientRingDynamics crd, GameInfo gameInfo, Holdem holdemSession) throws AbortException;
	public abstract Agent.Capabilities capabilities ();
	public abstract void shutdown ();

}
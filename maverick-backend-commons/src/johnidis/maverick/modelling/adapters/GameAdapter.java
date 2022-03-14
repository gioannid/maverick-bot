package johnidis.maverick.modelling.adapters;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

import johnidis.utils.AbortException;
import poker.HandEvaluator;
import pokerai.game.eval.twoplustwo.Evaluator;
import simulation.real.SimulationGame;
import simulation.real.datastructure.RealPlayerState;
import ca.ualberta.cs.poker.free.dynamics.Card;
import ca.ualberta.cs.poker.free.dynamics.RingDynamics;
import ca.ualberta.cs.poker.free.dynamics.Card.Suit;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Hand;

import data.Bucket;
import data.CardGenerator;

public abstract class GameAdapter {
	
	protected static final int[] suitPSimtoUoA = {
		Suit.HEARTS.index, Suit.DIAMONDS.index, Suit.CLUBS.index, Suit.SPADES.index
	};
	protected static final int[] suitUoAtoPSim = {
		3, 0, 1, 2
	};
	
	public static final int 			INVALID 						= -10;
	public static final char			ACTION_NA						= (char) 0;
	public static final int				PRECALCULATED_HR_NOT_AVAILABLE	= -1;

	public static final int 			ALL_ROUNDS 						= -1;
	public static final int				ROUND_PREFLOP					= 0;
	public static final int				ROUND_FLOP						= 1;
	public static final int				ROUND_TURN						= 2;
	public static final int				ROUND_RIVER						= 3;
	public static final int				ROUND_SHOWDOWN					= 4;

	public static final double			MAX_RANKING						= Evaluator.FOURKIND << 12;
	public static final double 			MAX_PLAYERS 					= 9;

	protected static final double 		MAX_RAISES 						= 3;
	
	static private Map<BitSet,HandTexture> cacheBoardRankings = new BoardCache<HandTexture>();
	
	public boolean report;
	public char lastAction = 0;

	public long id = INVALID;
	public int seatToAct = INVALID;
	public int dealtPlayers = INVALID;
	public int numActivePlayers = INVALID; 
	public int roundIndex = INVALID;
	public double amountToCall = INVALID; 
	public double sumPots = INVALID;
	public double inPot = INVALID;
	public Card[] board = null;
	public Card[][] hole = null;
	public int[] suits = null;
	public int[] ranks = null;
	public int roundPlayers = INVALID;
	public StringBuilder[] playerActions = null;
	
	CardGenerator deck = null;
	BitSet boardId = null;
	
	
	private static int flushOuts (int[] suits, final int optimalCards) {
		int flushOuts = 0;
		for (int s = 0; s < 4; s++) {
			if (suits[s] + optimalCards >= 5)
				flushOuts++;
		}
		return flushOuts;
	}
	
	private static int straightOuts (int[] ranks, final int optimalCards) {
		int straightOuts = 0;
		for (int start = 0; start < 8; start++) {
			int gaps = 0;
			for (int r = 0; r < 5; r++) {
				if (ranks[start + r] == 0)
					gaps++;
			}
			if (gaps - optimalCards <= 0)
				straightOuts++;
		}
		return straightOuts;
	}

	public class HandTexture {
		
		public final int ranking;
		public final int flushOuts3;
		public final int straightOuts3;
		public final int flushOuts4;
		public final int straightOuts4;
		
		private double potentialRanking = INVALID;
		
		HandTexture(int player) {
			int[] suits;
			int[] ranks;
			Card[] holeCards = (player >= 0) ? GameAdapter.this.hole[player] : null;
			
			if (GameAdapter.this.roundIndex == GameAdapter.ROUND_PREFLOP) {
				ranking = GameAdapter.INVALID;
				flushOuts3 = GameAdapter.INVALID;
				straightOuts3 = GameAdapter.INVALID;
				flushOuts4 = GameAdapter.INVALID;
				straightOuts4 = GameAdapter.INVALID;
			} else {
				ranking = HandEvaluator.rankHand_Java(
						GameAdapter.handToEvaluateUoA(holeCards, GameAdapter.this.board));
				if (player >= 0) {
					suits = GameAdapter.this.suits.clone();
					ranks = GameAdapter.this.ranks.clone();
					for (int c = 0; c < 2; c++) {
						suits[holeCards[c].suit.index]++;
						ranks[holeCards[c].rank.index]++;
					}
				} else {
					suits = GameAdapter.this.suits;
					ranks = GameAdapter.this.ranks;
				}
				if ((player >= 0) && GameAdapter.this.roundIndex >= GameAdapter.ROUND_TURN) {
					flushOuts3 = 0;
					straightOuts3 = 0;
					if (GameAdapter.this.roundIndex == GameAdapter.ROUND_RIVER) {
						flushOuts4 = 0;
						straightOuts4 = 0;
					} else {
						flushOuts4 = flushOuts(suits, 1);
						straightOuts4 = straightOuts(ranks, 1);
					}
				} else {
					flushOuts3 = flushOuts(suits, 2);
					straightOuts3 = straightOuts(ranks, 2);
					flushOuts4 = flushOuts(suits, 1);
					straightOuts4 = straightOuts(ranks, 1);
				}
			}
		}

		public HandTexture(HandTexture other) {
			ranking = other.ranking;
			potentialRanking = other.potentialRanking;
			flushOuts3 = other.flushOuts3;
			straightOuts3 = other.straightOuts3;
			flushOuts4 = other.flushOuts4;
			straightOuts4 = other.straightOuts4;
		}
		
		public HandTexture(Hand hand, boolean hole) {
			int[] suits = new int[4];
			int[] ranks = new int[13];
			if (hand.size() < 3) {
				ranking = GameAdapter.INVALID;
				flushOuts3 = GameAdapter.INVALID;
				straightOuts3 = GameAdapter.INVALID;
				flushOuts4 = GameAdapter.INVALID;
				straightOuts4 = GameAdapter.INVALID;
			} else {
				ranking = HandEvaluator.rankHand_Java(hand);
				int roundIndex = hand.size() - 2 - (hole ? 2 : 0);
				for (int c = 0; c < hand.size(); c++) {
					com.biotools.meerkat.Card card = hand.getCard(c+1);
					suits[card.getSuit()]++;
					ranks[card.getRank()]++;
				}
				if (hole && (roundIndex >= GameAdapter.ROUND_TURN)) {
					flushOuts3 = 0;
					straightOuts3 = 0;
					if (roundIndex == GameAdapter.ROUND_RIVER) {
						flushOuts4 = 0;
						straightOuts4 = 0;
					} else {
						flushOuts4 = flushOuts(suits, 1);
						straightOuts4 = straightOuts(ranks, 1);
					}
				} else {
					flushOuts3 = flushOuts(suits, 2);
					straightOuts3 = straightOuts(ranks, 2);
					flushOuts4 = flushOuts(suits, 1);
					straightOuts4 = straightOuts(ranks, 1);
				}
			}
		}
		
		
		void updatePotentialRanking(GameAdapter adapter, int depth) {
			if (potentialRanking != GameAdapter.INVALID)
				throw new RuntimeException ("potentialRanking cannot be mutated, use a cloned HandTexture");
			if (adapter.roundIndex > GameAdapter.ROUND_PREFLOP) {
				long board_eval = 0;
				int iterations = 0;
				int[] h = GameAdapter.handToEvaluate (null, adapter.board);
				for (int c1 = 0; c1 < depth - adapter.deck.openedCards(); c1++)
					for (int c2 = c1 + 1; c2 < depth - adapter.deck.openedCards(); c2++) {
						h[0] = Evaluator.index(adapter.deck.upcoming(c1));
						h[1] = Evaluator.index(adapter.deck.upcoming(c2));
						board_eval += Bucket.evaluator.handRank(h);
						iterations++;
				}
				potentialRanking = ((double) board_eval) / iterations;
			}
		}
		
		public double getPotentialRanking() {
			return potentialRanking;
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append("ranking=").append(ranking).append(" (").append(HandEvaluator.name_hand(ranking)).append(") ");
			if (potentialRanking != GameAdapter.INVALID)
				str.append("potentialRanking=").append(potentialRanking).append(' ');
			str.append("flushOuts3=").append(flushOuts3).append(' ');
			str.append("flushOuts4=").append(flushOuts4).append(' ');
			str.append("straightOuts3=").append(straightOuts3).append(' ');
			str.append("straightOuts4=").append(straightOuts4).append(' ');
			return str.toString();
		}
		
	}
	
	
	static public int[] handToEvaluate (Card[] hole, Card[] board) {
		int[] h = new int[(board[4] == null) ? ((board[3] == null) ? 5 : 6) : 7];
		for (int b = 0; b < board.length; b++)
			if ((b < 3) || (board[b] != null))
				h[b+2] = Evaluator.index(board[b]);
		if (hole != null) {
			h[0] = Evaluator.index(hole[0]);
			h[1] = Evaluator.index(hole[1]);
		}
		return h;
	}
	
	static public int[][] holeAndBoardToEvaluate (Card[] hole, Card[] board) {
		int[][] h = new int[2][];
		h[0] = new int[2];
		h[1] = new int[(board[4] == null) ? ((board[3] == null) ? 3 : 4) : 5];
		for (int b = 0; b < board.length; b++)
			if ((b < 3) || (board[b] != null))
				h[1][b] = Evaluator.index(board[b]);
		if (hole != null) {
			h[0][0] = Evaluator.index(hole[0]);
			h[0][1] = Evaluator.index(hole[1]);
		}
		return h;
	}
	
	static public int[] handToEvaluate (Hand hole, Hand board) {
		int[] h = new int[hole.size() + board.size()];
		for (int b = 1; b <= board.size(); b++)
			h[b+1] = Evaluator.index(board.getCard(b));
		if (hole != null) {
			h[0] = Evaluator.index(hole.getCard(1));
			h[1] = Evaluator.index(hole.getCard(2));
		}
		return h;
	}
	
	static public int[][] holeAndBoardToEvaluate (Hand hole, Hand board) {
		int[][] h = new int[2][];
		h[0] = new int[2];
		h[1] = new int[board.size()];
		for (int b = 1; b <= board.size(); b++)
			h[0][b-1] = Evaluator.index(board.getCard(b));
		if (hole != null) {
			h[0][0] = Evaluator.index(hole.getCard(1));
			h[0][1] = Evaluator.index(hole.getCard(2));
		}
		return h;
	}
	
	static public Hand handToEvaluateUoA (Card[] hole, Card[] board) {
		Hand hand = new Hand();
		for (int b = 0; b < 5; b++)
			if (board[b] != null)
				hand.addCard(CardGenerator.index(board[b]));
		if (hole != null) {
			hand.addCard(CardGenerator.index(hole[0]));
			hand.addCard(CardGenerator.index(hole[1]));
		}
		return hand;
	}
	
	static public double normalize (double x, double upper) {
		double result = x / upper;
		return (result > 1.0) ? 1.0 :
				(result < -1.0) ? -1.0 :
				result;
	}
	
	static public double normalizeLog (double x, double upper) {
		double result = Math.log(x) / Math.log(upper);
		return (result > 1.0) ? 1.0 :
				(result < -1.0) ? -1.0 :
				result;
	}

	static public char getAction (Action action) {
		switch (action.getType()) {
		case Action.BET:
			return 'b';
		case Action.RAISE:
			return 'r';
		case Action.CALL:
			if (action.getToCall() == 0)
				return 'k';
			else
				return 'c';
		case Action.CHECK:
			return 'k';
		case Action.FOLD:
			return 'f';
		case Action.BIG_BLIND:
		case Action.SMALL_BLIND:
			return 'B';
		case Action.SIT_OUT:
			return 'Q';
		default:
			return '-';
		}
	}
	
	static public int getActionIndex (char action) {
		switch (action) {
		case 'f':
			return 0;
		case 'k':
		case 'c':
			return 1;
		case 'b':
		case 'r':
			return 2;
		}
		return -1;
	}
	
	static public int seatToPlayer (GameInfo gameInfo, int seat) {
		int np = gameInfo.getNumPlayers();
		return ((np - gameInfo.getButtonSeat() - 1 + seat) % np);
	}
	
	static public int playerToSeat (GameInfo gameInfo, int player) {
		int np = gameInfo.getNumPlayers();
		return ((np + player - (gameInfo.getButtonSeat() + 1)) % np);
	}
	
	static public int occurrences (CharSequence string, char aChar) {
		int occ = 0;
		for (int i = 0; i < string.length(); i++)
			if (string.charAt(i) == aChar)
				occ++;
		return occ;
	}
	
	
	public GameAdapter (boolean r) {
		report = r;
	}
	
	public GameAdapter (GameAdapter other, Object game) {
		report = other.report;
		id = other.id;
		seatToAct = other.seatToAct;
		dealtPlayers = other.dealtPlayers;
		numActivePlayers = other.numActivePlayers; 
		roundIndex = other.roundIndex;
		amountToCall = other.amountToCall; 
		sumPots = other.sumPots;
		inPot = other.inPot;
		hole = new Card[other.hole.length][];
		for (int i = 0; i < other.hole.length; i++)
			if (other.hole[i] != null)
				hole[i] = other.hole[i].clone();
		if (other.board != null)
			board = other.board.clone();
		suits = other.suits.clone();
		ranks = other.ranks.clone();
		roundPlayers = other.roundPlayers;
		playerActions = new StringBuilder[other.playerActions.length];
		for (int i = 0; i < other.playerActions.length; i++)
			if (other.playerActions[i] != null)
				playerActions[i] = new StringBuilder(other.playerActions[i]);
		if (other.deck != null)
			deck = new CardGenerator(other.deck);
		if (board[2] == null)
			boardId = null;
		else
			boardId = BoardCache.key(board);
	}
	
	
	public HandTexture getBoardTexture () {
		if (roundIndex == ROUND_PREFLOP)
			return new HandTexture (INVALID);
		HandTexture texture;
		synchronized (cacheBoardRankings) {
			texture = cacheBoardRankings.get(boardId);
			if (texture == null) {
				texture = new HandTexture(INVALID);
				cacheBoardRankings.put(boardId, texture);
			}
		}
		return texture;
	}
	
	public HandTexture boardPotentialRanking (int depth) {
		HandTexture texture = getBoardTexture();
		if (texture.getPotentialRanking() != INVALID)
			return texture;
		texture.updatePotentialRanking(this, depth);
		synchronized (cacheBoardRankings) {
			cacheBoardRankings.put(boardId, texture);
		}
		return texture;
	}
	
	public void update (Object game) {
		if (game instanceof RingDynamics) {
			id = ((RingDynamics) game).handNumber;
			seatToAct = ((RingDynamics) game).seatToAct;
			dealtPlayers = ((RingDynamics) game).numPlayers;
			numActivePlayers = ((RingDynamics) game).numActivePlayers;
			roundIndex = ((RingDynamics) game).roundIndex;
			amountToCall = ((RingDynamics) game).getAmountToCall(seatToAct);
			sumPots = ((RingDynamics) game).getSumPotsAfterCall(seatToAct) - amountToCall;
			inPot = ((RingDynamics) game).inPot[seatToAct];
			board = ((RingDynamics) game).getBoard(true);
			hole = ((RingDynamics) game).hole;
		} else if (game instanceof GameInfo) {
			id = ((GameInfo) game).getGameID();
			seatToAct = playerToSeat((GameInfo) game, ((GameInfo) game).getCurrentPlayerSeat());
			dealtPlayers = ((GameInfo) game).getNumPlayers();
			numActivePlayers = ((GameInfo) game).getNumActivePlayers();
			roundIndex = ((GameInfo) game).getStage();
			amountToCall = (int) ((GameInfo) game).getAmountToCall(((GameInfo) game).getCurrentPlayerSeat());
			sumPots = (int) ((GameInfo) game).getTotalPotSize();
			inPot = (int) ((GameInfo) game).getPlayer(((GameInfo) game).getCurrentPlayerSeat()).getAmountInPot();
			board = new Card[5];
			for (int c = 0; c < ((GameInfo) game).getBoard().size(); c++) {
				com.biotools.meerkat.Card card = ((GameInfo) game).getBoard().getCard(c+1);
				if (card != null)
					board[c] = CardGenerator.getCard(card);
			}
			hole = new Card[dealtPlayers][];
			for (int p = 0; p < dealtPlayers; p++) {
				int s = playerToSeat((GameInfo) game, p);
				if (((GameInfo) game).getPlayer(p).getRevealedHand() != null) {
					hole[s] = new Card[2];
					hole[s][0] = CardGenerator.getCard(((GameInfo) game).getPlayer(p).getRevealedHand().getFirstCard());
					hole[s][1] = CardGenerator.getCard(((GameInfo) game).getPlayer(p).getRevealedHand().getSecondCard());
				}
			}
		} else if (game instanceof SimulationGame) {
			id = ((SimulationGame) game).crd.handNumber;
			seatToAct = ((SimulationGame) game).actualSeat;
			dealtPlayers = ((SimulationGame) game).crd.numPlayers;
			numActivePlayers = ((SimulationGame) game).activePlayers;
			roundIndex = ((SimulationGame) game).round - 1;
			amountToCall = ((SimulationGame) game).getAmountToCall(seatToAct);
			sumPots = ((SimulationGame) game).getTotalPotSize();
			inPot = ((SimulationGame) game).playerStates[seatToAct].getAmountIn();
			board = ((SimulationGame) game).board;
			hole = new Card[dealtPlayers][];
			if (((SimulationGame) game).playerStates[seatToAct] instanceof RealPlayerState)
				for (int s = 0; s < dealtPlayers; s++)
					hole[s] = ((RealPlayerState) ((SimulationGame) game).playerStates[seatToAct]).getHole();
		} else {
			throw new RuntimeException ("GameAdapter.update(): unknown game object "+game);
		}
	}
	
	public void onRoundStart (Object game) {
		update (game);
		if (game instanceof RingDynamics) {
			roundPlayers = ((RingDynamics) game).numActivePlayers;
		} else if (game instanceof GameInfo) {
			roundPlayers = ((GameInfo) game).getNumActivePlayers();
		} else if (game instanceof SimulationGame) {
			roundPlayers = ((SimulationGame) game).activePlayers;
		} else {
			throw new RuntimeException ("GameSnapshot.onRoundStart(): unknown game object "+game);
		}
		suits = new int[4];
		ranks = new int[13];
		playerActions = new StringBuilder[dealtPlayers];
		if ((roundIndex >= ROUND_FLOP) && (roundIndex <= ROUND_RIVER)) {
			deck = new CardGenerator();
			for (int b = 0; b < roundIndex + 2; b++)
				deck.removeCard(board[b]);
			for (int b = 0; b < board.length; b++) {
				if (board[b] == null)
					break;
				suits[board[b].suit.index]++;
				ranks[board[b].rank.index]++;
			}
			boardId = BoardCache.key(board);
		}
	}
	
	public void onAction (Object game, char action) {
		update (game);
		if (playerActions[seatToAct] == null)
			playerActions[seatToAct] = new StringBuilder();
		playerActions[seatToAct].append(action);
	}
	
	protected void assertValidity () throws AbortException {
		if (! (
				(id != INVALID) &&
				(seatToAct != INVALID) &&
				(dealtPlayers != INVALID) &&
				(numActivePlayers != INVALID) && 
				(roundIndex != INVALID) &&
				(amountToCall != INVALID) && 
				(sumPots != INVALID) &&
				(inPot != INVALID) &&
				(suits != null) &&
				(ranks != null) &&
				(roundPlayers != INVALID) &&
				(playerActions != null)
				)) {
			throw new AbortException ("GameAdapter failed validation!");
		}
	}
	
	public abstract void collectInputs (char action, double[] input) throws AbortException;

	public void collectIdeals (char action, double[] ideal) throws AbortException {
		assertValidity ();
		lastAction = action;
	}
	
	public NamedSymbol[] allSymbols (int index) {
		return null;
	}
	
	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(hashCode()).append(" (");
		sb.append(seatToAct).append(") \n");
		sb.append("  Hole: ").append(Arrays.deepToString(hole[seatToAct])).
		append(", Board: ").append(Arrays.deepToString(board)).append('\n');
		sb.append("  Dealt players: ").append(dealtPlayers).
				append(", currently active: ").append(numActivePlayers).append('\n'); 
		sb.append("  Amount to call: ").append(amountToCall).
				append(", total pot: ").append(sumPots).append(", player in pot: ").append(inPot).append('\n');
		sb.append("  Round: ").append(roundIndex).append(", players: ").append(roundPlayers).append('\n');
		sb.append("  Player actions: ").append(Arrays.deepToString(playerActions));
		return sb.toString();
	}

	public abstract NamedSymbol inputAsSymbol (double[] input, int index);
	public abstract NamedSymbol idealAsSymbol (double[] ideal, int index);
	public abstract String field (int index);
	
}
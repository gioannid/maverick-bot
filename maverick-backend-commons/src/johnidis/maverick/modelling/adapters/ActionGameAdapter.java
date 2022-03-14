package johnidis.maverick.modelling.adapters;

import java.util.Arrays;

import simulation.real.SimulationGame;
import simulation.real.datastructure.PlayerState;

import com.biotools.meerkat.GameInfo;

import ca.ualberta.cs.poker.free.dynamics.RingDynamics;
import johnidis.maverick.modelling.data.MLData;
import johnidis.utils.AbortException;
import data.ArrayTools;

/* EVALUATED DATA:
 *
 * Player Inputs Type
 *
 * Committed Bool
 * First to act Bool
 * Last to act Bool
 * # of bets to call Numeric
 * Last action was a bet -->REMOVED: or call<-- Bool
 * Preflop raiser Bool
 * Flop raiser Bool
 * # of calls and raises (preflop) Numeric
 * # of calls and raises (current stage) Numeric
 * # of checks (current stage) Numeric
 * Position Numeric
 * # players dealt in Numeric
 * # of players left to act Numeric
 *
 * Game Inputs Type
 *
 * Pot odds Numeric
 * Bet ratio Numeric
 * Pot ratio Numeric
 *
 * Board Inputs Type
 *
 * # of possible flushes Numeric 
 * # of possible straights Numeric
 * # of same card ranks (e.g. 5♣5♦2♥ would be 2) Numeric
 * # of same card suits (e.g. 8♠3♠7♠ would be 3) Numeric
 * # of Aces, Kings and Queens Numeric
 *
 * Outputs Type
 *
 * Fold Bool
 * Check or Call Bool
 * Bet or Raise Bool 
*/

public class ActionGameAdapter extends GameAdapter {
	
	private static final boolean		DEBUG					= false;
	
	private static final int 			BOARD_RANKING_DEPTH 	= 42;

	public static enum InputData {
		
		COMMITTED(true), FIRST_TOACT(true), LAST_TOACT(true), BETSTOCALL(false), LASTACTION_RAISE(true),
		PREFLOP_RAISER(true), FLOP_RAISER(true), PREFLOP_COMMITS(false), ROUND_COMMITS(false), ROUND_CHECKS(false),
		PLAYERRELPOSITION(false), DEALTPLAYERS(false), REMAININGPLAYERS(false),
		POTODDS(false), BETRATIO(false), POTRATIO(false),
		POSSIBLEFLUSHES(false), POSSIBLESTRAIGHTS(false), MAXSAMERANK(false), MAXSAMESUIT(false), AKQ(false),
		BOARDRANKING(false);
		
		public static final int			FIELDS					= values().length;
		
		public final boolean isBoolean;
		
		private InputData(boolean bool) {
			isBoolean = bool;
		}

		public int index () {
			return ordinal();			
		}

	}

	public static class IdealData {
		
		public static final int			FIELDS					= InputData.FIELDS;
		
		public static final IdealData	FOLD					= new IdealData("FOLD", 0, FIELDS / 3);
		public static final IdealData	CHECKORCALL				= new IdealData("CHECKORCALL", FIELDS / 3, 2 * FIELDS / 3);
		public static final IdealData	RAISE					= new IdealData("RAISE", 2 * FIELDS / 3, FIELDS);
		
		public final String				name;
		private final int				nFrom, nTo;
		
		private IdealData(String name, int from, int to) { 
			nFrom = from;
			nTo = to;
			this.name = name;
		}
		
		public void setIdeals(double[] ideals) {
			for (int n = nFrom; n < nTo; n++)
				ideals[n] = 1.0D;
		}
		
		public double getEstimation(MLData predictions) {
			double total = 0D;
			for (int n = nFrom; n < nTo; n++)
				total += predictions.getData(n);
			return total / (nTo - nFrom);
		}
		
		public void toString(StringBuilder str, double[] ideals) {
			str.append(name).append("=(");
			for (int n = nFrom; n < nTo; n++) {
				if (n != nFrom)
					str.append(',');
				str.append(ideals[n]);
			}
			str.append(") ");
		}

	}
	
	
	public boolean[] committed = null;
	public int maxSameSuit = INVALID;
	public int maxSameRank = INVALID;
	public char[] lastPlayerAction = null;
	public boolean firstToAct = true;
	public boolean lastToAct = false;
	public int maxBet = INVALID;
	public StringBuilder[] preflopPlayerActions;
	public boolean[] flopRaiser;
	public HandTexture boardTexture = null;
	public boolean AKQ = false;
	public int relPosition = INVALID;


	public ActionGameAdapter(boolean r) {
		super(r);
	}
	
	public ActionGameAdapter(GameAdapter other, Object game) {
		super (other, game);
		if (other instanceof ActionGameAdapter) {
			committed = ((ActionGameAdapter) other).committed.clone();
			maxSameRank = ((ActionGameAdapter) other).maxSameRank;
			maxSameSuit = ((ActionGameAdapter) other).maxSameSuit;
			lastPlayerAction = ((ActionGameAdapter) other).lastPlayerAction.clone();
			firstToAct = ((ActionGameAdapter) other).firstToAct;
			lastToAct = ((ActionGameAdapter) other).lastToAct;
			maxBet = ((ActionGameAdapter) other).maxBet;
			preflopPlayerActions = new StringBuilder[((ActionGameAdapter) other).preflopPlayerActions.length];
			for (int i = 0; i < ((ActionGameAdapter) other).preflopPlayerActions.length; i++)
				if (((ActionGameAdapter) other).preflopPlayerActions[i] != null)
					preflopPlayerActions[i] = new StringBuilder(((ActionGameAdapter) other).preflopPlayerActions[i]);
			if (((ActionGameAdapter) other).flopRaiser != null)
				flopRaiser = ((ActionGameAdapter) other).flopRaiser.clone();
			boardTexture = ((ActionGameAdapter) other).boardTexture;
			AKQ = ((ActionGameAdapter) other).AKQ;
			relPosition = ((ActionGameAdapter) other).relPosition;
		}
	}
	
	
	@Override
	public void onRoundStart(Object game) {
		super.onRoundStart(game);
		if (DEBUG)
			System.out.println("ActionGameAdapter: New Round: "+roundIndex+" ("+seatToAct+") "+Thread.currentThread().getName());
		committed = new boolean[dealtPlayers];
		lastPlayerAction = new char[dealtPlayers];
		firstToAct = true;
		lastToAct = false;
		maxSameRank = 0;
		maxSameSuit = 0;
		AKQ = false;
		relPosition = 0;
		for (int p = 0; p < dealtPlayers; p++) {
			if (p == seatToAct)
				break;
			if (game instanceof RingDynamics) {
				if (((RingDynamics) game).active[p])
					relPosition++;
			} else if (game instanceof GameInfo) {
				if (((GameInfo) game).isActive(p))
					relPosition++;
			} else if (game instanceof SimulationGame) {
				if (((SimulationGame) game).playerStates[p].isActive())
					relPosition++;
			} else {
				throw new RuntimeException ("ActionGameAdapter.onRoundStart()[relPosition]: unknown game object "+game);
			}
		}
		boardTexture = getBoardTexture();
		if (roundIndex == ROUND_PREFLOP) {
			preflopPlayerActions = new StringBuilder[dealtPlayers];
			if (game instanceof RingDynamics) {
				maxBet = ((RingDynamics) game).getLimitBet();
			} else if (game instanceof GameInfo) {
				maxBet = (int) (((GameInfo) game).getCurrentBetSize());
			} else if (game instanceof SimulationGame) {
				maxBet = (((SimulationGame) game).bettingRound < 2) ? 
						((SimulationGame) game).crd.info.smallBetSize :
						((SimulationGame) game).crd.info.bigBetSize; 
			} else {
				throw new RuntimeException ("ActionGameAdapter.onRoundStart()[maxBet]: unknown game object "+game);
			}
		} else {
			if (roundIndex == ROUND_FLOP)
				flopRaiser = new boolean[dealtPlayers];
			boardTexture = boardPotentialRanking(BOARD_RANKING_DEPTH);
			for (int s = 0; s < 4; s++)
				if (maxSameSuit < suits[s])
					maxSameSuit = suits[s];
			for (int r = 0; r < 13; r++) {
				if (maxSameRank < ranks[r])
					maxSameRank = ranks[r];
				if ((r >= 10) && (ranks[r] > 0))
					AKQ = true;
			}
		}
	}

	@Override
	public void update(Object game) {
		super.update(game);
		if (game instanceof RingDynamics) {
			lastToAct = (((RingDynamics) game).getNumPlayersLeftToAct() == 1);
		} else if (game instanceof GameInfo) {
			lastToAct = (((GameInfo) game).getNumToAct() == 1);
		} else if (game instanceof SimulationGame) {
			int playersCanAct = 0;
			for (PlayerState player : ((SimulationGame) game).playerStates)
				if (player.canAct(((SimulationGame) game).maxInPot))
					playersCanAct++;
			lastToAct = (playersCanAct == 1);
		} else {
			throw new RuntimeException ("ActionGameAdapter.update(): unknown game object "+game);
		}
	}

	@Override
	public void onAction(Object game, char action) {
		super.onAction(game, action);
		if ((action == 'b') || (action == 'r') || (action == 'c'))
			committed[seatToAct] = true;
		if (roundIndex == ROUND_PREFLOP) {
			preflopPlayerActions[seatToAct] = new StringBuilder (playerActions[seatToAct]);
		} else if (roundIndex == ROUND_FLOP)
			if ((action == 'b') || (action == 'r'))
				flopRaiser[seatToAct] = true;
		lastPlayerAction[seatToAct] = action;
		if (DEBUG)
			System.out.println ("Action "+action+": "+toString());
		firstToAct = false;
	}

	@Override
	protected void assertValidity() throws AbortException {
		super.assertValidity();
		if (! (
				(maxSameSuit != INVALID) &&
				(maxSameRank != INVALID) &&
				(maxBet != INVALID) &&
				(committed != null) &&
				(lastPlayerAction != null) &&
				(preflopPlayerActions != null) &&
				(boardTexture != null)
				)) {
			throw new AbortException ("ActionGameAdapter failed validation!");
		}
	}
	
	@Override
	public void collectInputs (char action, double[] input) throws AbortException {
		String preflopActions = (preflopPlayerActions[seatToAct] != null) ? preflopPlayerActions[seatToAct].toString() : "";
		String roundActions = (playerActions[seatToAct] != null) ? playerActions[seatToAct].toString() : "";

		assertValidity ();
		input[InputData.COMMITTED.index()] = committed[seatToAct] ? 1 : 0;
		input[InputData.FIRST_TOACT.index()] = firstToAct ? 1 : 0;
		input[InputData.LAST_TOACT.index()] = lastToAct ? 1 : 0;
		input[InputData.BETSTOCALL.index()] = normalize (amountToCall, maxBet * 4);
		input[InputData.LASTACTION_RAISE.index()] = ((lastPlayerAction[seatToAct] == 'b') || 
				(lastPlayerAction[seatToAct] == 'r')) ? 1 : 0;
		input[InputData.PREFLOP_RAISER.index()] = (preflopActions.contains("b") || preflopActions.contains("r")) ? 1 : 0;
		input[InputData.FLOP_RAISER.index()] = (flopRaiser != null) ? (flopRaiser[seatToAct] ? 1 : 0) : 0;
		input[InputData.PREFLOP_COMMITS.index()] = normalize (occurrences(preflopActions, 'b') + 
				occurrences(preflopActions, 'r') + occurrences(preflopActions, 'c'), dealtPlayers * 2);
		input[InputData.ROUND_COMMITS.index()] = normalize (occurrences(roundActions, 'b') + 
				occurrences(roundActions, 'r') + occurrences(roundActions, 'c'), dealtPlayers * 2);
		input[InputData.ROUND_CHECKS.index()] = normalize (occurrences(roundActions, 'k'),
				dealtPlayers * 2);
		input[InputData.PLAYERRELPOSITION.index()] = normalize (seatToAct, numActivePlayers);
		input[InputData.DEALTPLAYERS.index()] = normalize (dealtPlayers, MAX_PLAYERS);
		input[InputData.REMAININGPLAYERS.index()] = normalize (numActivePlayers, MAX_PLAYERS);
		input[InputData.POTODDS.index()] = (sumPots + amountToCall != 0) ? amountToCall / (sumPots + amountToCall) : 0;
		input[InputData.BETRATIO.index()] = (sumPots != 0) ? amountToCall / sumPots : 0;
		input[InputData.POTRATIO.index()] = (sumPots != 0) ? inPot / sumPots : 0;
		input[InputData.POSSIBLEFLUSHES.index()] = normalize (Math.max(boardTexture.flushOuts3, 0), 2);
		input[InputData.POSSIBLESTRAIGHTS.index()] = normalize (Math.max(boardTexture.straightOuts3, 0), 4);
		input[InputData.MAXSAMERANK.index()] = normalize (maxSameRank, 4);
		input[InputData.MAXSAMESUIT.index()] = normalize (maxSameSuit, 5);
		input[InputData.AKQ.index()] = AKQ ? 1 : 0;
		input[InputData.BOARDRANKING.index()] = normalize (Math.max(boardTexture.getPotentialRanking(), 0), MAX_RANKING);
		if (report)
			System.out.println (id + ": " + ArrayTools.deepToString(InputData.values(), input));
		if (action != ACTION_NA)
			lastAction = action;
	}

	@Override
	public void collectIdeals(char action, double[] ideal) throws AbortException {
		super.collectIdeals(action, ideal);
		switch (action) {
		case 'f':
			IdealData.FOLD.setIdeals(ideal);
			break;
		case 'b':
		case 'r':
			IdealData.RAISE.setIdeals(ideal);
			break;
		case 'c':
		case 'k':
			IdealData.CHECKORCALL.setIdeals(ideal);
			break;
		case 'Q':
		case 'K':
			break;
		default:
			throw new RuntimeException ("Not permitted action in ActionGameAdapter.collectIdeals()");
		}
		if (report) {
			StringBuilder str = new StringBuilder(Long.toString(id));
			str.append(": ");
			IdealData.FOLD.toString(str, ideal);
			IdealData.CHECKORCALL.toString(str, ideal);
			IdealData.RAISE.toString(str, ideal);
			System.out.println(str);
		}
	}
	
	@Override
	public String field(int index) {
		if (index >= InputData.FIELDS) {
			int i = index - InputData.FIELDS;
			if ((IdealData.FOLD.nFrom >= i) && (IdealData.FOLD.nTo < i))
				return IdealData.FOLD.name + i;
			else if ((IdealData.CHECKORCALL.nFrom >= i) && (IdealData.CHECKORCALL.nTo < i))
				return IdealData.CHECKORCALL.name + i;
			else if ((IdealData.RAISE.nFrom >= i) && (IdealData.RAISE.nTo < i))
				return IdealData.RAISE.name + i;
			else
				throw new RuntimeException("IdealData unknown for index " + index);
		} else
			return InputData.values()[index].toString();
	}

	@Override
	public NamedSymbol idealAsSymbol(double[] ideal, int index) {
		throw new RuntimeException ("BasicGameAdapter.idealAsSymbol() not supported");
	}

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\n  Preflop player actions: ").append(Arrays.deepToString(preflopPlayerActions)).append('\n');
		sb.append("  Acting order: ");
		if (firstToAct)
			sb.append("First to act ");
		if (lastToAct)
			sb.append("Last to act");
		sb.append('\n');
		sb.append("  Board ranking: ").append(boardTexture).append('\n');
		sb.append("  Board max same rank: ").append(maxSameRank).
				append(", max same suit: ").append(maxSameSuit);
		return sb.toString();
	}

	@Override
	public NamedSymbol inputAsSymbol(double[] input, int index) {
		return null;
	}
}
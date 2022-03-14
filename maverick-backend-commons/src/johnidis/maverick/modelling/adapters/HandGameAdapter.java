package johnidis.maverick.modelling.adapters;

import java.util.Arrays;

import pokerai.game.eval.twoplustwo.Evaluator;
import johnidis.utils.AbortException;
import data.ArrayTools;
import data.Bucket;

public class HandGameAdapter extends GameAdapter {
	
	private static final boolean		DEBUG							= false;
	
	public static final int				MAX_BOARD_OUTS					= 6;					
	
	private static final int			BOARD_RANKING_DEPTH 			= 42;
	
	public static enum InputData {
		
		DEALTPLAYERS, PREFLOP_PLAYERRELPOSITION, 
		PREFLOP_RAISES, FLOP_RAISES, TURN_RAISES, RIVER_RAISES,
		PREFLOP_TOTALRAISES, FLOP_TOTALRAISES, TURN_TOTALRAISES, RIVER_TOTALRAISES,
		PREFLOP_CHECKSORCALLS, FLOP_CHECKSORCALLS, TURN_CHECKSORCALLS, RIVER_CHECKSORCALLS,
		FLOP_BOARDOUTS, TURN_BOARDOUTS, RIVER_BOARDOUTS,
		FLOP_BOARDRANKING, TURN_BOARDRANKING, RIVER_BOARDRANKING;
		
		public static final int			FIELDS				= values().length;

		public int index () {
			return ordinal();			
		}
		
	}

	public static enum IdealData {
		
		HANDRANK;
		
		public static final int			FIELDS				= values().length;

		public int index () {
			return ordinal();			
		}

	}
	
	public HandTexture[] streetBoardTexture = null;
	public StringBuilder[][] streetPlayerActions;
	public int[] roundRaises = null;
	public int precalculatedHandrank = PRECALCULATED_HR_NOT_AVAILABLE;
	public int lastRank = INVALID;
	
	private boolean sawShowdown = false;


	public HandGameAdapter(boolean r) {
		super(r);
	}

	public HandGameAdapter(HandGameAdapter other, Object game) {
		super(other, game);
		streetPlayerActions = new StringBuilder[ROUND_SHOWDOWN][other.dealtPlayers];
		for (int r = 0; r < ROUND_SHOWDOWN; r++)
			for (int p = 0; p < other.dealtPlayers; p++)
				if (other.streetPlayerActions[r][p] != null)
					streetPlayerActions[r][p] = new StringBuilder(other.streetPlayerActions[r][p]);
		streetBoardTexture = new HandTexture[ROUND_SHOWDOWN-1];
		for (int i = 0; i < ROUND_SHOWDOWN-1; i++)
			if (other.streetBoardTexture[i] != null)
				streetBoardTexture[i] = new HandTexture(other.streetBoardTexture[i]);
		roundRaises = other.roundRaises.clone();
		precalculatedHandrank = other.precalculatedHandrank;
	}

	@Override
	public void onRoundStart(Object game) {
		int round = (roundIndex == ROUND_SHOWDOWN) ? ROUND_RIVER : roundIndex;
		if (dealtPlayers != INVALID) {
			streetPlayerActions[round] = new StringBuilder[dealtPlayers];
			for (int p = 0; p < dealtPlayers; p++)
				if (playerActions[p] != null)
					streetPlayerActions[round][p] = new StringBuilder(playerActions[p]);
		} else
			streetPlayerActions = new StringBuilder[ROUND_SHOWDOWN][];
		if (game != null)
			super.onRoundStart(game);
		else
			roundIndex = round + 1;
		if (roundIndex == ROUND_PREFLOP) {
			streetPlayerActions = new StringBuilder[ROUND_SHOWDOWN][dealtPlayers];
			streetBoardTexture = new HandTexture[ROUND_SHOWDOWN-1];
			roundRaises = new int[ROUND_SHOWDOWN];
		} else {
			if (roundIndex < ROUND_SHOWDOWN) {
				boardPotentialRanking(BOARD_RANKING_DEPTH);
				streetBoardTexture[roundIndex-1] = getBoardTexture();
			}
			else
				sawShowdown = true;
		}
		if (DEBUG) {
			System.out.println("HandGameAdapter: New Round: "+roundIndex+" ("+seatToAct+") "+Thread.currentThread().getName());
			System.out.println(this);
		}
	}

	@Override
	public void onAction(Object game, char action) {
		super.onAction(game, action);
		if ((action == 'r') || (action == 'b'))
			roundRaises[roundIndex]++;
	}

	@Override
	protected void assertValidity() throws AbortException {
		super.assertValidity();
		if (! (
				(streetPlayerActions != null) &&
				(roundRaises != null) &&
				(streetBoardTexture != null)
				)) throw new AbortException ("HandGameSnapshot failed validation!");

	}

	@Override
	public void update(Object game) {
		if (game != null)
			super.update(game);
		precalculatedHandrank = PRECALCULATED_HR_NOT_AVAILABLE;
	}

	@Override
	public void collectInputs(char action, double[] input) {
		if (DEBUG) {
			System.out.println ("HandGameAdapter.collectInputs()");
			System.out.println (this);
		}
		if (roundIndex < ROUND_RIVER)
			throw new RuntimeException ("HandGameAdapted.collectInputs() should only be called upon showdown");
		if (! sawShowdown)
			onRoundStart (null);
		input[InputData.DEALTPLAYERS.index()] = normalize (dealtPlayers, MAX_PLAYERS);
		input[InputData.PREFLOP_PLAYERRELPOSITION.index()] = normalize (seatToAct, dealtPlayers);
		for (int r = ROUND_PREFLOP; r <= ROUND_RIVER; r++) {
			String roundActions = streetPlayerActions[r][seatToAct].toString();
			input[InputData.PREFLOP_RAISES.index() + r] = 
					normalize (occurrences(roundActions, 'b') + occurrences(roundActions, 'r'), MAX_RAISES);
			input[InputData.PREFLOP_TOTALRAISES.index() + r] = normalize (roundRaises[r], MAX_RAISES);
			input[InputData.PREFLOP_CHECKSORCALLS.index() + r] = 
					normalize (occurrences(roundActions, 'k') + occurrences(roundActions, 'c'), dealtPlayers * 2);
			if (r >= ROUND_FLOP) {
				input[InputData.FLOP_BOARDRANKING.index() + r - 1] = 
						normalize (streetBoardTexture[r-1].getPotentialRanking(), MAX_RANKING);
				input[InputData.FLOP_BOARDOUTS.index() + r - 1] = normalize (
						streetBoardTexture[r-1].flushOuts3 + streetBoardTexture[r-1].straightOuts3, MAX_BOARD_OUTS);
			}
		}
		if (report)
			System.out.println (id + ": " + ArrayTools.deepToString(InputData.values(), input));
	}

	@Override
	public void collectIdeals(char action, double[] ideal) throws AbortException {
		if (roundIndex < ROUND_RIVER)
			throw new RuntimeException ("HandGameAdapted.collectInputs() should only be called upon showdown");
		if (! sawShowdown)
			onRoundStart (null);
		super.collectIdeals(action, ideal);
		int[][] hand = holeAndBoardToEvaluate (hole[seatToAct], board);
		if (precalculatedHandrank != PRECALCULATED_HR_NOT_AVAILABLE) {
			lastRank = precalculatedHandrank;
		} else
			lastRank = Bucket.evaluator.handRank(hand[0], hand[1]);
		ideal[IdealData.HANDRANK.index()] = normalize (lastRank, MAX_RANKING);
		if (report)
			System.out.println (id + ": " + ArrayTools.deepToString(IdealData.values(), ideal) + 
					", rank = " + lastRank + " [" + Evaluator.HAND[lastRank >>> 12] + "]");
	}

	@Override
	public NamedSymbol inputAsSymbol(double[] input, int index) {
		return null;
	}

	@Override
	public NamedSymbol idealAsSymbol(double[] ideal, int index) {
		return null;
	}

	@Override
	public String field(int index) {
		if (index >= InputData.FIELDS)
			return IdealData.values()[index - InputData.FIELDS].toString();
		else
			return InputData.values()[index].toString();
	}

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder(super.toString());
		for (int r = 0; r <= Math.min(roundIndex, ROUND_RIVER); r++) {
			sb.append("\n  Round ").append(r).append(" betting patterns: ").append(Arrays.deepToString(streetPlayerActions[r])).
					append(", round raises: ").append(roundRaises[r]);
			if (r >= ROUND_FLOP)
				sb.append(", board ranking: ").append(streetBoardTexture[r-1]);
		}
		return sb.toString();
	}

}
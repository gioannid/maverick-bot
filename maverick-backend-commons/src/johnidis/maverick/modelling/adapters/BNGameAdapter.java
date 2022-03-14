package johnidis.maverick.modelling.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import norsys.netica.Caseset;
import norsys.netica.Environ;
import norsys.netica.Net;
import norsys.netica.NeticaException;
import norsys.netica.NodeList;
import norsys.netica.Streamer;
import poker.HandEvaluator;
import johnidis.maverick.Holdem;
import johnidis.maverick.modelling.data.MLData;
import johnidis.utils.AbortException;
import data.ArrayTools;
import data.Bucket;

@SuppressWarnings("unchecked")
public class BNGameAdapter extends GameAdapter {
	
	private static final boolean				DEBUG				= false;
	
	public static final int						MASK_ROUND			= 1 << 8;
	public static final int						MAX_CARDINALITY		= 7;
	
	public static final NamedSymbol<String>		NA					= new NamedSymbol<>("*");
	public static final NamedSymbol<Boolean>	FALSE 				= new NamedSymbol<>(Boolean.FALSE, 0);
	public static final NamedSymbol<Boolean> 	TRUE 				= new NamedSymbol<>(Boolean.TRUE, 1);
	public static final NamedSymbol<String>		NO_VPIP				= new NamedSymbol<>("NO_VPIP", 0);
	public static final NamedSymbol<String>		VPIP_NO_PFR			= new NamedSymbol<>("VPIP_NO_PFR", 1);
	public static final NamedSymbol<String> 	PFR 				= new NamedSymbol<>("PFR", 2);
	public static final NamedSymbol<String>		BLIND				= new NamedSymbol<>("BLIND", 0);
	public static final NamedSymbol<String>		EARLY_MIDDLE_POS	= new NamedSymbol<>("EARLY_MIDDLE_POS", 1);
	public static final NamedSymbol<String>		LATE_POS			= new NamedSymbol<>("LATE_POS", 2);
	public static final NamedSymbol<String>		BUTTON				= new NamedSymbol<>("BUTTON", 3);
	public static final NamedSymbol<String> 	FOLD 				= new NamedSymbol<String>("FOLD", 0);
	public static final NamedSymbol<String> 	CHECKORCALL			= new NamedSymbol<String>("CHECKORCALL", 1);
	public static final NamedSymbol<String> 	RAISE 				= new NamedSymbol<String>("RAISE", 2);

	public static final NamedSymbol[]	 		SET_BOOLEAN			= new NamedSymbol[] {FALSE, TRUE};
	public static final NamedSymbol[]			SET_PREFLOP			= new NamedSymbol[] {NO_VPIP, VPIP_NO_PFR, PFR};
	public static final NamedSymbol[][]			SET_CARDINAL		= new NamedSymbol[MAX_CARDINALITY+1][];
	public static final NamedSymbol[]			SET_PLAYERS			= new NamedSymbol[MAX_CARDINALITY-2];
	public static final NamedSymbol[]			SET_RELPOSITION		= new NamedSymbol[] {BLIND, EARLY_MIDDLE_POS, LATE_POS, BUTTON};
	public static final NamedSymbol[] 			SET_HAND_FINAL		=
			bucketSymbols(new String[] {"BUSTED", "PAIR", "TWOPAIR", "TRIPLE", "STRAIGHT", "FLUSH", "_AtLeast_FULLHOUSE"}, false);
	public static final NamedSymbol[] 			SET_HAND_INTERIM	=
			bucketSymbols(new String[] {"BUSTED", "PAIR", "TWOPAIR", "TRIPLE", "STRAIGHT", "FLUSH", "_AtLeast_FULLHOUSE"}, true);
	public static final NamedSymbol[] 			SET_BOARD			=
			bucketSymbols(new String[] {"BUSTED", "PAIR", "TWOPAIR", "_AtLeast_TRIPLE"}, true);
	public static final NamedSymbol[] 			SET_ACTION			= new NamedSymbol[] {FOLD, CHECKORCALL, RAISE};
	public static final NamedSymbol[]			SET_HOLE			= new NamedSymbol[5];
	public static final int[]					MAP_HAND_INTERIM_TO_FINAL = new int[SET_HAND_INTERIM.length];
	
	private static final AtomicInteger			casesetId			= new AtomicInteger(0);
	
	public static class HandEstimations {
		
		public final ArrayList<MLData>[] round = new ArrayList[3];
		
		public HandEstimations() {
			for (int r = 0; r < 3; r++)
				round[r] = new ArrayList<MLData>();
		}

		public HandEstimations(HandEstimations other) {
			for (int r = 0; r < 3; r++)
				round[r] = new ArrayList<MLData>(other.round[r]);
		}
		
	}
	
	public static class PostflopBucket {
		
		public static final int UNUSED = -1;
		public static final List<String> potentials = new ArrayList<>();
		public static final List<String> kickers = new ArrayList<>();

		public final String name;
		public final int hand;
		public final int potential;
		public final int kicker;
		
		public PostflopBucket(int hand, String handname, String potential, String kicker) {
			this.hand = hand;
			this.potential = index (potentials, potential);
			this.kicker = index (kickers, kicker);
			StringBuilder str = new StringBuilder();
			if (this.hand != UNUSED)
				str.append(handname).append("_");
			if (this.potential != UNUSED)
				str.append(potential).append("_");
			if (this.kicker != UNUSED)
				str.append(kicker).append("_");
			if (str.length() == 0)
				name = "UNCLASSIFIED";
			else
				name = str.substring(0, str.length() - 1).toString();
		}
		
		private int index (List<String> keys, String key) {
			if (key == null)
				return UNUSED;
			int i = keys.indexOf(key);
			if (i == -1) {
				keys.add(key);
				i = keys.size() - 1;
			}
			return i;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
	public interface Data {
		public int index();
		public Data[] allDataFields();
		public NamedSymbol[] allValues();
	}
	
	public static enum InputData implements Data {

		PREFLOP_VPIP_PFR (SET_PREFLOP),
		BOARD (SET_BOARD),
		REMAININGPLAYERS (SET_PLAYERS),
		DEALTPLAYERS (SET_PLAYERS),
		PLAYERRELPOSITION (SET_RELPOSITION),
		POTRATIO (SET_CARDINAL[4]),
		POTODDS (SET_CARDINAL[4]),
		ROUND_RAISES (SET_CARDINAL[3]),
		ROUND_CHECKS (SET_CARDINAL[3]),
		ROUND_PLAYERRAISES (SET_CARDINAL[3]),
		COMMITTED (SET_BOOLEAN);
		
		public static final int			FIELDS					= values().length;
		
		private final NamedSymbol[] valueSet;
		
		private InputData (NamedSymbol[] set) {
			valueSet = set;
		}

		@Override
		public int index () {
			return ordinal();			
		}
		
		@Override
		public Data[] allDataFields() {
			return values();
		}

		@Override
		public NamedSymbol[] allValues() {
			return valueSet;
		}

	}
	
	public static enum IdealDataInterim implements Data {

		ACTION (SET_ACTION),
		HOLE (SET_HOLE),
		HAND (SET_HAND_INTERIM);
		
		public static final int			FIELDS					= values().length;
		
		private final NamedSymbol[] valueSet;
		
		private IdealDataInterim (NamedSymbol[] set) {
			valueSet = set;
		}

		@Override
		public int index () {
			return ordinal();			
		}

		@Override
		public Data[] allDataFields() {
			return values();
		}

		@Override
		public NamedSymbol[] allValues() {
			return valueSet;
		}
		
	}
	
	public static enum IdealDataFinal implements Data {

		ACTION (SET_ACTION),
		HOLE (SET_HOLE),
		HAND (SET_HAND_FINAL);
		
		public static final int			FIELDS					= values().length;
		
		private final NamedSymbol[] valueSet;
		
		private IdealDataFinal (NamedSymbol[] set) {
			valueSet = set;
		}

		@Override
		public int index () {
			return ordinal();			
		}

		@Override
		public Data[] allDataFields() {
			return values();
		}

		@Override
		public NamedSymbol[] allValues() {
			return valueSet;
		}
		
	}
	
	
	public StringBuilder[] preflopPlayerActions;
	public NamedSymbol<PostflopBucket> boardBucket = null;
	public NamedSymbol[] handBucket = null;
	public HandEstimations[] handEstimations = new HandEstimations[Holdem.MAX_PLAYERS];
	public int originalRoundIndex = -1;
	
	
	static {
		for (int o = 0; o < MAX_CARDINALITY; o++) {
			NamedSymbol<Integer> intSymbol = new NamedSymbol<Integer>(o, o);
			for (int s = 3; s <= MAX_CARDINALITY; s++) {
				if (SET_CARDINAL[s] == null)
					SET_CARDINAL[s] = new NamedSymbol[s];
				if (o < s)
					SET_CARDINAL[s][o] = intSymbol;
			}
		}
		for (int p = 2; p < MAX_CARDINALITY; p++)
			SET_PLAYERS[p-2] = new NamedSymbol<Integer>(p, p - 2);
		for (int b = 0; b < SET_HOLE.length; b++)
			SET_HOLE[b] = new NamedSymbol<String>("BUCKET_"+b, b);
		for (int h = 0; h < SET_HAND_INTERIM.length; h++)
			MAP_HAND_INTERIM_TO_FINAL[h] = bucketSymbol(((PostflopBucket) (SET_HAND_INTERIM[h].entity)).hand,
					PostflopBucket.UNUSED, ((PostflopBucket) (SET_HAND_INTERIM[h].entity)).kicker, SET_HAND_FINAL).intValue();
/*		for (int h = 0; h < SET_HAND_FINAL.length; h++)
			HistogramSymbolicHandModel.setName(h, "PREDICTED_"+SET_HAND_FINAL[h].toString());*/
	}
	
	static public NodeList learningNodes (Net net) throws NeticaException {
		NodeList nodes = new NodeList(net);
		nodes.add(net.getNode(IdealDataFinal.ACTION.toString()));
		nodes.add(net.getNode(InputData.PREFLOP_VPIP_PFR.toString()));
		nodes.add(net.getNode(InputData.POTRATIO.toString()));
		nodes.add(net.getNode(InputData.ROUND_PLAYERRAISES.toString()));
		return nodes;
	}

	static public int bucket (double x, double upper, int buckets) {
		if (upper == 0)
			return 0;
		int result = (int) (x * buckets / upper);
		if (DEBUG)
			System.out.println("Bucket for "+x+"/"+upper+" = "+result+" out of "+buckets);
		return (result >= buckets) ? buckets - 1 :
				(result < 0) ? 0 :
				result;
	}
	
	static private String field0(int index) {
		if (index >= InputData.FIELDS)
			return IdealDataFinal.values()[index - InputData.FIELDS].toString();
		else
			return InputData.values()[index].toString();
	}

	static private NamedSymbol inputAsSymbol0(double[] input, int index) {
		return InputData.values()[index].valueSet[(int) input[index]];
	}

	static public NamedSymbol idealAsSymbol0(double[] ideal, int index) {
		int round = (int) (Math.abs(ideal[IdealDataFinal.HAND.index()]) / MASK_ROUND);
		if (ideal[index] < 0)
			return NA;
		else {
			Data[] values = (round == ROUND_RIVER) ? IdealDataFinal.values() : IdealDataInterim.values();
			return values[index].allValues()[(int)
					((index == IdealDataFinal.HAND.index()) ? ideal[index] % MASK_ROUND : ideal[index])];
		}
	}
	
	static private NamedSymbol[] bucketSymbols (String[] hands, boolean hasPotential) {
		List<NamedSymbol<PostflopBucket>> holes = new ArrayList<>();
		if (DEBUG)
			System.out.println("Symbols for "+Arrays.deepToString(hands));
		int id = 0;
		for (int h = 0; h < hands.length; h++) {
			if (h <= 2) {
				for (String potential : (hasPotential ? 
						new String[] {"NOSTRAIGHTFLUSH", "STRAIGHTFLUSH3", "STRAIGHTFLUSH4"} : new String[] {null})) {
					if (h <= 1) {
						holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, "LO"), id++));
						holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, "MID"), id++));
						holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, "HI"), id++));
					} else {
						holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, "LO"), id++));
						holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, "HI"), id++));
					}
				}
			} else if (h == 3) {
				for (String potential : (hasPotential ? 
						new String[] {"STRAIGHTFLUSH_AtMost_3", "STRAIGHTFLUSH4"} : new String[] {null})) {
					holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, null), id++));
				}
			} else if (h == 4) {
				for (String potential : (hasPotential ? 
						new String[] {"FLUSH_AtMost_3", "FLUSH4"} : new String[] {null})) {
					holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], potential, null), id++));
				}
			} else {
				holes.add(new NamedSymbol<>(new PostflopBucket(h, hands[h], null, null), id++));
			}
		}
		NamedSymbol[] symbols = new NamedSymbol[holes.size()];
		for (int s = 0; s < holes.size(); s++) {
			symbols[s] = holes.get(s);
			if (DEBUG)
				System.out.println("  "+symbols[s]);
		}
		return symbols;
	}
	
	static public NamedSymbol<PostflopBucket> bucketSymbol (int hand, int potential, int kicker, NamedSymbol<PostflopBucket>[] set) {
		for (int s = set.length - 1; s >= 0; s--) {
			NamedSymbol<PostflopBucket> bucket = set[s];
			if (((bucket.entity.hand == hand) || (hand == PostflopBucket.UNUSED) || (bucket.entity.hand == PostflopBucket.UNUSED)) &&
					((bucket.entity.potential == potential) || (potential == PostflopBucket.UNUSED) || (bucket.entity.potential == PostflopBucket.UNUSED)) &&
					((bucket.entity.kicker == kicker) || (kicker == PostflopBucket.UNUSED) || (bucket.entity.kicker == PostflopBucket.UNUSED)))
				return bucket;
		}
		return null;
	}
	
	static public NamedSymbol<PostflopBucket> bucketSymbol (HandTexture texture, int maxRank, NamedSymbol<PostflopBucket>[] set) {
		int hand = Math.min(texture.ranking / HandEvaluator.ID_GROUP_SIZE, maxRank);
		int kicker = -1;
		int kicker1 = (int) (texture.ranking % HandEvaluator.ID_GROUP_SIZE);
		int kicker2 = 0;
		int potential;
		if (hand <= 2) {
			if ((texture.flushOuts4 > 0) || (texture.straightOuts4 > 0))
				potential = PostflopBucket.potentials.indexOf("STRAIGHTFLUSH4");
			else if ((texture.flushOuts3 > 0) || (texture.straightOuts3 > 0))
				potential = PostflopBucket.potentials.indexOf("STRAIGHTFLUSH3");
			else
				potential = PostflopBucket.potentials.indexOf("NOSTRAIGHTFLUSH");
			if (hand == 0) {
				do {
					kicker1 /= (int) HandEvaluator.NUM_RANKS;
				} while (kicker1 > 12);
				if (kicker1 >= 10)
					kicker = PostflopBucket.kickers.indexOf("HI");
				else if (kicker1 >= 5)
					kicker = PostflopBucket.kickers.indexOf("MID");
				else
					kicker = PostflopBucket.kickers.indexOf("LO");
			} else if (hand == 1) {		// LO, MID, HI
				kicker1 /= (HandEvaluator.NUM_RANKS * HandEvaluator.NUM_RANKS * HandEvaluator.NUM_RANKS);
				if (kicker1 >= 10)
					kicker = PostflopBucket.kickers.indexOf("HI");
				else if (kicker1 >= 5)
					kicker = PostflopBucket.kickers.indexOf("MID");
				else
					kicker = PostflopBucket.kickers.indexOf("LO");
			} else {					// LO, HI
				kicker2 = kicker1 / (HandEvaluator.NUM_RANKS * HandEvaluator.NUM_RANKS);
				kicker1 = (int) (kicker1 % (HandEvaluator.NUM_RANKS * HandEvaluator.NUM_RANKS) / HandEvaluator.NUM_RANKS);
				if ((kicker1 >= 10) || (kicker2 >= 10))
					kicker = PostflopBucket.kickers.indexOf("HI");
				else
					kicker = PostflopBucket.kickers.indexOf("LO");
			}
		} else if (hand == 3) {
			if ((texture.flushOuts4 > 0) || (texture.straightOuts4 > 0))
				potential = PostflopBucket.potentials.indexOf("STRAIGHTFLUSH4");
			else
				potential = PostflopBucket.potentials.indexOf("STRAIGHTFLUSH_AtMost_3");				
		} else if (hand == 4) {
			if (texture.flushOuts4 > 0)
				potential = PostflopBucket.potentials.indexOf("FLUSH4");
			else
				potential = PostflopBucket.potentials.indexOf("FLUSH_AtMost_3");				
		} else
			potential = -1;
		NamedSymbol<PostflopBucket> bucket = bucketSymbol (hand, potential, kicker, set);
		if (bucket == null)
			throw new RuntimeException ("BNGameAdapter.bucketSymbol(): Unclassified hand "+texture);
		if (DEBUG)
			System.out.println(bucket+" ("+texture+", "+kicker1+", "+kicker2+")");
		return bucket;
	}
	
	@SuppressWarnings("deprecation")
	static public Caseset asCaseset(List<double[]> input, List<double[]> ideal) throws NeticaException {
		StringBuilder str = new StringBuilder();
		for (int a = 0; a < InputData.FIELDS + IdealDataFinal.FIELDS; a++) {
			str.append(field0(a));
			str.append(' ');
		}
		str.append('\n');
		for (int l = 0; l < input.size(); l++) {
			for (int a = 0; a < InputData.FIELDS + IdealDataFinal.FIELDS; a++) {
				str.append((a >= InputData.FIELDS) ? idealAsSymbol0(ideal.get(l), a - InputData.FIELDS) :
						inputAsSymbol0(input.get(l), a));
				str.append(' ');
			}		
			str.append('\n');
		}
		if (DEBUG)
			System.out.println(str);
		Streamer streamer = new Streamer(new java.io.StringBufferInputStream(str.toString()),
				"data"+casesetId.getAndIncrement()+".txt", Environ.getDefaultEnviron());
		Caseset cases = new Caseset();
		cases.addCases(streamer, 1, null);
		streamer.finalize();
		return cases;
	}

	
	public BNGameAdapter(boolean r) {
		super(r);
	}
	
	public BNGameAdapter(GameAdapter other, Object game) {
		super (other, game);
		if (other instanceof BNGameAdapter) {
			originalRoundIndex = other.roundIndex;
			if (((BNGameAdapter) other).boardBucket != null)
				boardBucket = ((BNGameAdapter) other).boardBucket;
			handBucket = ((BNGameAdapter) other).handBucket.clone();
			preflopPlayerActions = new StringBuilder[((BNGameAdapter) other).preflopPlayerActions.length];
			for (int i = 0; i < ((BNGameAdapter) other).preflopPlayerActions.length; i++)
				if (((BNGameAdapter) other).preflopPlayerActions[i] != null)
					preflopPlayerActions[i] = new StringBuilder(((BNGameAdapter) other).preflopPlayerActions[i]);
			for (int p = 0; p < Holdem.MAX_PLAYERS; p++)
				if (((BNGameAdapter) other).handEstimations[p] != null)
					handEstimations[p] = new HandEstimations (((BNGameAdapter) other).handEstimations[p]);
		}
	}
	
	
	public void calculateHandBuckets () {
		for (int s = 0; s < dealtPlayers; s++) {
			if ((hole[s] != null) && (hole[s][0] != null))  {
				if (DEBUG)
					System.out.print("Calculating bucket for hand "+Arrays.deepToString(hole[s]));
				if (roundIndex <= ROUND_TURN)
					handBucket[s] = bucketSymbol(new HandTexture(s), 6, SET_HAND_INTERIM);
				else
					handBucket[s] = bucketSymbol(new HandTexture(s), 6, SET_HAND_FINAL);
			}
		}
	}
	
	@Override
	public void onRoundStart(Object game) {
		super.onRoundStart(game);
		if (DEBUG)
			System.out.println("BNGameAdapter: New Round: "+roundIndex+" ("+seatToAct+") "+Thread.currentThread().getName());
		handBucket = new NamedSymbol[dealtPlayers];
		if (roundIndex == ROUND_PREFLOP) {
			boardBucket = null;
			preflopPlayerActions = new StringBuilder[dealtPlayers];
		} else {
			if (DEBUG)
				System.out.print("Bucket for board "+Arrays.deepToString(board)+": ");
			boardBucket = bucketSymbol(getBoardTexture(), 3, SET_BOARD);
			calculateHandBuckets();
		}
	}

	@Override
	public void onAction(Object game, char action) {
		super.onAction(game, action);
		if (roundIndex == ROUND_PREFLOP) {
			preflopPlayerActions[seatToAct] = new StringBuilder (playerActions[seatToAct]);
		}
	}

	@Override
	protected void assertValidity() throws AbortException {
		super.assertValidity();
		if (! (
				(preflopPlayerActions != null)
				)) {
			throw new AbortException ("BNGameAdapter failed validation!");
		}
	}
	
	@Override
	public void collectInputs (char action, double[] input) throws AbortException {
		String preflopActions = (preflopPlayerActions[seatToAct] != null) ? preflopPlayerActions[seatToAct].toString() : "";
		String roundActions = (playerActions[seatToAct] != null) ? playerActions[seatToAct].toString() : "";
		assertValidity ();
		
		input[InputData.PREFLOP_VPIP_PFR.index()] = (preflopActions.contains("r") ? PFR.intValue() : 
				(preflopActions.contains("c")) ? VPIP_NO_PFR.intValue() : NO_VPIP.intValue());
		input[InputData.BOARD.index()] = boardBucket.intValue();
		input[InputData.REMAININGPLAYERS.index()] = numActivePlayers - 2;
		input[InputData.DEALTPLAYERS.index()] = dealtPlayers - 2;
		input[InputData.PLAYERRELPOSITION.index()] = (seatToAct == dealtPlayers - 1) ? BUTTON.intValue() :
				(seatToAct <= 1) ? BLIND.intValue() :
				(seatToAct == dealtPlayers - 2 ) ? LATE_POS.intValue() :
				EARLY_MIDDLE_POS.intValue();
		input[InputData.POTRATIO.index()] = bucket(2 * inPot, sumPots, InputData.POTRATIO.allValues().length);
		input[InputData.POTODDS.index()] = bucket(2 * amountToCall, sumPots + amountToCall, InputData.POTODDS.allValues().length);
		int rr = 0;
		for (StringBuilder r : playerActions)
			if (r != null)
				rr += occurrences (r, 'r') + occurrences (r, 'b');
		input[InputData.ROUND_RAISES.index()] = bucket(rr, numActivePlayers,InputData.ROUND_RAISES.allValues().length); 
		int rk = 0;
		for (StringBuilder r : playerActions)
			if (r != null)
				rk += occurrences (r, 'k');
		input[InputData.ROUND_CHECKS.index()] = bucket(rk, numActivePlayers, InputData.ROUND_CHECKS.allValues().length);
		int playerRaises = occurrences(roundActions, 'b') +	occurrences(roundActions, 'r'); 
		input[InputData.ROUND_PLAYERRAISES.index()] = Math.min(playerRaises, InputData.ROUND_PLAYERRAISES.allValues().length - 1);
		input[InputData.COMMITTED.index()] = (playerRaises + occurrences(roundActions, 'c')) == 0 ?
				FALSE.intValue() : TRUE.intValue();
		if (report)
			System.out.println (id + ": " + ArrayTools.deepToString(InputData.values(), input));
		if (action != ACTION_NA)
			lastAction = action;
	}

	@Override
	public void collectIdeals(char action, double[] ideal) throws AbortException {
		super.collectIdeals(action, ideal);
		calculateHandBuckets();
		switch (action) {
		case 'f':
			ideal[IdealDataFinal.ACTION.index()] = FOLD.intValue();
			break;
		case 'b':
		case 'r':
			ideal[IdealDataFinal.ACTION.index()] = RAISE.intValue();
			break;
		case 'c':
		case 'k':
			ideal[IdealDataFinal.ACTION.index()] = CHECKORCALL.intValue();
			break;
		case ACTION_NA:
			ideal[IdealDataFinal.ACTION.index()] = INVALID;
			break;
		case 'Q':
		case 'K':
			break;
		default:
			throw new RuntimeException ("Not permitted action in BNGameAdapter.collectIdeals()");
		}
		if ((hole[seatToAct] != null) && (hole[seatToAct][0] != null)) {
			ideal[IdealDataFinal.HOLE.index()] = Bucket.getPreflopBucket(hole[seatToAct]);
			ideal[IdealDataFinal.HAND.index()] = handBucket[seatToAct].intValue()
					+ roundIndex * MASK_ROUND;
		} else {
			ideal[IdealDataFinal.HOLE.index()] = INVALID;
			ideal[IdealDataFinal.HAND.index()] = - (roundIndex * MASK_ROUND);
		}
		
		if (report)
			System.out.println (id + ": " + ArrayTools.deepToString(
					(roundIndex == ROUND_RIVER) ? IdealDataFinal.values() : IdealDataInterim.values(), ideal));
	}
	
	@Override
	public String field(int index) {
		return field0(index);
	}

	@Override
	public NamedSymbol inputAsSymbol(double[] input, int index) {
		return inputAsSymbol0(input, index);
	}

	@Override
	public NamedSymbol idealAsSymbol(double[] ideal, int index) {
		return idealAsSymbol0(ideal, index);
	}
	
	@Override
	public NamedSymbol[] allSymbols (int index) {
		throw new RuntimeException ("BNGameAdapter.allSymbols() not supported");
	}

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\n  Preflop player actions: ").append(Arrays.deepToString(preflopPlayerActions)).append('\n');
		sb.append("  Board bucket: ").append((boardBucket != null) ? boardBucket.entity : '-');
		if (handBucket[seatToAct] != null)
			sb.append(", hand bucket: ").append(handBucket[seatToAct].entity);
		return sb.toString();
	}

}
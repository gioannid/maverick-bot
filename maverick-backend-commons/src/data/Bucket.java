package data;

import java.util.Arrays;

import pokerai.game.eval.twoplustwo.Evaluator;

import ca.ualberta.cs.poker.free.dynamics.Card;

public class Bucket {
	
	private static final boolean DEBUG = false;
	
	public static final int DECK_DEPTH = Card.DECKSIZE;
	public static final int BUCKET_COUNT = 5;
	public static final int ALL_PAIRS = 52*51;
	public static final int[] PAIRS_PER_BUCKET = {1736, 376, 288, 196, 56};
	public static final double[] PROB_PER_BUCKET = {((double) PAIRS_PER_BUCKET[0])/ALL_PAIRS, 
													((double) PAIRS_PER_BUCKET[1])/ALL_PAIRS,
													((double) PAIRS_PER_BUCKET[2])/ALL_PAIRS,
													((double) PAIRS_PER_BUCKET[3])/ALL_PAIRS,
													((double) PAIRS_PER_BUCKET[4])/ALL_PAIRS};
	public static final double[] CUMUL_PROB_PER_BUCKET = {PAIRS_PER_BUCKET[0]/((double) ALL_PAIRS), 
			(PAIRS_PER_BUCKET[0]+PAIRS_PER_BUCKET[1])/((double) ALL_PAIRS),
			(PAIRS_PER_BUCKET[0]+PAIRS_PER_BUCKET[1]+PAIRS_PER_BUCKET[2])/((double) ALL_PAIRS),
			(PAIRS_PER_BUCKET[0]+PAIRS_PER_BUCKET[1]+PAIRS_PER_BUCKET[2]+PAIRS_PER_BUCKET[3])/((double) ALL_PAIRS),
			1.0};
//	public static final double PAIRS_PER_BUCKET_GEOM_MEAN = 290.34532015443612341064808676036;

	public static Evaluator evaluator = Evaluator.getInstance("data/handrank.lut");

	public static final int[][] buckets = {
			{ 4, 4, 3, 3, 2, 2, 1, 1, 1, 1, 1, 1, 1 },
			{ 3, 4, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0 },
			{ 3, 3, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0 },
			{ 3, 3, 2, 4, 2, 1, 0, 0, 0, 0, 0, 0, 0 },
			{ 2, 2, 2, 2, 3, 2, 1, 0, 0, 0, 0, 0, 0 },
			{ 2, 2, 1, 1, 2, 3, 2, 1, 0, 0, 0, 0, 0 },
			{ 1, 1, 0, 0, 0, 1, 3, 1, 1, 0, 0, 0, 0 },
			{ 1, 0, 0, 0, 0, 0, 1, 2, 1, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 1, 2, 1, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 } };
	
	public static final int[][] ranking = {								/*  s */
			/*  A    K    Q    J    T    9    8    7    6    5    4    3    2 */    
	/*A*/	{   0,   4,   6,   7,  14,  33,  34,  35,  36,  37,  38,  39,  40 },  
	/*K*/	{   9,   1,   8,  13,  23,  49,  61,  62,  63,  64,  65,  66,  67 },  
	/*Q*/	{  15,  17,   2,  12,  19,  26,  69,  85,  86,  87,  88,  89,  91 },  
	/*J*/	{  22,  28,  29,   3,  11,  21,  50,  79,  92,  93,  94,  95,  97 },  
	/*T*/	{  43,  46,  47,  30,   5,  16,  27,  68,  90, 101, 103, 104, 105 },  
	/*9*/	{  71,  83,  72,  53,  55,  10,  20,  32,  76,  96, 110, 111, 114 },  
	/*8*/	{  99, 115, 117,  78,  84,  58,  18,  25,  45,  77, 102, 118, 120 },  
	/*7*/	{ 107, 123, 130, 128, 124, 121,  70,  24,  31,  51,  82, 108, 122 },  
	/*6*/	{ 116, 125, 136, 146, 139, 133, 126,  73,  42,  41,  54,  98, 113 },  
	/*5*/	{ 106, 127, 140, 148, 156, 149, 138, 129,  80,  44,  48,  56, 100 },  
	/*4*/	{ 109, 131, 142, 151, 157, 163, 155, 144, 135,  81,  52,  59,  74 },  
	/*3*/	{ 112, 132, 143, 152, 159, 164, 166, 160, 147, 137, 141,  57,  75 },  
/*o   2*/	{ 119, 134, 145, 154, 161, 165, 167, 168, 162, 150, 153, 158,  60 }  
	};
	

	private final static int AHEAD = 0;
	private final static int TIED = 1;
	private final static int BEHIND = 2;

	private static int[] suitCounts;
	private static int[] rankCounts;
	
	private int preflopBucket = -1;
	private int postflopBucket = -1;
	private int rank = -1;
	private int players = -1;
	private int[] hole;
	private int[] board;
	private Card[] holeCards;
	private Card[] boardCards;
	private double handStrength = -1;
	private double positivePotential = -1;
	private double effectiveHandStrength = -1;
	

	public static int getPreflopBucket (int rank1, int rank2, boolean suited) {
		// If rank of c2 greater c1 switch
		if (rank2 > rank1) {
			int temp = rank1;
			rank1 = rank2;
			rank2 = temp;
		}
		if (suited)
			return buckets[12-rank1][12-rank2];
		else
			return buckets[12-rank2][12-rank1];
	}

	public static int getPreflopBucket(Card[] cards) {
		if (cards == null || cards.length != 2 || cards[0] == null || cards[1] == null)
			return -1;
		return getPreflopBucket (cards[0].rank.index, cards[1].rank.index, (cards[0].suit == cards[1].suit));
	}
	
	public static int getPreflopBucket (int card1, int card2) {
		return getPreflopBucket (CardGenerator.rank(card1), CardGenerator.rank(card2), 
				(CardGenerator.suit(card1) == CardGenerator.suit(card2)));
	}

	public static int getPreflopRanking (int rank1, int rank2, boolean suited) {
		// If rank of c2 greater c1 switch
		if (rank2 > rank1) {
			int temp = rank1;
			rank1 = rank2;
			rank2 = temp;
		}
		if (suited)
			return ranking[12-rank1][12-rank2];
		else
			return ranking[12-rank2][12-rank1];
	}

	public static int getPreflopRanking(Card[] cards) {
		if (cards == null || cards.length != 2 || cards[0] == null || cards[1] == null)
			return -1;
		return getPreflopRanking (cards[0].rank.index, cards[1].rank.index, (cards[0].suit == cards[1].suit));
	}
	
	public static int getPreflopRanking (int card1, int card2) {
		return getPreflopRanking (CardGenerator.rank(card1), CardGenerator.rank(card2), 
				(CardGenerator.suit(card1) == CardGenerator.suit(card2)));
	}

	public static int[] getFlopTurnRiver(Card[] board, Card[] cards) {
		int len = board.length;
		for (int i = 0; i < board.length; i++) {
			if (board[i] == null)
				len--;
		}
		Card[] all_cards = new Card[len + cards.length];
		for (int i = 0; i < len; i++) {
			all_cards[i] = board[i];
		}
		for (int i = 0; i < cards.length; i++) {
			all_cards[i + len] = cards[i];
		}
		suitCounts = new int[Card.Suit.values().length];
		rankCounts = new int[Card.Rank.values().length];
		for (int i = 0; i < all_cards.length; i++) {
			suitCounts[all_cards[i].suit.index]++;
			rankCounts[all_cards[i].rank.index]++;
		}

		if (cards.length != 2)
			return new int[] { -1, -1, -1 };
		Card c1 = cards[0];
		Card c2 = cards[1];
		// If rank of c2 greater c1 switch
		if (c2.rank.index > c1.rank.index) {
			Card temp = c1;
			c1 = c2;
			c2 = temp;
		}
		int[] values = new int[] { -1, -1, -1 };
		int length = all_cards.length;
		// flop
		if (length == 5)
			values[0] = 0;
		// turn
		if (length == 6)
			values[1] = 0;
		// river
		if (length == 7)
			values[2] = 0;

		if (testContainsFlush())
			return new int[] { 0, 0, 13 };
		if (testContainsStraight())
			return new int[] { 0, 0, 12 };

		int mostFrequentRank = -1;
		int secondMostFrequentRank = -1;
		for (int i = 0; i < rankCounts.length; i++) {
			if ((mostFrequentRank == -1)
					|| rankCounts[i] >= rankCounts[mostFrequentRank]) {
				secondMostFrequentRank = mostFrequentRank;
				mostFrequentRank = i;
			} else if ((secondMostFrequentRank == -1)
					|| rankCounts[i] >= rankCounts[secondMostFrequentRank]) {
				secondMostFrequentRank = i;
			}
		}
		boolean bh = false;
		boolean sh = false;
		boolean kicker = false;
		if (c1.rank.index >= Card.Rank.KING.index)
			kicker = true;
		if (rankCounts[mostFrequentRank] > 1) {
			if (c1.rank.index == mostFrequentRank) {
				bh = true;
			}
			else if (c2.rank.index == mostFrequentRank) {
				sh = true;
			}
			if (rankCounts[secondMostFrequentRank] > 1) {
				if (c1.rank.index == secondMostFrequentRank) {
					bh = true;
				}
				else if (c2.rank.index == secondMostFrequentRank) {
					sh = true;
				}
			}
		}
		if(!sh && bh && c1.rank.index < Card.Rank.JACK.index) {
			sh = true;
			bh = false;
		}
		if (rankCounts[mostFrequentRank] == 4) {
			values = new int[] { 3, 3, 3 };
		} else if (rankCounts[mostFrequentRank] == 3
				&& rankCounts[secondMostFrequentRank] >= 2) {
			values = new int[] { 3, 3, 2 };
		} else if (rankCounts[mostFrequentRank] == 3) {
			if (bh) {
				values[0] = 3;
				values[1] = 3;
			} else if (sh) {
				values[0] = 2;
				values[1] = 2;
			} else {
				if (kicker) {
					values[0] = 2;
				}
			}
		} else if (rankCounts[mostFrequentRank] == 2) {
			if (rankCounts[secondMostFrequentRank] == 2) {
				if (bh && sh) {
					values[0] = 3;
					values[1] = 2;
				} else {

					if (sh || bh) {
						values[0] = 3;
						if (kicker) {
							values[1] = 2;
						}
					} else {
						if (kicker) {
							values[0] = 2;
						}
					}
				}
			} else {
				if (bh) {
					values[0] = 3;
				} else if (sh) {
					values[0] = 2;
				} else {
					if (kicker) {
						values[0] = 2;
					}
				}
			}
		}
		if (length < 7) {
			if (testContainsFlushDraw() || testContainsStraightDraw()) {
				values[0]++;
			}
		}
		return values;
	}

	private static boolean testContainsStraight() {
		int runningCount = 0;
		for (int i = rankCounts.length - 1; i >= 0; i--) {
			if (rankCounts[i] > 0) {
				runningCount++;
				if (runningCount == 5) {
					return true;
				}
			} else {
				runningCount = 0;
			}
		}
		if (runningCount == 4 && rankCounts[12] > 0) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean testContainsFlush() {
		for (int i = 0; i < suitCounts.length; i++) {
			if (suitCounts[i] >= 5) {
				return true;
			}
		}
		return false;
	}

	private static boolean testContainsFlushDraw() {
		for (int i = 0; i < suitCounts.length; i++) {
			if (suitCounts[i] >= 4) {
				return true;
			}
		}
		return false;
	}

	private static boolean testContainsStraightDraw() {
		int runningCount = 0;
		for (int i = rankCounts.length - 1; i >= 0; i--) {
			if (rankCounts[i] > 0) {
				runningCount++;
				if (runningCount == 4) {
					return true;
				}
			} else {
				runningCount = 0;
			}
		}
		return false;
	}
	
	static private CardGenerator newDeck (int[] hole, int[] board) {
		CardGenerator deck = new CardGenerator();
		deck.removeCard(Evaluator.getCard(hole[0]));		
		deck.removeCard(Evaluator.getCard(hole[1]));
		for (int b = 0; b < board.length; b++)
			deck.removeCard(Evaluator.getCard(board[b]));		
		return deck;
	}
	
	/**
  	 * Calculates the probability of having 
  	 * the best hand against several opponents.
	 * @param hole the hole, encoded per 2p2 Evaluator
	 * @param board the board, encoded per 2p2 Evaluator
	 * @param np the number of active opponents in the hand	 
	 * @return probability of having the best hand.
	 */
	static public double handStrength(int[] hole, int[] board, int np) {
		double HR = handStrength(hole, board);
		double H = HR; 
		for (int j = 0; j < np - 1; j++) 
			H *= HR;
		return H;
	}
		
	/**
  	 * Calculates the probability of having 
  	 * the best hand against one opponent.
	 * @param hole the hole, encoded per 2p2 Evaluator
	 * @param board the board, encoded per 2p2 Evaluator
	 * @return probability of having the best hand.
	 */
	static public double handStrength(int[] hole, int[] board) {
		int good = 0;
		int bad = 0;
		int tied = 0;
		int[] xxHole = new int[2];
		int myRank = evaluator.handRank(hole, board);
		CardGenerator deck = newDeck (hole, board);
	
		for (int i = 0; i < DECK_DEPTH - deck.deckPos; i++) {
			xxHole[0] = Evaluator.index(deck.upcoming(i));
			for (int j = i + 1; j < DECK_DEPTH - deck.deckPos; j++) {
				xxHole[1] = Evaluator.index(deck.upcoming(j));
				int xxRank = evaluator.handRank(xxHole, board);
				if (myRank > xxRank) 
					good++;
				else if (myRank < xxRank) 
					bad++;
				else 
					tied++;
			}
		}
		return (double)((double)(good+(double)(tied/2))/(double)(good+bad+tied));
	}

	/** 
	 * Calculate the PPot and NPot of a hand. (Papp 1998, 5.3). 
	 * @param hole the hole, encoded per 2p2 Evaluator
	 * @param board the board, encoded per 2p2 Evaluator
	 * @param full if true, a full 2-card look ahead will be done (slow)
	 * @param result an double array that, upon completion, will contain [ppot, npot]
	 */
	static public void calculatePotential(int[] hole, int[] board, boolean full, double[] result) {
		double[][] 	HP = new double[3][3];
		double[] 	HPTotal = new double[3];
		int			ourrank7,opprank;
		boolean		TwoCardLookAhead = (board.length==3 && full);
		int[]		futureBoard = Arrays.copyOf(board, 5);
		int[]		opHand = new int[2];
		int			index;
		CardGenerator deck = newDeck (hole, board);
		int ourrank5 = evaluator.handRank(hole, board);

		if (board.length == 5) {
			result[0] = 0;
			result[1] = 0;
			return;
		}
		
		// tally all possiblities
		for (int i = 0; i < DECK_DEPTH - deck.deckPos; i++) {
			opHand[0] = Evaluator.index(deck.upcoming(i));

			for (int j = i + 1; j < DECK_DEPTH - deck.deckPos; j++) {
				opHand[1] = Evaluator.index(deck.upcoming(j));			

				opprank = evaluator.handRank(opHand, board);
				if (ourrank5 > opprank) 
					index = AHEAD;
				else if (ourrank5 == opprank)
					index = TIED;
				else 
					index = BEHIND;
				HPTotal[index]++;

				// tally all possiblities
				for (int k = 0; k < DECK_DEPTH - deck.deckPos; k++) {
					if (i == k || j == k) 
						continue;
					futureBoard[board.length] = Evaluator.index(deck.upcoming(k));

					if (TwoCardLookAhead) {
						for (int l = k + 1; l < DECK_DEPTH - deck.deckPos; l++) {
							if (i == l || j == l) 
								continue;
							futureBoard[4] = Evaluator.index(deck.upcoming(l));
							ourrank7 = evaluator.handRank(hole, futureBoard);
							opprank = evaluator.handRank(opHand, futureBoard);
							if (ourrank7 > opprank) 
								HP[index][AHEAD]++;
							else if (ourrank7 == opprank) 
								HP[index][TIED]++;	
							else 
								HP[index][BEHIND]++;
						}
					} else {
						ourrank7 = evaluator.handRank(hole, futureBoard);
						opprank = evaluator.handRank(opHand, futureBoard);
						if (ourrank7 > opprank) 
							HP[index][AHEAD]++;
						else if (ourrank7 == opprank) 
							HP[index][TIED]++;	
						else 
							HP[index][BEHIND]++;
					}
				}
			}
		}
	
		int mult = (TwoCardLookAhead ? 990 : 45);
		double den1 = (mult*(HPTotal[BEHIND] + (HPTotal[TIED]/2.0)));
		double den2 = (mult*(HPTotal[AHEAD] + (HPTotal[TIED]/2.0)));
		if (den1 > 0) 
			result[0] = (HP[BEHIND][AHEAD] + (HP[BEHIND][TIED]/2) + (HP[TIED][AHEAD]/2)) / (double)den1;
		else 
			result[0] = 0;	
		if (den2 > 0) 
			result[1] = (HP[AHEAD][BEHIND] + (HP[AHEAD][TIED]/2) + (HP[TIED][BEHIND]/2)) / (double)den2;
		else 
			result[1] = 0;
		
		if (DEBUG) {
			System.out.println("AHEAD ==> AHEAD = " + HP[AHEAD][AHEAD]);
			System.out.println("AHEAD ==> TIED = " + HP[AHEAD][TIED]);
			System.out.println("AHEAD ==> BEHIND = " + HP[AHEAD][BEHIND]);
			System.out.println("TOTAL AHEAD = " + HPTotal[AHEAD] + "\n");

			System.out.println("TIED ==> AHEAD = " + HP[TIED][AHEAD]);
			System.out.println("TIED ==> TIED = " + HP[TIED][TIED]);
			System.out.println("TIED ==> BEHIND = " + HP[TIED][BEHIND]);
			System.out.println("TOTAL TIED = " + HPTotal[TIED] + "\n");

			System.out.println("BEHIND ==> AHEAD = " + HP[BEHIND][AHEAD]);
			System.out.println("BEHIND ==> TIED = " + HP[BEHIND][TIED]);
			System.out.println("BEHIND ==> BEHIND = " + HP[BEHIND][BEHIND]);		
			System.out.println("TOTAL BEHIND = " + HPTotal[BEHIND] + "\n");
		}

	}

	/**
	 * A crude but fast approximation of PPOT.
	 * @param hole the hole, encoded per 2p2 Evaluator
	 * @param board the board, encoded per 2p2 Evaluator
	 * @return an estimation of the probability of having the best hand
	 */
	static public double estimatePPot (int[] hole, int[] board) {
		double outs = 0;
		int top_r = Evaluator.DEUCE;
		int board_pair = -1;
		int board_cards = board.length;
		int[] num_suit = new int[Evaluator.NUM_SUITS];
		int[] present = new int[Evaluator.NUM_RANKS];

		if (board.length == 5) {
			return 0;
		}

		for (int i=0; i < board.length; i++) {
			int rk = Evaluator.rank(board[i]);
			if (rk > top_r) 
				top_r = rk;
			present[rk]++;
			if (present[rk] > 1)
				board_pair = rk;
			num_suit[Evaluator.suit(board[i])]++;
		}

		// - don't care about overpair
		if (Evaluator.rank(hole[0]) != Evaluator.rank(hole[1])) {
			for (int i = 0; i < 2; i++) {

				int ni = ((i == 0) ? 1 : 0);
				int rk = Evaluator.rank(hole[i]);

				if (rk > top_r)
					outs += 0.9*(Evaluator.NUM_SUITS-1);
				else if (top_r > Evaluator.rank(hole[ni]) && top_r > rk && 
						present[Evaluator.rank(hole[ni])]>0 && present[rk]==0)
					outs += 0.95*(Evaluator.NUM_SUITS-1);
				else if (rk < top_r && present[rk]==0) 
					outs += 0.6*(Evaluator.NUM_SUITS-1);
				else if (present[rk]==1 && board_pair < rk) 
					outs += 0.95*(Evaluator.NUM_SUITS - 1 - present[rk]);
				else if (present[rk]==2 && board_pair == rk) 
					outs += 0.95 * (Evaluator.NUM_SUITS-1) * (board_cards - 2);
			}
		} else {
			if (present[Evaluator.rank(hole[0])] != 0) {
				if (board_pair == 0) {
					outs += 0.9*(Evaluator.NUM_SUITS-1)*(board_cards-1);
				}
			} else {
				outs += 0.99*(Evaluator.NUM_SUITS-2);
				if (top_r < Evaluator.rank(hole[0]) && board_pair==0)
					outs += 0.25*(Evaluator.NUM_SUITS-1)*board_cards;
			}
		}

		int[] num_suit_hole = new int[] {0,0,0,0};
		int[] rank_suit_hole = new int[Evaluator.NUM_SUITS];
		for (int i=0; i < 2; i++) {
			num_suit_hole[Evaluator.suit(hole[i])]++;
			rank_suit_hole[Evaluator.suit(hole[i])] = Evaluator.rank(hole[i]);
		}

		for (int s = 0; s < Evaluator.NUM_SUITS; s++) {
			if (num_suit[s]+num_suit_hole[s]==4) {
				if (num_suit_hole[s]==1)
					outs += (0.5+0.5*rank_suit_hole[s]/Evaluator.NUM_RANKS)*(Evaluator.NUM_RANKS-4);
				else if (num_suit_hole[s]==2)
					outs += 1.0*(Evaluator.NUM_RANKS-4);
			}
		}

		// + id 4str, count hole
		int gap0_straight=0;
		int gap0_hole=0;
		int gap1_straight=0;
		int gap1_hole=0;
		int r1 = Evaluator.rank(hole[0]), r2=Evaluator.rank(hole[1]);

		if (present[Evaluator.ACE]!=0) gap0_straight++;
		else if (r1==Evaluator.ACE || r2==Evaluator.ACE) {
			gap0_straight++;
			gap0_hole++;
		} else gap1_straight=1;

		for (int rk=0; rk<Evaluator.NUM_RANKS; rk++) {
			if (present[rk]!=0) { 
				gap0_straight++; 
				gap1_straight++;
			} else if (r1==rk || r2==rk) {
				gap0_straight++; gap1_straight++;
				gap0_hole++; gap1_hole++;
			} else {
				if (gap1_straight>=5) {
					if (gap1_hole==0) ;
					else if (gap1_hole==1) outs += 0.7*Evaluator.NUM_SUITS;
					else outs += 1.0*Evaluator.NUM_SUITS;
				}
				// move gap0 to gap1
				if (rk==Evaluator.ACE) { 
					gap1_straight=gap0_straight;
					gap1_hole=gap0_hole;
				} else {
					gap1_straight = gap0_straight+1;
					gap1_hole = gap0_hole;
				}
				// reset gap0
				gap0_straight=0;
				gap0_hole=0;
			}
		}

		// TERMINATE
		if (gap1_straight>=5) {
			if (gap1_hole==0);
			else if (gap1_hole==1) outs += 0.7*Evaluator.NUM_SUITS;
			else outs+=1.*Evaluator.NUM_SUITS;
		}

		return (double)outs/(double)(50-(board.length + 2));
	}

	
	public Bucket (int[] h) {
		hole = h;
		update ();
	}
	
	public Bucket (int[] h, int[] b, int p) {
		hole = h;
		board = b;
		update (p);
	}
	
	public Bucket (Card[] hole) {
		this.hole = new int[2];
		this.hole[0] = Evaluator.index(hole[0]);
		this.hole[1] = Evaluator.index(hole[1]);
		holeCards = hole;
		update ();
	}
	
	public Bucket (Card[] hole, Card[] board, int p) {
		this.hole = new int[2];
		this.hole[0] = Evaluator.index(hole[0]);
		this.hole[1] = Evaluator.index(hole[1]);
		holeCards = hole;
		setBoard (board, p);
	}
	
	public Bucket (Bucket other) {
		preflopBucket = other.preflopBucket;
		postflopBucket = other.postflopBucket;
		rank = other.rank;
		handStrength = other.handStrength;
		effectiveHandStrength = other.effectiveHandStrength;
		positivePotential = other.positivePotential;
		hole = other.hole;
		if (other.board != null)
			board = other.board.clone();
		players = other.players;
	}
	

	private void update () {
		long time;
		if (DEBUG)
			time = System.currentTimeMillis();
		postflopBucket = -1;
		rank = -1;
		handStrength = positivePotential = effectiveHandStrength = -1;
		if (hole == null) {
			preflopBucket = -1;
			return;
		}
		preflopBucket = getPreflopBucket (hole[0], hole[1]);
		if ((board != null) && (players > 0)) {
/*
			hole = new int[] {23, 31};
			board = new int[] {27, 20, 11}; // turn exact pPot=0.40691615897882216, river exact pPot=0.6193285333755307
			players = 2;
			hole = new int[] {50, 41};
			board = new int[] {39, 9, 7}; // HS=0.5846438482886216
*/			
		    rank = evaluator.handRank(hole, board);
		    handStrength = handStrength (hole, board, players-1);
		    positivePotential = estimatePPot (hole, board);
		    effectiveHandStrength = handStrength + (1 - handStrength) * positivePotential;
		    for (int b = 0; b <= 4; b++)
		    	if (effectiveHandStrength <= CUMUL_PROB_PER_BUCKET[b]) {
		    		postflopBucket = b;
		    		break;
		    	}
		    if (DEBUG)
		    	System.out.println(System.currentTimeMillis() - time + ": " + toString());
		}
	}
	
	private void update (int p) {
		if ((players != p) || ((postflopBucket == -1) && (board != null))) {
			players = p;
			update ();
		}
	}
	
	public void setBoard (int[] b, int p) {
		board = b;
		players = -1;
		update (p);
	}
	
	public void setBoard (Card[] board, int p) {
		this.board = null;
		for (int b = board.length - 1; b >= 0; b--)
			if (board[b] != null) {
				if (this.board == null)
					this.board = new int[b + 1];
				this.board[b] = Evaluator.index(board[b]);
			}
		boardCards = board;
		players = -1;
		update (p);
	}
	
	public Card[] getHole () {
		if (holeCards == null) {
			holeCards = new Card[2];
			holeCards[0] = Evaluator.getCard(hole[0]);
			holeCards[1] = Evaluator.getCard(hole[1]);
		}
		return holeCards;
	}
	
	public Card[] getBoard () {
		if (board == null)
			return null;
		else if (boardCards == null) {
			boardCards = new Card[5];
			for (int b = 0; b < board.length; b++)
				boardCards[b] = Evaluator.getCard(board[b]);
		}
		return boardCards;
	}
	
	public int getLastBucket () {
		return (postflopBucket == -1) ? preflopBucket : postflopBucket;
	}
	
	/**
	 * Maps the hand to a "bucket" number, with respect to a hard coded hole distribution
	 * (preflop), or the effective hand strength in the following cumulative ranges (postflop):
	 *  {1736/2652, 376/2652, 288/2652, 196/2652, 56/2652}
	 * @param p the number of active players in the hand	 
	 * @return the bucket number (0-5, or -1 if unknown)
	 */
	public int getBucket (int p) {
		update (p);
		return getLastBucket ();
	}
	
	/**
	 * Get a numerical ranking of this hand.
	 * Given a 1-9 card hand, will return a unique rank 
 	 * such that any two hands will be ranked with the 
 	 * better hand having a higher rank. 
	 *
	 * @param p the number of active players in the hand	 
	 * @return a unique number representing the hand strength of the best 
	 * 5-card poker hand in the given 7 cards. The higher the number, the better
	 * the hand is.
	 */
	public int getRank (int p) {
		update (p);
		return rank;
	}
	
	/**
  	 * Calculates the probability of having 
  	 * the best hand against several opponents.
	 * @param p the number of active players in the hand	 
	 * @return probability of having the best hand.
	 */
	public double getHandStrength (int p) {
		update (p);
		return handStrength;
	}
	
	/**
	 * A crude but fast approximation of positive potential (a measure of the probability that
	 * the hand will improve over the next round)
	 * @return the ppot
	 */
	public double getPositivePotential() {
		update (players);
		return positivePotential;
	}

	/**
	 * An estimation of the hand strength, taking into account the possibility that
	 * hand can be improved in the future
	 * @param p the number of active players in the hand	 
	 * @return the effective hand strength
	 */
	public double getEffectiveHandStrength(int p) {
		update (p);
		return effectiveHandStrength;
	}

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder (Arrays.deepToString(getHole())).append(' ').append(Arrays.deepToString(getBoard())).
				append(" bucket=").append(getLastBucket()).append(" rank=").append(rank).append(" hand=").
				append(Evaluator.HAND[Evaluator.hand(rank)]).append(" HS=").append(handStrength).
				append(" pPot=").append(positivePotential).append(" EHS=").append(effectiveHandStrength);
		if (DEBUG) {
			double[] result = new double[2];
			calculatePotential (hole, board, false, result);
			sb.append(" / exact pPot=").append(result[0]);
		}
		return sb.toString();
	}

}

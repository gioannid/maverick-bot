package data; 

import java.util.*;

import ca.ualberta.cs.poker.free.dynamics.Card;
import ca.ualberta.cs.poker.free.dynamics.Card.Rank;
import ca.ualberta.cs.poker.free.dynamics.Card.Suit;
import data.Bucket;

public class CardGenerator {
	
	static protected final int RANKS = Rank.values().length;
	static protected final int SUITS = Suit.values().length;
	
	static protected Random random = new Random ();

	protected Card[] deck;
	protected boolean[] cardInDeck;
	protected int deckPos;
	
	static public int index (int rank, int suit) {
		return rank + suit * RANKS;
	}
	
	static public int index (Card c) {
		return c.getIndexSuitMajor();
	}
	
	static public int rank (int index) {
		return index % RANKS;
	}
	
	static public int suit (int index) {
		return index / RANKS;
	}
	
	static public Card getCard (int rank, int suit) {
		return Card.get (Rank.toRank(rank), Suit.toSuit(suit));
	}
	
	static public Card getCard (int index) {
		return Card.get (Rank.toRank(rank(index)), Suit.toSuit(suit(index)));
	}
	
	static public Card getCard (com.biotools.meerkat.Card card) {
		return getCard (card.getRank(), card.getSuit());
	}
	
	static public int hole (int index1, int index2) {
		if (index1 > index2)
			return index1 * Card.DECKSIZE + index2;
		else
			return index2 * Card.DECKSIZE + index1;
	}
	
	static public void setCards (int hole, int[] cards) {
		cards[0] = hole / Card.DECKSIZE;
		cards[1] = hole % Card.DECKSIZE;
	}
	

	public CardGenerator() {
		deck = new Card[Card.DECKSIZE];
		cardInDeck = new boolean[Card.DECKSIZE];
		for (int i = 0; i < Card.DECKSIZE; i++) {
			deck[i] = getCard(i);
			cardInDeck[i] = true;
		}
		deckPos = 0;
		shuffle ();
	}

	public CardGenerator(CardGenerator cardGenerator) {
		deck = cardGenerator.deck.clone();
		cardInDeck = cardGenerator.cardInDeck.clone();
		deckPos = cardGenerator.deckPos;
		shuffle ();
	}
	
	protected void shuffle () {
		/** Shuffling algorithm: http://www.cigital.com/papers/download/developer_gambling.php */
		for (int i = deckPos; i < Card.DECKSIZE; i++) {
			Card p0 = deck[i];
			int i1 = i + random.nextInt(Card.DECKSIZE - i);
			deck[i] = deck[i1];
			deck[i1] = p0;
		}
	}
	
	public Card upcoming (int offset) {
		if (deckPos + offset >= Card.DECKSIZE)
			return null;
		return deck[deckPos+offset];
	}
	
	public Card getNextAndRemoveCard() {
		if (deckPos == Card.DECKSIZE)
			return null;
		Card c = deck[deckPos];
		cardInDeck[index(deck[deckPos])] = false;
		deckPos++;
		return c;
	}

	private Card getNextAndRemoveCard(Suit suit) {
		int dp = deckPos;
		Card card;
		do {
			card = deck[dp++];
		} while ((dp < Card.DECKSIZE) && (card.suit != suit));
		if (dp == Card.DECKSIZE)
			return null;
		else {
			removeCard (card);
			return card;
		}
	}
	
	private Card getNextAndRemoveCard(Rank rank) {
		int dp = deckPos;
		Card card;
		do {
			card = deck[dp++];
		} while ((dp < Card.DECKSIZE) && (card.rank != rank));
		if (dp == Card.DECKSIZE)
			return null;
		else {
			removeCard (card);
			return card;
		}
	}
	
	public void removeCard(Card c) {
		int ci = index (c);
		if (! cardInDeck[ci])
			throw new RuntimeException ("Requested card to be removed "+c+" not found in CardGenerator deck");
		boolean found = false;
		for (int i = Card.DECKSIZE - 1; i > 0; i--) {
			if (deck[i] == c)
				found = true;
			if (found)
				deck[i] = deck[i-1];
		}
		deck[0] = c;
		cardInDeck[ci] = false;
		deckPos++;
	}

	public void addCard(Card c) {
		int ci = index (c);
		if (cardInDeck[ci])
			throw new RuntimeException ("Requested card to be added "+c+" already found in CardGenerator deck");
		deckPos--;
		int pos = random.nextInt(Card.DECKSIZE - deckPos) + deckPos;
		boolean found = false;
		for (int i = 0; i < pos; i++) {
			if (deck[i] == c)
				found = true;
			if (found)
				deck[i] = deck[i+1];
		}
		deck[pos] = c;
		cardInDeck[ci] = true;
	}

	public Card[] getHole() {
		Card[] result = new Card[2];
		result[0] = getNextAndRemoveCard();
		result[1] = getNextAndRemoveCard();
		return result;
	}

	public Card[] getHole(int minBucket, int maxBucket, Card[] board, int players) {
		if (deckPos + 1 >= Card.DECKSIZE)
			return null;
		Card[] result = new Card[2];
		for (int c0 = 0; c0 < Card.DECKSIZE - deckPos; c0++) {
			for (int c1 = c0 + 1; c1 < Card.DECKSIZE - deckPos; c1++) {
				result[0] = upcoming(c0);
				result[1] = upcoming(c1);
				Bucket bucket = new Bucket (result, board, players);
				if ((bucket.getLastBucket() >= minBucket) && (bucket.getLastBucket() <= maxBucket)) {
					removeCard (result[0]);
					removeCard (result[1]);
					return result;
				}
			}
		}
		return null;
	}
	
	public Card[] getHole(int minBucket, int maxBucket) {
		return getHole (minBucket, maxBucket, null, 0);
	}

	public Card[] getHole (boolean[][] ruleout) {
		if (deckPos + 1 >= Card.DECKSIZE)
			return null;
		Card[] result = new Card[2];
		for (int c0 = deckPos; c0 < Card.DECKSIZE; c0++) {
			for (int c1 = c0 + 1; c1 < Card.DECKSIZE; c1++) {
				if (! ruleout[index(deck[c0])][index(deck[c1])]) {
					result[0] = deck[c0];
					result[1] = deck[c1];
					removeCard (result[0]);
					removeCard (result[1]);
					return result;
				}
			}
		}
		System.err.print (" * ");
		return getHole();
	}

	public Card[] getHole(Card[] board) {
		Card[] result = new Card[2];
		result[0] = getNextAndRemoveCard();
		result[1] = getNextAndRemoveCard();
		int i = 0;
		while ((!hitBoard(result, board) || i < 100)) {
			addCard(result[0]);
			addCard(result[1]);
			result[0] = getNextAndRemoveCard();
			result[1] = getNextAndRemoveCard();
			i++;
		}
		return result;
	}
	
	protected Card[] getBoardNext(int minBucket, int maxBucket, Card[] hole, Card[] board, int players) {
		Card[] result = new Card[5];
		int offset;
		boolean flop;
		if ((board == null) || (board.length == 0) || (board[0] == null)) {
			offset = 0;
			flop = true;
		} else {
			result[0] = board[0];
			result[1] = board[1];
			result[2] = board[2];
			flop = false;
			if ((board.length == 3) || (board[3] == null)) {
				offset = 3;
			} else {
				result[3] = board[3];
				if ((board.length == 4) || (board[4] == null)) {
					offset = 4;
				} else {
					return null;
				}
			}
		}
		if (deckPos + (flop ? 2 : 0) >= Card.DECKSIZE)
			return null;
		for (int c0 = 0; c0 < Card.DECKSIZE - deckPos; c0++) {
			for (int c1 = c0 + 1; c1 < Card.DECKSIZE - deckPos; c1++) {
				for (int c2 = c1 + 1; c2 < Card.DECKSIZE - deckPos; c2++) {
					if ((! flop) && (c1 > c0 + 1) && (c2 > c0 + 2))
						continue;
					result[offset] = upcoming(c0);
					if (flop) {
						result[1] = upcoming(c1);
						result[2] = upcoming(c2);
					}
					Bucket bucket = new Bucket (hole, result, players);
					if ((bucket.getLastBucket() >= minBucket) && (bucket.getLastBucket() <= maxBucket)) {
						removeCard (result[offset]);
						if (flop) {
							removeCard (result[1]);
							removeCard (result[2]);
						}
						return result;
					}
				}
			}
		}
		return null;
	}
	
	public Card[] getBoard(int minBucket, int maxBucket, Card[] hole, int players) {
		Card[] board = getBoardNext (minBucket, maxBucket, hole, null, players);
		if (board == null)
			return null;
		board = getBoardNext (minBucket, maxBucket, hole, board, players);
		if (board == null)
			return null;
		board = getBoardNext (minBucket, maxBucket, hole, board, players);
		return board;
	}
	
	public Card[][] getHoleAndBoard(int minBucket, int maxBucket, int players) {
		if (deckPos + 6 >= Card.DECKSIZE)
			return null;
		Card[][] result = new Card[2][];
		result[0] = new Card[2];
		result[1] = new Card[5];
		for (int h0 = 0; h0 < Card.DECKSIZE - deckPos; h0++) {
			result[0][0] = upcoming(h0);
			for (int h1 = h0 + 1; h1 < Card.DECKSIZE - deckPos; h1++) {
				result[0][1] = upcoming(h1);
				int pb = Bucket.getPreflopBucket(result[0]);
				if ((pb >= minBucket) && (pb <= maxBucket)) {
					for (int b0 = 0; b0 < Card.DECKSIZE - deckPos; b0++) {
						if ((b0 == h0) || (b0 == h1))
							continue;
						result[1][0] = upcoming(b0);
						for (int b1 = b0 + 1; b1 < Card.DECKSIZE - deckPos; b1++) {
							if ((b1 == h0) || (b1 == h1))
								continue;
							result[1][1] = upcoming(b1);
							for (int b2 = b1 + 1; b2 < Card.DECKSIZE - deckPos; b2++) {
								if ((b2 == h0) || (b2 == h1))
									continue;
								result[1][2] = upcoming(b2);
								result[1][3] = null;
								result[1][4] = null;
								Bucket bucket = new Bucket (result[0], result[1], players);
								if ((bucket.getLastBucket() >= minBucket) && (bucket.getLastBucket() <= maxBucket)) {
									for (int b3 = b2 + 1; b3 < Card.DECKSIZE - deckPos; b3++) {
										if ((b3 == h0) || (b3 == h1))
											continue;
										result[1][3] = upcoming(b3);
										result[1][4] = null;
										bucket.setBoard(result[1], players);
										if ((bucket.getLastBucket() >= minBucket) && (bucket.getLastBucket() <= maxBucket)) {
											for (int b4 = b3 + 1; b4 < Card.DECKSIZE - deckPos; b4++) {
												if ((b4 == h0) || (b4 == h1))
													continue;
												result[1][4] = upcoming(b4);
												bucket.setBoard(result[1], players);
												if ((bucket.getLastBucket() >= minBucket) && (bucket.getLastBucket() <= maxBucket)) {
													removeCard (result[0][0]);
													removeCard (result[0][1]);
													removeCard (result[1][0]);
													removeCard (result[1][1]);
													removeCard (result[1][2]);
													removeCard (result[1][3]);
													removeCard (result[1][4]);
													return result;
												}
											}
										}
									}
								}
							}
						}
						System.out.print('.');
					}
				}
				System.out.print('/');
			}
			System.out.println();
		}
		return null;
	}

	private boolean hitBoard(Card[] result, Card[] board) {
		Card c1 = result[0];
		Card c2 = result[1];
		int highestRank = 0;
		for (Card c : board) {
			if (c != null) {
				if (c.rank.index > highestRank)
					highestRank = c.rank.index;
			}
		}

		if (c1.rank.index == c2.rank.index && c1.rank.index >= highestRank) {
			return true;
		}
		for (Card c : board) {
			if (c != null) {
				if (c.rank.index == c2.rank.index
						|| c.rank.index == c1.rank.index) {
					// System.out.println("c:" + c + " c1: " + c1 + " c2: " +
					// c2);
					return true;
				}
			}
		}
		return false;
	}

	public Card[] getHoleHigh(int bucket, int maxBucket, Card[] board) {
		Card[] result = new Card[2];
		
		result[0] = killerCard(board);
		if (result[0] == null) {
			//return getHole(bucket, maxBucket, board);
			Rank highestRank = Rank.TWO;
			for (Card c : board) {
				if (c != null) {
					if (c.rank.compareTo(highestRank) > 0)
						highestRank = c.rank;
				}
			}
			result[0] = getNextAndRemoveCard(highestRank);
			if (result[0] == null) {
				result[0] = getNextAndRemoveCard();
			}
			result[1] = getNextAndRemoveCard();
			return result;
		}
		result[1] = getNextAndRemoveCard();
		return result;
	}

	public Card killerCard(Card[] board) {
		int[] suitCounts = new int[SUITS];
		int[] rankCounts = new int[RANKS];
		int mostFrequentRank = -1;
		int secondMostFrequentRank = -1;
		int highestRank = -1;
		for (Card c : board) {
			if (c != null) {
				suitCounts[c.suit.index]++;
				rankCounts[c.rank.index]++;
			}
		}
		for (int i = 0; i < suitCounts.length; i++) {
			if (suitCounts[i] >= 3) {
				return getNextAndRemoveCard(Suit.toSuit(i));
			}
		}
		int runningCount = 0;
		int hole = 0;
		int holeRank = -1;
		int runningCountHole = 0;
		for (int i = rankCounts.length - 1; i >= 0; i--) {
			if (rankCounts[i] > 0) {
				runningCount++;
				if (runningCount == 4) {
					if(i+4 >= rankCounts.length){
						if(i-1 >=0)
							return getNextAndRemoveCard(Rank.toRank(i-1));
						else return getNextAndRemoveCard(Rank.ACE);
					}
					else return getNextAndRemoveCard(Rank.toRank(i + 4));
					
				}
				if (runningCount + runningCountHole == 4) {
					if(holeRank!=-1)
						return getNextAndRemoveCard(Rank.toRank(holeRank));
					else break;
				}
			} else {
				if (hole == 0) {
					hole++;
					runningCountHole = runningCount;
					holeRank = i;
				} else {
					hole = 0;
					runningCountHole = 0;
					holeRank = -1;
				}
				runningCount = 0;
			}
		}
		if (runningCount == 3 && rankCounts[12] > 0) {
			return getNextAndRemoveCard(Rank.toRank(3));
		}
		for (int i = 0; i < rankCounts.length; i++) {
			if (rankCounts[i] > 0)
				highestRank = i;
			if ((mostFrequentRank == -1)
					|| rankCounts[i] >= rankCounts[mostFrequentRank]) {
				secondMostFrequentRank = mostFrequentRank;
				mostFrequentRank = i;
			} else if ((secondMostFrequentRank == -1)
					|| rankCounts[i] >= rankCounts[secondMostFrequentRank]) {
				secondMostFrequentRank = i;
			}
		}
		if (rankCounts[mostFrequentRank] >= 2)
			return getNextAndRemoveCard(Rank.toRank(mostFrequentRank));
		if (rankCounts[mostFrequentRank] == 1)
			return getNextAndRemoveCard(Rank.toRank(highestRank));
		return null;
	}
	
	public int openedCards () {
		return deckPos;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder('[');
		for (int i = 0; i < deckPos; i++)
			sb.append(" ").append(deck[i]);
		sb.append("] [");
		for (int i = deckPos; i < Card.DECKSIZE; i++)
			sb.append(" ").append(deck[i]);
		sb.append(']');
		return sb.toString();
	}

}

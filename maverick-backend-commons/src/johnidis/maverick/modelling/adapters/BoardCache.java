package johnidis.maverick.modelling.adapters;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.biotools.meerkat.Hand;

import ca.ualberta.cs.poker.free.dynamics.Card;
import data.CardGenerator;

public class BoardCache<T> extends LinkedHashMap<BitSet,T> {

	private static final int DEFAULT_MAX_CACHE_ENTRIES = 10000;
	
	private static final long serialVersionUID = 724557997838542807L;
	
	private final int maxCacheEntries;

	
	static public BitSet key(Card[] board) {
		BitSet bitset = new BitSet(52);
		if (board[2] == null)
			throw new RuntimeException ("BoardCache.key(Card[]) cannot be called before flop");
		for (int c = 0; c < 5; c++)
			if (board[c] != null)
				bitset.set(CardGenerator.index(board[c]));
		return bitset;
	}

	static public BitSet key(Hand board) {
		BitSet bitset = new BitSet(52);
		if ((board.size() < 3) || (board.getCard(3) == null))
			throw new RuntimeException ("BoardCache.key(Hand) cannot be called before flop");
		for (int c = 1; c <= board.size(); c++)
			if (board.getCard(c) != null)
				bitset.set(board.getCard(c).getIndex());
		return bitset;
	}


	public BoardCache (int cacheEntries) {
		super (cacheEntries, 0.75F, true);
		maxCacheEntries = cacheEntries;
	}

	public BoardCache () {
		this (DEFAULT_MAX_CACHE_ENTRIES);
	}
	
	
	@Override
	protected boolean removeEldestEntry(Entry<BitSet,T> entry) {
		return size() > maxCacheEntries;
	}
	
}
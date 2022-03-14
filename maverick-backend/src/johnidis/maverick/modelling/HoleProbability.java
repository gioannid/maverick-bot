package johnidis.maverick.modelling;

import com.biotools.meerkat.Card;
import com.biotools.meerkat.Hand;

import data.ArrayTools;
import data.Bucket;

public class HoleProbability {
	
	public static final boolean DEBUG = false;
	
	public static final int NOT_UPDATED = -1;
	
	public double maxEntry;
	public double sumEntries;
	
	private final boolean[] refCardsExcluded;
	private final double[][] matrix;
	
	
	public HoleProbability () {
		matrix = new double[51][];
		refCardsExcluded = new boolean[52];
		for (int i = 0; i < 51; i++) {
			matrix[i] = new double[51-i];
			for (int j = 0; j < 51 - i; j++)
				matrix[i][j] = 1;
		}
		maxEntry = 1;
		sumEntries = 52 * 51 / 2;
	}
	
	public HoleProbability (HoleProbability aHoleProbability, boolean normalize) {
		synchronized (aHoleProbability) {
			matrix = ArrayTools.cloneArray(aHoleProbability.matrix);
		}
		refCardsExcluded = aHoleProbability.refCardsExcluded;
		if (normalize) {
			update();
			if (sumEntries == 0)
				return;
			for (int i = 0; i < 51; i++)
				for (int j = i + 1; j < 52; j++)
					matrix[i][j-i-1] /= sumEntries;
			maxEntry /= sumEntries;
			sumEntries = 1;
		} else {
			maxEntry = aHoleProbability.maxEntry;
			sumEntries = aHoleProbability.sumEntries;
		}
	}
	

	public synchronized double get (int card1, int card2) {
		return matrix[card1][card2-card1-1];
	}
	
	public synchronized void set (int card1, int card2, double value) {
		int c = card2 - card1 - 1;
		sumEntries += value - matrix[card1][c];
		matrix[card1][c] = value;
		maxEntry = NOT_UPDATED;
	}
	
	public synchronized void adjust (int card1, int card2, double factor) {
		int c = card2 - card1 - 1;
		sumEntries += (factor - 1) * matrix[card1][c];
		matrix[card1][c] *= factor;
		maxEntry = NOT_UPDATED;
	}
	
	public synchronized void preflopAdjust(int minRanking, int maxRanking, double almostZero) {
		maxEntry = -1;
		sumEntries = 0;
		if (DEBUG)
			System.out.println("preflopAdjust(minRanking="+minRanking+", maxRanking="+maxRanking+"):");
		for (int i = 0; i < 51; i++)
			for (int j = i + 1; j < 52; j++) {
				int rank = Bucket.getPreflopRanking(j, i); 
				if ((rank > maxRanking) || (rank < minRanking)) {
					int c = j-i-1;
					double factor;
					if (rank > maxRanking)
						factor = 1D - (double) (rank - maxRanking) / (169 - maxRanking);
					else
						factor = 1D - (double) (minRanking - rank) / minRanking;
					if (DEBUG)
						System.out.printf("  Cards = %s %s, rank = %d, factor = %5.3f\n", Card.get(i), Card.get(j), rank, factor);
					matrix[i][c] *= factor;
					if (matrix[i][c] < almostZero)
						matrix[i][c] = 0;
					if (maxEntry < matrix[i][c])
						maxEntry = matrix[i][c];
					sumEntries += matrix[i][c];
				}
			}
	}
	
	public synchronized void scratch(Hand hole) {
		int h1 = hole.getCardIndex(1);
		int h2 = hole.getCardIndex(2);
		if (h2 < h1) {
			int temp = h2;
			h2 = h1;
			h1 = temp;
		}
		sumEntries -= matrix[h1][h2-h1-1];
		maxEntry = NOT_UPDATED;
		matrix[h1][h2-h1-1] = 0;
		synchronized (refCardsExcluded) {
			refCardsExcluded[h1] = true;
			refCardsExcluded[h2] = true;
		}
	}

	public synchronized void scratch(Card theCard) {
		int card = theCard.getIndex();
		for (int c = 0; c < 52; c++)
			if (c < card) {
				matrix[c][card-c-1] = 0;
				sumEntries -= matrix[c][card-c-1];
			} else if (c > card) {
				matrix[card][c-card-1] = 0;
				sumEntries -= matrix[card][c-card-1];
			}
		maxEntry = NOT_UPDATED;
		synchronized (refCardsExcluded) {
			refCardsExcluded[card] = true;
		}
	}
	
	public boolean cardExcluded (int card) {
		synchronized (refCardsExcluded) {
			return refCardsExcluded[card];
		}
	}

	public synchronized void update () {
		synchronized (refCardsExcluded) {
			maxEntry = -1;
			sumEntries = 0;
			for (int i = 0; i < 51; i++)
				for (int j = i + 1; j < 52; j++) {
					int c = j - i - 1;
					if (refCardsExcluded[i] || refCardsExcluded[j])
						matrix[i][c] = 0;
					sumEntries += matrix[i][c];
					if (maxEntry < matrix[i][c])
						maxEntry = matrix[i][c];
				}
		}
	}
	
	@Override
	public String toString() {
		HoleProbability prob = new HoleProbability (this, true);
		if (prob.maxEntry == 0)
			return "(empty)";
		StringBuilder str = new StringBuilder("  3s 4s 5s 6s 7s 8s 9s Ts Js Qs Ks As |2h 3h 4h 5h 6h 7h 8h 9h Th Jh Qh Kh Ah |2d 3d 4d 5d 6d 7d 8d 9d Td Jd Qd Kd Ad |2c 3c 4c 5c 6c 7c 8c 9c Tc Jc Qc Kc Ac\n");
		for (int i = 0; i < 51; i++) {
			str.append(Card.get(i).toString());
			for (int j = 0; j < 52; j++) {
				if (j <= i) {
					if (j != 0)
						str.append("   ");
				} else {
					int p = (int) (prob.matrix[i][j-i-1] * 100 / prob.maxEntry);
					if (p == 100)
						p = 99;
					if (p < 10)
						str.append(' ');
					str.append(p).append(' ');
				}
				if ((j + 1) % 13 == 0)
					str.append('|');
			}
			str.append('\n');
			if ((i + 1) % 13 == 0)
				str.append("--------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
		}
		return str.toString();
	}
	
}
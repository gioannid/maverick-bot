package johnidis.maverick.modelling.data;

import java.util.Iterator;
import java.util.List;

public class FloatingDataSet extends BasicMLDataSet {
	
	private static final boolean DEBUG = false;

	private static final long serialVersionUID = 7337039128291468426L;
	private int windowSize;
	private int agingEpoch; 

	
	public class FloatingDatasetIterator implements Iterator<MLDataPair> {

		private int currentIndex = 0;
		private final long records = getRecordCount();
		private long epoch = (agingEpoch > 0) ? records / agingEpoch + 1 : 1;
		private MLDataPair next = findNext();
		
		private MLDataPair findNext() {
			MLDataPair result = null;
			while ((currentIndex < records) && (result == null)) {
				if ((agingEpoch > 0) && (((records - currentIndex) % agingEpoch) == 0))
					epoch--;
				if ((currentIndex % epoch) == 0)
					result = get(currentIndex);
				currentIndex++;
			}
			if (DEBUG)
				if (result != null)
					System.out.println("FloatingDatasetIterator: delivered "+currentIndex+" out of "+records);
			return result;
		}

		@Override
		public final boolean hasNext() {
			return (next != null);
		}

		@Override
		public final MLDataPair next() {
			MLDataPair result = next;
			next = findNext();
			return result;
		}

		@Override
		public final void remove() {
			throw new RuntimeException("Called remove, unsupported operation.");
		}
		
	}

	public FloatingDataSet(final double[][] input, final double[][] ideal, int window, int agingEpoch) {
		super (input, ideal);
		this.agingEpoch = agingEpoch;
		setWindowSize (window);
	}

	public FloatingDataSet(final List<MLDataPair> theData, int window, int agingEpoch) {
		super (theData);
		this.agingEpoch = agingEpoch;
		setWindowSize (window);
	}

	public FloatingDataSet(final MLDataSet set, int window, int agingEpoch) {
		super (set);
		this.agingEpoch = agingEpoch;
		windowSize = window;
	}
	
	public FloatingDataSet (int window, int agingEpoch) {
		super ();
		this.agingEpoch = agingEpoch;
		windowSize = window;
	}
	
	
	public void setWindowSize (int window) {
		if (window > -1) {
			synchronized (this) {
				List<MLDataPair> data = getData();
				int size = data.size();
				for (int i = 0; i < size - window; i++)
					data.remove(0);
			}
		}
		windowSize = window;
	}

	public int getWindowSize () {
		return windowSize;
	}

	public int getAgingEpoch() {
		return agingEpoch;
	}

	public void setAgingEpoch(int agingEpoch) {
		this.agingEpoch = agingEpoch;
	}

	@Override
	public void add(final MLDataPair inputData) {
		synchronized (this) {
			super.add(inputData);
			List<MLDataPair> data = getData();
			if (data.size() > windowSize)
				data.remove(0);
		}
	}

	@Override
	public void add(final MLData theData) {
		add(new BasicMLDataPair(theData));
	}

	@Override
	public void add(final MLData inputData, final MLData idealData) {
		add(new BasicMLDataPair(inputData, idealData));
	}

	@Override
	public long getRecordCount() {
		synchronized (this) {
			return super.getRecordCount();
		}
	}
	@Override
	public int size() {
		return (int)getRecordCount();
	}

	@Override
	public Iterator<MLDataPair> iterator() {
		return new FloatingDatasetIterator();
	}

}

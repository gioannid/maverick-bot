package johnidis.maverick.modelling.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stores data in an ArrayList. This class is memory based, so large enough
 * datasets could cause memory issues. Many other dataset types extend this
 * class.
 */
public class BasicMLDataSet implements MLDataSet, Serializable {

	private static final long serialVersionUID = -5206024783368426568L;

	/**
	 * An iterator to be used with the BasicMLDataSet. This iterator does not
	 * support removes.
	 */
	public class BasicMLIterator implements Iterator<MLDataPair> {

		/**
		 * The index that the iterator is currently at.
		 */
		private int currentIndex = 0;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final boolean hasNext() {
			return this.currentIndex < BasicMLDataSet.this.data.size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final MLDataPair next() {
			if (!hasNext()) {
				return null;
			}

			return BasicMLDataSet.this.data.get(this.currentIndex++);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void remove() {
			throw new RuntimeException("Called remove, unsupported operation.");
		}
	}

	/**
	 * The data held by this object.
	 */
	private List<MLDataPair> data = new ArrayList<MLDataPair>();

	/**
	 * Completely copy one array into another.
	 * 
	 * @param src
	 *            Source array.
	 * @param dst
	 *            Destination array.
	 */
	public static void arrayCopy(final double[] src, final double[] dst) {
		System.arraycopy(src, 0, dst, 0, src.length);
	}

	/**
	 * Default constructor.
	 */
	public BasicMLDataSet() {
	}

	/**
	 * Construct a data set from an input and idea array.
	 * 
	 * @param input
	 *            The input into the machine learning method for training.
	 * @param ideal
	 *            The ideal output for training.
	 */
	public BasicMLDataSet(final double[][] input, final double[][] ideal) {
		if (ideal != null) {
			for (int i = 0; i < input.length; i++) {
				final BasicMLData inputData = new BasicMLData(input[i]);
				final BasicMLData idealData = new BasicMLData(ideal[i]);
				this.add(inputData, idealData);
			}
		} else {
			for (final double[] element : input) {
				final BasicMLData inputData = new BasicMLData(element);
				this.add(inputData);
			}
		}
	}

	/**
	 * Construct a data set from an already created list. Mostly used to
	 * duplicate this class.
	 * 
	 * @param theData
	 *            The data to use.
	 */
	public BasicMLDataSet(final List<MLDataPair> theData) {
		this.data = theData;
	}

	/**
	 * Copy whatever dataset type is specified into a memory dataset.
	 * 
	 * @param set
	 *            The dataset to copy.
	 */
	public BasicMLDataSet(final MLDataSet set) {
		final int inputCount = set.getInputSize();
		final int idealCount = set.getIdealSize();

		for (final MLDataPair pair : set) {

			BasicMLData input = null;
			BasicMLData ideal = null;

			if (inputCount > 0) {
				input = new BasicMLData(inputCount);
				arrayCopy(pair.getFirstArray(), input.getData());
			}

			if (idealCount > 0) {
				ideal = new BasicMLData(idealCount);
				arrayCopy(pair.getSecondArray(), ideal.getData());
			}

			add(new BasicMLDataPair(input, ideal));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(final MLData theData) {
		this.data.add(new BasicMLDataPair(theData));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(final MLData inputData, final MLData idealData) {

		final MLDataPair pair = new BasicMLDataPair(inputData, idealData);
		this.data.add(pair);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(final MLDataPair inputData) {
		this.data.add(inputData);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		data.clear();
	}

	/**
	 * Get the data held by this container.
	 * 
	 * @return the data
	 */
	public List<MLDataPair> getData() {
		return this.data;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getIdealSize() {
		if (this.data.isEmpty()) {
			return 0;
		}
		final MLDataPair first = this.data.get(0);
		if (first.getSecond() == null) {
			return 0;
		}

		return first.getSecond().size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getInputSize() {
		if (this.data.isEmpty()) {
			return 0;
		}
		final MLDataPair first = this.data.get(0);
		return first.getFirst().size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void getRecord(final long index, final MLDataPair pair) {

		final MLDataPair source = this.data.get((int) index);
		pair.setFirstArray(source.getFirstArray());
		if (pair.getSecondArray() != null) {
			pair.setSecondArray(source.getSecondArray());
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getRecordCount() {
		return this.data.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSupervised() {
		if (this.data.size() == 0) {
			return false;
		}
		return this.data.get(0).isSupervised();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<MLDataPair> iterator() {
		final BasicMLIterator result = new BasicMLIterator();
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MLDataSet openAdditional() {
		return new BasicMLDataSet(this.data);
	}

	/**
	 * @param theData
	 *            the data to set
	 */
	public void setData(final List<MLDataPair> theData) {
		this.data = theData;
	}

	/**
	 * Concert the data set to a list.
	 * @param theSet The data set to convert.
	 * @return The list.
	 */
	public static List<MLDataPair> toList(MLDataSet theSet) {
		List<MLDataPair> list = new ArrayList<MLDataPair>();
		for(MLDataPair pair: theSet) {
			list.add(pair);
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return (int)getRecordCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MLDataPair get(int index) {
		return this.data.get(index);
	}

}

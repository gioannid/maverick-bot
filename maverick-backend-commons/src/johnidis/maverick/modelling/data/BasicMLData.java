package johnidis.maverick.modelling.data;

import java.io.Serializable;

import norsys.netica.Util;

/**
 * Basic implementation of the MLData interface that stores the data in an
 * array.
 */
public class BasicMLData implements MLData, Serializable {

	private static final long serialVersionUID = -350278088003791231L;
	/**
	 * The data held by this object.
	 */
	private double[] data;


	/**
	 * Construct this object with the specified data.
	 *
	 * @param d
	 *            The data to construct this object with.
	 */
	public BasicMLData(final double[] d) {
		this(d.length);
		System.arraycopy(d, 0, this.data, 0, d.length);
	}

	public BasicMLData(float[] d) {
		data = Util.toDoubles(d);
	}

	/**
	 * Construct this object with blank data and a specified size.
	 *
	 * @param size
	 *            The amount of data to store.
	 */
	public BasicMLData(final int size) {
		this.data = new double[size];
	}

	/**
	 * Construct a new BasicMLData object from an existing one. This makes a
	 * copy of an array.
	 *
	 * @param d
	 *            The object to be copied.
	 */
	public BasicMLData(final MLData d) {
		this(d.size());
		System.arraycopy(d.getData(), 0, this.data, 0, d.size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(final int index, final double value) {
		this.data[index] += value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		for (int i = 0; i < this.data.length; i++) {
			this.data[i] = 0;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MLData clone() {
		return new BasicMLData(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double[] getData() {
		return this.data;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getData(final int index) {
		return this.data[index];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setData(final double[] theData) {
		this.data = theData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setData(final int index, final double d) {
		this.data[index] = d;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return this.data.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("[");
		builder.append(this.getClass().getSimpleName());
		builder.append(":");
		for (int i = 0; i < this.data.length; i++) {
			if (i != 0) {
				builder.append(',');
			}
			builder.append(this.data[i]);
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Add one data element to another.  This does not modify the object.
	 * @param o The other data element
	 * @return The result.
	 */
	public MLData plus(MLData o)
	{
		if (size() != o.size())
			throw new IllegalArgumentException();
		
		BasicMLData result = new BasicMLData(size());
		for (int i = 0; i < size(); i++)
			result.setData(i, getData(i) + o.getData(i));
		
		return result;
	}
	
	/**
	 * Multiply one data element with another.  This does not modify the object.
	 * @param d The other data element
	 * @return The result.
	 */
	public MLData times(double d)
	{
		MLData result = new BasicMLData(size());
		
		for (int i = 0; i < size(); i++)
			result.setData(i, getData(i) * d);
		
		return result;
	}
	
	/**
	 * Subtract one data element from another.  This does not modify the object.
	 * @param o The other data element
	 * @return The result.
	 */
	public MLData minus(MLData o)
	{
		if (size() != o.size())
			throw new IllegalArgumentException();
		
		MLData result = new BasicMLData(size());
		for (int i = 0; i < size(); i++)
			result.setData(i,  getData(i) - o.getData(i));
		
		return result;
	}
}

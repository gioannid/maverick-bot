package johnidis.maverick.modelling.data;

import java.io.Serializable;


/**
 * A basic implementation of the MLDataPair interface. This implementation
 * simply holds and input and ideal MLData object.
 * 
 * For supervised training both input and ideal should be specified.
 * 
 * For unsupervised training the input property should be valid, however the
 * ideal property should contain null.
 */
public class BasicMLDataPair implements MLDataPair, Serializable {

	private static final long serialVersionUID = 9166087979169666206L;

	/**
	 * The significance.
	 */
	private double significance = 1.0;

	/**
	 * Create a new data pair object of the correct size for the machine
	 * learning method that is being trained. This object will be passed to the
	 * getPair method to allow the data pair objects to be copied to it.
	 * 
	 * @param inputSize
	 *            The size of the input data.
	 * @param idealSize
	 *            The size of the ideal data.
	 * @return A new data pair object.
	 */
	public static MLDataPair createPair(final int inputSize, 
			final int idealSize) {
		MLDataPair result;

		if (idealSize > 0) {
			result = new BasicMLDataPair(new BasicMLData(inputSize),
					new BasicMLData(idealSize));
		} else {
			result = new BasicMLDataPair(new BasicMLData(inputSize));
		}

		return result;
	}

	/**
	 * The the expected output from the machine learning method, or null for
	 * unsupervised training.
	 */
	private final MLData ideal;

	/**
	 * The training input to the machine learning method.
	 */
	private final MLData input;

	/**
	 * Construct the object with only input. If this constructor is used, then
	 * unsupervised training is being used.
	 * 
	 * @param theInput
	 *            The input to the machine learning method.
	 */
	public BasicMLDataPair(final MLData theInput) {
		this.input = theInput;
		this.ideal = null;
	}

	/**
	 * Construct a BasicMLDataPair class with the specified input and ideal
	 * values.
	 * 
	 * @param theInput
	 *            The input to the machine learning method.
	 * @param theIdeal
	 *            The expected results from the machine learning method.
	 */
	public BasicMLDataPair(final MLData theInput, final MLData theIdeal) {
		this.input = theInput;
		this.ideal = theIdeal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MLData getSecond() {
		return this.ideal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double[] getSecondArray() {
		if (this.ideal == null) {
			return null;
		}
		return this.ideal.getData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MLData getFirst() {
		return this.input;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double[] getFirstArray() {
		return this.input.getData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSupervised() {
		return this.ideal != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSecondArray(final double[] data) {
		this.ideal.setData(data);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFirstArray(final double[] data) {
		this.input.setData(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("[");
		builder.append(this.getClass().getSimpleName());
		builder.append(":");
		builder.append("Input:");
		builder.append(getFirst());
		builder.append("Ideal:");
		builder.append(getSecond());
		builder.append(",");
		builder.append("Significance:");
		builder.append(this.significance);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getSignificance() {
		return significance;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSignificance(double significance) {
		this.significance = significance;
	}

}

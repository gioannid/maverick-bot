package johnidis.maverick.modelling.data;

/**
 * Training data is stored in two ways, depending on if the data is for
 * supervised, or unsupervised training.
 * 
 * For unsupervised training just an input value is provided, and the ideal
 * output values are null.
 * 
 * For supervised training both input and the expected ideal outputs are
 * provided.
 * 
 * This interface abstracts classes that provide a holder for both of these two
 * data items.
 */
public interface MLDataPair {

	/**
	 * @return The ideal data that the machine learning method should produce 
	 * for the specified input.
	 */
	double[] getSecondArray();

	/**
	 * @return The input that the neural network
	 */
	double[] getFirstArray();

	/**
	 * Set the ideal data, the desired output.
	 * 
	 * @param data
	 *            The ideal data.
	 */
	void setSecondArray(double[] data);

	/**
	 * Set the input.
	 * 
	 * @param data
	 *            The input.
	 */
	void setFirstArray(double[] data);

	/**
	 * @return True if this training pair is supervised. That is, it has both
	 *         input and ideal data.
	 */
	boolean isSupervised();
	
	/**
	 * @return The ideal data that the neural network should produce for the
	 *         specified input.
	 */
	MLData getSecond();

	/**
	 * @return The input that the neural network
	 */
	MLData getFirst();
	
	/**
	 * Get the significance, 1.0 is neutral.
	 * @return The significance.
	 */
	double getSignificance();
	
	/**
	 * Set the significance, 1.0 is neutral.
	 * @param s The significance.
	 */
	void setSignificance(double s);

}

package johnidis.maverick.modelling.data;

/**
 * Defines an array of data. This is an array of double values that could be
 * used either for input data, actual output data or ideal output data.
 */
public interface MLData {

	/**
	 * Add a value to the specified index.
	 * 
	 * @param index
	 *            The index to add to.
	 * @param value
	 *            The value to add.
	 */
	void add(int index, double value);

	/**
	 * Clear any data to zero.
	 */
	void clear();

	/**
	 * Clone this object.
	 * 
	 * @return A cloned version of this object.
	 */
	MLData clone();

	/**
	 * @return All of the elements as an array.
	 */
	double[] getData();

	/**
	 * Get the element specified index value.
	 * 
	 * @param index
	 *            The index to read.
	 * @return The value at the specified inedx.
	 */
	double getData(int index);

	/**
	 * Set all of the data as an array of doubles.
	 * 
	 * @param data
	 *            An array of doubles.
	 */
	void setData(double[] data);

	/**
	 * Set the specified element.
	 * 
	 * @param index
	 *            The index to set.
	 * @param d
	 *            The data for the specified element.
	 */
	void setData(int index, double d);

	/**
	 * @return How many elements are stored in this object.
	 */
	int size();

}

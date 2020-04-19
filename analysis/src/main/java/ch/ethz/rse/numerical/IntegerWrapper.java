package ch.ethz.rse.numerical;

/**
 * Utility class needed by numerical analysis.
 */
public class IntegerWrapper {

	public int value;

	public IntegerWrapper(int v) {
		value = v;
	}
	
	/**
	 * Increments value field
	 * 
	 * @return new value
	 */
	public int increment() {
		value++;
		return value;
	}
}
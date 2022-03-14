package ringclient.interfaces;

/**
 * Function Object Pattern
 *
 * Created on 22.04.2008
 * @author Kami II
 */
public interface ITimerFunction {

	/**
	 * this method is called
	 */
	public void execute(long startTime, long stopTime);
}

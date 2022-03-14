package ringclient;

import ringclient.interfaces.ITimerFunction;

/**
 * the timer waits a given timeout before a certain function is executed
 *
 * Created on 22.04.2008
 * @author Kami II
 */
public class Timer extends Thread {

	/**
	 * the waiting time
	 */
    public long startTime;
    public long stopTime;
    
    /**
     * the function that should be executed after the waiting time
     */
    private ITimerFunction function;

    /**
     * creates a new timer
     * @param timeBorder the point in time to stop the timer
     * @param function the function that should be executed
     */
    public Timer(long startTime, long stopTime, ITimerFunction function) {
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.function = function;
    }

    /**
     * timer
     */
    public void run() {
    	synchronized (this) {
            try {
            	long now;
            	while ((now = System.currentTimeMillis()) < stopTime) {
            		wait(stopTime - now);
            	}
                if (function != null)
                	function.execute(startTime, stopTime);
            } catch(Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }    		
    	}
    }
}

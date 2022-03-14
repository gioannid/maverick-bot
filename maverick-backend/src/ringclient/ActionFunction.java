package ringclient;

import ringclient.interfaces.ITimerFunction;

/**
 * the function that should be called after the timer is done
 * Created on 22.04.2008
 * @author Kami II
 */
public class ActionFunction implements ITimerFunction {

	/**
	 * the main class of the PokerPlayer bot
	 */
    private AdvancedRingClient pokerPlayer;

    /**
     * creates a new instance of the function object
     * @param pokerPlayer the main class of the PokerPlayer
     */
    public ActionFunction(AdvancedRingClient pokerPlayer) {
        this.pokerPlayer = pokerPlayer;
    }

    /**
     * sends the action of the bot to the server
     */
    public void execute(long startTime, long stopTime) {
        pokerPlayer.takeAction(startTime, stopTime);
    }
}

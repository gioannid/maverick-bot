package ringclient.interfaces;

import ringclient.ClientRingDynamics;
import ca.ualberta.cs.poker.free.dynamics.Card;
import data.Action;
import data.GameState;

/**
 * The listener interface that listens to changes of the ClientRingDynamics
 * Created on 14.04.2008
 * Modifications for Maverick.
 * @author Kami II
 */
public interface StateChangeListener {

	/**
	 * this method is called upon game start, in order to be informed of the game participants 
	 * @param players the participants' names ordered by seats
	 * @param stack the participants' bankrolls ordered by seats
	 */
	public void gameStarts(ClientRingDynamics state);

	/**
	 * this method is called every time an action is performed by the player 
	 * currently playing
	 * @param seat the seat the player sits in
	 * @param action the action the player has performed
	 */
	public void actionPerformed(int seat, int player, Action action);
	
	/**
	 * is called when the game state has changed
	 * @param state the new game state
	 */
	public void stateChanged(GameState state);
	
	/**
	 * 
	 * @param ownID own number
	 * @param playerAtSeatZero number of player at seat zero
	 * @param amountWon array with the amount won by all players ordered by seats (like in ringdynamics)
	 * @param inPot array with the amount in Pot by all players ordered by seats (like in ringdynamics)
	 * @param hole cardset that is shown after the game
	 */
	public void roundFinished(int ownID, int playerAtSeatZero, int[] amountWon, int[] inPot, Card hole[][]);
}

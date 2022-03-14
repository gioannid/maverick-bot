package ringclient;

import ca.ualberta.cs.poker.free.client.PokerClient;
import ca.ualberta.cs.poker.free.dynamics.LimitType;
import ca.ualberta.cs.poker.free.dynamics.MatchType;
import data.Constants;

/**
 * An extension of PokerClient that contains a reference to a reproduction of what is happening on 
 * the server side (state).
 * Can overload takeAction() (instead of handleStateChange()) to only receive messages when it is
 * your turn to act.
 *
 * As before, actions can be taken with sendFold, sendCall(), and sendRaise()
 * @author Kami II
 * Modifications for Maverick
 */
public class AdvancedRingClient extends PokerClient {

	/**
     * A reproduction of what is happening on the server side.
     */
    public ClientRingDynamics state;
    
    /**
     * stores a complete history of all previous game rounds
     */
//    public History history;
    
    /**
     * handles the time we have per game state and start the timers
     */
    public TimerManager timerManager;
    
    /** 
     * Handles the state change. 
     * Updates state and calls takeAction()
     */
    public synchronized void handleStateChange(){
    	timerManager.turnStarted();
    	if (state == null) {
    		ClientRingStateParser crsp = new ClientRingStateParser();
    		crsp.parseMatchStateMessage(currentGameStateString);
    		MatchType mt = new MatchType(LimitType.LIMIT, false, 8000, 1000);
    		Constants.PLAYER_COUNT = crsp.numPlayers;
//    		CONSTANT.AGGRESSIVE_PREFLOP = new double[crsp.numPlayers];
//   		CONSTANT.AGGRESSIVE_RAISE = new double[crsp.numPlayers];
    		for(int i = 0; i < crsp.numPlayers; i++) {
//    			CONSTANT.AGGRESSIVE_PREFLOP[i] = CONSTANT.A_PREFLOP;
//    			CONSTANT.AGGRESSIVE_RAISE[i] = CONSTANT.A_RAISE;
    		}
    		state = new ClientRingDynamics(crsp.numPlayers, mt, crsp);
    		state.setParser(crsp);
//    		state.addStateChangeListener(history);
    		state.addStateChangeListener(timerManager);
    	}
    	
        state.setFromMatchStateMessage(currentGameStateString);
       	// make sure everything's fine with tracked state (should never be true)
       	if (!state.getMatchState(state.seatTaken).equals(currentGameStateString)) {
       		System.err.println("BADMATCHSTATESTRING: ");
       		System.err.println("     Localstate : " + state.getMatchState(state.seatTaken));
       		System.err.println("     From Server: " + currentGameStateString);
       	}
        if (state.isOurTurn()){
            startTakeAction();
        }
    }
    
    /** 
     * Creates a new instance of AdvancedRingClient. Must call connect(), then run() to start process 
     */
    public AdvancedRingClient(){
        state = null;
//        history = History.getHistory();
        timerManager = new TimerManager(Constants.HANDTIME, Constants.HANDCOUNT);
    }
    
    /**
     * override to start the decision process.
     */
    public synchronized void startTakeAction(){
    	takeAction(0, 0);
    }

    /**
     * override to send the decided action
     */
    public void sendAction() {
        try{
        	sendCall();
        } catch (Exception e){
        	System.out.println(e);
        }
    }

    /**
     * override to start calculations after our turn has ended
     */
	public void afterTakeAction() {
	}
    
    /**
     * DO NOT OVERRIDE!!!
     * this method is called from within the timer
     */
    public void takeAction(long startTime, long stopTime) {
    	sendAction();
    	timerManager.turnEnded(startTime, stopTime);
    	afterTakeAction();
    }
}

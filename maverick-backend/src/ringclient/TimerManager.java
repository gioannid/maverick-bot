package ringclient;

import ca.ualberta.cs.poker.free.dynamics.Card;
import ringclient.interfaces.ITimerFunction;
import ringclient.interfaces.StateChangeListener;
import data.Action;
import data.Constants;
import data.GameState;


/**
 * the TimerManager starts a timer for a certain game state
 * Created on 19.04.2008
 * Modifications for Maverick
 * @author Kami II
 */
public class TimerManager implements StateChangeListener {
	
	/**
	 * use 50% of the handTime for preflop decisions
	 * 	   30% for flop decisions
	 * 	   15% for turn decisions
	 *      5% for river decisions
	 *      
	 * because it is calculated from remaining time of one round
	 * the values are different!!!
	 */
	private double pPreflop = 0.5;
	private double pFlop = 0.6;
	private double pTurn = 0.75;
	private double pRiver = 1.0;
	
	private double betRoundFactor = 0.5;
	
	/**
	 * in one betting round we can raise 4 times at maximum
	 */
	protected int betsLeft = 4;
	
	/**
	 * here is the data saved to calculated a good buffer value 
	 */
	private long rounds = 1;
	private long timeBufferSum = 5;

	/**
	 * here are the average times saved for the decisions to make 
	 */	
	private long timeBuffer = 5;
	private long timeInState = 0;
	private long timePerRound = 0;
	   
	/**
	 * The hands and the time remaining (in milliseconds)
	 */
	protected long timeRemaining;
	private long roundTimeRemaining;
	private long handsRemaining;
	
	/**
	 * stores the time our turn has started
	 */
	private long turnStartedAt;
	
	/**
	 * the actual game state
	 */
	private GameState gameState;

	/**
	 * the time totally consumed by the bot and
	 * the median that was calculated in last round
	 */
	private long consumedTime;
	private double lastMedian;
	
	/**
	 * DEBUG-Stuff
	 */
	protected long time = 0;
	protected long PFT = 0;
	protected long FT = 0;
	protected long TT = 0;
	protected long RT = 0;
	protected long PFBR = 0;
	protected long FBR = 0;
	protected long TBR = 0;
	protected long RBR = 0;
	
	/**
	 * creates a new timer
	 * @param handTime the time we have to play one hand
	 * @param numHands the amount of hands we have to play
	 */
	public TimerManager(long handTime, long numHands) {
		timePerRound = handTime;
		handsRemaining = numHands;
		timeRemaining = timePerRound * handsRemaining;
		consumedTime = 0;
		
		timePerRound = (long)(timePerRound * 1.1);
		
		roundTimeRemaining = timePerRound;
		gameState = GameState.STARTING;
		calculateTimes();
	}
	
	/**
	 * calculates the time for all game states
	 */
	public void calculateTimes() {
		switch (gameState) {
			case PREFLOP:	timeInState = (long)((long)(roundTimeRemaining * pPreflop) * betRoundFactor); break;
			case FLOP:		timeInState = (long)((long)(roundTimeRemaining * pFlop) * betRoundFactor); break;
			case TURN:		timeInState = (long)((long)(roundTimeRemaining * pTurn) * betRoundFactor); break;
			case RIVER:		timeInState = (long)((long)(roundTimeRemaining * pRiver) * betRoundFactor); break;
		}
		if (timeInState < 200)
			timeInState = 200;
	}
	
	/**
	 * called when we received a new match state string
	 */
	public synchronized void turnStarted() {
		turnStartedAt = System.currentTimeMillis();
	}
	
	/**
	 * called when our turn to play is over
	 */
	public synchronized void turnEnded(long startTime, long stopTime) {
		long timeNeeded = System.currentTimeMillis() - startTime;
		consumedTime += timeNeeded;
        if (handsRemaining > 0) {
        	// calculate remaining time
        	timeRemaining -= timeNeeded;
        	roundTimeRemaining -= timeNeeded;

        	// calculate the time buffer
        	rounds++;
        	//tb=tb-(ts-tn)=tb-ts+tn
        	timeBufferSum = timeBufferSum + timeBuffer - timeInState + timeNeeded;
        	timeBuffer = timeBufferSum / rounds;
        	if (timeBuffer < 1)
        		timeBuffer = 1;
        	
        	// calculate the factor for the betting rounds
        	if (betRoundFactor > 0.2) {
        		betRoundFactor = betRoundFactor / 2;
        		timeInState = (long)(timeInState * betRoundFactor);
        	}
    		if (timeInState < 200)
    			timeInState = 200;

    		time += timeNeeded;
    		switch (gameState) {
				case PREFLOP:	PFT += timeNeeded;	PFBR++;	break;
				case FLOP:		FT += timeNeeded;	FBR++;	break;
				case TURN:		TT += timeNeeded;	TBR++;	break;
				case RIVER:		RT += timeNeeded;	RBR++;	break;
    		}
         }
	}

	/**
	 * wait a certain amount of time according to the given game state
	 * and than execute the given function
	 * @param function the function that should be called
	 */
	public synchronized void waitForDecision(ITimerFunction function) {
        Timer timer = new Timer(turnStartedAt, turnStartedAt + timeInState - timeBuffer, function);
        timer.start();
	}

	/**
	 * overrides the actionPerformed method from StateChangeListener to register performed action
	 */
	public void actionPerformed(int seat, int player, Action action) {
		if (action == Action.RAISE)
			betsLeft--;
	}
	
	/**
	 * overrides the stateChanged method from StateChangeListener to register a new state
	 */
	public void stateChanged(GameState state) {
		gameState = state;
		betsLeft = 4;
		betRoundFactor = 0.5;
		calculateTimes();
	}
	
	/**
	 * overrides the roundFinished method from StateChangeListener to register a new round
	 */
	public synchronized void roundFinished(int ownID, int playerAtSeatZero, int[] amountWon, int[] inPot, Card[][] hole) {
		handsRemaining--;
		betsLeft = 4;
		betRoundFactor = 0.5;
		
		long handTime = Math.round(Constants.HANDTIME * 0.95);
		long handCount = Constants.HANDCOUNT;
		long handNumber = handCount - handsRemaining;

//		kami's ultra neue hammer methode
		double median = (double)consumedTime / (double)(handNumber);
		if ((median < handTime && median <= lastMedian) || 
				(median > handTime && median >= lastMedian))
			timePerRound += (handTime - median) * 0.2;
		lastMedian = median;
		
//		alex's neue methode :)
//		timePerRound += handTime - median;
		
//		kami's neue methode
//		timePerRound += timePerRound * ((double)handTime - median) / ((double)handTime);
		
//		immi's alte methode
//        if (median < handTime)
//        	timePerRound += 100;
//        else
//        	timePerRound -= 100;

//		kami's uralte methode
//		if (handsRemaining > 1)
//			timePerRound = timeRemaining / handsRemaining;
//		else
//			timePerRound = timeRemaining;

		roundTimeRemaining = timePerRound;
        calculateTimes();
	}
	
	@Override
	public void gameStarts(ClientRingDynamics state) {
		;
	}
}

package johnidis.maverick;

import java.io.IOException;
import java.net.*;
import java.util.*;

import johnidis.maverick.exploitbot.ExpertGame;
import johnidis.maverick.exploitbot.Simulator;
import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.utils.AbortException;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Hand;

import ca.ualberta.cs.poker.free.dynamics.LimitType;
import ca.ualberta.cs.poker.free.dynamics.MatchType;
import data.Constants;
import data.CardGenerator;
import data.GameState;
import ringclient.ActionFunction;
import poker.MaverickGameInfo;
import ringclient.AdvancedRingClient;
import ringclient.ClientRingDynamics;
import ringclient.ClientRingStateParser;
import ringclient.TimerManager;
import ringclient.interfaces.ITimerFunction;
import simulation.real.SimulationGame;

public class SimBot extends AdvancedRingClient implements Agent {
	
	private static final boolean DEBUG = true;
	
	public static final long				COMPUTE_MOVE_TIMEOUT;

	private static final int				SEAT_UNKNOWN				= -1;
	private static final double				MAXIMUM_MODEL_ERROR			= 2;
	
    protected Simulator simulator;
    private ITimerFunction function;
	private volatile Action lastAction = null;
	private volatile boolean actionInProgress = false;
	private StringBuilder lastBettingSequence;
	private StringBuilder lastCards;
	private MaverickGameInfo gameInfo;
	private int mySeat = SEAT_UNKNOWN;
	private List<String> dirtBag = new Vector<String>();
	private ClientRingStateParser testParser = new ClientRingStateParser();
	private boolean stageCompleted;
	private final Holdem holdemSession;
	private final SimulationGame game;
	
	
	static {
		if (Preferences.SIM_HANDTIME.set()) {
			long ht = Preferences.SIM_HANDTIME.getValue();
			Constants.HANDTIME = ht;
			COMPUTE_MOVE_TIMEOUT = ht;
			System.out.println("Setting simulation time (ms) as " + ht);
		} else
			COMPUTE_MOVE_TIMEOUT = Constants.HANDTIME;
	}
	

	public SimBot (Holdem holdem, SimulationGame game) {
		this.game = game;
		simulator = new Simulator(holdem, game);
		function = new ActionFunction(this);
		holdemSession = holdem;
 		setVerbose (true);
	}
	
	
	public static int getLastStage (String from) {
		int s = Holdem.PREFLOP - 1;
		int ld = -1;
		do {
			s++;
			ld++;
			ld = from.indexOf("/", ld);
		} while ((ld > -1) && (s < Holdem.RIVER));
		return s;
	}

	
	public int getNextSeatToAct (String from) {
		if (gameInfo == null)
			return SEAT_UNKNOWN;
		int s = (gameInfo.isReverseBlinds() ? 1 : (gameInfo.getNumPlayers() > 2 ? 2 : 0));
		if (from == null)
			return s;
		int players = gameInfo.getNumPlayers();
		boolean folded[] = new boolean[players];
		int os = SEAT_UNKNOWN;
		for (int p = 0; p < from.length(); p++) {
			if (from.charAt(p) == '/') {
				s = 0;
				while (folded[s] && (s < (players - 1))) {
					s = (s + 1) % players;
				} 
				continue;
			}
			if (from.charAt(p) == 'f')
				folded[s] = true;
			os = s;
			do {
				s = (s + 1) % players;
			} while (folded[s] && (os != s));
		}
		return (s == os ? SEAT_UNKNOWN : s);
	}
	
	protected void initState () {
		timerManager = new TimerManager(Constants.HANDTIME*2, 1);
		ClientRingStateParser crsp = new ClientRingStateParser();
		crsp.parseMatchStateMessage(currentGameStateString);
		MatchType mt = new MatchType(LimitType.LIMIT, false, 0, 1);
		Constants.PLAYER_COUNT = crsp.numPlayers;
		state = new ClientRingDynamics(crsp.numPlayers, mt, crsp);
		state.info.bigBlindSize = (int) gameInfo.getBigBlindSize();
		state.info.smallBlindSize = (int) gameInfo.getSmallBlindSize();
		state.info.smallBetSize = (int) gameInfo.getBigBlindSize();
		state.info.bigBetSize = (int) gameInfo.getBigBetSize();
		state.additionalWon = null;
		for(int i = 0; i < crsp.numPlayers; i++) {
			state.stack[GameAdapter.playerToSeat(gameInfo,i)] = (int) gameInfo.getPlayer(i).getBankRoll();
		}
		state.setParser(crsp);
		state.addStateChangeListener(timerManager);
		for (int p = 0; p < crsp.numPlayers; p++) {
			String name = gameInfo.getPlayerName(p);
			int seat = GameAdapter.playerToSeat(gameInfo,p);
			state.botNames[seat] = name;
		}
		timerManager.gameStarts(state);
	}
	
	protected StringBuilder preflopHand (Card c1, Card c2, int players) {
		StringBuilder cards = new StringBuilder ();
		for (int p = 0; p < players; p++) {
			if ((p == mySeat) && (c1 != null) && (c2 != null))
				cards.append(c1.toString()).append(c2.toString());
			if (p < (players - 1))
				cards.append("|");
		}
		return cards;
	}
	
	protected StringBuilder matchStateMessage (long handNumber, String bettingSequence, String cards) {
		StringBuilder msg = new StringBuilder ("MATCHSTATE:"); 
		msg.append(mySeat).append(":").append(handNumber).append(":").append(bettingSequence).
				append(":").append(cards);
		return msg;
	}
	
	protected void emptyTrash () {
		if (dirtBag.size() > 0)
			dirtBag = new Vector<String> ();
	}
	
	protected int flushHoldStates () {
		if (state == null)
			return 0;
		int s = 0;
		s = dirtBag.size();
		for (String m : dirtBag)
			matchState(gameInfo.getGameID(), m, lastCards.toString());
		emptyTrash ();
		return s;
	}
	
	protected void inferMissingActions (int seatToAct, int stage, boolean roundFinished) {
		int ns = getNextSeatToAct(lastBettingSequence.toString());
		if ((seatToAct != ns) && (ns != SEAT_UNKNOWN)) {
			holdemSession.println("WARNING: Simbot lost sync ("+seatToAct+" instead of "+ns+"), inferring actions...");
			int p = ns;
			do {
				updateMatchBettingSequence (p, stage, 
						Iface.checkOrCall(gameInfo, (int) gameInfo.getBetAmount()), (! roundFinished));
				p = state.getNextActiveSeat(p);
			} while ((p != seatToAct) && (p != ns));
		}
	}
	
	protected void matchState (StringBuilder msg) {
		String message = msg.toString();
		holdemSession.println (message);
		testParser.parseMatchStateMessage(message);
		if ((mySeat >= 0) && ((testParser.hole[GameAdapter.playerToSeat(gameInfo,mySeat)] == null)
			|| (testParser.numPlayers != gameInfo.getNumPlayers())))
			return;
		updateGameState (message);
	}
	
	protected void matchState (long handNumber, String bettingSequence, String cards) {
		StringBuilder msg = matchStateMessage (handNumber, bettingSequence, cards);
		matchState (msg);
	}
	
	protected void updateMatchState (long handNumber, String bettingSequence, String cards) {
		matchState (handNumber, bettingSequence, cards);
		lastBettingSequence = new StringBuilder (bettingSequence);
		lastCards = new StringBuilder (cards);
	}
	
	protected void updateMatchState () {
		updateMatchState (gameInfo.getGameID(), lastBettingSequence.toString(), lastCards.toString());
	}
	
	protected void updateMatchBettingSequence (int seatToAct, int stage, Action action, boolean moreToAct) {
		flushHoldStates ();
		int lastStage = getLastStage (lastBettingSequence.toString());
		for (int s = 1; s <= (stage - lastStage); s++)
			lastBettingSequence.append('/');
		if (action.isBetOrRaise()) {
			lastBettingSequence.append('r');
		} else if (action.isCheckOrCall()) {
			lastBettingSequence.append('c');
		} else if (action.isFold ()) {
			lastBettingSequence.append('f');
		}
		if ((! moreToAct) && (gameInfo.getStage() < Holdem.RIVER) && (gameInfo.getNumActivePlayers() > 1))
			lastBettingSequence.append('/');
		if (moreToAct)
			matchState (gameInfo.getGameID(), lastBettingSequence.toString(), lastCards.toString());
		else {
			dirtBag.add (lastBettingSequence.toString());
			stageCompleted = true;
		}
	}

	protected void updateMatchCards (int seatToAct, int stage, Hand hand, int players) {
		if (stage == Holdem.SHOWDOWN)
			return;
		int lastStage = getLastStage (lastCards.toString());
		for (int s = 1; s <= (stage - lastStage); s++)
			lastCards.append("/");
		switch (stage) {
			case Holdem.PREFLOP:
				lastCards = preflopHand (hand.getCard(1), hand.getCard(2), players);
				break;
			case Holdem.FLOP:
				lastCards.append(hand.getCard(1).toString()).append(hand.getCard(2).toString()).
						append(hand.getCard(3).toString());
				break;
			case Holdem.TURN:
				lastCards.append(hand.getCard(4).toString());
				break;
			case Holdem.RIVER:
				lastCards.append(hand.getCard(5).toString());
				break;
		}
		emptyTrash ();
		matchState (gameInfo.getGameID(), lastBettingSequence.toString(), lastCards.toString());
	}
	
	@Override
	public synchronized void close() throws IOException {
		;
	}

	@Override
	public void connect(InetAddress arg0, int arg1) throws IOException,	SocketException {
        ;
	}

	@Override
	public String getClientID() {
		return "SimBot";
	}

	@Override
	public String receiveMessage() throws SocketException, IOException {
		throw new RuntimeException ("SimBot.receiveMessage() not supported");
	}

	@Override
	public void run() {
		;
	}

	@Override
	public synchronized void sendMessage(String arg0) throws SocketException, IOException {
		throw new RuntimeException ("SimBot.sendMessage(String) not supported");
	}
	
    public void sendAction() {
        try {
        	data.Action a = simulator.getDecision();
        	if (a == null) {
        		sendAction ('Q');
        		return;
        	}
        	if (a == data.Action.RAISE && state.canRaise(state.seatTaken))
        		sendRaise();
            else if (a == data.Action.RAISE || a == data.Action.CALL)
            	sendCall();
            else
            	sendFold();
        } catch(Exception e) {
            holdemSession.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void afterTakeAction() {
    	simulator.killTree();
    }
    
	@Override
	public void sendAction(char arg0) throws IOException, SocketException {
		if (arg0 == 'Q')
			lastAction = Action.muckAction();
		else
			throw new RuntimeException ("SimBot.sendAction(char) not supported for "+arg0);
	}

	@Override
	public void sendAction(String arg0) throws IOException, SocketException {
		throw new RuntimeException ("SimBot.sendAction(String) not supported");
	}

	@Override
	public void sendCall() throws IOException, SocketException {
		if (lastAction != null) {
			if (lastAction.isMuck()) {
				holdemSession.println  ("WARNING simulator has aborted, discarding sendCall()");
				return;
			}
			holdemSession.println  ("WARNING action overwritten by sendCall() : " + Iface.action(lastAction));
		}
		lastAction = Iface.checkOrCall (gameInfo, (int) gameInfo.getBetAmount());
	}

	@Override
	public void sendFold() throws IOException, SocketException {
		if (lastAction != null) {
			if (lastAction.isMuck()) {
				holdemSession.println  ("WARNING simulator has aborted, discarding sendFold()");
				return;
			}
			holdemSession.println  ("WARNING action overwritten by sendFold() : " + Iface.action(lastAction));
		}
		lastAction = Action.foldAction(gameInfo);
	}

	@Override
	public void sendRaise() throws IOException, SocketException {
		if (lastAction != null) {
			if (lastAction.isMuck()) {
				holdemSession.println  ("WARNING simulator has aborted, discarding sendRaise()");
				return;
			}
			holdemSession.println  ("WARNING action overwritten by sendRaise() : " + Iface.action(lastAction));
		}
		lastAction = Iface.betOrRaise (gameInfo, (int) (gameInfo.getBetAmount() + gameInfo.getBetSize()));
	}

	@Override
	public void sendRaise(int arg0) throws IOException, SocketException {
		if (lastAction != null) {
			if (lastAction.isMuck()) {
				holdemSession.println  ("WARNING simulator has aborted, discarding sendRaise(int)");
				return;
			}
			holdemSession.println  ("WARNING action overwritten by sendRaise(int) : " + Iface.action(lastAction));
		}
		lastAction = Iface.betOrRaise (gameInfo, arg0);
	}
	
	@Override
    public synchronized void startTakeAction() {
		if (game.capabilities().supported(Capabilities.BEHAVIORAL_MODELLING))
			for (int p = 0; p < state.numPlayers; p++)
				for (int r = state.roundIndex; r < Holdem.SHOWDOWN; r++)
					if (state.active[p]) {
						double error = Holdem.modeller.open(state.botNames[p]).getError(Holdem.BEHAVIORAL_MODEL_ACTION, r);
						if (error > MAXIMUM_MODEL_ERROR) {
							holdemSession.println("Too high error for "+state.botNames[p]+" during round "+r+
									" ("+error+"): ExploitSimulator aborting");
							lastAction = Action.muckAction();
							return;
						}
					}
		actionInProgress = true;
    	try {
        	simulator.startSimulation(state, gameInfo);
        	timerManager.waitForDecision(function);
    	} catch (AbortException e) {
			holdemSession.err(e.getMessage());
			simulator.killTree();
			lastAction = Action.muckAction();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

	public synchronized Action getLastAction (long timeout) {
		long time = System.currentTimeMillis();
		do {
			if (lastAction != null)
				break;
			else
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					lastAction = null;
				}
		} while ((System.currentTimeMillis() - time) < timeout);
		Action a = lastAction;
		if (a != null) {
			actionInProgress = false;
			if (a.isMuck())
				a = null;
		} else {
			holdemSession.err ("(null action)");
		}
		return a;
	}
	
	public void updateGameState (String message) {
        if (message.startsWith("MATCHSTATE:")) {
            currentGameStateString = message;
    		if (state == null)
    			initState ();
            handleStateChange();
        }
	}

	@Override
	public String getId() {
		return getClientID ();
	}

	@Override
	public Capabilities capabilities() {
		return game.capabilities();
	}

	@Override
	public void setEconomyMode(boolean econ) {
		//TODO : economy mode
	}

	@Override
	public void gameStartEvent(GameInfo gInfo) {
		if (! (gInfo instanceof MaverickGameInfo)) {
			throw new RuntimeException ("SimBot.gameStartEvent invoked without a MaverickGameInfo structure");
		}
		gameInfo = (MaverickGameInfo) gInfo;
		mySeat = SEAT_UNKNOWN;
		lastBettingSequence = new StringBuilder ();
		lastCards = preflopHand (null, null, gameInfo.getNumPlayers());
		stageCompleted = false;
	}

	@Override
	public void holeCards(Card c1, Card c2, int id) {
		mySeat = GameAdapter.playerToSeat(gameInfo,id);
		lastCards = preflopHand (c1, c2, gameInfo.getNumPlayers());
	}

	@Override
	public void actionEvent(int pos, Action act) {
		if (! isActionInProgress ())
			lastAction = null;
		if ((! stageCompleted) && (! act.isBlind())) {
			inferMissingActions (GameAdapter.playerToSeat(gameInfo,pos), gameInfo.getStage(), false);
			updateMatchBettingSequence (GameAdapter.playerToSeat(gameInfo,pos), gameInfo.getStage(), act, (gameInfo.getNumToAct() > 0));
		}
	}

	@Override
	public Action getAction() {
		if ((! isActionInProgress ()) && (lastAction == null)) {
			if (flushHoldStates () == 0)
				updateMatchState ();
		}
		return getLastAction ((long) (COMPUTE_MOVE_TIMEOUT * 1.5));
	}

	@Override
	public void stageEvent (int stage) {
		inferMissingActions (GameAdapter.playerToSeat(gameInfo,gameInfo.nextActivePlayer(gameInfo.getButtonSeat())), stage - 1, true);
		stageCompleted = false;
		updateMatchCards (gameInfo.getCurrentPlayerSeat(), stage, 
				gameInfo.getBoard(), gameInfo.getNumPlayers());
		if (game.capabilities().supported(Capabilities.EXPERT_MODELLING)) {
			ExpertGame.modeller.newStage(gameInfo);
			if ((DEBUG) && (stage == Holdem.FLOP))
				for (int s = 0; s < state.numPlayers; s++)
					if ((state.active[s]) && (s != mySeat)) {
						System.out.println(state.botNames[s]+" preflop probabilities:");
						System.out.println(ExpertGame.modeller.open(state.botNames[s]).toString(state.handNumber));
					}
		}
	}

	public void gameOver (boolean abort) {
		if ((gameInfo != null) && (state != null)) {
			int players = gameInfo.getNumPlayers();
			if (! abort) {
				int length = lastCards.length();
				int pld, ld = -1;
				for (int p = 0; p < players; p++) {
					if (ld < length) {
						pld = ld;
						ld = lastCards.indexOf("|", ld + 1);
						if (ld == -1)
							ld = length;
						if ((ld == pld + 1) && state.active[p]) {
							holdemSession.println ("No hole info for player " + p);
						}
					}
				}
			}
			if (! state.handOver) {
				state.readyToEndHand = true;
				flushHoldStates ();
				if (! state.handOver) {
					state.endHand();
					state.updateBankroll();
					state.setGameState(GameState.SHOWDOWN);
					state.roundFinished();
				}
			}
			if (actionInProgress) {
				getLastAction (10000);
			}
		}
		state = null;
		emptyTrash ();
	}

	@Override
	public synchronized boolean isActionInProgress() {
		return actionInProgress;
	}

	@Override
	public void init(com.biotools.meerkat.util.Preferences paramPreferences) {
		;
	}

	@Override
	public void showdownEvent(int id, Card c1, Card c2) {
		int ld = -1;
		int pid = GameAdapter.playerToSeat(gameInfo,id);
		for (int p = 0; p < pid; p++) {
			if ((ld > -1) || (p == 0))
				ld = lastCards.indexOf("|", ld + 1);
		}
		if ((ld > -1) || (pid == 0)) {
			lastCards.insert(ld + 1, c1.toString() + c2.toString());
		} else {
			holdemSession.println  ("WARNING lost hole cards during showdown : " + c1.toString() + c2.toString() + " for player "+ pid);
		}
		state.hole[pid] = new ca.ualberta.cs.poker.free.dynamics.Card[2];
		state.hole[pid][0] = CardGenerator.getCard (c1.getRank(), c1.getSuit());
		state.hole[pid][1] = CardGenerator.getCard (c2.getRank(), c2.getSuit());
		if (DEBUG)
			if (game.capabilities().supported(Capabilities.EXPERT_MODELLING))
				System.out.println(ExpertGame.modeller.open(state.botNames[pid]).toString(state.handNumber));
	}

	@Override
	public void dealHoleCardsEvent() {
		;
	}

	@Override
	public void gameOverEvent() {
		gameOver (false);
	}

	@Override
	public void winEvent(int paramInt, double paramDouble, String paramString) {
		if (state != null) {
			if (state.additionalWon == null)
				state.additionalWon = new int[state.numPlayers];
			state.additionalWon[GameAdapter.playerToSeat(gameInfo,paramInt)] = (int) paramDouble;
		}
	}

	@Override
	public void gameStateChanged() {
		;
	}

	@Override
	public void playerUpdate(int seat) {
		String name = gameInfo.getPlayerName(seat);
		if (state != null)
			state.botNames[GameAdapter.playerToSeat(gameInfo,seat)] = name;
	}

}

package johnidis.maverick;

import java.io.*;
import java.net.*;
import java.util.*;

import ca.ualberta.cs.poker.free.dynamics.Card;
import ca.ualberta.cs.poker.free.dynamics.LimitType;
import ca.ualberta.cs.poker.free.dynamics.MatchType;
import ca.ualberta.cs.poker.free.dynamics.RingDynamics;
import data.Bucket;
import data.CardGenerator;

public class TestClient extends RingDynamics implements Runnable {
	
	private boolean UNIFORM_BUCKET_DISTRIBUTION;
	
	public static final String			ALWAYS_CALL_OPPONENT		= "call";
	public static final String			ALWAYS_RAISE_OPPONENT		= "raise";
	public static final String			RANDOM_ACTION_OPPONENT		= "random";
	public static final String			HERO						= "hero";
	
	protected static final int			INITIAL_BANKROLL 			= 10000;
	protected static final double		RANDOM_ACTION_FOLD_PROB		= 0.1;
	protected static final double		RANDOM_ACTION_RAISE_PROB	= 
			(1 - RANDOM_ACTION_FOLD_PROB) / 2 + RANDOM_ACTION_FOLD_PROB;
	
	public static String[] opponents = null;
	
    protected int port;
	protected int roundBetToMeet;
	protected int ownPlayer = -1;
	protected Socket[] socket;
	protected Formatter[] out;
	protected BufferedReader[] in;
	protected long delay = 0;
	
	public TestClient (int players, int rounds) {
		super (players, new MatchType (LimitType.LIMIT, true, INITIAL_BANKROLL, rounds), null);
		if (rounds > 0)
			System.out.println("Initializing test subsystem with "+players+" players, running for "+rounds+" rounds.");
		else
			System.out.println("Initializing test subsystem with "+players+" players, running indefinitely.");
	}
	
	protected String ask (int i, String ftext, Object...args) {
		String result = null;
		out[i].format(ftext, args);
		out[i].flush();
		try {
			do {
				result = in[i].readLine();
			} while ((result.startsWith(Iface.MSG_WARNING)) || (result.startsWith(Iface.MSG_FROM)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	protected boolean tell (String ftext, Object...args) {
		boolean ok = true;
		for (int i = 0; i < numPlayers; i++)
			if (socket[i] != null)	{
				String result = ask (i, ftext, args);
				if (! "OK".equals(result))
					ok = false;
			}
		return ok;
	}
	
	protected void initConnection (int i, String addr, int port) throws UnknownHostException, IOException {
		socket[i] = new Socket (addr, port);
		out[i] = new Formatter(socket[i].getOutputStream());
		in[i] = new BufferedReader(new InputStreamReader(socket[i].getInputStream()));
		in[i].readLine();
		ask(i, Iface.CMD_TESTER + " %s\r\n", botNames[i]);
	}
	
	
	public void init () {
		UNIFORM_BUCKET_DISTRIBUTION = Preferences.TEST_UNIFORMBUCKETS.isOn();
		try {
			Long d = Preferences.TEST_ACTIONDELAY.getValue();
			if (d != null) {
				delay = d;
				System.out.println("Yielding "+delay+"ms per action");
			}
		} catch (NumberFormatException e) {
			System.out.println("Could not set action delay "+Preferences.TEST_ACTIONDELAY.get());
		}
		int p;
		for (p = 0; p < numPlayers; p++) {
			if (opponents[p] == null) {
				opponents[p] = HERO;
				ownPlayer = p;
				break;
			}
		}
		if (p == numPlayers)
			BotServer.invocationError(BotServer.ERROR_INVALID_MIX_OF_ARGUMENTS);
		System.out.println("Hero is on seat: "+ownPlayer);
		String[] names = new String[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			if (i == ownPlayer)
				names[i] = "Hero";
			else if (ALWAYS_CALL_OPPONENT.equals(opponents[i]))
				names[i] = "c"+i;
			else if (ALWAYS_RAISE_OPPONENT.equals(opponents[i]))
				names[i] = "r"+i;
			else if ((opponents[i] != null) && opponents[i].contains(":"))
				names[i] = "b"+i;
			else
				names[i] = "o"+i;
		}
		botNames = names;
	}

	public void connect (int p) {
		port = p;
		new Thread(this).start();
	}
	
	@Override
	public void handleCall () {
		tell (Iface.CALL+" %d %d\r\n", seatToPlayer(seatToAct), roundBetToMeet);
		super.handleCall ();
	}

	@Override
	public void handleFold () {
		tell (Iface.FOLD+" %d\r\n", seatToPlayer(seatToAct));
		super.handleFold ();
	}

	@Override
	public void handleRaise () {
		roundBetToMeet += getFullRaiseAmount();
		tell (Iface.RAISE+" %d %d\r\n", seatToPlayer(seatToAct), roundBetToMeet);
		super.handleRaise ();
	}
	
	public void handleRaiseIfPossible () {
		if (canRaise(seatToAct))
			handleRaise ();
		else
			handleCall ();
	}
	
	@Override
	public void incrementRound () {
		super.incrementRound();
		roundBetToMeet = 0;
		switch (roundIndex) {
			case Holdem.FLOP:
				tell (Iface.CMD_FLOP+" %s %s %s\r\n", board[0].toString(), board[1].toString(), board[2].toString());
				break;
			case Holdem.TURN:
				tell (Iface.CMD_TURN+" %s\r\n", board[3].toString());
				break;
			case Holdem.RIVER:
				tell (Iface.CMD_RIVER+" %s\r\n", board[4].toString());
				break;
			case Holdem.SHOWDOWN:
				for (int i = 0; i < numPlayers; i++) {
					if (socket[i] != null) {
						for (int p = 0; p < numPlayers; p++) {
							if ((i != p) && (active[playerToSeat(p)])) {
								ask (i, Iface.CMD_HOLE_CARDS+" %d %s %s\r\n", p, hole[playerToSeat(p)][0].toString(), hole[playerToSeat(p)][1].toString());
							}
						}
						out[i].format (Iface.CMD_WINNERS+" %d", numWinners);
						for (int p = 0; p < numPlayers; p++) {
							int s = playerToSeat(p);
							if (potWinners[s]) {
								out[i].format(" %d %d", p, grossWon[s]);
							}
						}
						ask (i, "\r\n");
					}
				}
		}
	}
	
	public int dealCards (int bucket) {
		int b = bucket;
		hole = new Card[numSeats][];
		CardGenerator deck = new CardGenerator ();
		Card[][] cards = deck.getHoleAndBoard(bucket, bucket, numPlayers);
		for (int i = 0; i < numSeats; i++)
			if (i != playerToSeat(ownPlayer))
				hole[i] = deck.getHole();
			else
				hole[i] = cards[0];
		board = cards[1];
		return b;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			;
		}
		socket = new Socket[numPlayers];
		out = new Formatter[numPlayers];
		in = new BufferedReader[numPlayers];
		try {
			initConnection (ownPlayer, null, port);
			for (int i = 0; i < numPlayers; i++)
				if ((opponents[i] != null) && opponents[i].contains(":")) {
					String[] token = opponents[i].split(":");
					initConnection (i, token[0], Integer.valueOf(token[1]));
				}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Starting testing...");
		while ((handNumber < info.numHands) || (info.numHands == 0)) {
  	
			try {

				setHandNumber(handNumber+1);
				if (hole!=null){
					nextSeats();
				}
				System.out.println("#######################");
				System.out.println("### Round "+handNumber);
				System.out.println("#######################");
		        initializeBets();
		        if (UNIFORM_BUCKET_DISTRIBUTION) {
		        	int bucket = (int) ((handNumber / numPlayers) % Bucket.BUCKET_COUNT);
		        	bucket = dealCards(bucket);
		        	System.out.println ("Hero hand bucket: "+bucket);
		        } else
		        	dealCards(Holdem.RNG);
				readyToEndHand = true;
				for (int j = 0; j < numPlayers; j++)
					if (socket[j] != null) {
						out[j].format(Iface.CMD_START_NEW_GAME+" %d %d %d %d", info.bigBlindSize, numPlayers, seatToPlayer(numSeats - 1), handNumber);
						int sbPlayer = seatToPlayer((numPlayers == 2 ? 1 : 0));
						int bbPlayer = seatToPlayer((numPlayers == 2 ? 0 : 1));
						for (int i = 0; i < numPlayers; i++) {
							int s = stack[i];
							if (i == sbPlayer)
								s += info.smallBlindSize;
							if (i == bbPlayer)
								s += info.bigBlindSize;
							out[j].format(" %s %d", botNames[i], s);
						}
						out[j].format("\r\n");
						out[j].format(Iface.CMD_HOLE_CARDS+" %d %s %s\r\n", j, 
								hole[playerToSeat(j)][0].toString(), hole[playerToSeat(j)][1].toString());
						out[j].format(Iface.CMD_BLIND+" %d %d\r\n"+Iface.CMD_BLIND+" %d %d\r\n", 
								sbPlayer, info.smallBlindSize, bbPlayer, info.bigBlindSize);
						out[j].flush();
						in[j].readLine();
						in[j].readLine();
						in[j].readLine();
						in[j].readLine();
					}

				roundBetToMeet = info.bigBlindSize;
				while(!isGameOver()){
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						;
					}
					int nextPlayer = seatToPlayer(seatToAct); 
					if (socket[nextPlayer] != null) {
						String action = ask (nextPlayer, Iface.CMD_NEXT_TO_ACT+" %d\r\n", nextPlayer);
						if (action.startsWith(Iface.RAISE)) {
							handleRaise ();
						} else if (action.startsWith(Iface.CALL)) {
							handleCall ();
						} else {
							handleFold ();
						}
					} else if (ALWAYS_CALL_OPPONENT.equals(opponents[nextPlayer])) {
						handleCall ();
					} else if (ALWAYS_RAISE_OPPONENT.equals(opponents[nextPlayer])) {
						handleRaiseIfPossible ();
					} else {
						double r = Holdem.RNG.nextDouble();
						if (r < RANDOM_ACTION_FOLD_PROB)
							handleFold ();
						else if (r < RANDOM_ACTION_RAISE_PROB)
							handleRaiseIfPossible ();
						else
							handleCall ();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		System.out.println("Testing completed.");
		for (int i = 0; i < numPlayers; i++)
			if (socket[i] != null){
				out[i].format(Iface.QUIT);
				out[i].flush();
			}
		try {
			for (int i = 0; i < numPlayers; i++) {
				if (in[i] != null)
					in[i].close();
				if (out[i] != null)
					out[i].close();
				if (socket[i] != null)
					socket[i].close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

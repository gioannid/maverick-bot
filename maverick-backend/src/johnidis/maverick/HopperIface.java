package johnidis.maverick;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import poker.MaverickGameInfo;
import johnidis.maverick.modelling.*;
import johnidis.maverick.modelling.modellers.Modeller;

public class HopperIface implements Runnable {
	
	static final Object instanceLock = new Object();
	static volatile WeakReference<HopperIface> instance = null;
	
	private static final long inactiveBotTimer;
	private static final boolean echo = Preferences.UI_ECHOHOPPER.isOn();

	Socket client;
	final List<String> externalMessages = new ArrayList<String>();

	private final BufferedWriter out;
	private final BufferedReader in;

	static {
		if (Preferences.UI_INACTIVETIMER.set()) {
			inactiveBotTimer = Preferences.UI_INACTIVETIMER.getValue();
			System.out.println("Session considered inactive after idling for "+
					inactiveBotTimer+" ms");
		} else
			inactiveBotTimer = 60 * 1000;
	}
	
	static HopperIface getInstance () {
		HopperIface hopper = null;
		synchronized (instanceLock) {
			if (instance != null) {
				hopper = HopperIface.instance.get();
				if ((hopper != null) && (hopper.client == null))
					hopper = null;
			}
		}
		return hopper;
	}


	public HopperIface (Socket s, BufferedReader i, BufferedWriter o) {
		client = s;
		in = i;
		out = o;
	}

	
	private void println(String str) {
		if (echo)
			System.out.println("#/ " + str);
	}

	private void out (String str) {
		try {
			out.write(str + "\r\n");
			out.flush();
			println(BotServer.OUTPUT_PREFIX + str);
		} catch (IOException e) {
			println(e.toString());
		}
	}

	private void send (String str) {
		if (Iface.MSG_NO_ACTION.equals(str))
			synchronized (externalMessages) {
				for (String message : externalMessages)
					out (message);
				if (externalMessages.size() > 0)
					externalMessages.clear();
			}
		out (str);
	}
	
	private BotServer bot (String session) {
		synchronized (BotServer.allInstances) {
			for (BotServer bot : BotServer.allInstances)
				if ((bot != null) && (bot.client != null) && (bot.holdemSession.toString().equals(session)))
					return bot;
			send (Iface.MSG_WARNING_SESSION_NOT_FOUND);
		}
		return null;
	}
	
	private void sendPlayerStats (String player, Modeller modeller) {
		send ("MODELLER "+modeller.toString());
		Object playerModeller = modeller.isOpen(player) ? 
				modeller.open (player) : 
				modeller.instantiate (modeller.playerKey(player), player);
		send (playerModeller.toString());
	}
	
	@Override
	public void run() {
		String msg;
		out (Iface.MSG_NO_ACTION);
		try {
			while (client != null) {
				msg = in.readLine();
				if (msg == null) {
					msg = Iface.QUIT;
				}
				println(BotServer.INPUT_PREFIX + msg);
				String[] token = msg.split(" ");
				if ((token != null) && (token.length > 0))	{					
					token[0] = token[0].toUpperCase();
					
					if (Iface.QUIT.equals(token[0])) {							// QUIT [session]
						if (token.length == 1) {
							println("Hopper, see you later...");
							return;
						} else {
							BotServer bot = bot (token[1]);
							if (bot != null)
								bot.client.close();
							send (Iface.MSG_NO_ACTION);
						}

					} else if (Iface.PING.equals(token[0])) {					// PING
						synchronized (BotServer.allInstances) {
							for (BotServer bot : BotServer.allInstances)
								if ((bot != null) && (bot.client != null))
									send (Iface.PONG + " " + bot.holdemSession.toString() + " " +
											((System.currentTimeMillis() - bot.lastCommandTimestamp > inactiveBotTimer) ?
													Iface.TOKEN_INACTIVE : Iface.TOKEN_ACTIVE));
						}
						send (Iface.MSG_NO_ACTION);

					} else if (Iface.CMD_CHATTER.equals(token[0])) {			// FROM session messagetosession
						BotServer bot = bot (token[1]);
						if (bot != null)
							bot.externalMessage(Iface.MSG_FROM, token, 2);
						send (Iface.MSG_NO_ACTION);
						
					} else if (Iface.CMD_WINNERS.equals(token[0])) {			// WINNER session
						BotServer bot = bot (token[1]);
						if (bot != null) {
							bot.gameOverEvent();
							bot.processingOn = false;
						}
						send (Iface.MSG_NO_ACTION);
					
					} else if (Iface.CMD_NEXT_TO_ACT.equals(token[0])) {		// ACTION? session
						BotServer bot = bot (token[1]);
						if (bot != null)
							bot.processingOn = true;
						send (Iface.MSG_NO_ACTION);
					
					} else if (Iface.CMD_HOLE_CARDS.equals(token[0])) {			// DEAL player
						sendPlayerStats (token[1], Holdem.modeller);
						sendPlayerStats (token[1], Holdem.histogramActions);
//						sendPlayerStats (token[1], Holdem.histogramHands);
						MaverickGameInfo gInfo = new MaverickGameInfo();
						gInfo.addPlayer(token[1], null);
						PaObserver pokiModeller = PaObserver.newInstance(true, gInfo);
						pokiModeller.gameStartEvent(gInfo);
						sendPlayerStats (token[1], pokiModeller);
						PaObserver.deleteInstance(gInfo);
						send (Iface.MSG_NO_ACTION);
					
					} else if (Iface.CMD_START_NEW_GAME.equals(token[0])) {		// NEWGAME [session]
						synchronized (BotServer.allInstances) {
							for (BotServer bot : BotServer.allInstances)
								if ((bot != null) && (bot.client != null) && 
										((token.length == 1) || (bot.holdemSession.toString().equals(token[1])))) {
									StringBuilder resp = new StringBuilder (Iface.CMD_START_NEW_GAME);
									resp.append(' ').append(bot.holdemSession.toString()).append(' ');
									if ((bot.gInfo != null) && (! bot.gInfo.isGameOver())) {
										resp.append((int) bot.gInfo.getBigBlindSize()).append(' ').append(bot.gInfo.getNumPlayers()).
										append(' ').append(bot.gInfo.getButtonSeat()).append(' ').append(bot.gInfo.getGameID());
										for (int p = 0; p < bot.gInfo.getNumPlayers(); p++)
											resp.append(' ').append(bot.gInfo.getPlayerName(p)).append(' ').
											append((int) bot.gInfo.getBankRoll(p));
									}
									send (resp.toString());
								}
						}
						send (Iface.MSG_NO_ACTION);

					} else {
						send(Iface.MSG_WARNING + " " + msg + "?"); 
						send(Iface.MSG_NO_ACTION);
					} 
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			send(Iface.MSG_ERROR + " " + e);
			send(Iface.QUIT);
		} finally {
			if (client != null)
				try {
					client.close();
				} catch (IOException e) {
					;
				} 
			client = null;
		}
	}
	
}

package johnidis.maverick;

import java.text.NumberFormat;
import java.util.Locale;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.GameInfo;

public class Iface {
	
	private static final boolean DEBUG = false;
	
	public static final String PING		 						= "PING";
	public static final String PONG								= "PONG";	
	public final static String FOLD								= "FOLD";
	public final static String CALL								= "CALL";
	public final static String RAISE							= "RAISE";
	public final static String SMALL_BLIND						= "BLIND";
	public final static String BIG_BLIND						= "STRADDLE";
	public static final String QUIT								= "QUIT";

	public static final String CMD_START_NEW_GAME 				= "NEWGAME";
	public static final String CMD_HOLE_CARDS 					= "DEAL";
	public static final String CMD_WINNERS 						= "WINNER";
	public static final String CMD_ABORT 						= CMD_WINNERS + " 0";
	public static final String CMD_CHATTER						= "FROM";
	public static final String CMD_NEXT_TO_ACT					= "ACTION?";
	public final static String CMD_FLOP							= "FLOP";
	public final static String CMD_TURN							= "TURN";
	public final static String CMD_RIVER 						= "RIVER";
	public final static String CMD_BLIND						= SMALL_BLIND;
	public final static String CMD_FRONTEND						= "FROM FRONTEND";
	public final static String CMD_HOPPER						= "FROM HOPPER";
	public final static String CMD_TESTER						= "FROM TESTER";

	public static final String MSG_NO_ACTION					= "OK";
	public final static String MSG_FROM							= "FROM BACKEND";
	public static final String MSG_BET							= RAISE;
	public final static String MSG_WARNING						= "WARNING";
	public final static String MSG_WARNING_NO_MORE_MOVES		= "WARNING NO MORE MOVES";
	public final static String MSG_WARNING_OUT_OF_SYNC			= "WARNING OUT OF SYNC";
	public final static String MSG_WARNING_GAME_OVER			= "WARNING GAME IS OVER";
	public final static String MSG_WARNING_SESSION_NOT_FOUND	= "WARNING SESSION NOT FOUND";
	public final static String MSG_WARNING_UNEXPECTED_MESSAGE	= "WARNING UNEXPECTED FRONTEND MESSAGE";
	public final static String MSG_ERROR						= "ERROR";
	public final static String MSG_ERROR_UNKNOWN_HERO			= "ERROR UNKNOWN HERO";
	
	public final static String TOKEN_INSTEAD_OF					= " INSTEAD OF ";
	public final static String TOKEN_UNKNOWN_NAME				= "PLAYERNAME";
	public final static String TOKEN_FRONTEND					= "FRONTEND";
	public final static String TOKEN_CHATTER					= "CHAT";
	public final static String TOKEN_ACTIVE						= "ACTIVE";
	public final static String TOKEN_INACTIVE					= "INACTIVE";
	public final static String TOKEN_SYMBOL						= "SYMBOL";
	
	public final static String LINE_SEPARATOR					= System.getProperty("line.separator");
	public final static NumberFormat FORMATTER					= NumberFormat.getInstance(Locale.getDefault());
	
	public static String action(Action action) {
		if (action == null)
			return FOLD;
		int amount = (int) action.getAmount();
		if (action.isBet()) {
			return MSG_BET + " " + amount;
		} else if (action.isRaise()) {
			return RAISE + " " + amount;
		} else if (action.isSmallBlind()) {
			return SMALL_BLIND + " " + amount;
		} else if (action.isBigBlind()) {
			return BIG_BLIND + " " + amount;
		} else if (action.isCheckOrCall()) {
			return CALL + " " + amount;
		} else if (action.isFold()) {
			return FOLD;
		} else if (action.isMuck()) {
			return MSG_WARNING + " MUCK action";
		} else {
			return MSG_WARNING + " ACTION=" + action+ " AMOUNT=" + amount;
		}
	}
	
	public static String externalMessage(String from, String[] token, int first) {
		StringBuilder message = new StringBuilder(from);
		for (int i = first; i < token.length; i++)
			message.append(' ').append(token[i]);
		return message.toString();
	}
	
	public static Action checkOrCall (GameInfo gInfo, int amnt) {
		if (amnt == gInfo.getPlayer(gInfo.getCurrentPlayerSeat()).getAmountInPotThisRound()) {
			if (DEBUG)
				System.out.println("Requested CHECK action");
			return Action.checkAction();		
		} else {
			if (DEBUG)
				System.out.println("Requested CALL "+amnt+" action");
			return new Action (Action.CALL, amnt, amnt); 
		}
	}
	
	public static Action betOrRaise (GameInfo gInfo, int amnt) {
		if (gInfo.getNumRaises() == 0) {
			if (DEBUG)
				System.out.println("Requested BET "+amnt+" action");
			return Action.betAction(amnt);
		} else {
			if (DEBUG)
				System.out.println("Requested RAISE "+amnt+" action");
			return Action.raiseAction (gInfo, amnt);						
		}
	}
	
}

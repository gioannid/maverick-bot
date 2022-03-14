/*This file is part of Fell Omen.

Fell Omen is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

Fell Omen is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Foobar.  If not, see <http://www.gnu.org/licenses/>.*/

package poker;

import poker.ai.HandPotential;
import johnidis.maverick.Agent;
import johnidis.maverick.Holdem;
import johnidis.maverick.Iface;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.util.Preferences;
import com.ibot.FellOmen_2_Impl;

/** 
 * Ian Bot
 * 
 * Description: {Insert Description here}
 * 
 * @author Ian Fellows
 */
public class FellOmen_2 implements Agent
{
	private static final int				SEAT_UNKNOWN				= -1;
	private static final Capabilities		CAPABILITIES				= 
			new Capabilities (2, 2, Capabilities.FIXED_LIMIT);

	private int ourSeat = SEAT_UNKNOWN;       // our seat for the current hand
	private Card c1, c2;       // our hole cards
	private MaverickGameInfo gameInfo;       // general game information
	private int preFlopHistory;
	private int flopHistory;
	private int turnHistory;
	private double flopHR;
	private double flopPOT;
	private double flopNPOT;
	private double turnHR;
	private double turnPOT;
	private double turnNPOT;
	private double riverHR;
	private int flopBDindex;
	private final Holdem holdemSession;

	private FellOmen_2_Impl ibot;   // The overridding action generator
	
	private boolean engaged;
	private int stage;

	public FellOmen_2(Holdem holdem) 
	{ 
		holdemSession = holdem;
		ibot = new FellOmen_2_Impl();
	}  
	
	private boolean checkForEngagement () {
		if ((! engaged) && (gameInfo.isActive(ourSeat))) {
			engaged = true;
			int halfStage = 0, opp = gameInfo.nextActivePlayer(ourSeat);
			String[] actions = new String[] {
					gameInfo.getPlayer(ourSeat).getActions(), gameInfo.getPlayer(opp).getActions()};
			debug("Engaging FellOmen2 for oppponent "+gameInfo.getPlayerName(opp)+", replaying actions "+
					actions[0]+", "+actions[1]);
			int[] a = new int[] {0, 0};
			int o = (actions[1].indexOf(MaverickPlayerInfo.ROUND_DELIMITER) >
					actions[0].indexOf(MaverickPlayerInfo.ROUND_DELIMITER) ? 1 : 0);
			while ((a[0] < actions[0].length()) || (a[1] < actions[1].length())) {
				for (int p = 0; p < 2; p++) {
					if (a[(p+o)%2] < actions[(p+o)%2].length()) {
						char ac = actions[(p+o)%2].charAt(a[(p+o)%2]);
						switch (ac) {
							case MaverickPlayerInfo.RAISE_ACTION:
								actionEvent ((p+o)%2 == 0 ? ourSeat : opp, Action.raiseAction(gameInfo));
								break;
							case MaverickPlayerInfo.CALL_ACTION:
								actionEvent ((p+o)%2 == 0 ? ourSeat : opp, Action.callAction(gameInfo));
								break;
							case MaverickPlayerInfo.ROUND_DELIMITER:
								halfStage++;
								if ((halfStage % 2) == 0)
									stageEvent(halfStage / 2);
						}
						a[(p+o)%2]++;
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * An event called to tell us our hole cards and seat number
	 * @param c1 your first hole card
	 * @param c2 your second hole card
	 * @param seat your seat number at the table
	 */
	@Override
	public void holeCards(Card c1, Card c2, int seat) {
		this.c1 = c1;
		this.c2 = c2;
		this.ourSeat = seat;
		debug(gameInfo.getPlayerName(ourSeat) +": Your Hole Cards Are: " + c1.toString() + " " + c2.toString());
	}

	/**
	 * Requests an Action from the player
	 * Called when it is the Player's turn to act.
	 */
	@Override
	public Action getAction() {
		if (gameInfo.getNumActivePlayers() > 2)
			return new Action (Action.INVALID, 0, 0);
		checkForEngagement ();
		if (stage == Holdem.PREFLOP) {
			return preFlopAction();
		}
		else if(stage == Holdem.FLOP)
			return flopAction();
		else if(stage == Holdem.TURN)
			return turnAction();
		else  
		{
			return riverAction();
		}
	}

	/**
	 * print a debug statement.
	 */
	public void debug(String str) {
		holdemSession.println(str);
	}

	/**
	 * A new betting round has started.
	 */ 
	@Override
	public void stageEvent(int st)
	{
		if (gameInfo.getNumActivePlayers() > 2)
			return;
		if (checkForEngagement ())
			return;
		stage = st;
		ibot.newRound(st);
		if (ourSeat > SEAT_UNKNOWN) {
			if(st == Holdem.FLOP)
			{
				debug("board: " + gameInfo.getBoard().toString());
				HandEvaluator 	he = new HandEvaluator();
				HandPotential   pot = new HandPotential();
				flopHR = he.handRank(c1.toString() , c2.toString() , gameInfo.getBoard().toString() );
				flopPOT = pot.ppot_raw(c1.toString() , c2.toString() , gameInfo.getBoard().toString() , true);
				flopNPOT = pot.getLastNPot();
				flopBDindex = ibot.getFlopBoardIndex(gameInfo.getBoard().toString());
				debug("flop board index = "+flopBDindex);
				debug("flop Hand Strength : " + flopHR + " flop potential: " + flopPOT + " flop negative potential : " + flopNPOT);
			}
			else if(st == Holdem.TURN)
			{
				debug("board: " + gameInfo.getBoard().toString());
				HandEvaluator 	he = new HandEvaluator();
				HandPotential   pot = new HandPotential();
				turnHR = he.handRank(c1.toString() , c2.toString() , gameInfo.getBoard().toString() );
				turnPOT = pot.ppot_raw(c1.toString() , c2.toString() , gameInfo.getBoard().toString() , true);
				turnNPOT = pot.getLastNPot();
				debug("turn Hand Strength : " + turnHR + " turn potential: " + turnPOT + " turn negative potential : " + turnNPOT);			
			}
			else if(st == Holdem.RIVER)
			{
				debug("board: " + gameInfo.getBoard().toString());
				HandEvaluator 	he = new HandEvaluator();
				riverHR = he.handRank(c1.toString() , c2.toString() , gameInfo.getBoard().toString() );
				debug("river Hand Strength : " + riverHR);
			}
		}
	}

	/**
	 * A showdown has occurred.
	 * @param pos the position of the player showing
	 * @param c1 the first hole card shown
	 * @param c2 the second hole card shown
	 */
	public void showdownEvent(int seat, Card c1, Card c2) {}

	/**
	 * An action has been observed. 
	 */
	@Override
	public void actionEvent(int pos, Action act) 
	{
		if (gameInfo.getNumActivePlayers() > 2)
			return;
		if (checkForEngagement ())
			return;
		if (act.isBetOrRaise()) 
			ibot.raise();

		int node = get_action_node(gameInfo);

		//if the end of a round is detected, we record the play history of
		//the round.
		if (act.isCheck() || act.isCall())
		{
			int terminalIndex = nodeToTerminalNodeIndex(node);
			debug("call action observed: " + Iface.action(act));
			debug("position of action : " + pos);
			debug("node of action: " + node + " Terminal Index : " + terminalIndex);
			if(stage == Holdem.PREFLOP)
				this.preFlopHistory=terminalIndex;
			else if(stage == Holdem.FLOP)
				this.flopHistory=terminalIndex;
			else if(stage == Holdem.TURN)
				this.turnHistory=terminalIndex;
		} else
			debug("action observed: " + Iface.action(act));
	}

	/**
	 * The game info state has been updated
	 * Called after an action event has been fully processed
	 */
	public void gameStateChanged() {}  

	/**
	 * The hand is now over. 
	 */
	@Override
	public void gameOverEvent() 
	{
		;
	}

	/**
	 * A player at pos has won amount with the hand handName
	 */
	public void winEvent(int pos, double amount, String handName) {}


	/**
	 * Decide what to do for a pre-flop action
	 *
	 * Uses a really simple hand selection, as a silly example.
	 */
	private Action preFlopAction() 
	{
		return (ibot.preFlopAction(gameInfo, c1, c2, ourSeat));
	}  
	private Action flopAction() 
	{
		return (ibot.flopAction(gameInfo, flopHR, flopPOT, flopNPOT,flopBDindex, ourSeat, preFlopHistory));
	}      
	private Action turnAction() 
	{
		return (ibot.turnAction(gameInfo, turnHR, turnPOT, turnNPOT,flopBDindex, ourSeat, preFlopHistory, flopHistory));
	}  
	private Action riverAction() 
	{
		return (ibot.riverAction(gameInfo, riverHR,flopBDindex, ourSeat, preFlopHistory, flopHistory, turnHistory));
	}  




	protected int get_action_node(MaverickGameInfo gi)
	{
		return(ibot.get_action_node(gi));
	}

	public int nodeToTerminalNodeIndex(int node)
	{
		if(node==1)
			return 0;
		else if (node==2)
			return 1;
		else if (node==3)
			return 2;
		else if (node==4)
			return 3;
		else if (node==6)
			return 4;
		else if (node==7)
			return 5;
		else if (node==8)
			return 6;
		else if (node==9)
			return 7;
		else if (node==10)
			return 8;
		else
			return 8;
	}

	@Override
	public Capabilities capabilities() {
		return CAPABILITIES;
	}

	@Override
	public void setEconomyMode(boolean e) {
		;
	}

	@Override
	public String getId() {
		return "FellOmen2";
	}

	@Override
	public boolean isActionInProgress() {
		return true;	// action is always available upon explicit request
	}

	@Override
	public void init(Preferences paramPreferences) {
		;
	}

	@Override
	public void gameStartEvent(GameInfo paramGameInfo) {
		if (! (paramGameInfo instanceof MaverickGameInfo)) {
			throw new RuntimeException ("FellOmen_2.gameStartEvent invoked without a MaverickGameInfo structure");
		}
		this.c1 = new Card();
		this.c2 = new Card();
		this.ourSeat = -1;
		gameInfo = (MaverickGameInfo) paramGameInfo;

		//clean out our internal data
		this.preFlopHistory=-1;
		this.flopHistory=-1;
		this.turnHistory=-1;

		this.flopHR=-1;
		this.flopPOT=-1;
		this.flopNPOT=-1;
		this.turnHR=-1;
		this.turnPOT=-1;
		this.turnNPOT=-1;
		this.turnHR=-1;
		this.flopBDindex=-1;
		
		engaged = false;
		ibot.newRound(Holdem.PREFLOP);
		stage = Holdem.PREFLOP;
	}

	@Override
	public void dealHoleCardsEvent() {
		;
	}

	@Override
	public void playerUpdate(int seat) {
		;
	}

}
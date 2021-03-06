package data;

/**
 * The game states
 *
 * @author Kami II
 */
public enum GameState {
	
	/**
	 * game state before hands are dealt by the server
	 */
	STARTING {
        public GameState nextState() {
            return PREFLOP;
        }		
	},
	
	/**
	 * game state before the flop is revealed by the server
	 */
	PREFLOP {
        public GameState nextState() {
            return FLOP;
        }
	},
	
	/**
	 * game state after the flop is revealed by the server
	 */
	FLOP {
        public GameState nextState() {
            return TURN;
        }		
	},
	
	/**
	 * game state after the turn is revealed by the server
	 */
	TURN {
        public GameState nextState() {
            return RIVER;
        }		
	},
	
	/**
	 * game state after the river is revealed by the server
	 */
	RIVER {
        public GameState nextState() {
            return SHOWDOWN;
        }		
	},
	
	/**
	 * game state after one game round
	 */
	SHOWDOWN {
        public GameState nextState() {
            return SHOWDOWN;
        }
	};
	
	/**
	 * @return the next game state according to the actual game state
	 */
	public abstract GameState nextState();
	
	/**
	 * resets the game state
	 * @return STARTING - the first game state
	 */
	public GameState reset() {
		return STARTING;
	}

}


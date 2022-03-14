package data;

/**
 * The action to be taken  
 *
 * Created on 16.04.2008
 * @author Kami II
 */
public enum Action {
	
	/**
	 * Fold
	 */
	FOLD {
		public char toChar() {
			return 'f';
		}
		public int toInt() {
			return 0;
		}
	},
		/**
	 * Raise or Bet
	 */
	RAISE {
		public char toChar() {
			return 'r';
		}
		public int toInt() {
			return 1;
		}
	},
	
	/**
	 * Call or Check
	 */
	CALL {
		public char toChar() {
			return 'c';
		}
		public int toInt() {
			return 2;
		}
	};
	
	/**
	 * method to transform the Action into a char
	 * @return the char representation of the action
	 */
	public abstract char toChar();

	public abstract int toInt();

	/**
	 * parses a char into an action enumeration
	 * @param c the char to be transformed
	 * @return the action the is represented by the given char
	 */
    public static Action parseAction(char c) {
        switch(c) {
        	case 'r':	return RAISE;
        	case 'c':	return CALL;
        	case 'f':	return FOLD;
        }
        return null;
    }
}

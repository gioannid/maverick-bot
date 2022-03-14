package johnidis.maverick.modelling.adapters;

import johnidis.maverick.modelling.data.KnownSymbolicValue;
import johnidis.maverick.modelling.data.SymbolicValue;
import johnidis.maverick.modelling.data.UnknownSymbolicValue;

public class NamedSymbol<T> {
	
	public static final int UNKNOWN = -1;
	
	private static final UnknownSymbolicValue NO_SYMBOL = new UnknownSymbolicValue();

	public final T entity;
	public final SymbolicValue value;
	

	public NamedSymbol (T n, SymbolicValue v) {
		entity = n;
		value = v;
	}
	
	public NamedSymbol (T n, int v) {
		entity = n;
		value = new KnownSymbolicValue(v);
	}
	
	public NamedSymbol (T n) {
		entity = n;
		value = NO_SYMBOL;
	}
	
	public int intValue () {
		if (value.isUnknown())
			return UNKNOWN;
		else
			return ((KnownSymbolicValue) value).intValue;
	}

	@Override
	public String toString() {
		return entity.toString();
	}
	
}

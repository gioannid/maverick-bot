package johnidis.maverick.modelling.data;

/**
 * This class implements the value of a symbolic attribute.
 **/
public final class KnownSymbolicValue extends SymbolicValue {
    
    /**
     * This attribute value represented as an integer.
     **/
    public final int intValue;

    
    /**
     * Creates a new symbolic value.
     *
     * @param value This attribute value represented as a (positive) integer.
     **/
    public KnownSymbolicValue(int value) {
	if (value < 0)
	    throw new IllegalArgumentException("Value must be positive");
	
	intValue = value;
    }

    public boolean isUnknown() {
	return false;
    }
    
    public boolean equals(Object o) {
	if (o == null || !(o instanceof KnownSymbolicValue))
	    return false;
	else
	    return ((KnownSymbolicValue) o).intValue == intValue;
    }
    
    public int hashCode() {
	return intValue;
    }

    public String toString() {
	return "" + intValue;
    }
}

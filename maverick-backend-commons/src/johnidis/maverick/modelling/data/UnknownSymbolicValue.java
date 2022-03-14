package johnidis.maverick.modelling.data;

/**
 * This class implements the value of a symbolic attribute.
 **/
public final class UnknownSymbolicValue extends SymbolicValue {
    
    public boolean isUnknown() {
	return true;
    }

    /**
     * Compares this value to another.  Behaves like <code>Object.equals</code>.
     *
     * @param o The object to test.
     * @return <code>true</code> iff <code>o == this</code>.
     **/
    public boolean equals(Object o) {
	return super.equals(o);
    }

    public String toString() {
	return "?";
    }
}

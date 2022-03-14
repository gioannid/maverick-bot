package johnidis.maverick.modelling.data;


/**
 * This class implements the value of an attribute.
 **/
public abstract class AttributeValue {
    
    /**
     * Checks if the value of this attribute is unknown.
     *
     * @return <code>true</code> iff this object implements an unknown value.
     **/
    public abstract boolean isUnknown();
}

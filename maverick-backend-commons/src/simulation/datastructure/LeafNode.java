package simulation.datastructure;

import simulation.interfaces.IArc;

public class LeafNode extends AbstractNode {
	
	public static final int VALUE_PENDING = Integer.MIN_VALUE;
	
	
	private int value = VALUE_PENDING;

	
	public LeafNode(IArc parentArc, long id) {
		super(parentArc, id);
	}

	public LeafNode(long id) {
		super(id);
	}

	public int getValue() {
		return value;
	}
	
	public void setValue(int value) {
		this.value = value;
	}
}

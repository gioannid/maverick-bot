package simulation.datastructure;

import java.util.Vector;

import simulation.interfaces.IArc;
import simulation.interfaces.INode;

public class AbstractNode implements INode {
	protected IArc parentArc;
	protected Vector<IArc> childArcs;
	public final long id;
	
	public AbstractNode(IArc parentArc, long id) {
		initParent(parentArc);
		this.childArcs = new Vector<IArc>();
		this.id = id;
	}
	
	public AbstractNode(long id) {
		this.childArcs = new Vector<IArc>();
		this.id = id;
	}
	
	public void addArc(IArc arc) {
		childArcs.add(arc);
		arc.setParent(this);
	}
	
	public int children() {
		return childArcs.size();
	}

	public Vector<IArc> getChildArcs() {
		return childArcs;
	}

	public IArc getParentArc() {
		return parentArc;
	}

	public void initParent(IArc arc) {
		this.parentArc = arc;
		arc.setChild(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractNode))
			return false;
		final AbstractNode other = (AbstractNode) obj;
		if (id != other.id)
			return false;
		return true;
	}

}

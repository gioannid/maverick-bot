package simulation.real.datastructure;

import simulation.datastructure.AbstractNode;

public class PlayerNode extends AbstractNode {

	public PlayerState player;

	public PlayerNode(PlayerState p, long nodeId) {
		super(nodeId);
		player = p;
	}
	
	public PlayerNode(long id) {
		super(id);
	}
	
	public String toString() {
		return player.getName();
	}


}
package simulation.interfaces;

import ringclient.ClientRingDynamics;
import data.Action;

public interface ISimulator {
	public Action getDecision();
	public void startSimulation(ClientRingDynamics crd);
	public void killTree();
}

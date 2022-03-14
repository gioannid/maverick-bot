package norsys.netica;

import norsys.neticaEx.NodeListEx;

public class MaverickBayesianNetwork extends Net {
	
	/** 
	 *  Make a copy of a net, giving it the name, newName.
	 *  @param net      the net to duplicate
	 *  @param newName  the name of the new net
	 */
	public static MaverickBayesianNetwork duplicate(Net net, String newName) throws NeticaException {
		MaverickBayesianNetwork newNet = new MaverickBayesianNetwork ();
		if (newName != null)
			newNet.setName (newName);
		newNet.copyNodes (net.getNodes());

		NodeList elimOrder = net.getElimOrder();
		NodeList newOrder = NodeListEx.mapNodeList (elimOrder, newNet);
		newNet.setElimOrder (newOrder);

		newNet.setAutoUpdate (net.getAutoUpdate());
		newNet.setTitle (net.getTitle());
		newNet.setComment (net.getComment());
		newNet.user().setReference (net.user().getReference());
		return newNet;
	}

	
	public MaverickBayesianNetwork(Streamer streamer) throws NeticaException {
		super(Environ.getDefaultEnviron());
		NETICA.DeleteNet_bn(this.netPtr);
		this.parentEnv.testForError();
		this.parentEnv = streamer.parentEnv;
		this.netPtr = NETICA.ReadNet_bn(streamer.streamPtr, 16);
//		hookupToNative(this.netPtr, this.parentEnv);
		NETICA.SetJavaApiNet_bn(this.netPtr, this);
		this.parentEnv.testForError();
	}

	public MaverickBayesianNetwork(Net other, String newName, Environ newEnv, String options) throws NeticaException {
		super(other, newName, newEnv, options);
	}
	
	private MaverickBayesianNetwork() throws NeticaException {
		super();
	}
	
	@Override
	public void addListener(NeticaListener paramNeticaListener) {
		; // neutralize; original version is screwed up
	}
	
	@Override
	public void removeListener(NeticaListener paramNeticaListener) throws NeticaException {
		; // neutralize; original version is screwed up
	}

	@Override
	public void finalize() throws NeticaException {
		NETICA.DeleteNet_bn(netPtr);
		parentEnv.testForError();
	}

}

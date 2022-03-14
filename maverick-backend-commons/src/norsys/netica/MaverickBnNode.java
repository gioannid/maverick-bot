package norsys.netica;

public class MaverickBnNode extends Node {

	public MaverickBnNode() throws NeticaException {
		super();
	}

	public MaverickBnNode(String paramString, int paramInt, Net paramNet) throws NeticaException {
		super(paramString, paramInt, paramNet);
	}

	public MaverickBnNode(String paramString1, String paramString2, Net paramNet) throws NeticaException {
		super(paramString1, paramString2, paramNet);
	}
	
	@Override
	public void addListener(NeticaListener paramNeticaListener) {
		; // neutralize; original version is screwed up
	}

	@Override
	public void removeListener(NeticaListener paramNeticaListener) throws NeticaException {
		; // neutralize; original version is screwed up
	}

}

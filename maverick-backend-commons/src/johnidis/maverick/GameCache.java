package johnidis.maverick;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class GameCache<T> extends LinkedHashMap<Long,T> {
	
	private final int maxCachedEntries;

	private static final long serialVersionUID = -5653982698530042988L;
	
	public GameCache (int maxEntries) {
		maxCachedEntries = maxEntries;
	}
	

	@Override
	protected boolean removeEldestEntry(Entry<Long,T> eldest) {
		return size() > maxCachedEntries;
	}
	
}
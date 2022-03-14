package johnidis.utils.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LevenshteinMap<V> implements Map<String,V> {
	
	public static final int			CACHE_ENTRIES_PER_MAP_ENTRY					= 4;
	
	public static int 				MAXIMUM_DISTANCE_PERCENTAGE_FOR_EQUALITY 	= 30;
	
	private static final boolean 	DEBUG										= false;

	protected Map<String,V> map = new HashMap<String,V>();
	protected Map<String,String> cache = new LinkedHashMap<String,String>();
	

	static protected int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}
	
	static public int distance (String string, String other) {
		int[][] distance = new int[string.length() + 1][other.length() + 1];
		for (int i = 0; i <= string.length(); i++)
			distance[i][0] = i;
		for (int j = 1; j <= other.length(); j++)
			distance[0][j] = j;
		for (int i = 1; i <= string.length(); i++)
			for (int j = 1; j <= other.length(); j++)
				distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
						distance[i - 1][j - 1] + ((string.charAt(i - 1) == other.charAt(j - 1)) ? 0 : 1));
		if (DEBUG)
			System.out.println("Comparing "+string + " - "+other+" = "+distance[string.length()][other.length()]);
		return distance[string.length()][other.length()];
	}
	
	static public boolean equal (String string, String s) {
		return ((distance(string, s) * 100) / (Math.max(string.length(), s.length())) <=
			MAXIMUM_DISTANCE_PERCENTAGE_FOR_EQUALITY);
	}

	
	protected String keyBySimilar (String similarKey) {
		String mapkey = cache.get(similarKey);
		if (mapkey != null) {
			if (DEBUG)
				System.out.println("Found in cache: "+similarKey+"->"+mapkey);
			return mapkey;
		}
		for (String k : map.keySet())
			if (equal(similarKey, k)) {
				if (cache.size() >= map.size() * CACHE_ENTRIES_PER_MAP_ENTRY) {
					String first = cache.keySet().iterator().next();
					if (DEBUG)
						System.out.println("Dropping from cache: "+first);
					cache.remove(first);
				}
				if (DEBUG)
					System.out.println("Adding in cache: "+similarKey+"->"+k);
				cache.put(similarKey, k);
				return k;
			}
		return null;
	}
	

	@Override
	public void clear() {
		synchronized (map) {
			map.clear();
			cache.clear();
		}
	}

	@Override
	public boolean containsKey(Object key) {
		synchronized (map) {
			if (map.containsKey(key))
				return true;
			return (keyBySimilar ((String) key) != null);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		synchronized (map) {
			return map.containsValue(value);
		}
	}

	@Override
	public Set<java.util.Map.Entry<String, V>> entrySet() {
		synchronized (map) {
			return map.entrySet();
		}
	}

	@Override
	public V get(Object key) {
		synchronized (map) {
			V result = map.get(key);
			if (result != null)
				return result;
			return map.get(keyBySimilar ((String) key));
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized (map) {
			return map.isEmpty();
		}
	}

	@Override
	public Set<String> keySet() {
		synchronized (map) {
			return map.keySet();
		}
	}

	@Override
	public V put(String key, V value) {
		synchronized (map) {
			return map.put(key, value);
		}
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> m) {
		synchronized (map) {
			map.putAll(m);
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (map) {
			Iterator<String> iterator = cache.values().iterator();
			while (iterator.hasNext()) {
				Object sk = iterator.next();
				if (key.equals(sk)) {
					if (DEBUG)
						System.out.println("Removing from cache: "+sk);
					iterator.remove();
				}
			}
			return map.remove(key);
		}
	}

	@Override
	public int size() {
		synchronized (map) {
			return map.size();
		}
	}

	@Override
	public Collection<V> values() {
		synchronized (map) {
			return map.values();
		}
	}
	
	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder ("{");
		for (String k : map.keySet()) {
			sb.append("(").append(k).append(", ").append(map.get(k).toString()).append(") ");
		}
		sb.append("}");
		return sb.toString();
	}

}

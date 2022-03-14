package data;

import java.util.Vector;

/**
 *
 * Created on 21.05.2008
 * @author Kami II
 * Corrections/Modifications for Maverick
 */
public class ArrayTools {

	static public String deepToString (int[] array) {
		StringBuilder s = new StringBuilder ("( ");
		for (int e = 0; e < array.length; e++)
			s.append(array[e]).append(" ");
		s.append(')');
		return s.toString();
	}
	
	static public String deepToString (double[] array) {
		StringBuilder s = new StringBuilder ("( ");
		for (int e = 0; e < array.length; e++)
			s.append(array[e]).append(" ");
		s.append(')');
		return s.toString();
	}
	
	static public String deepToString (float[] array) {
		StringBuilder s = new StringBuilder ("( ");
		for (int e = 0; e < array.length; e++)
			s.append(array[e]).append(" ");
		s.append(')');
		return s.toString();
	}
	
	static public String deepToString (Object[] id, double[] array) {
		StringBuilder s = new StringBuilder ("( ");
		for (int e = 0; e < array.length; e++)
			s.append(id[e].toString()).append('=').append(array[e]).append(" ");
		s.append(')');
		return s.toString();
	}
	
	static public String deepToString (Object[] id, Object[] array) {
		StringBuilder s = new StringBuilder ("( ");
		for (int e = 0; e < array.length; e++)
			s.append(id[e].toString()).append('=').append(array[e].toString()).append(" ");
		s.append(')');
		return s.toString();
	}
	
	public static double[][] cloneArray(double[][] array) {
		if (array == null)
			return null;
		double[][] ca = array.clone();
		for (int x = 0; x < array.length; x++) {
			if (ca[x] != null)
				ca[x] = ca[x].clone();
		}
		return ca;
	}

	public static double[][][] cloneArray(double[][][] array) {
		if (array == null)
			return null;
		double[][][] ca = array.clone();
		for(int index = 0; index < ca.length; index++)
			ca[index] = cloneArray(ca[index]);
		return ca;
	}

	public static double[][][][] cloneArray(double[][][][] array) {
		if (array == null)
			return null;
		double[][][][] ca = array.clone();
		for(int index = 0; index < ca.length; index++)
			ca[index] = cloneArray(ca[index]);
		return ca;
	}

	public static Vector<double[]> cloneList1dDouble(Vector<double[]> list) {
		if (list == null)
			return null;
		Vector<double[]> ca = new Vector<double[]>();
		for (int x = 0; x < list.size(); x++) {
			double[] element = list.get(x);
			if (element != null)
				ca.add(element.clone());
		}
		return ca;
	}

	public static Vector<double[][]> cloneList2dDouble(Vector<double[][]> list) {
		if (list == null)
			return null;
		Vector<double[][]> ca = new Vector<double[][]>();
		for (int index = 0; index < list.size(); index++) {
			double[][] element = list.get(index);
			element = cloneArray(element);
			ca.add(element);
		}
		return ca;
	}

	public static Vector<double[][][]> cloneList3dDouble(Vector<double[][][]> list) {
		if (list == null)
			return null;
		Vector<double[][][]> ca = new Vector<double[][][]>();
		for (int index = 0; index < list.size(); index++) {
			double[][][] element = list.get(index);
			element = cloneArray(element);
			ca.add(element);
		}
		return ca;
	}
	
	public static int[][] cloneArray(int[][] array) {
		if (array == null)
			return null;
		int[][] ca = array.clone();
		for (int x = 0; x < array.length; x++) {
			if (ca[x] != null)
				ca[x] = ca[x].clone();
		}
		return ca;
	}

	public static int[][][] cloneArray(int[][][] array) {
		if (array == null)
			return null;
		int[][][] ca = array.clone();
		for(int index = 0; index < ca.length; index++)
			ca[index] = cloneArray(ca[index]);
		return ca;
	}

	public static int[][][][] cloneArray(int[][][][] array) {
		if (array == null)
			return null;
		int[][][][] ca = array.clone();
		for(int index = 0; index < ca.length; index++)
			ca[index] = cloneArray(ca[index]);
		return ca;
	}

	public static Vector<int[]> cloneList1dInt(Vector<int[]> list) {
		if (list == null)
			return null;
		Vector<int[]> ca = new Vector<int[]>();
		for (int x = 0; x < list.size(); x++) {
			int[] element = list.get(x);
			if (element != null)
				ca.add(element.clone());
		}
		return ca;
	}

	public static Vector<int[][]> cloneList2dInt(Vector<int[][]> list) {
		if (list == null)
			return null;
		Vector<int[][]> ca = new Vector<int[][]>();
		for (int index = 0; index < list.size(); index++) {
			int[][] element = list.get(index);
			element = cloneArray(element);
			ca.add(element);
		}
		return ca;
	}

	public static Vector<int[][][]> cloneList3dInt(Vector<int[][][]> list) {
		if (list == null)
			return null;
		Vector<int[][][]> ca = new Vector<int[][][]>();
		for (int index = 0; index < list.size(); index++) {
			int[][][] element = list.get(index);
			element = cloneArray(element);
			ca.add(element);
		}
		return ca;
	}
	
	public static String[] mergeAsStrings (Object[] a1, Object[] a2) {
		String[] allValues;
		if (a2 != null)
			allValues = new String[a1.length + a2.length];
		else
			allValues = new String[a1.length];
		int i = 0;
		for (Object data : a1)
			allValues[i++] = data.toString();
		if (a2 != null)
			for (Object data : a2)
				allValues[i++] = data.toString();
		return allValues;
	}
	
}

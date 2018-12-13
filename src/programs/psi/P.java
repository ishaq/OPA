interface MPCAnnotation {
	// represents a MUX node
	public int MUX(int a, int b, boolean cond);
	// is used to mark input variables
	public void IN(int x);
	// is used to mark output variables
	public void OUT(int x);
}

class MPCAnnotationImpl implements MPCAnnotation {
	private static MPCAnnotation v = null;

	private MPCAnnotationImpl() {

	}

	public int MUX(int a, int b, boolean cond) {
		return (cond ? a : b);
	}
	
	public void IN(int x) {
	}
	
	public void OUT(int x) {
	}

	public static MPCAnnotation v() {
		if (v == null) {
			v = new MPCAnnotationImpl();
		}
		return v;
	}
}

public class P {
	static final int SIZE1 = 10;
	static final int SIZE2 = 10;
	
	public static void init() {
		// TODO: fill in the sets
	}
	
	public static int contains(int[] haystack, int needle, int haystack_size) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int result = 0;
		for(int i = 0; i < haystack_size; i++) {
			boolean flag = (haystack[i] == needle);
			result = mpc.MUX(1, 0, flag);
		}
		return result;
	}

	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		
		int[] pset1 = new int[SIZE1];
		for(int i = 0; i < SIZE1; i++) {
			mpc.IN(pset1[i]);
		}
		int[] pset2 = new int[SIZE2];
		for(int i = 0; i < SIZE2; i++) {
			mpc.IN(pset2[i]);
		}
		
		int[] intersection = new int[SIZE1];
		int size = 0;
		for(int i = 0; i < SIZE1; i++) {
			int flag1 = contains(pset2, pset1[i], SIZE2);
			boolean flag2 = (flag1 == 1);
			int newsize = size + 1;
			int newvalue = pset1[i];
			int value = mpc.MUX(newvalue, intersection[size], flag2);
			size = mpc.MUX(newsize, size, flag2);
			intersection[size] = value;
		}
		
		for(int i = 0; i < SIZE1; i++) {
			mpc.OUT(intersection[i]);
		}
		mpc.OUT(size);
	}
	
	// manually inlined version
//	public static void main2(String[] args) {
//		int[] intersection = new int[SIZE1];
//		int size = 0;
//		for(int i = 0; i < SIZE1; i++) {
//			for(int j = 0; j < SIZE2; j ++) {
//				boolean flag = (pset2[j] == pset1[i]);
//				int newsize = size + 1;
//				int newvalue = pset1[i];
//				int value = MultiplexImpl.v().MUX(newvalue, intersection[size], flag);
//				size = MultiplexImpl.v().MUX(newsize, size, flag);
//				intersection[size] = value;
//			}
//		}
//	}
}
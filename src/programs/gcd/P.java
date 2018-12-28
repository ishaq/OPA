interface MPCAnnotation {
	// represents a MUX node
	public int MUX(int a, int b, boolean cond);

	// is used to mark output variables
	public void OUT(int x);
	
	// used to mark input variables (input vars should be assigned the return value)
	// only use this method if java stops compiling because variables are not initialized
	public int IN();
}

class MPCAnnotationImpl implements MPCAnnotation {
	private static MPCAnnotation v = null;

	private MPCAnnotationImpl() {

	}

	public int MUX(int a, int b, boolean cond) {
		return (cond ? a : b);
	}

	public void OUT(int x) {
	}

	public static MPCAnnotation v() {
		if (v == null) {
			v = new MPCAnnotationImpl();
		}
		return v;
	}
	
	public int IN() {
		return 100;
	}
}

public class P {
	static final int len = 32;

	public static int rem(int x, int y) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int rem = 0;
		for (int j = len-1; j >= 0; j--) {
			rem = rem << 1;
			// rem[0] = x[j] // note that we use >>> for unsigned shift right
			 rem = rem + ((x >>> j) & 1);
			 
//			 if (rem >= y) {
//					rem = rem - y;
//				}
			
			boolean geq = (rem >= y);
			int rem2 = rem - y;
			rem = mpc.MUX(rem2, rem, geq);
			
		}
		return rem;
	}

	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int a = mpc.IN();
		int b = mpc.IN();

		int rem = 0;
		for (int i = 0; i < len; i++) {
//    		if (b != 0) {
//    			int t = b;
//    			b = rem(a, b);
//    			a = t;
//    		}
			int temp = b;
			boolean neq = (b != 0);
			rem = rem(a, b);
			//rem = a % b;
			b = mpc.MUX(rem, b, neq);
			a = mpc.MUX(temp, a, neq);
		}

		mpc.OUT(rem);
		//System.out.println("GCD of " + a + ", " + b + " is " + rem);
	}
}
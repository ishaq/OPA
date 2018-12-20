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
	static final int len = 32;
	
//	public static int rem(int x, int y) {
//		int rem = 0;
//		for (int j = len-1; j >= 0; j--) {
//			rem = rem << 1;
//			// rem[0] = x[j] // note that we use >>> for unsigned shift right
//			 rem = rem + ((x >>> j) & 1);
//			
//			if (rem >= y) {
//				rem = rem - y;
//			}
//		}
//		return rem;
//	}
	
    public static void main(String[] args) {
    	MPCAnnotation mpc = MPCAnnotationImpl.v();
    	int a = 100;
    	int b = 60;
    	mpc.IN(a);
    	mpc.IN(b);
    	
    	int rem;
    	for(int i = 0; i < len; i++) {
//    		if (b != 0) {
//    			int t = b;
//    			b = rem(a, b);
//    			a = t;
//    		}
    		int temp = b;
    		boolean neq = (b != 0);
    		rem = a % b;
    		b = MultiplexImpl.v.MUX(rem, b, neq);
    		a = MultiplexImpl.v.MUX(temp,  a, neq);
    	}
    	
    	mpc.OUT(rem);
    }
}
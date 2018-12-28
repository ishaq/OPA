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
			
			boolean lt = (rem < y);
			int rem2 = rem - y;
			rem = mpc.MUX(rem, rem2, lt);
			
		}
		return rem;
	}
	public static int mul_mod(int mul1, int mul2, int mod) {
		int prod = mul1 * mul2;
		int rem = rem(prod, mod);
		return rem;
	}
    public static void main(String[] args) {
    	MPCAnnotation mpc = MPCAnnotationImpl.v();
    	int base = mpc.IN();
    	int exp = mpc.IN();
    	int mod = 10485760; // 2^20 * 10
    	int res = 1;
    	for(int i = len-1; i >= 0; i--) {
    		res = mul_mod(res, res, mod);
    		int cnd_mul = mul_mod(res, base, mod);
    		int bit_mask = 1 << i;
    		int int_flag = (exp & bit_mask);
    		boolean flag = int_flag != 0;
    		res = mpc.MUX(cnd_mul, res, flag);
    	}
    	mpc.OUT(res);
    }
}
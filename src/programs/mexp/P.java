interface MPCAnnotation {
	// is used to mark output variables
	public void OUT(int x);

	// used to mark input variables (input vars should be assigned the return value)
	// it's a convenient method to shutup the compiler when it complains that
	// variables
	// are not initialized
	public int IN();
}

class MPCAnnotationImpl implements MPCAnnotation {
	private static MPCAnnotation v = null;

	private MPCAnnotationImpl() {
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
		return 57; // Grothendieck Prime
	}
}

public class P {
	static final int len = 64;

	public static int rem(int x, int y) {
		int rem = 0;
		for (int j = len - 1; j >= 0; j--) {
			rem = rem << 1;
			// rem[0] = x[j] // note that we use >>> for unsigned shift right
			rem = rem + ((x >>> j) & 1);

			if (rem >= y) {
				rem = rem - y;
			}
//			
//			boolean lt = (rem < y);
//			int rem2 = rem - y;
//			rem = mpc.MUX(rem, rem2, lt);

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
		for (int i = len - 1; i >= 0; i--) {
			// res = mul_mod(res, res, mod);
			int prod1 = res * res;
			res = rem(prod1, mod);

			// int cnd_mul = mul_mod(res, base, mod);
			int prod2 = res * base;
			int cnd_mul = rem(prod2, mod);

			int bit_mask = 1 << i;
			int int_flag = (exp & bit_mask);
			if (int_flag != 0) {
				res = cnd_mul;
			}
//    		boolean flag = int_flag != 0;
//    		res = mpc.MUX(cnd_mul, res, flag);
		}
		mpc.OUT(res);
	}
}
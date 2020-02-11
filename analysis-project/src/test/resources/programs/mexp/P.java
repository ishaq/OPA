package programs.mexp;

import mixedProtocolsAnalysis.*;

public class P {
	static final int len = 32;

	public static int rem(int x, int y) {
		int rem = 0;
		for (int j = len - 1; j >= 0; j--) {
			rem = rem << 1;
			// rem[0] = x[j] // note that we use >>> for unsigned shift right
			rem = rem + ((x >>> j) & 1);

			if (rem >= y) {
				rem = rem - y;
			}
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
		}
		mpc.OUT(res);
	}
}
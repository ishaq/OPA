package programs.gcd;

import mixedProtocolsAnalysis.*;

public class P {
	static final int len = 32;

	public static int rem(int x, int y) {
		int rem = 0;
		for (int j = len - 1; j >= 0; j--) {
			rem = rem << 1;
			// rem[0] = x[j] // note that we use >>> for unsigned shift right
			rem = rem + ((x >>> j) & 1);

			int rem2 = rem - y;
			if (rem >= y) {
				rem = rem2;
			}
		}
		return rem;
	}

	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int a = mpc.IN();
		int b = mpc.IN();

		int gcd = 0;
		for (int i = 0; i < len; i++) {
			gcd = rem(a, b);
			int temp = b;
			if (b != 0) {
				b = gcd;
				a = temp;
			}
		}

		mpc.OUT(gcd);
		// System.out.println("GCD of " + a + ", " + b + " is " + rem);
	}
}
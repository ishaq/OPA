interface MPCAnnotation {
	// is used to mark output variables
	public void OUT(int x);
	
	// used to mark input variables (input vars should be assigned the return value)
	// it's a convenient method to shutup the compiler when it complains that variables 
	// are not initialized. it also helps in recognizing  which variables are 
	// input variables when one is looking at shimple code.
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
	static final int size = 100;
	static final int dim = 4;

//	public static void init(int[] C, int[][] S) {
//		// TODO: fill in C and S
//	}

	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		// -- INPUT PREPROCESSING START --
		// NONE OF THIS CODE NEEDS TO BE INSIDE MPC, CLIENT/SERVER EXECUTE IT LOCALLY
		/* secret array of which we are looking to find a match */
//		int[] C = new int[dim];
		int[][] S = new int[size][dim];
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < dim; j++) {
				S[i][j] = mpc.IN();
			}
		}
		

		// now that we have S and C, compute S_sqr and C_sqr, also compute 2*C
		int[] S_sqr = new int[size];
//		for (int i = 0; i < size; i++) {
//			int sqr_sum = 0;
//			for (int j = 0; i < dim; j++) {
//				int sqr = S[i][j] * S[i][j];
//				sqr_sum = sqr_sum + sqr;
//			}
//			S_sqr[i] = sqr_sum;
//		}
		for(int i = 0; i < size; i++) {
			S_sqr[i] = mpc.IN();
		}
		
		int C_sqr = 0;
//		for (int i = 0; i < dim; i++) {
//			int sqr = C[i] * C[i];
//			C_sqr = C_sqr + sqr;
//		}
		C_sqr = mpc.IN();
		
		int[] twoC = new int[dim];
//		for (int i = 0; i < dim; i++) {
//			twoC[i] = 2 * C[i];
//		}
		for(int i = 0; i < dim; i++) {
			twoC[i] = mpc.IN();
		}
		// -- INPUT PREPROCESSING END --

		// -- MPC PORTION --
		int[] D = new int[size];
		for (int i = 0; i < size; i++) {
			int asqr_plus_bsqr = S_sqr[i] + C_sqr;
			int twoab = 0;
			for (int j = 0; j < dim; j++) {
				int temp = S[i][j] * twoC[j];
				twoab = twoab + temp;
			}
			int diff = asqr_plus_bsqr - twoab;
			D[i] = diff;
		}

		int minDiff = D[0];
		int minIndex = 0;
		for (int k = 1; k < size; k++) {
			if(D[k] < minDiff) {
				minDiff = D[k];
				minIndex = k;
			}
//			boolean flag = D[k] < minDiff;
//			minDiff = mpc.MUX(D[k], minDiff, flag);
//			minIndex = mpc.MUX(k, minIndex, flag);
		}
		// -- MPC PORTION END --
		
		// -- OUTPUT ANNOTATION --
		mpc.OUT(minDiff);
		mpc.OUT(minIndex);
	}
}
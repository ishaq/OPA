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
		int[] C = new int[dim];
		int[][] S = new int[size][dim];
//		init(C, S);
		

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
			mpc.IN(S_sqr[i]);
		}
		
		int C_sqr = 0;
//		for (int i = 0; i < dim; i++) {
//			int sqr = C[i] * C[i];
//			C_sqr = C_sqr + sqr;
//		}
		mpc.IN(C_sqr);
		
		int[] twoC = new int[dim];
//		for (int i = 0; i < dim; i++) {
//			twoC[i] = 2 * C[i];
//		}
		for(int i = 0; i < dim; i++) {
			mpc.IN(twoC[i]);
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
			boolean flag = D[k] < minDiff;
			minDiff = mpc.MUX(D[k], minDiff, flag);
			minIndex = mpc.MUX(k, minIndex, flag);
		}
		// -- MPC PORTION END --
		
		// -- OUTPUT ANNOTATION --
		mpc.OUT(minDiff);
		mpc.OUT(minIndex);
	}
}
/**
 * playground, basically a test bed to test ideas
 * @author ishaq
 *
 */


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
	static final int len = 32;
	
	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int a = mpc.IN();
		int b = mpc.IN();
		int c = a;
		if(a > b) {
			c = b;
		}
		mpc.OUT(c);
	}


//	public static void main(String[] args) {
//		MPCAnnotation mpc = MPCAnnotationImpl.v();
//		int a = mpc.IN();
//		int b = mpc.IN();
//		
//		int c = a * b;
//		int flag = (c > 100) ? 1 : 0;
//		mpc.OUT(flag);
//		//System.out.println("GCD of " + a + ", " + b + " is " + rem);
//	}
}
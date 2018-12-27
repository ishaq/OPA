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


	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int a = mpc.IN();
		int b = mpc.IN();
		
		int c = a * b;
		int flag = (c > 100) ? 1 : 0;
		mpc.OUT(flag);
		//System.out.println("GCD of " + a + ", " + b + " is " + rem);
	}
}
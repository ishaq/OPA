interface MPCAnnotation {
	// represents a MUX node
	public int MUX(int a, int b, boolean cond);

	// is used to mark input variables
	public void IN(int x);

	// is used to mark output variables
	public void OUT(int x);
	
	// just to get rid of not initialized error
	public int FIX_NOT_INITIALIZED_ERROR();
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
	
	public int FIX_NOT_INITIALIZED_ERROR() {
		return 100;
	}
}

public class P {
	static final int len = 32;


	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int a = mpc.FIX_NOT_INITIALIZED_ERROR();
		int b = mpc.FIX_NOT_INITIALIZED_ERROR();
		mpc.IN(a);
		mpc.IN(b);
		
		int c = a * b;
		int flag = (c > 100) ? 1 : 0;
		mpc.OUT(flag);
		//System.out.println("GCD of " + a + ", " + b + " is " + rem);
	}
}
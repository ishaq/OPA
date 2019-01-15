interface MPCAnnotation {
	// is used to mark output variables
	public void OUT(int x);
	
	// used to mark input variables (input vars should be assigned the return value)
	// it's a convenient method to shutup the compiler when it complains that variables 
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
	
	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		// Secret bids;
		int a1 = mpc.IN();
		int b1 = mpc.IN();
		int a2 = mpc.IN();
		int b2 = mpc.IN();
		
		boolean r1 = a1 > b1;
		System.out.println("Winner of first round is " + r1);
		
		int sa = a1 + a2;
		int sb = b1 + b2;
		
		boolean r2 = sa > sb;

		System.out.println("Winner of auction is: " + r2);
	}
}
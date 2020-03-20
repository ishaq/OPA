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
	static final int SIZE1 = 32; // alice/client
	static final int SIZE2 = 1024; // bob/server
	
	public static int contains(int[] haystack, int needle, int haystack_size) {
		int result = 0;
		for(int i = 0; i < haystack_size; i++) {
			int old_result = result;
			if(haystack[i] == needle) {
				result = 1;
			}
			else {
				result = old_result;
			}
		}
		return result;
	}

	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		
		int[] pset1 = new int[SIZE1];
		for(int i = 0; i < SIZE1; i++) {
			pset1[i] = mpc.IN();
		}
		int[] pset2 = new int[SIZE2];
		for(int i = 0; i < SIZE2; i++) {
			pset2[i] = mpc.IN();
		}
		
		int[] intersection = new int[SIZE1];
		for(int i = 0; i < SIZE1; i++) {
			int result = contains(pset2, pset1[i], SIZE2);
			intersection[i] = result;
		}
		
		// alice learns the output
		for(int i = 0; i < SIZE1; i++) {
			mpc.OUT(intersection[i]);
		}
	}
}
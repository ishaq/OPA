interface Multiplex {
	public int MUX(int a, int b, boolean cond);
}

class MultiplexImpl implements Multiplex {
	public static Multiplex v = null;
	private MultiplexImpl() {
		
	}
	public int MUX(int a, int b, boolean cond) {
		return (cond ? a : b);
	}
	
	public static Multiplex v() {
		if(v == null) {
			v = new MultiplexImpl();
		}
		return v;
	}
}

public class P {
	static final int len = 32;
	
//	public static int rem(int x, int y) {
//		int rem = 0;
//		for (int j = len-1; j >= 0; j--) {
//			rem = rem << 1;
//			// rem[0] = x[j] // note that we use >>> for unsigned shift
//			 rem = rem + ((x >>> j) & 1);
//			
//			if (rem >= y) {
//				rem = rem - y;
//			}
//		}
//		return rem;
//	}
	
    public static void main(String[] args) {
    	int a = 100;
    	int b = 60;
    	int rem;
    	for(int i = 0; i < len; i++) {
//    		if (b != 0) {
//    			int t = b;
//    			b = rem(a, b);
//    			a = t;
//    		}
    		int temp = b;
    		boolean neq = (b != 0);
    		rem = a % b;
    		b = MultiplexImpl.v.MUX(rem, b, neq);
    		a = MultiplexImpl.v.MUX(temp,  a, neq);
    	}
    }
}
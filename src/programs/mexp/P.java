interface Multiplex {
	public int MUX(int a, int b, boolean cond);
}

class MultiplexImpl implements Multiplex {
	private static Multiplex v = null;
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
	public static int mul_mod(int mul1, int mul2, int mod) {
		int prod = mul1 * mul2;
		int rem = prod % mod;
		return rem;
	}
    public static void main(String[] args) {
    	int base = 32;
    	int exp = 3;
    	int mod = 10485760; // 2^20 * 10
    	int res = 1;
    	for(int i = len-1; i >= 0; i--) {
    		res = mul_mod(res, res, mod);
    		int cnd_mul = mul_mod(res, base, mod);
    		int bit_mask = 1 << i;
    		int int_flag = (exp & bit_mask);
    		boolean flag = int_flag != 0;
    		res = MultiplexImpl.v().MUX(cnd_mul, res, flag);
    	}

    }
}
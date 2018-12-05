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
	
	public static void main(String[] args) {
		P p = new P();
		// Secret bids;
		int a1 = 100;
		int b1 = 101;
		int a2 = 99;
		int b2 = 100;
		
		boolean r1 = a1 > b1;
		System.out.println("Winner of first round is " + r1);

		int bigger = MultiplexImpl.v.MUX(a1, b1, r1);
		System.out.println("Bigger Bid is: " + bigger);
		
		int sa = a1 + a2;
		int sb = b1 + b2;
		
		boolean r2 = sa > sb;

		System.out.println("Winner of auction is: " + r2);
	}
}
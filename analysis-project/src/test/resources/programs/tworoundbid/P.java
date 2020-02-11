package programs.tworoundbid;

import mixedProtocolsAnalysis.*;

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
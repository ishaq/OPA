package programs.playground;

import mixedProtocolsAnalysis.*;

public class P {
	static final int len = 32;
	
	public static void main(String[] args) {
		MPCAnnotation mpc = MPCAnnotationImpl.v();
		int[] array = new int[512];
		for(int i = 0; i < 512; i++) {
			array[i] = mpc.IN();
		}

		int[] array2 = new int[512];
		for(int i = 0; i < 512; i++) {
			array2[i] = array[i] * array[i];
			int x = array2[i];
			int nx = 0;
			if(x > 100) {
				nx = 1;
			}
			array2[i] = nx;
		}
		for(int i = 0; i < 512; i++) {
			mpc.OUT(array2[i]);
		}
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
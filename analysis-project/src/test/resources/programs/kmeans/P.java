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

	public static final int BIT_LEN = 32;

	public static final int D = 2; // Dimension (fix)
	public static final int NA = 100; // Number of data points from Party A
	public static final int NB = 100; // Number of data points from Party B
	public static final int NC = 5; // Number of clusters
	public static final int PRECISION = 4;

	public static final int LEN = (NA+NB);
	public static final int LEN_OUTER = 10;
	public static final int LEN_INNER = (LEN/LEN_OUTER);


	/* returns val/mod, integer division */
	public static int quot(int val, int mod) {
		int quot = 0;
		int rem = 0;
		for (int j = BIT_LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE

			int newrem = rem - mod;
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		return quot;
	}

	public static int dist2(int x1, int y1, int x2, int y2) {
		// TODO: should I check whether x1 is bigger than x2 and switch computation?
		// currently I am not implementing any of such checks because 1) currently want to 
		// translate HyCC code as fast as possible. 2) this is an implementation detail
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	public static void iteration(int[] data, int[] cluster, int[] OUTPUT_cluster, 
			int len, int num_cluster) {
		int i, c;	
		int[] new_cluster = new int[NC*D];
		int[] bestMap = new int[len];

		// Compute nearest clusters for Data item i
		// ToDo Min tree
		for(i = 0; i < len; i++) {
			bestMap[i] = 0;
			int dx = data[i*D];
			int dy = data[i*D+1];
//			int best_dist = dist2(cluster[0], cluster[1], dx, dy);
			int xdiff = cluster[0] - dx;
			int ydiff = cluster[1] - dy;
			int best_dist = xdiff * xdiff + ydiff * ydiff;
			for(c = 1; c < num_cluster; c++) {
//				int dist = dist2(cluster[D*c], cluster[D*c+1], dx, dy);
				xdiff = cluster[D*c] - dx;
				ydiff = cluster[D*c+1] - dy;
				int dist = xdiff * xdiff + ydiff * ydiff;
				int newBestDist;
				int newC;
				if(dist < best_dist) {
					newBestDist = dist;
					newC = c;
				}
				else {
					newBestDist = best_dist;
					newC = bestMap[i];
				}
				best_dist = newBestDist;
				bestMap[i] = newC;
			}
		}
		// Recompute cluster Pos

		int[] count = new int[num_cluster];
		for(c = 0; c < num_cluster; c++) {
			new_cluster[c*D] = 0;
			new_cluster[c*D+1] = 0;
			count[c] = 0;
		}

		for(i = 0; i < len; i++) {
			int c2 = bestMap[i];
			new_cluster[c2*D] += data[i*D];
			new_cluster[c2*D+1] += data[i*D+1];
			count[c2] = count[c2] + 1;
		}
		for(c = 0; c < num_cluster; c++) {
			int div;
			//		  if(count[c] >0 ) {
			//			new_cluster[c*D] /= count[c];
			//			new_cluster[c*D+1] /= count[c];
			//		  }
			if(count[c] > 0) {
				div = count[c];
			}
			else {
				div = 1;
			}

			//			new_cluster[c*D] /= div;
			int val = new_cluster[c*D];
			int mod = div;
			// manually inlined quot start
			int quot = 0;
			int rem = 0;
			for (int j = BIT_LEN - 1; j >= 0; j--) {
				// rem <<= 1
				rem = rem << 1;
				// rem[0] = val[j]
				rem = rem + ((val >> j) & 1); // EMPHASIS HERE

				int newrem = rem - mod;
				// quot[j] = 1;
				int newquot = (quot | (1 << j));
				if(rem >= mod) {
					rem = newrem;
					quot = newquot;
				}
			}
			int retVal = quot;
			// manually inlined quot end
			new_cluster[c*D] = retVal;


			//			new_cluster[c*D+1] /= div;
			val = new_cluster[c*D+1];
			mod = div;
			// manually inlined quot start
			quot = 0;
			rem = 0;
			for (int j = BIT_LEN - 1; j >= 0; j--) {
				// rem <<= 1
				rem = rem << 1;
				// rem[0] = val[j]
				rem = rem + ((val >> j) & 1); // EMPHASIS HERE

				int newrem = rem - mod;
				// quot[j] = 1;
				int newquot = (quot | (1 << j));
				if(rem >= mod) {
					rem = newrem;
					quot = newquot;
				}
			}
			retVal = quot;
			// manually inlined quot end
			new_cluster[c*D+1] = quot;

		}
		for(i = 0; i < num_cluster*D;i++) {
			OUTPUT_cluster[i] = new_cluster[i];
		}
	}

	public static void kmeans(int[] data, int[] OUTPUT_res) {
		int c, p;
		int[] cluster = new int[NC*D];

		// Assign random start cluster from data
		for(c = 0; c < NC; c++) {
			//			cluster[c*D] = data[((c+3)%LEN)*D];
			//			cluster[c*D+1] = data[((c+3)%LEN)*D+1];
			int offset = (c+3) % LEN;
			int index = offset*D;
			cluster[c*D] = data[index];
			cluster[c*D+1] = data[index+1];

		}

		for (p = 0; p < PRECISION; p++) { 
			int[] new_cluster = new int[NC*D];
			//			iteration_unrolled_outer(data, cluster, new_cluster);
			iteration(data, cluster, new_cluster, LEN, NC);

			// We need to copy inputs to outputs
			for( c = 0; c < NC*D; c++) {
				cluster[c] = new_cluster[c];
			}
		}
		for(c = 0; c < NC; c++) {  
			OUTPUT_res[c*D] = cluster[c*D];
			OUTPUT_res[c*D+1] = cluster[c*D+1];
		}
	}



	public static void main(String[] args) {
		int[] inputA_dataA = new int[D*NA];
		int[] inputB_dataB = new int[D*NA];
		int[] output_cluster = new int[D*NC];

		MPCAnnotation mpc = MPCAnnotationImpl.v();
		for(int i = 0; i < D*NA; i++) {
			inputA_dataA[i] = mpc.IN();
		}
		for(int i = 0; i < D*NA; i++) {
			inputB_dataB[i] = mpc.IN();
		}

		// implementation here
		int[] data = new int[LEN*D];
		for(int i = 0; i < NA*D; i++) {
			data[i] = inputA_dataA[i];
		}
		int offset = NA*D;
		for(int i = 0; i < NB*D; i++) {
			data[i+offset] = inputB_dataB[i];
		}
//		kmeans(data, output_cluster);
		// manually inlined kmeans starts here
		int c, p;
		int[] cluster = new int[NC*D];

		// Assign random start cluster from data
		for(c = 0; c < NC; c++) {
			// cluster[c*D] = data[((c+3)%LEN)*D];
			// cluster[c*D+1] = data[((c+3)%LEN)*D+1];
			int index_multiplier = (c+3) % LEN;
			int index = index_multiplier*D;
			cluster[c*D] = data[index];
			cluster[c*D+1] = data[index+1];

		}

		for (p = 0; p < PRECISION; p++) { 
			int[] new_cluster = new int[NC*D];
			iteration(data, cluster, new_cluster, LEN, NC);

			// We need to copy inputs to outputs
			for( c = 0; c < NC*D; c++) {
				cluster[c] = new_cluster[c];
			}
		}
		for(c = 0; c < NC; c++) {  
			output_cluster[c*D] = cluster[c*D];
			output_cluster[c*D+1] = cluster[c*D+1];
		}
		// manually inlined kmeans ends here

		for(int i = 0; i < D*NC; i++) {
			mpc.OUT(output_cluster[i]);
		}
	}
}
